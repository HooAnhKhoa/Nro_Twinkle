package nro.models.event.trungthu2026;

import java.sql.*;
import java.io.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.server.Manager;
import nro.models.player_system.Template.*;
import nro.models.shop.*;
import nro.models.services.ItemService;

/** Exercises the real shop packet and buy route against the isolated QA database. */
public class PointShopTest {
 static int checks;static final int BASE=2026102200;
 static void check(boolean value,String text){if(!value)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class User extends TrungThuRuntimeTest.User {
  byte[] packet;String notice;
  User(int id){super(id);session.userId=1;event.setEventPoint(20000);inventory.gem=500;for(int n=0;n<5;n++)inventory.itemsBag.add(new Item());}
  @Override public void sendMessage(Message m){if(m.command==-44)packet=m.getData();if(m.command==-25)try{notice=new DataInputStream(new ByteArrayInputStream(m.getData())).readUTF();}catch(Exception ignored){}}
 }
 static User user(Connection c,int n)throws Exception{User p=new User(BASE+n);try(var q=c.prepareStatement("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task,event_point) VALUES(?,?,1,0,'[0,0,0,0]','[]','[0,500,0,0,0]','[-1,0,0,0,20,0]',20000) ON DUPLICATE KEY UPDATE event_point=20000,account_id=1")){q.setLong(1,p.id);q.setString(2,"Point QA "+n);q.executeUpdate();}return p;}
 static long scalar(Connection c,String sql)throws Exception{try(var q=c.createStatement();var r=q.executeQuery(sql)){r.next();return r.getLong(1);}}
 static long qty(User p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToLong(i->i.quantity).sum();}
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
   try(var q=c.createStatement();var r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){var t=new ItemTemplate((short)r.getInt("id"),r.getByte("TYPE"),r.getByte("gender"),r.getString("NAME"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),false,0);t.head=r.getShort("head");t.body=r.getShort("body");t.leg=r.getShort("leg");Manager.ITEM_TEMPLATES.add(t);}}
   try(var q=c.createStatement();var r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   var shop=new Shop();shop.tagName="SHOP_DOI_DIEM";shop.typeShop=3;var tab=new TabShop();tab.id=59;tab.name="Đổi thưởng";tab.shop=shop;shop.tabShops.add(tab);
   var sale=new ItemShop();sale.id=90000;sale.temp=Manager.ITEM_TEMPLATES.get(1632);sale.cost=10000;sale.iconSpec=100;sale.tabShop=tab;sale.options.add(new Item.ItemOption(50,25));tab.itemShops.add(sale);Manager.SHOPS.add(shop);
   User p=user(c,1);ShopService.gI().opendShop(p,shop.tagName,true);
   try(var in=new DataInputStream(new ByteArrayInputStream(p.packet))){check(in.readUnsignedByte()==3&&in.readUnsignedByte()==1,"native point shop packet");in.readUTF();check(in.readUnsignedByte()==1&&in.readShort()==1632,"new Himmel costume is shown");in.readShort();check(in.readInt()==10000,"native displayed price is configured 10000 points");}
   ShopService.gI().takeItem(p,(byte)0,1632);check(qty(p,1632)==1&&p.event.getEventPoint()==10000&&p.inventory.gem==500,"real buy route charges configured points only");
   check(scalar(c,"SELECT event_point FROM player WHERE id="+p.id)==10000&&scalar(c,"SELECT JSON_LENGTH(items_bag) FROM player WHERE id="+p.id)==5,"reward and remaining points persisted together");
   sale.cost=1234;User changed=user(c,2);ShopService.gI().opendShop(changed,shop.tagName,true);ShopService.gI().takeItem(changed,(byte)0,1632);check(changed.event.getEventPoint()==18766&&qty(changed,1632)==1,"changed price is not tied to template ID");
   sale.temp=Manager.ITEM_TEMPLATES.get(1567);sale.cost=4321;User old=user(c,3);ShopService.gI().opendShop(old,shop.tagName,true);ShopService.gI().takeItem(old,(byte)0,1567);check(old.event.getEventPoint()==15679,"old costume also honors edited price rather than hardcoded 999");
   User poor=user(c,4);poor.event.setEventPoint(1);check(!PointShop.buy(poor,sale).success()&&poor.event.getEventPoint()==1&&qty(poor,1567)==0,"insufficient points cannot buy");
   User full=user(c,5);for(int n=0;n<full.inventory.itemsBag.size();n++)full.inventory.itemsBag.set(n,ItemService.gI().createNewItem((short)888,1));check(!PointShop.buy(full,sale).success()&&full.event.getEventPoint()==20000&&scalar(c,"SELECT event_point FROM player WHERE id="+full.id)==20000,"full bag preserves memory and saved points");
   User failed=user(c,6);try(var q=c.createStatement()){q.execute("DROP TRIGGER IF EXISTS qa_point_failure");q.execute("CREATE TRIGGER qa_point_failure BEFORE UPDATE ON player FOR EACH ROW BEGIN IF OLD.id="+failed.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected QA point rollback'; END IF; END");}
   try{check(!PointShop.buy(failed,sale).success()&&failed.event.getEventPoint()==20000&&qty(failed,1567)==0&&scalar(c,"SELECT event_point FROM player WHERE id="+failed.id)==20000,"failed SQL restores reward and points");}finally{try(var q=c.createStatement()){q.execute("DROP TRIGGER qa_point_failure");}}
   check(PointShop.buy(failed,sale).success(),"retry after database recovery succeeds");
   sale.cost=-1;check(!PointShop.buy(failed,sale).success(),"negative price cannot grant points");
   sale.cost=0;check(PointShop.buy(failed,sale).success()&&failed.event.getEventPoint()==15679,"explicitly configured free item remains free");
   System.out.println("PASS: "+checks+" point shop checks");
  }catch(Throwable error){error.printStackTrace();System.exit(1);}finally{LocalManager.close();}System.exit(0);
 }
}
