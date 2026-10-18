package nro.models.event.trungthu2026;
import nro.models.skill.NClass;import java.sql.*;import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
import nro.models.data.LocalManager;import nro.models.server.Manager;import nro.models.item.Item;import nro.models.player_system.Template.*;import nro.models.services.*;import nro.models.event.*;import nro.models.event.october20.*;import nro.models.npc_list.ChiChi;import nro.models.consts.ConstNpc;

/** Integration against a guarded QA database only; exercises real NPC, item and stat paths. */
public class October20Test {
 static int checks;static final int BASE=2026102800;
 static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS: "+name);}
 static void enabled(boolean on)throws Exception {Field f=EventManager.class.getDeclaredField("switches");f.setAccessible(true);f.set(null,Map.of("october20",on,"mob_rewards",true));try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){s.executeUpdate("UPDATE panel_event_config SET enabled="+(on?1:0)+" WHERE event_key='october20'");}}
 static TrungThuRuntimeTest.User user(int n,int slots,boolean save)throws Exception{
  var p=new TrungThuRuntimeTest.User(BASE+n);p.nPoint.hpg=p.nPoint.mpg=10000;p.nPoint.dameg=1000;p.inventory.gold=100000000;p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();while(p.inventory.itemsBody.size()<11)p.inventory.itemsBody.add(new Item());for(int i=0;i<slots;i++)p.inventory.itemsBag.add(new Item());p.zone=Manager.MAPS.get(0).zones.get(0);
  if(save)try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task) VALUES(?,?,1,0,'[0,0,0,0]','[]','[0,0,0,0,0]','[-1,0,0,0,20,0]')")){q.setLong(1,p.id);q.setString(2,p.name);q.executeUpdate();}return p;
 }
 static long scalar(String sql)throws Exception {try(Connection c=LocalManager.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void ingredients(TrungThuRuntimeTest.User p){for(int id:new int[]{1508,1505,1507,1509})TrungThuRuntimeTest.put(p,id,100);}
 static void cleanup(Connection c)throws Exception {try(Statement s=c.createStatement()){for(String table:List.of("october20_exchange","october20_player"))s.executeUpdate("DELETE FROM "+table+" WHERE player_id BETWEEN "+BASE+" AND "+(BASE+99));s.executeUpdate("DELETE FROM player WHERE id BETWEEN "+BASE+" AND "+(BASE+99));}}
 public static void main(String[] args)throws Exception {
  String before=null;long revision=1;int toggle=0;
  try {
   try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
    check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
    for(String file:List.of("event_schema.sql","october20_schema.sql"))for(String sql:Files.readString(Path.of("../../web",file)).split(";"))if(!sql.isBlank())s.execute(sql);
    cleanup(c);
    try(ResultSet r=s.executeQuery("SELECT revision,settings FROM october20_config WHERE id=1")){r.next();revision=r.getLong(1);before=r.getString(2);}
    try(ResultSet r=s.executeQuery("SELECT enabled FROM panel_event_config WHERE event_key='october20'")){r.next();toggle=r.getInt(1);}
    October20Config.reload(c);
    try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate(r.getShort("id"),r.getByte("type"),r.getByte("gender"),r.getString("name"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),false,0));}
    try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("name"),0));}
   }
   for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());TrungThuRuntimeTest.map(5,(byte)0);enabled(true);var cfg=October20Config.current();
   check(cfg.n("sd.min")==15&&cfg.n("sd.max")==20&&cfg.n("cm.min")==5&&cfg.n("cute.max")==7,"approved stronger stat ranges seeded");
   for(String bad:List.of(before.replace("\"sd.min\":15","\"sd.min\":101"),before.replace("\"cm.min\":5","\"cm.min\":8"),before.replace("\"special.stones\":50","\"special.stones\":99"),before.replace("\"permanent\":1","\"permanent\":1.5"))){boolean rejected=false;try{October20Config.parse(1,bad);}catch(Exception expected){rejected=true;}check(rejected,"invalid settings rejected before publishing");}
   for(String kind:List.of("normal","special")){
    int accessories=0,stones=0,support=0;
    for(int draw=0;draw<100;draw++){final int first=draw;Random rng=new Random(){int n;@Override public int nextInt(int bound){return n++==0?first:0;}};Item item=October20.reward(cfg,kind,rng);int id=item.template.id;if(Arrays.stream(October20.ACCESSORIES).anyMatch(v->v==id))accessories++;else if(id>=220&&id<=224)stones++;else support++;}
    check(accessories==cfg.n(kind+".accessory")&&stones==cfg.n(kind+".stones")&&support==100-accessories-stones,kind+" reward weights are mutually exclusive and total 100%");
   }
   var stats=user(1,10,true);Item reward=October20.accessory(cfg,1515,7,new Random(1));stats.inventory.itemsBody.set(8,reward);stats.nPoint.calPoint();
   int sd=reward.itemOptions.stream().filter(o->o.optionTemplate.id==50).findFirst().get().param;
   int cute=reward.itemOptions.stream().filter(o->o.optionTemplate.id==226).findFirst().get().param;
   int crit=reward.itemOptions.stream().filter(o->o.optionTemplate.id==14).findFirst().get().param;
   int sdcm=reward.itemOptions.stream().filter(o->o.optionTemplate.id==5).findFirst().get().param;
   check(stats.nPoint.dame==(1000*(100+sd)/100)*(100+cute)/100&&stats.nPoint.crit==crit&&stats.nPoint.tlSDCM==sdcm,"equipped accessory actually adds SD, critical damage, critical chance and Cute once");
   check(stats.nPoint.hpMax==10000&&stats.nPoint.mpMax==10000,"accessory does not invent HP/KI stats");
   check(reward.itemOptions.stream().anyMatch(o->o.optionTemplate.id==93&&o.param==7)&&reward.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30),"temporary accessories have native expiry and trade lock");
   var max=new HashMap<>(cfg.values());max.put("permanent",100);max.put("normal.accessory",100);max.put("normal.stones",0);var permanent=October20.reward(new October20Config.Snapshot(cfg.revision(),Map.copyOf(max)),"normal",new Random(1));check(permanent.itemOptions.stream().noneMatch(o->o.optionTemplate.id==93),"permanent reward has no expiry option");
   var craft=user(2,10,true);ingredients(craft);long gold=craft.inventory.gold;
   check(October20.execute(craft,cfg,0)==BagExchange.Result.SUCCESS,"normal recipe succeeds");
   check(TrungThuRuntimeTest.qty(craft,1719)==1&&TrungThuRuntimeTest.qty(craft,1508)==80&&TrungThuRuntimeTest.qty(craft,1505)==90&&TrungThuRuntimeTest.qty(craft,1507)==95&&TrungThuRuntimeTest.qty(craft,1509)==99&&craft.inventory.gold==gold-1000000,"recipe consumes exactly the displayed materials and gold");
   check(scalar("SELECT COUNT(*) FROM player WHERE id="+craft.id+" AND items_bag LIKE '%1719%'")==1,"craft saved before acknowledgement");
   check(October20.execute(craft,cfg,0)==BagExchange.Result.COOLDOWN&&TrungThuRuntimeTest.qty(craft,1719)==1,"rapid repeated requests cannot duplicate reward");
   var missing=user(3,8,true);check(October20.execute(missing,cfg,1)==BagExchange.Result.MISSING_ITEMS&&missing.inventory.gold==100000000,"missing ingredients do not charge gold");
   var special=user(4,10,true);ingredients(special);check(October20.execute(special,cfg,1)==BagExchange.Result.SUCCESS&&TrungThuRuntimeTest.qty(special,1720)==1&&special.inventory.gold==97000000,"special box uses separate recipe and cost");
   var full=user(5,1,true);TrungThuRuntimeTest.put(full,1719,2);October20.use(full,full.inventory.itemsBag.get(0));check(TrungThuRuntimeTest.qty(full,1719)==2&&October20Store.points(full)==0,"full bag preserves entire box stack and points");
   var unsaved=user(6,10,false);ingredients(unsaved);check(October20.execute(unsaved,cfg,0)==BagExchange.Result.SAVE_FAILED&&TrungThuRuntimeTest.qty(unsaved,1508)==100&&unsaved.inventory.gold==100000000,"database failure rolls back ingredients, gold, bag and points");
   var opener=user(7,10,true);TrungThuRuntimeTest.put(opener,1720,1);enabled(false);October20.use(opener,opener.inventory.itemsBag.get(0));check(TrungThuRuntimeTest.qty(opener,1720)==0&&October20Store.points(opener)==5,"owned box opens while event is disabled and saves five points");
   check(scalar("SELECT COUNT(*) FROM october20_exchange WHERE player_id="+opener.id+" AND operation='open.special'")==1,"box grant has exactly one durable receipt");
   check(October20.execute(missing,cfg,0)==BagExchange.Result.INVALID_REWARD,"disabled event blocks new crafting and redemption");enabled(true);
   var poor=user(8,10,true);check(October20.execute(poor,cfg,2)==BagExchange.Result.SAVE_FAILED&&poor.inventory.itemsBag.stream().noneMatch(Item::isNotNullItem),"insufficient points cannot grant accessory");
   try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){s.executeUpdate("INSERT INTO october20_player(player_id,points) VALUES("+poor.id+",100)");}
   check(October20.execute(poor,cfg,2)==BagExchange.Result.SUCCESS&&October20Store.points(poor)==0&&TrungThuRuntimeTest.qty(poor,1515)==1,"guaranteed chosen accessory costs exactly 100 durable points");
   var stale=user(9,10,true);ingredients(stale);try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){s.executeUpdate("UPDATE october20_config SET revision=revision+1 WHERE id=1");}
   check(October20.execute(stale,cfg,0)==BagExchange.Result.SAVE_FAILED&&TrungThuRuntimeTest.qty(stale,1508)==100,"commit rejects a config changed after confirmation without taking materials");
   try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){s.executeUpdate("UPDATE october20_config SET revision="+revision+" WHERE id=1");}
   var npc=new ChiChi(5,1,0,0,ConstNpc.CHI_CHI,0);October20.open(npc,stale);check(stale.idMark.getIndexMenu()==202610400,"NPC opens new ranking and shop menu");long beforeMaterials=TrungThuRuntimeTest.qty(stale,1508);October20.menu(npc,stale,0);check(TrungThuRuntimeTest.qty(stale,1508)==beforeMaterials&&TrungThuRuntimeTest.qty(stale,1719)==0,"Chi Chi directs rose buyers to Quy Lao without crafting legacy boxes");
   check(PanelBridgeCheck(cfg),"drop category is gated by event and common event switch");
   enabled(true);nro.models.server.PanelBridge.dropMultiplier=100;
   var mob=new nro.models.mob.Mob();mob.zone=stats.zone;mob.tempId=1;var drops=new ArrayList<nro.models.map.ItemMap>();October20.drops(mob,stats,drops,20,30);
   check(drops.size()==4&&drops.stream().allMatch(i->i.playerId==stats.id),"real ordinary mob creates all four configured materials for the killer");
   mob.tempId=nro.models.consts.ConstMob.MOC_NHAN;drops.clear();October20.drops(mob,stats,drops,20,30);check(drops.isEmpty(),"training dummy cannot drop event materials");
   mob.tempId=1;enabled(false);October20.drops(mob,stats,drops,20,30);check(drops.isEmpty(),"disabled event stops mob materials");
   Field switches=EventManager.class.getDeclaredField("switches");switches.setAccessible(true);switches.set(null,Map.of("october20",true,"mob_rewards",false));October20.drops(mob,stats,drops,20,30);check(drops.isEmpty(),"common event drop switch also stops materials");nro.models.server.PanelBridge.dropMultiplier=1;
   System.out.println("PASS: "+checks+" October 20 checks");
  }finally{try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){if(before!=null){try(PreparedStatement q=c.prepareStatement("UPDATE october20_config SET settings=?,revision=? WHERE id=1")){q.setString(1,before);q.setLong(2,revision);q.executeUpdate();}s.executeUpdate("UPDATE panel_event_config SET enabled="+toggle+" WHERE event_key='october20'");}cleanup(c);}}
  System.exit(0);
 }
 static boolean PanelBridgeCheck(October20Config.Snapshot c)throws Exception {enabled(true);boolean a=nro.models.server.PanelBridge.dropChance("october20",5,100)==5;enabled(false);return a&&nro.models.server.PanelBridge.dropChance("october20",5,100)==0;}
}
