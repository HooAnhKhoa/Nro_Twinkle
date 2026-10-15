package nro.models.server;

import java.lang.management.ManagementFactory;
import java.sql.*;
import java.nio.file.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.consts.AppearType;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.network.SessionManager;
import nro.models.utils.Logger;

/** Database bridge. Boss state transitions remain on each boss's update thread. */
public final class PanelBridge {
    private static final Map<String, CopyOnWriteArrayList<WeakReference<Boss>>> bosses = new ConcurrentHashMap<>();
    private static volatile Map<String, BossConfig> bossConfig = Collections.emptyMap();
    private static volatile Map<Integer, Double> skillRates = Collections.emptyMap();
    private static volatile double[] upgrades = {80,50,20,10,7,5,1,.3};
    private static volatile double[] stars = {50,20,10,5,1,.7,.5,.1,.1};
    public static volatile double dropMultiplier = 1;
    private static volatile Map<String, DropConfig> dropRates = Collections.emptyMap();
    private static volatile Map<String, DropRule> dropRules = Collections.emptyMap();
    private static final class DropRule {final String category;final double base,value;DropRule(String category,double base,double value){this.category=category;this.base=base;this.value=value;}}
    private static final class DropConfig {String mode;double value;DropConfig(String mode,double value){this.mode=mode;this.value=value;}}
    private static final Map<String, Long> spawnRequests = new ConcurrentHashMap<>();
    private static final Queue<CommandResult> results = new ConcurrentLinkedQueue<>();
    private static final class CommandResult { final long id;final String text;CommandResult(long id,String text){this.id=id;this.text=text;} }
    private static final class PanelCommand {long id,playerId;String action;boolean stale;}
    private static final class BossConfig {int hp,damage,rest;boolean enabled;}
    private static String key(Boss b) {return b.getClass().getName();}
    public static void register(Boss b) {bosses.computeIfAbsent(key(b),k->new CopyOnWriteArrayList<>()).add(new WeakReference<>(b));}
    public static double upgrade(int level,double original) {return level>=0&&level<upgrades.length?upgrades[level]:original;}
    public static double star(int level,double original) {return level>=0&&level<stars.length?stars[level]:original;}
    public static boolean rollDrop(double numerator,double denominator) {return denominator>0 && ThreadLocalRandom.current().nextDouble()*denominator<Math.min(denominator,Math.max(0,numerator*dropMultiplier));}
    public static boolean dropCategoryEnabled(String category) {
        if("hung_vuong".equals(category))return nro.models.event.EventManager.enabled("hung_vuong")&&nro.models.event.EventManager.enabled("mob_rewards");
        if("summer".equals(category))return nro.models.event.EventManager.enabled("summer")&&nro.models.event.EventManager.enabled("mob_rewards");
        return !"event".equals(category)||nro.models.event.EventManager.enabled("mob_rewards");
    }
    public static double dropChance(String category,double numerator,double denominator) {
        if(denominator<=0 || !dropCategoryEnabled(category))return 0;
        DropConfig config=dropRates.get(category);
        double chance=config!=null&&"percent".equals(config.mode)?config.value:100*numerator/denominator*dropMultiplier*(config==null?1:config.value);
        return Math.min(100,Math.max(0,chance));
    }
    public static boolean rollDrop(String category,double numerator,double denominator) {return ThreadLocalRandom.current().nextDouble()*100<dropChance(category,numerator,denominator);}
    public static double dropChance(String category,String ruleKey,double numerator,double denominator) {
        if(denominator<=0 || !dropCategoryEnabled(category))return 0;
        DropConfig group=dropRates.get(category);
        // Explicit zero still disables the category or the whole random-drop system.
        if(dropMultiplier<=0 || (group!=null && group.value<=0))return 0;
        DropRule rule=dropRules.get(ruleKey);
        if(rule==null || !category.equals(rule.category) || rule.base<=0)return dropChance(category,numerator,denominator);
        // A regional override replaces the multipliers, retaining the branch's existing bonus ratio.
        return Math.min(100,Math.max(0,rule.value*(100*numerator/denominator)/rule.base));
    }
    public static boolean rollDrop(String category,String ruleKey,double numerator,double denominator) {return ThreadLocalRandom.current().nextDouble()*100<dropChance(category,ruleKey,numerator,denominator);}
    public static long skillDamage(Player p,long original) {
        if(p==null||!p.isPl()||p.playerSkill==null||p.playerSkill.skillSelect==null)return original;
        if(nro.models.event.trungthu2026.TrungThuStore.carrot(p))original=original*(100-nro.models.event.trungthu2026.TrungThuConfig.number("buff.carrot-damage-reduction-percent"))/100;
        return (long)Math.min(Integer.MAX_VALUE,Math.max(0,original*skillRates.getOrDefault((int)p.playerSkill.skillSelect.template.id,1d)));
    }
    public static boolean canSpawn(Boss b) {return b.panelCanSpawn() && b.data[0].getTypeAppear()==AppearType.DEFAULT_APPEAR && b.data[0].getMapJoin()!=null && b.data[0].getMapJoin().length>0;}
    public static boolean enabled(Boss b) {BossConfig c=bossConfig.get(key(b));return nro.models.event.EventManager.bossEnabled(b)&&(c==null||c.enabled);}
    public static void configure(Boss b) {
        BossConfig c=bossConfig.get(key(b));
        if(c!=null){BossData d=b.data[0];d.setHp(new int[]{c.hp});d.setDame(c.damage);d.setSecondsRest(c.rest);b.panelSetRest(c.rest);}
    }
    public static void applySpawnStats(Boss b) {
        BossConfig c=bossConfig.get(key(b));
        if(c!=null && b.currentLevel==0 && !b.panelStatsApplied){
            b.nPoint.hpg=c.hp;b.nPoint.hpMax=c.hp;b.nPoint.hp=c.hp;
            b.nPoint.dameg=c.damage;b.nPoint.dame=c.damage;b.panelStatsApplied=true;
        }
    }
    public static void tickBoss(Boss b) {
        configure(b);
        Long request=spawnRequests.get(key(b));
        if(request!=null&&canSpawn(b)&&enabled(b)) {
            if(b.zone!=null) {if(spawnRequests.remove(key(b),request)) results.add(new CommandResult(request,"Boss đã xuất hiện trên bản đồ."));}
            else if(spawnRequests.remove(key(b),request)){b.panelSpawn();results.add(new CommandResult(request,"Đã đánh thức boss; chờ chu kỳ vào bản đồ."));}
        }
    }
    public static void start() {
        try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()) {
            for(String file:new String[]{"panel_schema.sql","boss_web.sql","server_rates.sql","drop_rates.sql","drop_rules.sql","intrinsic_schema.sql","event_schema.sql"}) {
                for(String sql:Files.readString(Path.of("web",file)).split(";"))if(!sql.trim().isEmpty())s.execute(sql);
            }
            nro.models.event.EventManager.reload(c);
            double[] defaults={50,20,10,5,1,.7,.5,.1,.1};
            for(int i=1;i<=9;i++)try(ResultSet columns=s.executeQuery("SHOW COLUMNS FROM server_rates LIKE 'star_"+i+"'")) {
                if(!columns.next())try(Statement migration=c.createStatement()){migration.execute("ALTER TABLE server_rates ADD star_"+i+" DECIMAL(6,3) NOT NULL DEFAULT "+defaults[i-1]);}
            }
            try(ResultSet columns=s.executeQuery("SHOW COLUMNS FROM boss_web_config LIKE 'head_id'")) {
                if(!columns.next())try(Statement migration=c.createStatement()){migration.execute("ALTER TABLE boss_web_config ADD head_id INT NOT NULL DEFAULT 0");}
            }
            try(ResultSet columns=s.executeQuery("SHOW COLUMNS FROM boss_web_config LIKE 'current_locations'")) {
                if(!columns.next())try(Statement migration=c.createStatement()){migration.execute("ALTER TABLE boss_web_config ADD current_locations TEXT NULL");}
            }
        }catch(Exception e){Logger.logException(PanelBridge.class,e);return;}
        Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"PanelBridge");t.setDaemon(true);return t;}).scheduleWithFixedDelay(PanelBridge::poll,0,3,TimeUnit.SECONDS);
    }
    private static void poll() {
        List<PanelCommand> commands=new ArrayList<>();
        try(Connection c=LocalManager.getConnection()) {
            Runtime rt=Runtime.getRuntime();Double cpu=null;
            java.lang.management.OperatingSystemMXBean os=ManagementFactory.getOperatingSystemMXBean();
            if(os instanceof com.sun.management.OperatingSystemMXBean){double load=((com.sun.management.OperatingSystemMXBean)os).getProcessCpuLoad();if(load>=0)cpu=load*100;}
            try(PreparedStatement s=c.prepareStatement("UPDATE panel_runtime SET heartbeat=NOW(),online_players=?,sessions=?,threads=?,ram_used=?,ram_max=?,cpu=?,maintenance=? WHERE id=1")){
                s.setInt(1,Client.gI().getPlayers().size());s.setInt(2,SessionManager.gI().getSessions().size());s.setInt(3,ManagementFactory.getThreadMXBean().getThreadCount());s.setLong(4,rt.totalMemory()-rt.freeMemory());s.setLong(5,rt.maxMemory());if(cpu==null)s.setNull(6,Types.DOUBLE);else s.setDouble(6,cpu);s.setInt(7,Maintenance.isRunning?1:0);s.executeUpdate();
            }
            long rateRevision=0;
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM server_rates WHERE id=1")) {
                if(r.next()){rateRevision=r.getLong("revision");Manager.RATE_EXP_SERVER=(byte)r.getInt("exp_rate");dropMultiplier=r.getDouble("drop_multiplier");double[] next=new double[8];for(int i=0;i<8;i++)next[i]=r.getDouble("upgrade_"+(i+1));upgrades=next;double[] nextStars=new double[9];for(int i=0;i<9;i++)nextStars[i]=r.getDouble("star_"+(i+1));stars=nextStars;}
            }
            Map<String,DropConfig> nextDrops=new HashMap<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT category,mode,value FROM panel_drop_rate")){while(r.next())nextDrops.put(r.getString(1),new DropConfig(r.getString(2),r.getDouble(3)));}dropRates=nextDrops;
            Map<String,DropRule> nextRules=new HashMap<>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT rule_key,category,base_percent,percent_override FROM panel_drop_rule WHERE percent_override IS NOT NULL")) {
                while(r.next())nextRules.put(r.getString(1),new DropRule(r.getString(2),r.getDouble(3),r.getDouble(4)));
            }
            dropRules=nextRules;
            try(PreparedStatement s=c.prepareStatement("UPDATE panel_drop_runtime SET heartbeat=NOW(),applied_revision=? WHERE id=1")){s.setLong(1,rateRevision);s.executeUpdate();}
            try(PreparedStatement s=c.prepareStatement("UPDATE server_rates SET heartbeat=NOW(),applied_revision=? WHERE id=1")){s.setLong(1,rateRevision);s.executeUpdate();}
            Map<Integer,Double> nextSkills=new HashMap<>();try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM panel_skill_rate")){while(r.next())nextSkills.put(r.getInt(1),r.getDouble(2));}skillRates=nextSkills;
            try{nro.models.services.IntrinsicService.gI().reloadCatalog(c);}catch(SQLException error){Logger.logException(PanelBridge.class,error);}
            try{nro.models.event.EventManager.reload(c);}catch(SQLException error){Logger.logException(PanelBridge.class,error);}
            publishBosses(c);Map<String,BossConfig> next=new HashMap<>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM boss_web_config WHERE customized=1")){while(r.next()){BossConfig cfg=new BossConfig();cfg.hp=r.getInt("hp");cfg.damage=r.getInt("damage");cfg.rest=r.getInt("rest_seconds");cfg.enabled=r.getBoolean("enabled");next.put(r.getString("boss_key"),cfg);}}bossConfig=next;
            Set<Long> activeRequests=new HashSet<>();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT id,boss_key FROM boss_web_command WHERE completed_at IS NULL AND requested_at>=NOW()-INTERVAL 30 SECOND ORDER BY id LIMIT 20")){while(r.next()){activeRequests.add(r.getLong(1));spawnRequests.putIfAbsent(r.getString(2),r.getLong(1));}}
            spawnRequests.entrySet().removeIf(e->!activeRequests.contains(e.getValue()));
            CommandResult result;while((result=results.poll())!=null)try(PreparedStatement s=c.prepareStatement("UPDATE boss_web_command SET completed_at=NOW(),result=? WHERE id=? AND completed_at IS NULL")){s.setString(1,result.text);s.setLong(2,result.id);s.executeUpdate();}
            try(Statement s=c.createStatement()){s.executeUpdate("UPDATE boss_web_command SET completed_at=NOW(),result='Lệnh hết hạn; boss chưa thể xuất hiện.' WHERE completed_at IS NULL AND requested_at<NOW()-INTERVAL 30 SECOND");}
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM panel_command WHERE completed_at IS NULL ORDER BY id LIMIT 20")){
                while(r.next()){
                    PanelCommand command=new PanelCommand();command.id=r.getLong("id");command.playerId=r.getLong("player_id");command.action=r.getString("action");command.stale=r.getTimestamp("requested_at").getTime()<System.currentTimeMillis()-30000;commands.add(command);
                }
            }
        }catch(Exception e){Logger.logException(PanelBridge.class,e);}
        // Release the bridge connection before kick saves players, including pools of size one.
        for(PanelCommand command:commands)try {
            String message;
            if(command.stale)message="Lệnh hết hạn; không thực hiện.";
            else if("kick".equals(command.action)){Player p=Client.gI().getPlayer(command.playerId);if(p==null)message="Nhân vật đã offline.";else{Client.gI().kickSession(p.getSession());message="Đã kick nhân vật theo quy trình lưu / thoát của server.";}}
            else if("maintenance".equals(command.action)){Maintenance.gI().startSeconds(15);message="Đã bắt đầu bảo trì sau 15 giây.";}
            else message="Lệnh không hợp lệ.";
            try(Connection c=LocalManager.getConnection();PreparedStatement done=c.prepareStatement("UPDATE panel_command SET completed_at=NOW(),result=? WHERE id=?")){done.setString(1,message);done.setLong(2,command.id);done.executeUpdate();}
        }catch(Exception e){Logger.logException(PanelBridge.class,e);}
    }
    private static void publishBosses(Connection c)throws SQLException {
        for(Map.Entry<String,CopyOnWriteArrayList<WeakReference<Boss>>> entry:bosses.entrySet()){
            Boss first=null;int online=0;Map<String,Map<String,Object>> locations=new LinkedHashMap<>();
            for(WeakReference<Boss> ref:entry.getValue()){
                Boss b=ref.get();if(b==null){entry.getValue().remove(ref);continue;}if(first==null)first=b;
                nro.models.map.Zone zone=b.zone;
                if(zone!=null&&zone.map!=null){online++;String position=zone.map.mapId+":"+zone.zoneId;
                    Map<String,Object> location=locations.get(position);
                    if(location==null){location=new LinkedHashMap<>();location.put("map_id",zone.map.mapId);location.put("map_name",zone.map.mapName);location.put("zone_id",zone.zoneId);location.put("count",0);locations.put(position,location);}
                    location.put("count",((Integer)location.get("count"))+1);
                }
            }if(first==null)continue;
            BossData d=first.data[0];
            try(PreparedStatement s=c.prepareStatement("INSERT INTO boss_web_config(boss_key,name,hp,damage,rest_seconds,map_ids,can_spawn,head_id,online_count,current_locations,heartbeat) VALUES(?,?,?,?,?,?,?,?,?,?,NOW()) ON DUPLICATE KEY UPDATE online_count=VALUES(online_count),current_locations=VALUES(current_locations),can_spawn=VALUES(can_spawn),head_id=VALUES(head_id),rest_seconds=IF(customized=0,VALUES(rest_seconds),rest_seconds),heartbeat=NOW(),applied_at=NOW()")){
                s.setString(1,entry.getKey());s.setString(2,d.getName());s.setInt(3,d.getHp()[0]);s.setInt(4,Math.max(0,d.getDame()));s.setInt(5,Math.max(0,d.getSecondsRest()));s.setString(6,Arrays.toString(d.getMapJoin()));s.setInt(7,canSpawn(first)?1:0);s.setInt(8,d.getOutfit()[0]);s.setInt(9,online);s.setString(10,new com.google.gson.Gson().toJson(locations.values()));s.executeUpdate();
            }
        }
    }
}
