package nro.models.boss;

import java.lang.ref.WeakReference;
import java.sql.*;import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.*;
import nro.models.data.LocalManager;
import nro.models.map.Zone;import nro.models.map.ItemMap;
import nro.models.player.*;
import nro.models.services.Service;
import nro.models.server.PanelBridge;
import nro.models.utils.Logger;

/** Kill-only bounty. Cash is idempotent in the database; ground items live for the map's normal lifetime. */
public final class BossBounty {
    public record Tier(int level,int vnd,int barsMin,int barsMax,double four,double three,double two){}
    public record Loot(int id,int quantity){}
    private record Grant(String key,int account,long player,String bossKey,String name,int map,int area,long hp,long damage,Tier tier,List<Loot> loot,int gems,Zone zone,int x,int y,WeakReference<Player> owner){}
    private static final List<Tier> TIERS=List.of(
        new Tier(1,1000,1,2,8,1,0),new Tier(2,2000,2,3,10,3,.5),
        new Tier(3,3000,3,5,12,6,1),new Tier(4,4000,5,8,15,10,3),new Tier(5,5000,8,12,20,15,5));
    private static final Set<String> PACKAGES=Set.of("Nappa","tieu_doi_sat_thu","tieu_doi_sat_thu_namek","Android","Frieza","Cell","trai_dat","Cold","Black_Goku","cumber","Baby","Broly","MajinBuu_12h","MajinBuu_14h","ma_vuong_picolo","Tau_PayPay","doanh_trai","ban_do_kho_bau","khi_gas","event_hung_vuong","event_tet");
    private static final ThreadLocal<Grant> SCOPE=new ThreadLocal<>();
    private static final ConcurrentMap<String,Grant> PENDING=new ConcurrentHashMap<>();
    private static final Gson JSON=new Gson();
    private static boolean started;
    private record Saved(String key,int account,long player,String bossKey,String name,int map,int area,long hp,long damage,Tier tier,List<Loot> loot,int gems,int x,int y){}
    private static final Path JOURNAL=Path.of("data/boss-bounty-pending");
    private static Path journalPath(String key){return JOURNAL.resolve(UUID.fromString(key).toString()+".json");}
    private static void journal(Grant g){
        try{
            Files.createDirectories(JOURNAL);Path path=journalPath(g.key);if(Files.exists(path))return;
            Saved data=new Saved(g.key,g.account,g.player,g.bossKey,g.name,g.map,g.area,g.hp,g.damage,g.tier,g.loot,g.gems,g.x,g.y);
            Path temp=JOURNAL.resolve(g.key+".tmp");Files.writeString(temp,JSON.toJson(data));Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE);
        }catch(Exception error){Logger.logException(BossBounty.class,error);}
    }
    // Gson 2.8.2 predates Java records: decode explicitly instead of reflective record construction.
    private static Saved decode(String text){
        JsonObject j=new JsonParser().parse(text).getAsJsonObject(),t=j.getAsJsonObject("tier");List<Loot> loot=new ArrayList<>();
        for(JsonElement value:j.getAsJsonArray("loot")){JsonObject row=value.getAsJsonObject();loot.add(new Loot(row.get("id").getAsInt(),row.get("quantity").getAsInt()));}
        Tier tier=new Tier(t.get("level").getAsInt(),t.get("vnd").getAsInt(),t.get("barsMin").getAsInt(),t.get("barsMax").getAsInt(),t.get("four").getAsDouble(),t.get("three").getAsDouble(),t.get("two").getAsDouble());
        return new Saved(j.get("key").getAsString(),j.get("account").getAsInt(),j.get("player").getAsLong(),j.get("bossKey").getAsString(),j.get("name").getAsString(),j.get("map").getAsInt(),j.get("area").getAsInt(),j.get("hp").getAsLong(),j.get("damage").getAsLong(),tier,List.copyOf(loot),j.has("gems")?j.get("gems").getAsInt():0,j.get("x").getAsInt(),j.get("y").getAsInt());
    }
    private static void clearJournal(String key){try{Files.deleteIfExists(journalPath(key));}catch(Exception error){Logger.logException(BossBounty.class,error);}}
    public static void recoverPending(){
        if(!Files.isDirectory(JOURNAL))return;
        try(var files=Files.list(JOURNAL)){
            for(Path file:files.filter(p->p.getFileName().toString().endsWith(".json")).toList())try{
                Saved s=decode(Files.readString(file));
                if(!file.getFileName().toString().equals(UUID.fromString(s.key).toString()+".json")||s.account<=0||s.player<=0||!combat(s.bossKey)||s.tier==null||!TIERS.contains(s.tier)||s.loot==null||s.loot.size()>2||s.gems<0||s.gems>100)throw new IllegalArgumentException("Invalid pending bounty");
                Set<Integer> ids=new HashSet<>();
                for(Loot row:s.loot){if(!ids.add(row.id))throw new IllegalArgumentException("Duplicate pending loot");
                    if(row.id==457){if(row.quantity<s.tier.barsMin||row.quantity>s.tier.barsMax)throw new IllegalArgumentException("Invalid pending bars");}
                    else if(row.id<15||row.id>17||row.quantity!=1)throw new IllegalArgumentException("Invalid pending ball");
                }
                if(s.loot.stream().filter(row->row.id>=15&&row.id<=17).count()>1)throw new IllegalArgumentException("Multiple pending balls");
                Zone zone=null;for(nro.models.map.Map map:nro.models.server.Manager.MAPS)if(map.mapId==s.map)for(Zone candidate:map.zones)if(candidate.zoneId==s.area)zone=candidate;
                Grant grant=new Grant(s.key,s.account,s.player,s.bossKey,s.name,s.map,s.area,s.hp,s.damage,s.tier,List.copyOf(s.loot),s.gems,zone,s.x,s.y,new WeakReference<>(nro.models.server.Client.gI().getPlayer(s.player)));PENDING.putIfAbsent(grant.key,grant);
            }catch(Exception error){Logger.logException(BossBounty.class,error);}
        }catch(Exception error){Logger.logException(BossBounty.class,error);}
    }
    public static boolean combat(String key){
        String prefix="nro.models.boss.";if(!key.startsWith(prefix))return false;
        String name=key.substring(prefix.length());int dot=name.lastIndexOf('.');String pack=dot<0?"":name.substring(0,dot);
        return PACKAGES.contains(pack)||name.equals("Golden_fireza.GoldenFrieza")||name.equals("Boss_mini.Virut")||name.equals("Boss_mini.RongNhi")||pack.equals("event.Halloween")||name.startsWith("event_trung_thu.MoonBoss$");
    }
    public static Tier tier(long hp,long damage,int map){
        int h=hp<10000000?1:hp<50000000?2:hp<200000000?3:hp<1000000000?4:5;
        int d=damage<10000?1:damage<50000?2:damage<200000?3:damage<500000?4:5;
        int m=map==155?5:map>=105&&map<=110||map>=114&&map<=120||map==127||map==128?4:
            map>=92&&map<=94||map>=96&&map<=100||map==102||map==103?3:map>=63&&map<=83?2:1;
        int level=h==5?5:Math.max(1,Math.min(5,(h*3+d+m+2)/5));return TIERS.get(level-1);
    }
    public static double barsPercent(Tier tier){return tier.level()*5d;}
    public static int rollGems(Random random){return 1+random.nextInt(100);}
    public static List<Loot> roll(Tier tier,Random random){
        List<Loot> loot=new ArrayList<>();if(random.nextDouble()*100<barsPercent(tier))loot.add(new Loot(457,tier.barsMin()+random.nextInt(tier.barsMax()-tier.barsMin()+1)));
        // A single mutually exclusive draw: at most one 2/3/4-star ball per kill.
        double draw=random.nextDouble()*100;
        if(draw<tier.two())loot.add(new Loot(15,1));else if(draw<tier.two()+tier.three())loot.add(new Loot(16,1));else if(draw<tier.two()+tier.three()+tier.four())loot.add(new Loot(17,1));
        return List.copyOf(loot);
    }
    public static Map<String,Object> preview(Boss b){
        if(!combat(b.getClass().getName()))return Map.of("eligible",false);
        int index=Math.max(0,Math.min(b.currentLevel,b.data.length-1));BossData data=b.data[index];
        long hp=b.zone!=null?b.nPoint.hpMax:data.getHp()[0],damage=b.zone!=null?b.nPoint.dame:data.getDame();
        int map=b.zone!=null?b.zone.map.mapId:data.getMapJoin()!=null&&data.getMapJoin().length>0?data.getMapJoin()[0]:-1;
        Tier t=tier(hp,damage,map);Map<String,Object> result=new LinkedHashMap<>();result.putAll(Map.of("eligible",true,"tier",t.level(),"vnd",t.vnd(),"bars_min",t.barsMin(),"bars_max",t.barsMax(),"ball_4",t.four(),"ball_3",t.three(),"ball_2",t.two(),"live",b.zone!=null));result.put("bars_percent",barsPercent(t));result.put("gems_min",1);result.put("gems_max",100);return result;
    }
    private static Grant prepare(Boss boss,Player attacker){
        Player p=attacker instanceof Pet pet?pet.master:attacker;
        if(!combat(boss.getClass().getName())||!boss.isDie()||boss.nPoint.hpMax<=0||!PanelBridge.enabled(boss)||p==null||!p.isPl()||p.id<=0||p.getSession()==null||p.getSession().userId<=0||boss.zone==null||p.zone!=boss.zone)return null;
        Zone zone=boss.zone;Tier tier=tier(boss.nPoint.hpMax,boss.nPoint.dame,zone.map.mapId);int y=boss.location.y;
        try{y=zone.map.yPhysicInTop(boss.location.x,boss.location.y-24);}catch(RuntimeException ignored){}
        return new Grant(boss.bountyEncounterKey(),p.getSession().userId,p.id,boss.getClass().getName(),boss.name==null?boss.getClass().getSimpleName():boss.name,zone.map.mapId,zone.zoneId,boss.nPoint.hpMax,boss.nPoint.dame,tier,roll(tier,ThreadLocalRandom.current()),rollGems(ThreadLocalRandom.current()),zone,boss.location.x,y,new WeakReference<>(p));
    }
    public static boolean suppress(ItemMap item){Grant grant=SCOPE.get();return grant!=null&&item.zone==grant.zone&&(item.itemTemplate.id==457||item.itemTemplate.id>=15&&item.itemTemplate.id<=17);}
    public static void reward(Boss boss,Player attacker,Runnable nativeReward){
        Grant grant=prepare(boss,attacker);Grant previous=SCOPE.get();if(grant!=null)SCOPE.set(grant);
        try{nativeReward.run();}finally{if(previous==null)SCOPE.remove();else SCOPE.set(previous);if(grant!=null){journal(grant);attempt(grant,true);}}
    }
    private static void attempt(Grant grant,boolean notifyFailure){
        Player current=nro.models.server.Client.gI().getPlayer(grant.player);
        Player p=current!=null?current:grant.owner.get();Object lock=p!=null?p:grant;
        synchronized(lock){
            try{
                Credit credited=credit(grant,p);
                if(credited.fresh&&grant.gems>0&&p!=null)p.inventory.gem=credited.gems;
                if(p!=null&&p.getSession()!=null&&p.getSession().userId==grant.account)p.getSession().vnd=credited.balance;
                PENDING.remove(grant.key,grant);clearJournal(grant.key);
                if(!credited.fresh)return;
                // Emit outside both native and custom drop scopes, including nested encounter callbacks.
                Grant previous=SCOPE.get();SCOPE.remove();
                try{BossDrops.withoutScope(()->{if(grant.zone!=null)for(Loot row:grant.loot){ItemMap item=new ItemMap(grant.zone,row.id,row.quantity,grant.x,grant.y,grant.player);Service.gI().dropItemMap(grant.zone,item);}});}
                catch(RuntimeException error){Logger.logException(BossBounty.class,error);}
                finally{if(previous!=null)SCOPE.set(previous);}
                if(p!=null&&p.getSession()!=null){Service.gI().sendMoney(p);Service.gI().sendThongBao(p,"Hạ "+grant.name+": +"+String.format(Locale.US,"%,d",grant.tier.vnd())+" VNĐ (bậc "+grant.tier.level()+"): +"+grant.gems+" ngọc xanh. "+(grant.loot.isEmpty()?"Lần này không rơi thỏi vàng/ngọc rồng.":"Vật phẩm trúng thưởng rơi tại vị trí boss."));}
            }catch(InvalidGrant error){Logger.logException(BossBounty.class,error);PENDING.remove(grant.key,grant);clearJournal(grant.key);}
            catch(Exception error){
                if(PENDING.putIfAbsent(grant.key,grant)==null){Logger.logException(BossBounty.class,error);if(notifyFailure&&p!=null&&p.getSession()!=null)Service.gI().sendThongBao(p,"Thưởng boss đang chờ lưu. Hệ thống sẽ thử lại, không cần đánh lại boss.");}
            }
        }
    }
    private record Credit(boolean fresh,int balance,int gems){}
    private static final class InvalidGrant extends SQLException{InvalidGrant(String message){super(message);}}
    private static Credit credit(Grant grant,Player owner)throws SQLException{
        try(Connection c=LocalManager.getConnection()){
            c.setAutoCommit(false);
            try{
                int before;
                try(PreparedStatement q=c.prepareStatement("SELECT a.vnd FROM account a JOIN player p ON p.account_id=a.id WHERE a.id=? AND p.id=? FOR UPDATE")){
                    q.setInt(1,grant.account);q.setLong(2,grant.player);try(ResultSet r=q.executeQuery()){if(!r.next())throw new InvalidGrant("Boss bounty account/player mismatch");before=r.getInt(1);}
                }
                try(PreparedStatement q=c.prepareStatement("SELECT account_id,player_id FROM boss_bounty_receipt WHERE encounter_key=? FOR UPDATE")){
                    q.setString(1,grant.key);try(ResultSet r=q.executeQuery()){if(r.next()){if(r.getInt(1)!=grant.account||r.getLong(2)!=grant.player)throw new InvalidGrant("Encounter already belongs to another player");c.commit();return new Credit(false,before,owner==null?0:owner.inventory.gem);}}
                }
                if(before<0||(long)before+grant.tier.vnd()>Integer.MAX_VALUE)throw new SQLException("VNĐ balance limit; bounty pending");
                int after=before+grant.tier.vnd(),gemBefore=0,gemAfter=0;
                if(grant.gems>0){
                    if(owner==null||owner.beforeDispose||owner.getSession()==null||owner.getSession().userId!=grant.account)throw new SQLException("Ngọc thưởng boss chờ người chơi đăng nhập lại");
                    gemBefore=owner.inventory.gem;
                    if(gemBefore<0||(long)gemBefore+grant.gems>Integer.MAX_VALUE)throw new SQLException("Gem balance limit; bounty pending");
                    gemAfter=gemBefore+grant.gems;
                    JsonArray wallet=new JsonParser().parse(nro.models.event.trungthu2026.TrungThuStore.money(owner)).getAsJsonArray();wallet.set(1,new JsonPrimitive(gemAfter));
                    try(PreparedStatement update=c.prepareStatement("UPDATE player SET data_inventory=? WHERE id=? AND account_id=?")){
                        update.setString(1,wallet.toString());update.setLong(2,grant.player);update.setInt(3,grant.account);if(update.executeUpdate()!=1)throw new InvalidGrant("Gem player mismatch");
                    }
                }
                try(PreparedStatement q=c.prepareStatement("INSERT INTO boss_bounty_receipt(encounter_key,account_id,player_id,boss_key,boss_name,map_id,zone_id,hp,damage,tier,vnd,balance_before,balance_after,ground_items,gems,gems_before,gems_after) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                    q.setString(1,grant.key);q.setInt(2,grant.account);q.setLong(3,grant.player);q.setString(4,grant.bossKey);q.setString(5,grant.name);q.setInt(6,grant.map);q.setInt(7,grant.area);q.setLong(8,grant.hp);q.setLong(9,grant.damage);q.setInt(10,grant.tier.level());q.setInt(11,grant.tier.vnd());q.setInt(12,before);q.setInt(13,after);q.setString(14,JSON.toJson(grant.loot));q.setInt(15,grant.gems);q.setInt(16,gemBefore);q.setInt(17,gemAfter);q.executeUpdate();
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE account SET vnd=vnd+? WHERE id=? AND vnd=?")){
                    q.setInt(1,grant.tier.vnd());q.setInt(2,grant.account);q.setInt(3,before);if(q.executeUpdate()!=1)throw new SQLException("Boss bounty balance changed");
                }
                c.commit();return new Credit(true,after,gemAfter);
            }catch(SQLException|RuntimeException error){c.rollback();throw error;}
        }
    }
    public static void ensureSchema(Connection c)throws SQLException{
        for(String column:List.of("gems","gems_before","gems_after"))try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SHOW COLUMNS FROM boss_bounty_receipt LIKE '"+column+"'")){
            if(!r.next())try(Statement alter=c.createStatement()){alter.execute("ALTER TABLE boss_bounty_receipt ADD "+column+" INT NOT NULL DEFAULT 0");}
        }
    }
    public static void retryPending(){recoverPending();for(Grant grant:PENDING.values())attempt(grant,false);}
    public static synchronized void start(){if(started)return;started=true;Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Boss Bounty Retry");t.setDaemon(true);return t;}).scheduleWithFixedDelay(BossBounty::retryPending,5,5,TimeUnit.SECONDS);}
    private BossBounty(){}
}
