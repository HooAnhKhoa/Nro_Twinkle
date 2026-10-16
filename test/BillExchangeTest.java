package nro.models.shop;
import java.sql.*;import java.util.*;
import nro.models.data.LocalManager;import nro.models.item.Item;import nro.models.player.Player;import nro.models.player_system.Template.*;import nro.models.server.Manager;import nro.models.services.*;import nro.models.network.*;import nro.models.npc_list.Bill;
public class BillExchangeTest {
 static int checks;static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);checks++;System.out.println("PASS: "+msg);}
 static class User extends Player {User(){id=12345;name="Bill QA";isPlayer=true;for(int i=0;i<10;i++)inventory.itemsBag.add(new Item());for(int i=0;i<11;i++)inventory.itemsBody.add(new Item());int[] ids={555,556,562,563,561};for(int i=0;i<5;i++)inventory.itemsBody.set(i,item(ids[i],1));inventory.gold=3000000000L;inventory.gem=inventory.ruby=inventory.coupon=100;var m=new nro.models.map.Map(48,"Bill QA",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[24][60],new int[]{0},1,20,new ArrayList<>());zone=m.zones.get(0);}@Override public void sendMessage(Message m){}}
 static Item item(int id,int qty){Item i=ItemService.gI().createNewItem((short)id);i.quantity=qty;return i;}
 static User user(){User p=new User();p.inventory.itemsBag.set(0,item(663,50));p.inventory.itemsBag.set(1,item(663,49));return p;}
 static long count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToLong(i->i.quantity).sum();}
 static ItemShop price(int type,int cost){ItemShop i=new ItemShop();i.temp=ItemService.gI().getTemplate((short)650);i.typeSell=(byte)type;i.cost=cost;return i;}
 static String buy(Player p,ItemShop price,nro.models.event.trungthu2026.BagExchange.Commit save){return ShopService.gI().exchangeBillItem(p,price,item(650,1),save);}
 public static void main(String[] args)throws Exception{try{
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
   try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){ItemTemplate t=new ItemTemplate(r.getShort("id"),r.getByte("type"),r.getByte("gender"),r.getString("name"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),r.getBoolean("is_up_to_up"),0);t.level=r.getByte("level");Manager.ITEM_TEMPLATES.add(t);}}
   try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("name"),0));}
  }
  User p=user();check(InventoryService.gI().canOpenBillShop(p),"99 same food split 50+49 opens Bill");
  Shop shop=new Shop();shop.tagName="BILL";shop.typeShop=0;TabShop tab=new TabShop();tab.id=1;tab.name="Hủy Diệt";tab.itemShops.add(price(0,800000000));shop.tabShops.add(tab);Manager.SHOPS.add(shop);
  p.idMark.setIndexMenu(100);new Bill(48,1,0,0,55,0).confirmMenu(p,0);check("BILL".equals(p.idMark.getTagNameShop()),"actual confirmation menu 100 OK opens BILL shop");
  int[] saved={0};check(buy(p,price(0,800000000),q->saved[0]++)==null&&count(p,663)==0&&count(p,650)==1&&p.inventory.gold==2200000000L&&saved[0]==1,"exchange grants one item, consumes 99 across slots and exact gold, saves once");
  long g=p.inventory.gold;check(buy(p,price(0,800000000),q->{})!=null&&p.inventory.gold==g&&count(p,650)==1,"repeat purchase without food charges nothing");
  p=user();p.inventory.itemsBag.set(1,item(664,49));check(!InventoryService.gI().x99ThucAn(p)&&buy(p,price(0,1),q->{})!=null&&p.inventory.gold==3000000000L,"different food types do not combine into 99");
  p=user();p.inventory.itemsBody.set(4,new Item());check(buy(p,price(0,1),q->{})!=null&&count(p,663)==99,"removing God ring after opening shop rejects without loss");
  p=user();p.inventory.itemsBody.clear();check(!InventoryService.gI().fullSetThan(p),"unloaded equipment list does not throw");
  p=user();p.zone=null;check(buy(p,price(0,1),q->{})!=null&&count(p,663)==99,"cannot buy away from Bill");
  p=user();p.inventory.gold=1;check(buy(p,price(0,800000000),q->{})!=null&&count(p,663)==99&&count(p,650)==0,"insufficient gold keeps all food");
  p=user();p.inventory.itemsBag.set(0,item(663,100));p.inventory.itemsBag.set(1,item(663,100));for(int j=2;j<10;j++)p.inventory.itemsBag.set(j,item(650,1));check(buy(p,price(0,1),q->{})!=null&&count(p,663)==200&&p.inventory.gold==3000000000L,"full bag restores food and gold");
  p=user();p.inventory.itemsBag.clear();p.inventory.itemsBag.add(item(663,99));check(buy(p,price(0,1),q->{})==null&&count(p,650)==1,"consumed food slot can hold equipment when bag was full");
  for(int type:new int[]{0,1,3,4}){p=user();check(buy(p,price(type,20),q->{throw new SQLException("intentional QA save failure");})!=null&&count(p,663)==99&&count(p,650)==0&&p.inventory.gold==3000000000L&&p.inventory.gem==100&&p.inventory.ruby==100&&p.inventory.coupon==100,"save failure rolls back food/item/all currencies, type "+type);}
  for(int type:new int[]{1,3,4}){p=user();check(buy(p,price(type,20),q->{})==null&&(type==1?p.inventory.gem:type==3?p.inventory.ruby:p.inventory.coupon)==80,"supported currency charges exactly once, type "+type);}
  p=user();p.inventory.itemsBag.set(0,item(663,98));p.inventory.itemsBag.set(1,new Item());check(!InventoryService.gI().x99ThucAn(p),"98 food is insufficient");
  System.out.println("PASS: "+checks+" Bill checks");
 }finally{LocalManager.close();}System.exit(0);}
}
