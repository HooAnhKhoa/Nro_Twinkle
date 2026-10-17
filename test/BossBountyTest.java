package nro.models.boss.Nappa;

import java.sql.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;import java.lang.reflect.*;
import nro.models.boss.*;import nro.models.consts.*;import nro.models.data.LocalManager;import nro.models.item.Item;import nro.models.map.ItemMap;import nro.models.network.*;import nro.models.player.*;import nro.models.player_system.Template.*;import nro.models.server.*;import nro.models.services.*;import nro.models.skill.NClass;

/** Real reward entrypoint, real ground items and guarded QA-only balances. */
public class BossBountyTest {
 static final int BASE=2026103000;static int checks;static nro.models.map.Map map;
 static void check(boolean b,String text){if(!b)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class Socket extends java.net.Socket{
  @Override public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("127.0.0.1",1);}
  @Override public java.net.InetAddress getInetAddress(){return java.net.InetAddress.getLoopbackAddress();}
  @Override public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(new byte[0]);}
  @Override public java.io.OutputStream getOutputStream(){return new java.io.ByteArrayOutputStream();}
 }
 static class User extends Player {User(int id){this.id=id;name="Bounty QA "+id;isPlayer=true;session=new MySession(new Socket());session.userId=id;session.version=220;session.vnd=999999;nPoint.hp=nPoint.hpMax=100;nPoint.power=10000;}@Override public void sendMessage(Message message){}}
 static class Sample extends Boss {
  int nativeCalls;boolean fail;
  Sample()throws Exception{super(-2026103000,new BossData("QA Nappa",(byte)0,new short[]{0,1,2,-1,-1,-1},10000,new int[]{25000000},new int[]{63},new int[][]{},new String[]{},new String[]{},new String[]{},60));currentLevel=0;name="QA Nappa";zone=map.zones.get(0);nPoint.hpMax=25000000;nPoint.hp=0;nPoint.dame=10000;}
  @Override public void initBase(){nPoint.hpMax=25000000;nPoint.hp=25000000;nPoint.dame=10000;}
  @Override public void reward(Player p){nativeCalls++;for(int id:new int[]{457,15,16,17,190})Service.gI().dropItemMap(zone,new ItemMap(zone,id,id==457?99:1,0,0,p.id));if(fail)throw new IllegalStateException("Expected native reward failure");}
 }
 static long scalar(String sql)throws Exception{try(Connection c=LocalManager.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void sql(String text)throws Exception{try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){s.execute(text);}}
 static User user(int n)throws Exception {User p=new User(BASE+n);p.zone=map.zones.get(0);p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();for(int i=0;i<11;i++)p.inventory.itemsBody.add(new Item());for(int i=0;i<10;i++)p.inventory.itemsBag.add(new Item());
  try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("INSERT INTO account(id,username,is_admin,vnd,tongnap,active,last_time_login,last_time_logout) VALUES(?,?,0,10000,777,1,'2000-01-01','2000-01-02')")){q.setLong(1,p.id);q.setString(2,"boss_bounty_qa_"+n);q.executeUpdate();}
  sql("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task) VALUES("+p.id+",'Bounty QA',"+p.id+",0,'[0,0,0,0]','[]','[0,0,0,0,0]','[-1,0,0,0,20,0]')");Client.gI().put(p);return p;
 }
 static long balance(User p)throws Exception{return scalar("SELECT vnd FROM account WHERE id="+p.id);}
 static long items(Sample b,int id){return b.zone.items.stream().filter(i->i.itemTemplate.id==id).count();}
 static Path journal(Sample b){return Path.of("data/boss-bounty-pending",b.bountyEncounterKey()+".json");}
 static void clearItems(){map.zones.get(0).items.clear();}
 static void cleanup()throws Exception {sql("DROP TRIGGER IF EXISTS qa_bounty_failure");for(String table:List.of("boss_bounty_receipt","player","account"))sql("DELETE FROM "+table+" WHERE "+(table.equals("boss_bounty_receipt")?"account_id":"id")+" BETWEEN "+BASE+" AND "+(BASE+99));}
 public static void main(String[] args)throws Exception {
  List<Path> journals=new ArrayList<>();
  try{
   try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
    check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");for(String sql:Files.readString(Path.of("../../web/boss_bounty_schema.sql")).split(";"))if(!sql.isBlank())s.execute(sql);
    BossBounty.ensureSchema(c);BossBounty.ensureSchema(c);
    try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate(r.getShort("id"),r.getByte("type"),r.getByte("gender"),r.getString("name"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),false,0));}
    try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("name"),0));}
   }cleanup();for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());map=new nro.models.map.Map(63,"QA Nappa",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[24][60],new int[]{0},1,20,new ArrayList<>());Manager.MAPS.add(map);
   check(BossBounty.tier(1000000,1000,63).level()==1,"early Nappa boss starts at 1000 VNĐ");
   check(BossBounty.tier(25000000,10000,79).level()==2,"Ginyu-tier HP and Nappa map receive 2000 VNĐ");
   check(BossBounty.tier(150000000,35000,103).level()==3,"Cell-tier HP and future map receive 3000 VNĐ");
   check(BossBounty.tier(200000000,32000,110).level()==4,"Cooler-tier HP and cold map receive 4000 VNĐ");
   check(BossBounty.tier(1000000000,100000,6).level()==5&&BossBounty.tier(2000000000L,200000,14).level()==5,"Golden Frieza and Baby receive 5000 VNĐ even on early maps");
   check(BossBounty.tier(40000000,1000,1).level()<BossBounty.tier(40000000,500000,155).level(),"damage and map raise bounty beyond HP alone");
   for(String key:List.of("yardrat.CHIENBINH0","yardrat.DOITRUONG5","luyen_tap_tu_dong.Whis","nhan_ban.NhanBan","sieu_hang.SuperRank","Boss_mini.MatTroi","Boss_mini.Odo","Boss_mini.SoiHecQuyn","Boss_mini.AnTrom","Golden_fireza.DeathBeam1","event_noel.OngGiaNoel"))check(!BossBounty.combat("nro.models.boss."+key),"excluded non-combat/repeatable opponent "+key);
   for(String key:List.of("Nappa.Kuku","Cold.Cooler","Golden_fireza.GoldenFrieza","Baby.Baby","event_hung_vuong.SonTinh","event_trung_thu.MoonBoss$Gogeta","doanh_trai.NinjaAoTim"))check(BossBounty.combat("nro.models.boss."+key),"combat boss eligible "+key);
   for(int level=1;level<=5;level++){
    BossBounty.Tier tier=BossBounty.tier(level==5?1000000000L:level==4?200000000:level==3?100000000:level==2?25000000:1000000,level==1?1000:10000,level==4?110:level==3?100:level==2?79:1);int[] counts=new int[3];
    int bars=0;
    for(int roll=0;roll<10000;roll++){final double draw=roll/10000d;var loot=BossBounty.roll(tier,new Random(){int call;@Override public double nextDouble(){return call++==0?draw:.9999;}@Override public int nextInt(int bound){return bound-1;}});checkLoot(loot,tier);if(loot.stream().anyMatch(x->x.id()==457))bars++;}
    for(int roll=0;roll<10000;roll++){final double draw=roll/10000d;var loot=BossBounty.roll(tier,new Random(){int call;@Override public double nextDouble(){return call++==0?.9999:draw;}@Override public int nextInt(int bound){return bound-1;}});for(var row:loot)if(row.id()>=15&&row.id()<=17)counts[row.id()-15]++;}
    check(bars==level*500&&BossBounty.barsPercent(tier)==level*5,"tier "+level+" exact gold-bar chance grows with strength");
    check(counts[0]==Math.round(tier.two()*100)&&counts[1]==Math.round(tier.three()*100)&&counts[2]==Math.round(tier.four()*100),"tier "+level+" original dragon-ball probabilities remain independent");
   }
   int[] gemCounts=new int[101];for(int i=0;i<10000;i++){final int draw=i%100;int gem=BossBounty.rollGems(new Random(){@Override public int nextInt(int bound){return draw;}});gemCounts[gem]++;}check(gemCounts[1]==100&&gemCounts[100]==100&&Arrays.stream(gemCounts).filter(x->x==100).count()==100,"green gems uniformly cover inclusive 1..100");

   User p=user(1);Sample b=new Sample();String key=b.bountyEncounterKey();journals.add(journal(b));b.giveReward(p);
   check(balance(p)==12000&&p.getSession().vnd==12000,"real kill credits current DB balance and session, ignoring stale session value");
   int firstGems=p.inventory.gem;check(firstGems>=1&&firstGems<=100&&scalar("SELECT JSON_EXTRACT(data_inventory, '$[1]') FROM player WHERE id="+p.id)==firstGems,"kill persists actual green gems and updates online inventory");
   check(scalar("SELECT tongnap FROM account WHERE id="+p.id)==777,"boss cash never counts as topped-up money");
   check(groundMatches(b)&&b.zone.items.stream().filter(i->i.itemTemplate.id==457).allMatch(i->i.quantity>=2&&i.quantity<=3&&i.playerId==p.id),"native 99-bar drop is replaced by the saved chance-based ground plan");
   check(b.zone.items.stream().filter(i->i.itemTemplate.id>=15&&i.itemTemplate.id<=17).count()<=1&&items(b,190)==1,"one controlled dragon ball at most; unrelated native item survives");
   check(scalar("SELECT COUNT(*) FROM boss_bounty_receipt WHERE encounter_key='"+key+"'")==1&&!Files.exists(journal(b)),"cash receipt is durable and completed journal is removed");
   b.giveReward(p);check(balance(p)==12000&&b.nativeCalls==1&&p.inventory.gem==firstGems,"repeated giveReward cannot mint cash, bars or native loot twice");
   BossBounty.reward(b,p,()->{});check(balance(p)==12000&&groundMatches(b),"database encounter key blocks replay even past in-memory death guard");
   clearItems();b.currentLevel=-1;b.respawn();check(!key.equals(b.bountyEncounterKey()),"new respawn gets a distinct encounter key");b.nPoint.hp=0;b.giveReward(p);check(balance(p)==14000,"next actual life awards normally after respawn");
   clearItems();User alive=user(2);Sample living=new Sample();living.nPoint.hp=1;living.giveReward(alive);check(balance(alive)==10000,"an alive boss cannot generate bounty cash");
   clearItems();User bot=user(3);bot.isBot=true;Sample botBoss=new Sample();botBoss.giveReward(bot);check(balance(bot)==10000&&botBoss.nativeCalls==0,"bot kills do not produce bounty or native drops");
   clearItems();User master=user(4);Pet pet=new Pet(master);pet.id=-master.id;pet.zone=master.zone;Sample petBoss=new Sample();petBoss.giveReward(pet);check(balance(master)==12000&&petBoss.zone.items.stream().filter(i->i.itemTemplate.id==457).allMatch(i->i.playerId==master.id),"pet kills credit and reserve bounty for the master");
   clearItems();User race=user(5);Sample raced=new Sample();var pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->raced.giveReward(race));var z=pool.submit(()->raced.giveReward(race));a.get();z.get();}finally{pool.shutdownNow();}check(balance(race)==12000&&raced.nativeCalls==1&&groundMatches(raced),"concurrent death callbacks award once");
   clearItems();User failure=user(6);Sample failed=new Sample();journals.add(journal(failed));sql("CREATE TRIGGER qa_bounty_failure BEFORE UPDATE ON account FOR EACH ROW BEGIN IF OLD.id="+failure.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected bounty storage failure'; END IF; END");failed.giveReward(failure);
   check(balance(failure)==10000&&items(failed,457)==0&&scalar("SELECT COUNT(*) FROM boss_bounty_receipt WHERE account_id="+failure.id)==0&&Files.exists(journal(failed)),"database failure rolls back receipt/cash, delays bars, and leaves a durable pending journal");
   int plannedGems=new com.google.gson.JsonParser().parse(Files.readString(journal(failed))).getAsJsonObject().get("gems").getAsInt();
   check(failure.inventory.gem==0&&scalar("SELECT JSON_EXTRACT(data_inventory, '$[1]') FROM player WHERE id="+failure.id)==0,"failed cash transaction also rolls back gems");
   sql("DROP TRIGGER qa_bounty_failure");Field pending=BossBounty.class.getDeclaredField("PENDING");pending.setAccessible(true);((Map<?,?>)pending.get(null)).clear();BossBounty.retryPending();
   check(balance(failure)==12000&&groundMatches(failed)&&!Files.exists(journal(failed)),"restart-style recovery restores pending cash and the original ground plan");BossBounty.retryPending();check(balance(failure)==12000&&groundMatches(failed),"recovery retry cannot duplicate cash or ground items");
   check(failure.inventory.gem==plannedGems&&plannedGems>=1&&plannedGems<=100,"restart retry awards original journal gem roll once");
   clearItems();User bad=user(7);Sample nativeError=new Sample();nativeError.fail=true;try{nativeError.giveReward(bad);}catch(IllegalStateException expected){}check(balance(bad)==12000&&groundMatches(nativeError),"bounty is preserved if an unrelated native reward handler throws");
   clearItems();User overflow=user(8);Sample max=new Sample();journals.add(journal(max));sql("UPDATE account SET vnd=2147483647 WHERE id="+overflow.id);max.giveReward(overflow);check(balance(overflow)==2147483647L&&Files.exists(journal(max))&&items(max,457)==0,"balance overflow is held pending rather than wrapped or partially granted");sql("UPDATE account SET vnd=10000 WHERE id="+overflow.id);BossBounty.retryPending();check(balance(overflow)==12000&&groundMatches(max),"pending overflow reward can be credited after balance has space");
   clearItems();User cap=user(9);Sample gemCap=new Sample();journals.add(journal(gemCap));cap.inventory.gem=Integer.MAX_VALUE;gemCap.giveReward(cap);check(balance(cap)==10000&&Files.exists(journal(gemCap)),"gem overflow holds entire bounty pending");cap.inventory.gem=10;BossBounty.retryPending();check(cap.inventory.gem>=11&&cap.inventory.gem<=110&&balance(cap)==12000,"gem overflow retry safely credits after room exists");
   clearItems();User deferred=user(10);Sample deferredBoss=new Sample();journals.add(journal(deferredBoss));deferred.beforeDispose=true;deferredBoss.giveReward(deferred);check(balance(deferred)==10000&&deferred.inventory.gem==0&&Files.exists(journal(deferredBoss)),"disconnecting owner keeps gems and cash durably pending");int deferredGems=new com.google.gson.JsonParser().parse(Files.readString(journal(deferredBoss))).getAsJsonObject().get("gems").getAsInt();((Map<?,?>)pending.get(null)).clear();BossBounty.retryPending();check(balance(deferred)==10000,"offline/disposing recovery cannot write stale inventory");deferred.beforeDispose=false;BossBounty.retryPending();check(balance(deferred)==12000&&deferred.inventory.gem==deferredGems,"reconnected owner receives exact saved gem roll");
   clearItems();User legacy=user(11);Sample legacyBoss=new Sample();journals.add(journal(legacyBoss));legacy.beforeDispose=true;legacyBoss.giveReward(legacy);var legacyJson=new com.google.gson.JsonParser().parse(Files.readString(journal(legacyBoss))).getAsJsonObject();legacyJson.remove("gems");Files.writeString(journal(legacyBoss),legacyJson.toString());((Map<?,?>)pending.get(null)).clear();BossBounty.retryPending();check(balance(legacy)==12000&&legacy.inventory.gem==0&&!Files.exists(journal(legacyBoss)),"pre-upgrade journal without gems settles old cash without invented gem rewards");
   check(Boolean.TRUE.equals(BossBounty.preview(max).get("eligible"))&&(int)BossBounty.preview(max).get("vnd")==2000,"boss timing preview uses the same tier function as payout");
   System.out.println("PASS: "+checks+" boss bounty checks; 100000 deterministic loot rolls");
  }finally{cleanup();for(Path p:journals)Files.deleteIfExists(p);LocalManager.close();}System.exit(0);
 }
 static void checkLoot(List<BossBounty.Loot> loot,BossBounty.Tier tier){if(loot.size()>2)throw new AssertionError("invalid loot");for(var row:loot)if(row.id()==457&&(row.quantity()<tier.barsMin()||row.quantity()>tier.barsMax()))throw new AssertionError("bars");}
 static boolean groundMatches(Sample b)throws Exception{try(Connection c=LocalManager.getConnection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT ground_items FROM boss_bounty_receipt WHERE encounter_key='"+b.bountyEncounterKey()+"'")){if(!r.next())return false;int expected=0,quantity=0;for(var value:new com.google.gson.JsonParser().parse(r.getString(1)).getAsJsonArray()){var row=value.getAsJsonObject();if(row.get("id").getAsInt()==457){expected++;quantity=row.get("quantity").getAsInt();}}return items(b,457)==expected&&(expected==0||b.zone.items.stream().filter(i->i.itemTemplate.id==457).findFirst().orElseThrow().quantity==quantity);}}
}
