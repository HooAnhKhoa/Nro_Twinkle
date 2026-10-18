package nro.models.event.october20;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.event.trungthu2026.*;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.npc.Npc;
public class October20CompetitionTest {
 static int checks;static final int BASE=2026101600;
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 static void sql(String sql)throws Exception{try(Connection c=LocalManager.getConnection();Statement q=c.createStatement()){q.executeUpdate(sql);}}
 static long n(String sql)throws Exception{try(Connection c=LocalManager.getConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void reload()throws Exception{try(Connection c=LocalManager.getConnection()){c.setAutoCommit(false);try{var s=October20Competition.reload(c);c.commit();October20Competition.publish(s);}catch(Exception e){c.rollback();throw e;}}}
 static class Socket extends java.net.Socket {public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("127.0.0.1",1);}public java.net.InetAddress getInetAddress(){return java.net.InetAddress.getLoopbackAddress();}public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(new byte[0]);}public java.io.OutputStream getOutputStream(){return new java.io.ByteArrayOutputStream();}}
 static class User extends Player {byte[] menu,shop;User(int id){isPlayer=true;this.id=id;name="Oct QA "+id;session=new nro.models.network.MySession(new Socket());session.version=220;nPoint.hp=nPoint.hpMax=100;nPoint.power=10000;}public void sendMessage(Message m){if(m.command==32)menu=m.getData();if(m.command==-44)shop=m.getData();}}
 static User user(int number)throws Exception{
  User p=new User(BASE+number);p.inventory.gold=1000000000;p.inventory.gem=10000;for(int i=0;i<20;i++)p.inventory.itemsBag.add(new Item());
  try(Connection c=LocalManager.getConnection()){
   Map<String,Object> row=new LinkedHashMap<>();try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM player WHERE id<"+BASE+" ORDER BY id LIMIT 1")){if(!r.next())throw new Exception("Missing fixture");var m=r.getMetaData();for(int i=1;i<=m.getColumnCount();i++)row.put(m.getColumnName(i),r.getObject(i));}
   row.put("id",p.id);row.put("name",p.name);row.put("account_id",p.id);row.put("items_bag","[]");row.put("data_inventory","[0,0,0,0,0]");
   String cols=row.keySet().stream().map(k->"`"+k+"`").collect(java.util.stream.Collectors.joining(","));try(PreparedStatement q=c.prepareStatement("INSERT INTO player("+cols+") VALUES("+String.join(",",Collections.nCopies(row.size(),"?"))+")")){int i=1;for(Object v:row.values())q.setObject(i++,v);q.executeUpdate();}
  }return p;
 }
 static int qty(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 static Item item(int id,int amount){return ItemService.gI().createNewItem((short)id,amount);}
 static void pause()throws Exception{Thread.sleep(550);}
 public static void main(String[] args)throws Exception{
  try{
   try(Connection c=LocalManager.getConnection()){
    ok("nro_october20_top_20261018".equals(c.getCatalog()),"isolated QA database guard");
    try(Statement q=c.createStatement()){
     for(String table:List.of("october20_purchase","october20_prize","october20_score","october20_season"))q.executeUpdate("DELETE FROM "+table);
     q.executeUpdate("DELETE FROM player WHERE id BETWEEN "+(BASE+40)+" AND "+(BASE+59));q.execute("DROP TRIGGER IF EXISTS qa_october_fail");
     try(ResultSet r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),r.getBoolean("is_up_to_up"),r.getInt("power_require")));}
     try(ResultSet r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
    }October20Config.reload(c);
    try(PreparedStatement q=c.prepareStatement("INSERT INTO october20_season(name,start_ms,points_end_ms,end_ms,scheduled,rewards) VALUES('QA',?,?,?,1,?)")){long now=System.currentTimeMillis();q.setLong(1,now+60000);q.setLong(2,now+600000);q.setLong(3,now+1200000);q.setString(4,Files.readString(Path.of("october20-top-rewards.json")));q.executeUpdate();}
   }
   reload();User rose=user(40),card=user(41),gift=user(42),full=user(43),fail=user(44),train=user(45);
   long gold=rose.inventory.gold;int gems=rose.inventory.gem;October20Competition.buy(rose,1720,10);ok(qty(rose,1720)==0&&rose.inventory.gem==gems,"no purchase before scheduled start");
   sql("UPDATE october20_season SET start_ms="+(System.currentTimeMillis()-1000));reload();ok(nro.models.event.EventManager.enabled("october20"),"schedule opens October 20 independently of base switch");
   October20Competition.buy(rose,1720,10);ok(qty(rose,1720)==10&&rose.inventory.gem==gems-200&&rose.inventory.gold==gold,"ten rose boxes cost exactly 200 green gems");
   ok(n("SELECT score FROM october20_score WHERE player_id="+rose.id+" AND board='roses'")==10,"one point per purchased rose box");
   card.inventory.ruby=30;gems=card.inventory.gem;October20Competition.buy(card,1718,1);ok(qty(card,1718)==1&&card.inventory.ruby==0&&card.inventory.gem==gems-20,"VIP card uses 30 rubies plus 20 gems at price 50");
   pause();card.inventory.ruby=3000;gems=card.inventory.gem;October20Competition.buy(card,1718,50);ok(qty(card,1718)==51&&card.inventory.ruby==500&&card.inventory.gem==gems,"fifty VIP cards cost 2500 arbitrary gems and stack");
   gold=gift.inventory.gold;October20Competition.buy(gift,1719,100);ok(qty(gift,1719)==100&&gift.inventory.gold==gold-1000000000L,"100 gift boxes cost exactly one billion gold");
   ok(n("SELECT score FROM october20_score WHERE player_id="+gift.id+" AND board='gifts'")==100,"100 purchased gifts add 100 points only to gifts board");
   pause();long points=n("SELECT score FROM october20_score WHERE player_id="+card.id+" AND board='cards'");Item used=card.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==1718).findFirst().get();October20.use(card,used);ok(n("SELECT score FROM october20_score WHERE player_id="+card.id+" AND board='cards'")==points,"opening VIP card does not increase purchase ranking");
   pause();Item roseBox=rose.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==1720).findFirst().get();October20.use(rose,roseBox);ok(qty(rose,1720)==9&&n("SELECT score FROM october20_score WHERE player_id="+rose.id+" AND board='roses'")==10,"replacement box opens without increasing purchase ranking");rose.inventory.itemsBag.add(item(1720,100));ok(n("SELECT score FROM october20_score WHERE player_id="+rose.id+" AND board='roses'")==10,"admin or traded items do not count as purchases");
   sql("UPDATE october20_season SET paused=1");reload();gems=rose.inventory.gem;October20Competition.buy(rose,1720,10);ok(rose.inventory.gem==gems&&!nro.models.event.EventManager.enabled("october20"),"manual pause blocks purchases and gates event");sql("UPDATE october20_season SET paused=0");reload();
   full.inventory.itemsBag.clear();full.inventory.itemsBag.add(item(0,1));full.inventory.itemsBag.add(item(1,1));String before=TrungThuStore.bag(full);October20Competition.buy(full,1718,10);ok(before.equals(TrungThuStore.bag(full))&&n("SELECT COUNT(*) FROM october20_score WHERE player_id="+full.id)==0,"full bag preserves money and awards no points");
   fail.inventory.gem=0;fail.inventory.ruby=0;October20Competition.buy(fail,1718,100);ok(qty(fail,1718)==0,"insufficient combined gems cannot buy cards");fail.inventory.gem=10000;before=TrungThuStore.bag(fail);gems=fail.inventory.gem;
   sql("CREATE TRIGGER qa_october_fail BEFORE UPDATE ON player FOR EACH ROW BEGIN IF NEW.id="+fail.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='expected October save failure'; END IF; END");
   try{October20Competition.buy(fail,1720,10);ok(before.equals(TrungThuStore.bag(fail))&&fail.inventory.gem==gems&&n("SELECT COUNT(*) FROM october20_score WHERE player_id="+fail.id)==0&&n("SELECT COUNT(*) FROM october20_purchase WHERE player_id="+fail.id)==0,"SQL failure rolls back currency items score and receipt");}finally{sql("DROP TRIGGER qa_october_fail");}
   sql("UPDATE october20_season SET rose_gems=21");October20Competition.buy(fail,1720,10);ok(before.equals(TrungThuStore.bag(fail))&&fail.inventory.gem==gems,"stale price snapshot cannot charge wrong amount");sql("UPDATE october20_season SET rose_gems=20");reload();
   Npc npc=new Npc(5,1,0,0,13,0){public void openBaseMenu(Player p){}public void confirmMenu(Player p,int select){}};
   October20Competition.openNpc(rose,npc);October20Competition.confirm(rose,npc,0);try(var r=new java.io.DataInputStream(new java.io.ByteArrayInputStream(rose.menu))){r.readShort();String text=r.readUTF();ok(text.contains("10 món:")&&text.contains("100 món:")&&r.readUnsignedByte()==5,"Quy Lao shows 1 10 50 100 batch choices and prices");}
   int beforeBoxes=qty(rose,1720);October20Competition.confirm(rose,npc,1);ok(qty(rose,1720)==beforeBoxes+10,"native batch menu buys ten boxes");October20Competition.confirm(rose,npc,1);ok(qty(rose,1720)==beforeBoxes+10,"repeated native batch selection cannot buy again");
   October20Shop.open(card);ok(card.shop!=null&&card.idMark.getShopOpen()!=null&&October20Shop.TAG.equals(card.idMark.getShopOpen().tagName),"Chi Chi opens ordinary native shop");pause();int old=qty(card,1718);nro.models.shop.ShopService.gI().takeItem(card,(byte)0,1718);ok(qty(card,1718)==old+1,"ordinary native shop purchase routes into ranked transaction");
   for(int k=0;k<7;k++)train.inventory.itemsBody.add(new Item());train.nPoint.hpg=10000;train.nPoint.mpg=10000;train.nPoint.dameg=1000;train.nPoint.calPoint();long base=train.nPoint.dame;int baseHp=train.nPoint.hpMax,baseKi=train.nPoint.mpMax,baseDef=train.nPoint.def;Item armor=item(2006,1);armor.itemOptions.add(new Item.ItemOption(77,15));armor.itemOptions.add(new Item.ItemOption(103,15));armor.itemOptions.add(new Item.ItemOption(94,5));armor.itemOptions.add(new Item.ItemOption(47,50000));armor.itemOptions.add(new Item.ItemOption(9,0));train.inventory.itemsBag.set(0,armor);Message request=new Message((byte)-43,new byte[]{0,1,0});UseItem.gI().doItem(train,request);request.cleanup();ok(train.nPoint.hpMax==baseHp*115/100&&train.nPoint.mpMax==baseKi*115/100&&train.nPoint.def==baseDef+50000&&train.nPoint.tlGiap==5,"armor 5 grants actual HP KI defense and damage reduction from reference");ok(train.inventory.itemsBody.get(6).isNotNullItem()&&train.inventory.itemsBody.get(6).template.id==2006&&train.nPoint.dame==base*60/100,"training armor 5 equips and reduces attack by 40 percent");
   armor=train.inventory.itemsBody.get(6);armor.itemOptions.stream().filter(o->o.optionTemplate.id==9).findFirst().get().param=10;train.inventory.itemsBody.set(6,new Item());train.inventory.itemsBag.set(0,armor);train.nPoint.calPoint();ok(train.nPoint.dame==base*150/100&&ItemService.gI().isTrainArmor(armor),"trained armor 5 gives 50 percent attack after removal");
   ok(train.nPoint.hpMax==baseHp&&train.nPoint.mpMax==baseKi&&train.nPoint.def==baseDef&&train.nPoint.tlGiap==0,"armor defensive bonuses stop after unequip");User chest=user(46);chest.inventory.itemsBag.set(0,item(1173,5));nro.models.npc.NpcFactory.createNpcConMeo();Npc cat=Manager.NPCS.stream().filter(v->v.tempId==5).findFirst().get();
   for(int selection=0;selection<5;selection++){Message req=new Message((byte)-43,new byte[]{0,1,0});UseItem.gI().doItem(chest,req);req.cleanup();cat.confirmMenu(chest,selection);ok(qty(chest,1066+selection)==1,"angel chest grants selected fragment "+selection);}ok(qty(chest,1173)==0,"five angel chests consume exactly five");cat.confirmMenu(chest,0);ok(qty(chest,1066)==1,"angel chest selection cannot replay");
   User existingSet=user(47);existingSet.inventory.itemsBag.set(0,item(2102,1));Message setRequest=new Message((byte)-43,new byte[]{0,1,0});UseItem.gI().doItem(existingSet,setRequest);setRequest.cleanup();cat.confirmMenu(existingSet,2);ok(qty(existingSet,2102)==0&&existingSet.inventory.itemsBag.stream().anyMatch(v->v.isNotNullItem()&&v.template.type==2),"approved Beerus substitute grants one chosen existing activation piece");
   long season=n("SELECT MAX(id) FROM october20_season");for(String board:List.of("roses","cards","gifts")){for(int x=0;x<105;x++)sql("INSERT INTO october20_score VALUES("+season+",'"+board+"',"+(3000000+x)+","+(40000-x)+","+(System.currentTimeMillis()+x)+")");sql("INSERT INTO october20_score VALUES("+season+",'"+board+"',"+rose.id+",60000,1) ON DUPLICATE KEY UPDATE score=60000,reached_ms=1");}
   sql("UPDATE october20_score SET score=50000 WHERE player_id="+card.id+" AND board='cards'");sql("UPDATE october20_season SET points_end_ms="+(System.currentTimeMillis()-1));reload();ok(Boolean.TRUE.equals(October20Competition.scheduledEnabled()),"event remains open between point cutoff and final end");
   ok(n("SELECT COUNT(*) FROM october20_prize WHERE board IN('roses','cards','gifts')")==300,"exactly top100 per board receive rank prizes");ok(n("SELECT COUNT(*) FROM october20_prize WHERE board IN('rose_bonus','card_bonus')")==2,"milestone bonuses go only to two eligible first places");ok(n("SELECT COUNT(*) FROM october20_prize WHERE player_id="+card.id+" AND board='card_bonus'")==0,"rank2 above threshold does not receive rank1 milestone");
   long score=n("SELECT SUM(score) FROM october20_score");pause();October20Competition.buy(card,1718,10);ok(n("SELECT SUM(score) FROM october20_score")==score,"purchase after point cutoff grants no extra points");reload();reload();ok(n("SELECT COUNT(*) FROM october20_prize")==302,"repeated close or restart cannot duplicate rewards");
   sql("UPDATE october20_season SET end_ms="+(System.currentTimeMillis()-1));reload();old=qty(card,1718);October20Competition.buy(card,1718,10);ok(!Boolean.TRUE.equals(October20Competition.scheduledEnabled())&&qty(card,1718)==old,"event end blocks purchases automatically");
   rose.inventory.itemsBag.clear();rose.inventory.itemsBag.add(item(0,1));October20Competition.claim(rose);ok(n("SELECT COUNT(*) FROM october20_prize WHERE player_id="+rose.id+" AND claimed_at IS NOT NULL")==0,"full bag leaves prize receipt pending");for(int i=0;i<20;i++)rose.inventory.itemsBag.add(new Item());October20Competition.claim(rose);long claimed=n("SELECT COUNT(*) FROM october20_prize WHERE player_id="+rose.id+" AND claimed_at IS NOT NULL");ok(claimed==1&&qty(rose,2006)==1,"offline queued rank prize is claimable after event ends");October20Competition.claim(rose);ok(n("SELECT COUNT(*) FROM october20_prize WHERE player_id="+rose.id+" AND claimed_at IS NOT NULL")==claimed,"immediate repeated claim does not duplicate first package");
   System.out.println("PASS: "+checks+" October 20 competition checks");
  }finally{LocalManager.close();}
  System.exit(0);
 }
}
