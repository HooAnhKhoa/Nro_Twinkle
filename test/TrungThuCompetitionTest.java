package nro.models.event.trungthu2026;
import java.sql.*;import java.nio.file.*;import java.util.*;
import nro.models.data.LocalManager;import nro.models.server.Manager;import nro.models.player_system.Template.*;import nro.models.item.Item;import nro.models.services.*;
public class TrungThuCompetitionTest{
 static int checks;static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static long n(Connection c,String sql)throws Exception{return TrungThuRuntimeTest.scalar(c,sql);}
 static void sql(Connection c,String s)throws Exception{try(Statement q=c.createStatement()){q.executeUpdate(s);}}
 static String rewards;
 static void season(Connection c,long start,long end,String basis)throws Exception{
  try(PreparedStatement q=c.prepareStatement("INSERT INTO trung_thu_season(name,start_ms,end_ms,scheduled,basis,vip_gems,rewards) VALUES('QA',?,?,1,?,20,?)")){q.setLong(1,start);q.setLong(2,end);q.setString(3,basis);q.setString(4,rewards);q.executeUpdate();}
 }
 static void reload(Connection c)throws Exception{c.setAutoCommit(false);try{var s=TrungThuCompetition.reload(c);c.commit();TrungThuCompetition.publish(s);}catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
 public static void main(String[] a)throws Exception{
  try(Connection c=LocalManager.getConnection()){
   ok("nro_trungthu_rules_20261017".equals(c.getCatalog()),"QA database guard");
   for(String s:Files.readString(Path.of("web/trung_thu_top_schema.sql")).split(";"))if(!s.isBlank())sql(c,s);
   for(String t:List.of("trung_thu_prize","trung_thu_score","trung_thu_season"))sql(c,"DELETE FROM "+t);
   sql(c,"DELETE FROM player WHERE id BETWEEN 2026101670 AND 2026101679");
   try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){ItemTemplate t=new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),false,r.getInt("power_require"));t.head=(short)r.getInt("head");t.body=(short)r.getInt("body");t.leg=(short)r.getInt("leg");Manager.ITEM_TEMPLATES.add(t);}}
   try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   rewards=Files.readString(Path.of("top-rewards.json"));
   long now=System.currentTimeMillis();season(c,now+60000,now+120000,"opened");reload(c);
   ok(!nro.models.event.EventManager.enabled("trung_thu"),"future schedule overrides manual ON/OFF");
   var p=TrungThuRuntimeTest.user(c,70,30);
   TrungThuCompetition.count(c,p,2078,"opened");ok(n(c,"SELECT COUNT(*) FROM trung_thu_score")==0,"no points before start");
   sql(c,"UPDATE trung_thu_season SET start_ms="+(now-1000)+",end_ms="+(now+600000));reload(c);
   ok(nro.models.event.EventManager.enabled("trung_thu"),"schedule automatically opens event");
   long season=n(c,"SELECT MAX(id) FROM trung_thu_season");
   TrungThuCompetition.count(c,p,2078,"acquired");ok(n(c,"SELECT COUNT(*) FROM trung_thu_score")==0,"wrong scoring basis ignored");
   var exchange=new BagExchange(0,System::currentTimeMillis);
   TrungThuRuntimeTest.put(p,2078,1);
   var missing=exchange.execute(p,TrungThuCommerce.cost(Map.of(2078,2),0,0),TrungThuCommerce.gain(TrungThuRuntimeTest.item(987,1)),x->TrungThuCompetition.persist(x,2078,"opened"));
   ok(missing==BagExchange.Result.MISSING_ITEMS&&n(c,"SELECT COUNT(*) FROM trung_thu_score")==0,"failed exchange awards no points");
   var success=exchange.execute(p,TrungThuCommerce.cost(Map.of(2078,1),0,0),TrungThuCommerce.gain(TrungThuRuntimeTest.item(987,1)),x->TrungThuCompetition.persist(x,2078,"opened"));
   ok(success==BagExchange.Result.SUCCESS&&n(c,"SELECT score FROM trung_thu_score WHERE player_id="+p.id)==1,"point committed with actual consumed box");
   ok(TrungThuRuntimeTest.qty(p,2078)==0,"counted box really consumed");
   TrungThuCompetition.count(c,p,1316,"opened");ok(n(c,"SELECT score FROM trung_thu_score WHERE player_id="+p.id)==2,"legacy normal box joins same leaderboard");
   var ghost=new TrungThuRuntimeTest.User(2026101679);ghost.inventory.itemsBag.add(new Item());
   var failed=exchange.execute(ghost,TrungThuCommerce.cost(Map.of(),0,0),TrungThuCommerce.gain(TrungThuRuntimeTest.item(987,1)),x->TrungThuCompetition.persist(x,2078,"opened"));
   ok(failed==BagExchange.Result.SAVE_FAILED&&n(c,"SELECT COUNT(*) FROM trung_thu_score WHERE player_id="+ghost.id)==0&&TrungThuRuntimeTest.qty(ghost,987)==0,"SQL failure rolls back points and inventory together");
   sql(c,"UPDATE trung_thu_season SET basis='acquired'");reload(c);long gold=p.inventory.gold;int gem=p.inventory.gem;
   TrungThuCompetition.buyVip(p);ok(TrungThuRuntimeTest.qty(p,2079)==1&&p.inventory.gem==gem-20&&p.inventory.gold==gold,"VIP purchase uses configured real currency");
   ok(n(c,"SELECT score FROM trung_thu_score WHERE player_id="+p.id+" AND board='vip'")==1,"VIP purchase counted only once");
   ok(TrungThuCommerce.boxKind(2079)!=null,"VIP box has an actual open handler");
   p.inventory.gem=10000;
   int totalBoxes=1;
   for(int quantity:new int[]{10,50,100}){
    Thread.sleep(550);int beforeGems=p.inventory.gem;int beforeBoxes=TrungThuRuntimeTest.qty(p,2079);
    TrungThuCompetition.buyVip(p,quantity);totalBoxes+=quantity;
    ok(TrungThuRuntimeTest.qty(p,2079)==beforeBoxes+quantity&&p.inventory.gem==beforeGems-20*quantity,"bulk "+quantity+" grants exact quantity and cost");
    ok(n(c,"SELECT score FROM trung_thu_score WHERE player_id="+p.id+" AND board='vip'")==totalBoxes,"bulk "+quantity+" counts every acquired box");
   }
   int boxesBefore=TrungThuRuntimeTest.qty(p,2079),gemsBefore=p.inventory.gem;
   TrungThuCompetition.buyVip(p,11);ok(TrungThuRuntimeTest.qty(p,2079)==boxesBefore&&p.inventory.gem==gemsBefore,"unsupported batch does not charge");
   Thread.sleep(550);p.inventory.gem=0;TrungThuCompetition.buyVip(p,100);ok(TrungThuRuntimeTest.qty(p,2079)==boxesBefore,"insufficient gems gives no boxes");p.inventory.gem=gemsBefore;
   var npc=new nro.models.npc.Npc(0,1,0,0,13,0){public void openBaseMenu(nro.models.player.Player p){}public void confirmMenu(nro.models.player.Player p,int i){}};
   TrungThuCompetition.openNpc(p,npc);TrungThuCompetition.confirm(p,npc,1);
   try(var reader=new java.io.DataInputStream(new java.io.ByteArrayInputStream(p.menuPacket))){reader.readShort();String text=reader.readUTF();ok(text.contains("10 hộp:")&&text.contains("50 hộp:")&&text.contains("100 hộp:")&&reader.readUnsignedByte()==5,"native menu shows batch quantities and total prices");}
   Thread.sleep(550);TrungThuCompetition.confirm(p,npc,1);ok(TrungThuRuntimeTest.qty(p,2079)==boxesBefore+10,"menu selection one exchanges ten boxes");
   TrungThuCompetition.confirm(p,npc,1);ok(TrungThuRuntimeTest.qty(p,2079)==boxesBefore+10,"replayed menu packet cannot exchange twice");

   // Deterministic tie: earlier reached_ms precedes smaller ID; ID is final tie breaker.
   for(String board:List.of("vip","normal","vegetarian"))for(int i=0;i<105;i++)sql(c,"INSERT INTO trung_thu_score VALUES("+season+",'"+board+"',"+(3000000+i)+","+(40000-i)+","+(now+i)+")");
   sql(c,"UPDATE trung_thu_score SET score=50000,reached_ms="+now+" WHERE player_id="+p.id+" AND board='normal'");
   sql(c,"UPDATE trung_thu_score SET score=50000,reached_ms="+(now-1)+" WHERE player_id=3000001 AND board='normal'");
   sql(c,"UPDATE trung_thu_season SET end_ms="+(System.currentTimeMillis()-1));reload(c);
   ok(!nro.models.event.EventManager.enabled("trung_thu"),"event stops automatically at exclusive end");
   int endedBoxes=TrungThuRuntimeTest.qty(p,2079);TrungThuCompetition.buyVip(p,100);ok(TrungThuRuntimeTest.qty(p,2079)==endedBoxes,"bulk exchange remains closed after event end");
   ok(n(c,"SELECT COUNT(*) FROM trung_thu_prize WHERE board IN ('vip','normal','vegetarian')")==300,"only top100 on each board receive rank packages");
   ok(n(c,"SELECT place FROM trung_thu_prize WHERE player_id="+p.id+" AND board='normal'")==2,"tied points rank earlier achievement first");
   ok(n(c,"SELECT COUNT(*) FROM trung_thu_prize WHERE board='milestone'")==105,"VIP milestone includes qualified players outside top100");
   ok(n(c,"SELECT COUNT(*) FROM trung_thu_season WHERE closed_at IS NOT NULL")==1,"season closed durably");
   reload(c);reload(c);ok(n(c,"SELECT COUNT(*) FROM trung_thu_prize")==405,"restart/repeated close never duplicates prizes");
   long count=n(c,"SELECT SUM(score) FROM trung_thu_score");TrungThuCompetition.count(c,p,2079,"acquired");ok(n(c,"SELECT SUM(score) FROM trung_thu_score")==count,"no points after closing");
   p.inventory.itemsBag.clear();p.inventory.itemsBag.add(TrungThuRuntimeTest.item(0,1));TrungThuCompetition.claim(p);
   ok(n(c,"SELECT COUNT(*) FROM trung_thu_prize WHERE player_id="+p.id+" AND claimed_at IS NOT NULL")==0,"full bag keeps prize pending");
   for(int i=0;i<30;i++)p.inventory.itemsBag.add(new Item());TrungThuCompetition.claim(p);
   ok(n(c,"SELECT COUNT(*) FROM trung_thu_prize WHERE player_id="+p.id+" AND claimed_at IS NOT NULL")==1,"claim works after event OFF");
   ok(TrungThuRuntimeTest.qty(p,2059)==1&&TrungThuRuntimeTest.qty(p,1283)==60,"claimed items match frozen rank2 package");
   Thread.sleep(510);TrungThuCompetition.claim(p);ok(TrungThuRuntimeTest.qty(p,2059)==1,"same prize cannot be claimed twice");
   season(c,System.currentTimeMillis()-10000,System.currentTimeMillis()-1,"opened");long bad=n(c,"SELECT MAX(id) FROM trung_thu_season");sql(c,"INSERT INTO trung_thu_score VALUES("+bad+",'normal',"+p.id+",1,"+now+")");
   try(PreparedStatement q=c.prepareStatement("UPDATE trung_thu_season SET rewards=? WHERE id=?")){q.setString(1,rewards.replace("\"id\":2059","\"id\":2082"));q.setLong(2,bad);q.executeUpdate();}
   boolean error=false;try{reload(c);}catch(SQLException expected){error=true;}
   ok(error&&n(c,"SELECT COUNT(*) FROM trung_thu_prize WHERE season_id="+bad)==0&&n(c,"SELECT COUNT(*) FROM trung_thu_season WHERE id="+bad+" AND closed_at IS NULL")==1,"invalid assets prevent partial payout and preserve retry");
   try(PreparedStatement q=c.prepareStatement("UPDATE trung_thu_season SET rewards=? WHERE id=?")){q.setString(1,rewards);q.setLong(2,bad);q.executeUpdate();}reload(c);
   ok(n(c,"SELECT COUNT(*) FROM trung_thu_prize WHERE season_id="+bad)==1,"retry succeeds after invalid reward fixed");
   System.out.println("PASS "+checks+" competition checks");
  }System.exit(0);
 }
}
