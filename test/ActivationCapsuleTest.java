package nro.models.event.trungthu2026;
import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.network.Message;
public class ActivationCapsuleTest {
 static int checks;
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 static void load(Connection c)throws Exception{
  try(Statement q=c.createStatement()){
   try(ResultSet r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),false,r.getInt("power_require")));}
   try(ResultSet r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   var shop=new nro.models.shop.Shop();var tab=new nro.models.shop.TabShop();shop.tabShops.add(tab);Manager.SHOPS.add(shop);
   try(ResultSet r=q.executeQuery("SELECT i.id,i.temp_id,o.option_id,o.param FROM srcnrofree.item_shop i JOIN srcnrofree.item_shop_option o ON o.item_shop_id=i.id WHERE i.temp_id BETWEEN 0 AND 281 ORDER BY i.id")){
    Map<Integer,nro.models.shop.ItemShop> sales=new HashMap<>();while(r.next()){var sale=sales.get(r.getInt(1));if(sale==null){sale=new nro.models.shop.ItemShop();sale.temp=Manager.ITEM_TEMPLATES.get(r.getInt(2));sales.put(r.getInt(1),sale);tab.itemShops.add(sale);}sale.options.add(new Item.ItemOption(r.getInt(3),r.getInt(4)));}
   }
  }
 }
 static List<Item> gear(TrungThuRuntimeTest.User p){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.type<=4).toList();}
 static void validate(Item item,int gender){ok(item.template.gender==gender||item.template.gender==3,"reward matches race "+gender);int[][] sets={{128,129,127,233,245},{130,131,132,233,237},{133,135,134,233,241}};int set=item.itemOptions.stream().mapToInt(o->o.optionTemplate.id).filter(id->Arrays.stream(sets[gender]).anyMatch(v->v==id)).findFirst().orElse(-1);ok(set>=0,"reward carries activation set");for(int id:ItemService.gI().getOptionIdsBySKH(set))ok(item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==id),"reward carries set effect "+id);}
 public static void main(String[] args)throws Exception{
  try{
   List<TrungThuRuntimeTest.User> users=new ArrayList<>();
   try(Connection c=LocalManager.getConnection()){ok("nro_trungthu_rules_20261017".equals(c.getCatalog()),"QA DB guard");try(Statement q=c.createStatement()){q.executeUpdate("DELETE FROM player WHERE id BETWEEN 2026101690 AND 2026101697");q.execute("DROP TRIGGER IF EXISTS qa_capsule_save_fail");}load(c);for(int n=90;n<=97;n++)users.add(TrungThuRuntimeTest.user(c,n,20));}
   for(int gender=0;gender<3;gender++){
    var p=users.get(gender);p.gender=(byte)gender;p.inventory.itemsBag.set(0,TrungThuRuntimeTest.item(1559,6));
    for(int type=0;type<5;type++){ItemService.gI().OpenSKH(p,1559,type);Item reward=gear(p).get(type);ok(reward.template.type==type,"one-piece type "+type);validate(reward,gender);}
    {Message msg=new Message((byte)-43,new byte[]{0,1,0});try{UseItem.gI().doItem(p,msg);}finally{msg.cleanup();}}
    ok(TrungThuRuntimeTest.qty(p,1559)==0&&gear(p).size()==6,"native UseItem packet opens canonical 1559");
    {Message msg=new Message((byte)-43,new byte[]{0,1,0});try{UseItem.gI().doItem(p,msg);}finally{msg.cleanup();}}ok(gear(p).size()==6,"empty capsule slot cannot duplicate gear");
    try(Connection c=LocalManager.getConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT items_bag FROM player WHERE id="+p.id)){r.next();ok(r.getString(1).equals(TrungThuStore.bag(p)),"reward and consumption persisted together");}
   }
   var chosen=users.get(3);chosen.inventory.itemsBag.set(0,TrungThuRuntimeTest.item(1655,1));ItemService.gI().OpenSKH(chosen,1655,2);ok(gear(chosen).size()==1&&gear(chosen).get(0).template.type==2&&TrungThuRuntimeTest.qty(chosen,1655)==0,"self-select capsule still grants selected gloves");
   var full=users.get(4);full.inventory.itemsBag.clear();full.inventory.itemsBag.add(TrungThuRuntimeTest.item(1559,2));full.inventory.itemsBag.add(TrungThuRuntimeTest.item(457,1));String before=TrungThuStore.bag(full);ItemService.gI().OpenSKH(full,1559,0);ok(before.equals(TrungThuStore.bag(full)),"full bag preserves stacked capsules");full.inventory.itemsBag.get(0).quantity=1;ItemService.gI().OpenSKH(full,1559,0);ok(gear(full).size()==1&&TrungThuRuntimeTest.qty(full,1559)==0,"last capsule frees its own reward slot");
   var failure=users.get(5);failure.inventory.itemsBag.set(0,TrungThuRuntimeTest.item(1559,1));TrungThuStore.persistBag(failure);before=TrungThuStore.bag(failure);
   try(Connection c=LocalManager.getConnection();Statement q=c.createStatement()){q.execute("CREATE TRIGGER qa_capsule_save_fail BEFORE UPDATE ON player FOR EACH ROW BEGIN IF NEW.id="+failure.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='expected capsule save failure'; END IF; END");}
   try{ItemService.gI().OpenSKH(failure,1559,0);ok(before.equals(TrungThuStore.bag(failure)),"save failure rolls back capsule and reward");}finally{try(Connection c=LocalManager.getConnection();Statement q=c.createStatement()){q.execute("DROP TRIGGER qa_capsule_save_fail");}}
   ItemService.gI().OpenSKH(failure,1559,0);ok(gear(failure).size()==1&&TrungThuRuntimeTest.qty(failure,1559)==0,"retry succeeds once after save recovers");
   System.out.println("PASS: "+checks+" activation capsule checks");
  }finally{LocalManager.close();}
  System.exit(0);
 }
}
