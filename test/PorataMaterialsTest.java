package nro.models.combine;
import java.sql.*;import java.util.*;import java.io.*;
import nro.models.data.LocalManager;import nro.models.item.Item;import nro.models.player.Player;import nro.models.player_system.Template.*;import nro.models.server.Manager;import nro.models.services.*;import nro.models.network.*;import nro.models.npc.Npc;import nro.models.consts.ConstNpc;
public class PorataMaterialsTest {
 static int checks;static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;System.out.println("PASS: "+msg);}
 static class Socket extends java.net.Socket {
  public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("127.0.0.1",1);}public java.net.InetAddress getInetAddress(){return java.net.InetAddress.getLoopbackAddress();}public InputStream getInputStream(){return new ByteArrayInputStream(new byte[0]);}public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
 }
 static class User extends Player {byte[] menu;User(){id=12345;isPlayer=true;name="Porata QA";session=new MySession(new Socket());session.version=220;for(int i=0;i<12;i++)inventory.itemsBag.add(new Item());inventory.gold=8600000000L;inventory.gem=83410;}public void sendMessage(Message m){if(m.command==32)menu=m.getData();}}
 static Item item(int id,int qty){Item i=ItemService.gI().createNewItem((short)id);i.quantity=qty;return i;}
 static User user(int level,int qty,boolean legacy){User p=new User();Item e=item(level==2?454:921,1),m=item(level==2?933:1820,legacy?1:qty);if(legacy)m.itemOptions.add(new Item.ItemOption(31,qty));p.inventory.itemsBag.set(0,e);p.inventory.itemsBag.set(1,m);p.combineNew.itemsCombine.add(e);p.combineNew.itemsCombine.add(m);return p;}
 static void show(User p,int level){if(level==2)NangCapBongTai.showInfoCombine(p);else NangCapBongTai3.showInfoCombine(p);}
 static void upgrade(User p,int level,boolean success){if(level==2)NangCapBongTai.upgrade(p,success);else NangCapBongTai3.upgrade(p,success);}
 public static void main(String[] args)throws Exception{try{
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
   try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){ItemTemplate t=new ItemTemplate(r.getShort("id"),r.getByte("type"),r.getByte("gender"),r.getString("name"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),r.getBoolean("is_up_to_up"),0);t.level=r.getByte("level");Manager.ITEM_TEMPLATES.add(t);}}
   try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("name"),0));}
  }
  Manager.NPCS.add(new Npc(5,1,0,0,ConstNpc.BA_HAT_MIT,0){public void confirmMenu(Player p,int select){}});
  for(int level:new int[]{2,3})for(boolean legacy:new boolean[]{false,true}){
   int id=level==2?933:1820,need=level==2?9999:20000,fail=level==2?99:200;
   User p=user(level,99999,legacy);show(p,level);DataInputStream menu=new DataInputStream(new ByteArrayInputStream(p.menu));menu.readShort();String text=menu.readUTF();check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE&&!text.contains("Còn thiếu"),"real menu accepts 99999 material: level "+level+", legacy "+legacy);
   upgrade(p,level,true);check(p.inventory.itemsBag.get(0).template.id==(level==2?921:1819)&&PorataMaterials.count(p,id)==99999-need&&p.inventory.gold==8400000000L&&p.inventory.gem==82410,"success deducts exact material/gold/gem: level "+level+", legacy "+legacy);
   p=user(level,99999,legacy);upgrade(p,level,false);check(p.inventory.itemsBag.get(0).template.id==(level==2?454:921)&&PorataMaterials.count(p,id)==99999-fail&&p.inventory.gold==8400000000L,"failure deducts only fail material plus fee: level "+level+", legacy "+legacy);
   p=user(level,need-1,legacy);upgrade(p,level,true);check(PorataMaterials.count(p,id)==need-1&&p.inventory.gold==8600000000L&&p.inventory.gem==83410,"insufficient material cannot bypass confirmation: level "+level+", legacy "+legacy);
  }
  User p=user(2,9999,false);p.inventory.itemsBag.set(1,item(933,4000));p.combineNew.itemsCombine.set(1,p.inventory.itemsBag.get(1));p.inventory.itemsBag.set(2,item(933,5999));check(PorataMaterials.count(p,933)==9999,"counts multiple quantity stacks");upgrade(p,2,true);check(PorataMaterials.count(p,933)==0&&p.combineNew.itemsCombine.size()==1,"success consumes across slots and removes exhausted selection");
  p=user(2,9999,false);p.inventory.itemsBag.set(1,new Item());upgrade(p,2,true);check(p.inventory.gold==8600000000L&&p.inventory.itemsBag.get(0).template.id==454,"stale selected item rejects without charging");
  p=user(2,99999,false);p.inventory.gem=999;upgrade(p,2,true);check(PorataMaterials.count(p,933)==99999&&p.inventory.gold==8600000000L,"insufficient gems keeps materials and gold");
  p=user(2,99999,false);p.inventory.gold=199999999;upgrade(p,2,true);check(PorataMaterials.count(p,933)==99999&&p.inventory.gem==83410,"insufficient gold keeps materials and gems");
  p=user(2,99999,false);Item drop=item(933,1);drop.itemOptions.add(new Item.ItemOption(31,1));check(InventoryService.gI().addItemList(p.inventory.itemsBag,drop)&&PorataMaterials.count(p,933)==100000,"actual legacy drop is not lost beside quantity stack");
  Item next=item(933,1);next.itemOptions.add(new Item.ItemOption(31,1));check(InventoryService.gI().addItemList(p.inventory.itemsBag,next)&&PorataMaterials.count(p,933)==100001,"later legacy drops merge into correct legacy slot");
  for(boolean legacy:new boolean[]{false,true}){p=user(2,1,false);Item soul=item(934,legacy?1:250);if(legacy)soul.itemOptions.add(new Item.ItemOption(31,250));p.inventory.itemsBag.set(3,soul);check(PorataMaterials.consume(p,934,200)&&PorataMaterials.count(p,934)==50,"soul material supports current and legacy representation: "+legacy);check(!PorataMaterials.consume(p,934,51)&&PorataMaterials.count(p,934)==50,"insufficient consume is all-or-nothing: "+legacy);}
  for(int level:new int[]{2,3})for(boolean legacy:new boolean[]{false,true}){
   p=new User();Item e=item(level==2?921:1819,1),soul=item(934,legacy?1:250),stone=item(935,1);if(legacy)soul.itemOptions.add(new Item.ItemOption(31,250));p.inventory.itemsBag.set(0,e);p.inventory.itemsBag.set(1,soul);p.inventory.itemsBag.set(2,stone);p.combineNew.itemsCombine.addAll(List.of(e,soul,stone));
   if(level==2)NangChiSoBongTai.showInfoCombine(p);else NangChiSoBongTai3.showInfoCombine(p);
   check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE,"actual soul menu accepts material: level "+level+", legacy "+legacy);
   if(level==2)NangChiSoBongTai.nangChiSoBongTai(p);else NangChiSoBongTai3.nangChiSoBongTai(p);
   check(PorataMaterials.count(p,934)==(level==2?50:151)&&p.inventory.gem==82410&&!p.inventory.itemsBag.get(2).isNotNullItem(),"actual stat upgrade consumes exact soul, stone, gems: level "+level+", legacy "+legacy);
  }
  System.out.println("PASS: "+checks+" Porata checks");
 }finally{LocalManager.close();}System.exit(0);}
}
