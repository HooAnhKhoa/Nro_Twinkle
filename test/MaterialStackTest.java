package nro.models.services;
import java.sql.*;import java.nio.file.*;import java.util.*;
import nro.models.data.LocalManager;import nro.models.item.Item;import nro.models.player_system.Template.*;import nro.models.server.Manager;

/** Guarded QA templates and real inventory operations; never edits player inventories in production. */
public class MaterialStackTest {
 static int checks;static void check(boolean b,String text){if(!b)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static Item item(int id,int qty){return ItemService.gI().createNewItem((short)id,qty);}
 static long total(List<Item> items,int id){return items.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToLong(i->i.quantity).sum();}
 static long stacks(List<Item> items,int id){return items.stream().filter(i->i.isNotNullItem()&&i.template.id==id).count();}
 public static void main(String[] args)throws Exception {
  Map<Integer,Boolean> before=new HashMap<>();
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
   try(ResultSet r=s.executeQuery("SELECT id,is_up_to_up FROM item_template WHERE id IN(1505,1507,1508,1509)")){while(r.next())before.put(r.getInt(1),r.getBoolean(2));}
   try{
    s.executeUpdate("UPDATE item_template SET is_up_to_up=0 WHERE id IN(1505,1509)");
    for(int run=0;run<2;run++)for(String sql:Files.readString(Path.of("../../web/material_stack_schema.sql")).split(";"))if(!sql.isBlank())s.execute(sql);
    try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate(r.getShort("id"),r.getByte("type"),r.getByte("gender"),r.getString("name"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),r.getBoolean("is_up_to_up"),0));}
    try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("name"),0));}
    check(item(1505,1).template.isUpToUp&&item(1509,1).template.isUpToUp,"idempotent migration enables both materials");
    check(item(1507,1).template.isUpToUp==before.get(1507)&&item(1508,1).template.isUpToUp==before.get(1508),"other event material settings remain unchanged");
    var inventory=InventoryService.gI();var bag=new ArrayList<Item>();bag.add(item(1505,2));Item unrelated=item(190,1);bag.add(unrelated);Item paper=item(1505,3);paper.itemOptions.add(new Item.ItemOption(73,0));bag.add(paper);bag.add(item(1509,4));bag.add(item(1509,7));
    inventory.compactEventMaterialStacks(bag);check(total(bag,1505)==5&&stacks(bag,1505)==1&&bag.size()==5,"old paper stacks consolidate without changing quantity or capacity");check(total(bag,1509)==11&&stacks(bag,1509)==1,"old bow stacks consolidate");check(bag.get(1)==unrelated&&!bag.get(2).isNotNullItem()&&!bag.get(4).isNotNullItem(),"unrelated slots stay fixed and consumed slots become empty");
    inventory.compactEventMaterialStacks(bag);check(total(bag,1505)==5&&total(bag,1509)==11&&stacks(bag,1505)==1,"repeated login consolidation is idempotent");
    for(int id:new int[]{1505,1509}){
     var full=new ArrayList<Item>(List.of(item(id,10)));inventory.compactEventMaterialStacks(full);check(inventory.addItemList(full,item(id,5))&&total(full,id)==15&&full.size()==1,"new material "+id+" stacks even when no empty slot exists");
     var near=new ArrayList<Item>(List.of(item(id,99998),item(id,5)));inventory.compactEventMaterialStacks(near);check(near.get(0).quantity==99999&&near.get(1).quantity==4&&total(near,id)==100003,"material "+id+" keeps remainder beyond stack limit");
    }
    Item locked=item(1505,5);locked.itemOptions.add(new Item.ItemOption(30,0));var locks=new ArrayList<Item>(List.of(item(1505,3),locked));inventory.compactEventMaterialStacks(locks);check(stacks(locks,1505)==2&&total(locks,1505)==8,"trade-locked paper is not merged into unlocked paper");
    Item timed=item(1509,2);timed.itemOptions.add(new Item.ItemOption(93,3));Item timed2=ItemService.gI().copyItem(timed);timed2.createTime=1;var dates=new ArrayList<Item>(List.of(timed,timed2));inventory.compactEventMaterialStacks(dates);check(stacks(dates,1509)==2&&total(dates,1509)==4,"special expiration items retain their separate lifetimes");
    var others=new ArrayList<Item>(List.of(item(1507,2),item(1507,3)));inventory.compactEventMaterialStacks(others);check(stacks(others,1507)==2,"login consolidation changes only the two requested item IDs");
    var box=new ArrayList<Item>(List.of(item(1505,1),item(1505,9)));inventory.compactEventMaterialStacks(box);check(stacks(box,1505)==1&&total(box,1505)==10,"storage box list uses the same safe consolidation");
    System.out.println("PASS: "+checks+" material stack checks");
   }finally{try(PreparedStatement q=c.prepareStatement("UPDATE item_template SET is_up_to_up=? WHERE id=?")){for(var row:before.entrySet()){q.setBoolean(1,row.getValue());q.setInt(2,row.getKey());q.addBatch();}q.executeBatch();}}
  }finally{LocalManager.close();}System.exit(0);
 }
}
