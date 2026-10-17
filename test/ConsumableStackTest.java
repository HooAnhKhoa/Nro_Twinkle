package nro.models.services;
import java.sql.*;import java.nio.file.*;import java.util.*;import org.json.simple.*;import org.json.simple.parser.JSONParser;
import nro.models.data.LocalManager;import nro.models.item.Item;import nro.models.player_system.Template.*;import nro.models.server.Manager;
public class ConsumableStackTest{
 static int checks;static void ok(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static Item item(int id,int n,int...opts){Item i=ItemService.gI().createNewItem((short)id,n);for(int k=0;k<opts.length;k+=2)i.itemOptions.add(new Item.ItemOption(opts[k],opts[k+1]));return i;}
 static long qty(List<Item> items,int id){return items.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToLong(i->i.quantity).sum();}
 static long stacks(List<Item> items,int id){return items.stream().filter(i->i.isNotNullItem()&&i.template.id==id).count();}
 static ArrayList<Item> bag(String json)throws Exception{
  var result=new ArrayList<Item>();for(Object x:(JSONArray)new JSONParser().parse(json)){
   JSONArray r=(JSONArray)new JSONParser().parse(x.toString());int id=((Number)r.get(0)).intValue();if(id<0){result.add(new Item());continue;}Item i=item(id,((Number)r.get(1)).intValue());i.createTime=((Number)r.get(3)).longValue();
   for(Object v:(JSONArray)new JSONParser().parse(r.get(2).toString())){JSONArray o=(JSONArray)new JSONParser().parse(v.toString());i.itemOptions.add(new Item.ItemOption(((Number)o.get(0)).intValue(),((Number)o.get(1)).intValue()));}result.add(i);
  }return result;
 }
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   ok("nro_trungthu_rules_20261017".equals(c.getCatalog()),"QA database guard");
   for(int k=0;k<2;k++)for(String sql:Files.readString(Path.of("web/consumable_stack_schema.sql")).split(";"))if(!sql.isBlank())s.execute(sql);
   try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate(r.getShort("id"),r.getByte("TYPE"),r.getByte("gender"),r.getString("NAME"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),r.getBoolean("is_up_to_up"),0));}
   try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
  }
  var inv=InventoryService.gI();int merged=0;
  for(var t:Manager.ITEM_TEMPLATES){var a=item(t.id,2);if(!ItemStacks.stackable(a))continue;var list=new ArrayList<Item>(List.of(a));ok(inv.addItemList(list,item(t.id,3))&&qty(list,t.id)==5&&stacks(list,t.id)==1,"same consumable merges in full bag #"+t.id);merged++;}
  ok(merged>800,"all consumable families covered");
  for(int id:new int[]{63,457,886,887,888,889,890,891,1311,1312,1505,1509,1628,2078,2079,2080}){
   var list=new ArrayList<Item>(List.of(item(id,2),item(id,3,73,0)));ItemStacks.compact(list);ok(stacks(list,id)==1&&qty(list,id)==5,"login merges old plain/dummy-option stacks #"+id);
  }
  var beans=new ArrayList<Item>(List.of(item(63,13,2,8),item(63,13,2,8),item(63,13,2,8),item(63,13,2,8)));ItemStacks.compact(beans);ok(qty(beans,63)==52&&stacks(beans,63)==1&&beans.size()==4,"beuyen bean pattern: 52 retained, three slots freed");
  var ordered=new ArrayList<Item>(List.of(item(381,2,50,20,30,0),item(381,3,30,0,50,20)));ItemStacks.compact(ordered);ok(stacks(ordered,381)==1&&qty(ordered,381)==5,"option order does not prevent same-stack identity");
  var locked=new ArrayList<Item>(List.of(item(886,2),item(886,3,30,0)));ItemStacks.compact(locked);ok(stacks(locked,886)==2,"trade locks stay separate");
  var stats=new ArrayList<Item>(List.of(item(381,2,50,20),item(381,3,50,25)));ItemStacks.compact(stats);ok(stacks(stats,381)==2,"different combat stats stay separate");
  Item timed=item(886,2,93,3),timed2=item(886,3,93,3);timed.createTime=1;var expiry=new ArrayList<Item>(List.of(timed,timed2));ItemStacks.compact(expiry);ok(stacks(expiry,886)==2&&timed.createTime==1,"timed items never refresh or merge expiry");
  var limit=new ArrayList<Item>(List.of(item(886,99998),item(886,5)));ItemStacks.compact(limit);ok(limit.get(0).quantity==99999&&limit.get(1).quantity==4,"stack cap preserves remainder");
  var full=new ArrayList<Item>(List.of(item(886,99998)));Item incoming=item(886,5);ok(!inv.addItemList(full,incoming)&&full.get(0).quantity==99998&&incoming.quantity==5,"insufficient capacity never partially mutates a stack");
  var split=new ArrayList<Item>(List.of(new Item(),new Item()));ok(inv.addItemList(split,item(886,100004))&&split.get(0).quantity==99999&&split.get(1).quantity==5,"oversized reward safely splits across free slots");
  for(int id:new int[]{0,5,892,1884,735,1885}){var list=new ArrayList<Item>(List.of(item(id,1),item(id,1)));ItemStacks.compact(list);ok(stacks(list,id)==2,"equipment/pet/title stays separate #"+id);}
  // Guard against the old generic option-1 accumulator changing equipment stats.
  var gear=new ArrayList<Item>(List.of(item(0,1,1,30),new Item()));ok(inv.addItemList(gear,item(0,1,1,50))&&gear.get(0).itemOptions.get(0).param==30&&stacks(gear,0)==2,"equipment option1 is not accumulated as a counter");
  var legacy=new ArrayList<Item>(List.of(item(933,1,31,9000),item(933,1,31,1000)));ItemStacks.compact(legacy);ok(stacks(legacy,933)==1&&legacy.get(0).quantity==1&&legacy.get(0).getOptionParam(31)==10000,"legacy Porata counter totals preserved");
  var tdlt=new ArrayList<Item>(List.of(item(521,1,1,300),item(521,1,1,500)));ItemStacks.compact(tdlt);ok(stacks(tdlt,521)==1&&tdlt.get(0).getOptionParam(1)==800,"TDLT minute counter preserved");
  var boundCounter=new ArrayList<Item>(List.of(item(933,1,31,1),item(933,1,31,1,30,0)));ItemStacks.compact(boundCounter);ok(stacks(boundCounter,933)==2,"legacy counters cannot erase trade locks");
  if(Files.exists(Path.of("beuyen-stacks.json"))){JSONObject snapshot=(JSONObject)new JSONParser().parse(Files.readString(Path.of("beuyen-stacks.json")));var actual=bag((String)snapshot.get("items_bag"));long before=qty(actual,63);int size=actual.size();ItemStacks.compact(actual);ok(before>0&&qty(actual,63)==before&&stacks(actual,63)==1&&actual.size()==size,"actual read-only beuyen snapshot replay preserves current quantity and bag capacity");}
  System.out.println("PASS "+checks+" stack checks");System.exit(0);
 }
}
