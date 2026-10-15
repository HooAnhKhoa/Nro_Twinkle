package nro.models.event.trungthu2026;

import java.io.*;
import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.skill.NClass;

/** Real shop packets, item-use routing and combat stats; QA database only. */
public class TrungThuCakeBuffTest {
 static final int BASE=2026101900;static int checks;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class User extends TrungThuRuntimeTest.User {
  Map<Integer,Integer> timers=new HashMap<>();byte[] shop;
  User(int id){super(id);nPoint.hpg=10000;nPoint.mpg=10000;nPoint.dameg=1000;}
  @Override public void sendMessage(Message m){if(m.command==-44)shop=m.getData();if(m.command==-106)try{var in=new DataInputStream(new ByteArrayInputStream(m.getData()));timers.put(in.readUnsignedShort(),(int)in.readShort());}catch(Exception error){throw new AssertionError(error);}}
 }
 static User user(Connection c,int n)throws Exception{
  User p=new User(BASE+n);p.inventory.gem=1000;for(int i=0;i<20;i++)p.inventory.itemsBag.add(new Item());p.zone=Manager.MAPS.get(0).zones.get(0);
  try(PreparedStatement q=c.prepareStatement("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task) VALUES(?,?,1,0,'[0,0,0,0]','[]','[0,1000,0,0,0]','[-1,0,0,0,20,0]') ON DUPLICATE KEY UPDATE name=VALUES(name)")){q.setLong(1,p.id);q.setString(2,"Cake QA "+n);q.executeUpdate();}return p;
 }
 static int qty(User p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 static void put(User p,int id,int quantity){InventoryService.gI().addItemList(p.inventory.itemsBag,ItemService.gI().createNewItem((short)id,quantity));}
 static long scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void expire(Connection c,User p)throws Exception{try(Statement s=c.createStatement()){s.executeUpdate("UPDATE trung_thu_2026_cake SET until_ms=1 WHERE player_id="+p.id);}TrungThuCakeBuff.invalidate(p.id);}
 static void use(User p,int id)throws Exception{int index=-1;for(int i=0;i<p.inventory.itemsBag.size();i++){Item item=p.inventory.itemsBag.get(i);if(item.isNotNullItem()&&item.template.id==id){index=i;break;}}if(index<0)throw new AssertionError("No cake");UseItem.gI().doItem(p,new Message((byte)0,new byte[]{0,1,(byte)index}));}
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
   try(Statement s=c.createStatement()){
    for(String table:List.of("shop","tab_shop","item_shop","item_shop_option","shop_ky_gui","npc_template"))s.execute("CREATE TABLE IF NOT EXISTS "+table+" LIKE srcnrofree."+table);
    TrungThuStore.ensureData(c);s.executeUpdate("DELETE FROM trung_thu_2026_cake WHERE player_id BETWEEN "+BASE+" AND "+(BASE+99));s.executeUpdate("DELETE FROM player WHERE id BETWEEN "+BASE+" AND "+(BASE+99));
    try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),r.getBoolean("is_up_to_up"),r.getInt("power_require")));}
    try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   }
   for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());TrungThuRuntimeTest.map(5,(byte)0);TrungThuRuntimeTest.enabled(true);TrungThuRuntimeTest.loadShop(c);
   User buyer=user(c,1);TrungThuShop.open(buyer);int cakes=0;
   try(var packet=new DataInputStream(new ByteArrayInputStream(buyer.shop))){check(packet.readUnsignedByte()==0&&packet.readUnsignedByte()==4,"native shop has four tabs");for(int tab=0;tab<4;tab++){packet.readUTF();for(int n=packet.readUnsignedByte();n>0;n--){int id=packet.readShort(),gold=packet.readInt(),gems=packet.readInt();Map<Integer,Integer> options=new HashMap<>();for(int count=packet.readUnsignedByte();count>0;count--)options.put(packet.readUnsignedByte(),(int)packet.readShort());packet.readByte();if(packet.readByte()==1)for(int part=0;part<4;part++)packet.readShort();var kind=TrungThuCakeBuff.kind(id);if(kind!=null){cakes++;check(gold==0&&gems==kind.gems()&&options.get(50)==kind.percent()&&options.get(77)==kind.percent()&&options.get(103)==kind.percent()&&options.get(65)==10,"cake "+id+" displays exact SD/HP/KI tier, gem price and ten minutes");}}}check(packet.available()==0&&cakes==3,"all three cake rows decode cleanly");}
   for(var kind:TrungThuCakeBuff.kinds()){
    User p=user(c,10+kind.id()%10);p.nPoint.calPoint();int damage=p.nPoint.dame,hp=p.nPoint.hpMax,ki=p.nPoint.mpMax;
    TrungThuShop.open(p);nro.models.shop.ShopService.gI().takeItem(p,(byte)0,kind.id());check(qty(p,kind.id())==1&&p.inventory.gem==1000-kind.gems(),"native purchase charges correct gems and gives cake "+kind.id());
    use(p,kind.id());check(qty(p,kind.id())==0&&TrungThuCakeBuff.percent(p)==kind.percent(),"real UseItem consumes one cake and applies tier "+kind.percent());
    check(p.nPoint.dame==damage*(100+kind.percent())/100&&p.nPoint.hpMax==hp*(100+kind.percent())/100&&p.nPoint.mpMax==ki*(100+kind.percent())/100,"real combat stats gain exactly "+kind.percent()+" percent in all three stats");
    long remaining=TrungThuCakeBuff.state(p).until()-System.currentTimeMillis();check(remaining>595000&&remaining<=600000,"cake duration starts at ten minutes");
    check(p.timers.get((int)Manager.ITEM_TEMPLATES.get(kind.id()).iconID)>0&&p.timers.get((int)Manager.ITEM_TEMPLATES.get(kind.id()).iconID)<=600,"native timer uses the real cake icon and valid seconds");
   }
   User tier=user(c,30);put(tier,465,2);put(tier,466,2);put(tier,472,3);use(tier,465);Thread.sleep(550);use(tier,466);Thread.sleep(550);use(tier,472);
   check(TrungThuCakeBuff.percent(tier)==30&&tier.nPoint.dame==1300,"upgrading 20 to25 to30 keeps only the strongest 30 percent");
   int lowerBefore=qty(tier,465);long until=TrungThuCakeBuff.state(tier).until();use(tier,465);check(qty(tier,465)==lowerBefore&&TrungThuCakeBuff.state(tier).until()==until,"weaker cake cannot consume or extend the stronger buff");
   Thread.sleep(550);use(tier,472);check(TrungThuCakeBuff.percent(tier)==30&&TrungThuCakeBuff.state(tier).until()==until+600000,"same tier adds exactly ten minutes without stacking percent");
   User reconnect=new User((int)tier.id);reconnect.zone=tier.zone;reconnect.nPoint.calPoint();ItemTimeService.gI().sendAllItemTime(reconnect);check(TrungThuCakeBuff.percent(reconnect)==30&&reconnect.nPoint.dame==1300&&reconnect.timers.get(4125)>600,"reconnect restores persisted tier and remaining native timer");
   TrungThuRuntimeTest.enabled(false);TrungThuCakeBuff.tick(tier);check(TrungThuCakeBuff.percent(tier)==0&&tier.nPoint.dame==1000&&tier.timers.values().stream().allMatch(t->t==0),"event OFF stops cake stats and removes icons");
   int offBefore=qty(tier,472);use(tier,472);check(qty(tier,472)==offBefore,"event OFF keeps unused cake");TrungThuRuntimeTest.enabled(true);TrungThuCakeBuff.tick(tier);check(TrungThuCakeBuff.percent(tier)==30&&tier.nPoint.dame==1300,"event ON restores an unexpired saved tier");
   tier.nPoint.hp=tier.nPoint.hpMax;tier.nPoint.mp=tier.nPoint.mpMax;expire(c,tier);TrungThuCakeBuff.tick(tier);check(TrungThuCakeBuff.percent(tier)==0&&tier.nPoint.dame==1000&&tier.nPoint.hp==10000&&tier.nPoint.mp==10000,"expiry restores base stats and clamps HP/KI without healing");
   User failure=user(c,40);put(failure,472,1);try(Statement s=c.createStatement()){s.execute("DROP TRIGGER IF EXISTS qa_cake_failure");s.execute("CREATE TRIGGER qa_cake_failure BEFORE UPDATE ON player FOR EACH ROW BEGIN IF OLD.id="+failure.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected QA cake save failure'; END IF; END");}
   try{use(failure,472);check(qty(failure,472)==1&&TrungThuCakeBuff.percent(failure)==0&&scalar(c,"SELECT COUNT(*) FROM trung_thu_2026_cake WHERE player_id="+failure.id)==0,"SQL failure rolls back cake consumption and saved buff");}finally{try(Statement s=c.createStatement()){s.execute("DROP TRIGGER qa_cake_failure");}}
   nro.models.server.Maintenance.isRunning=true;try{use(failure,472);check(qty(failure,472)==1&&TrungThuCakeBuff.percent(failure)==0,"maintenance cannot consume cake");}finally{nro.models.server.Maintenance.isRunning=false;}
   try(Statement s=c.createStatement()){s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id SET i.cost=21 WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=465");TrungThuShop.ensureData(c);check(scalar(c,"SELECT MAX(i.cost) FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=465")==21,"migration preserves admin-edited cake prices");s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id SET i.cost=20 WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=465");}
   check(scalar(c,"SELECT COUNT(*) FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id WHERE sh.tag_name='TRUNG_THU_2026'")==11,"restarting never duplicates cake rows");
   System.out.println("PASS: "+checks+" Trung Thu cake checks; QA only");
  }catch(Throwable error){error.printStackTrace();System.exit(1);}finally{LocalManager.close();}System.exit(0);
 }
}