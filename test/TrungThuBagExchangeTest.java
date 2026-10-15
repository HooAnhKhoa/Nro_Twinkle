import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import nro.models.event.trungthu2026.BagExchange;
import nro.models.event.trungthu2026.BagExchange.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;

public class TrungThuBagExchangeTest {
 static int checks;
 static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static Item item(int id,int qty){return ItemService.gI().createNewItem((short)id,qty);}
 static Player player(Item... items){Player p=new Player();p.inventory.gold=40000000;p.inventory.gem=40;p.inventory.itemsBag.addAll(Arrays.asList(items));return p;}
 static int quantity(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 public static void main(String[] args)throws Exception{
  for(int id=0;id<2001;id++)Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)id,(byte)27,(byte)3,"QA "+id,"",(short)0,(short)0,true,0));
  for(int id=0;id<300;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
  AtomicLong now=new AtomicLong();BagExchange exchange=new BagExchange(500,now::get);Cost cost=new Cost(Map.of(888,10),20000000,0);
  Player split=player(item(888,6),item(888,4));Item prototype=item(890,1);check(exchange.execute(split,cost,List.of(prototype))==Result.SUCCESS&&quantity(split,888)==0&&quantity(split,890)==1&&split.inventory.gold==20000000,"ingredients across stacks are consumed exactly once");check(prototype.quantity==1,"reward prototype is not mutated by inventory insertion");
  Player lastSlot=player(item(888,10));check(exchange.execute(lastSlot,cost,List.of(item(890,1)))==Result.SUCCESS&&quantity(lastSlot,890)==1,"consuming the last ingredient frees a slot for the result");
  Player shortItems=player(item(888,9),new Item());check(exchange.execute(shortItems,cost,List.of(item(890,1)))==Result.MISSING_ITEMS&&quantity(shortItems,888)==9&&shortItems.inventory.gold==40000000,"insufficient quantity cannot charge money");
  Player poor=player(item(888,10));poor.inventory.gold=19999999;check(exchange.execute(poor,cost,List.of(item(890,1)))==Result.MISSING_GOLD&&quantity(poor,888)==10,"insufficient gold preserves ingredients");Cost gemCost=new Cost(Map.of(888,10),0,20);poor.inventory.gem=19;check(exchange.execute(poor,gemCost,List.of(item(890,1)))==Result.MISSING_GEMS&&quantity(poor,888)==10,"insufficient gems preserve ingredients");
  Item merge=item(220,99998);merge.itemOptions.add(new Item.ItemOption(73,0));merge.id=421;merge.expire=123456L;
  Player partial=player(item(888,20),merge,item(14,1));check(exchange.execute(partial,cost,List.of(item(220,2)))==Result.BAG_FULL&&quantity(partial,220)==99998&&quantity(partial,888)==20&&partial.inventory.gold==40000000,"partial stack insertion is fully rolled back");Item restored=InventoryService.gI().findItemBag(partial,220);check(restored.id==421&&restored.expire==123456L,"rollback preserves item identity and expiry metadata");
  Player bonus=player(item(888,20),new Item());check(exchange.execute(bonus,gemCost,List.of(item(890,1),item(891,1)))==Result.BAG_FULL&&quantity(bonus,890)==0&&quantity(bonus,891)==0&&quantity(bonus,888)==20&&bonus.inventory.gem==40,"failed bonus output rolls back all rewards, materials and gems");
  Player spam=player(item(888,20),new Item(),new Item());List<Result> results=Collections.synchronizedList(new ArrayList<>());CountDownLatch start=new CountDownLatch(1);Runnable call=()->{try{start.await();results.add(exchange.execute(spam,cost,List.of(item(890,1))));}catch(Exception e){throw new RuntimeException(e);}};Thread a=new Thread(call),b=new Thread(call);a.start();b.start();start.countDown();a.join();b.join();check(Collections.frequency(results,Result.SUCCESS)==1&&Collections.frequency(results,Result.COOLDOWN)==1&&quantity(spam,890)==1&&quantity(spam,888)==10&&spam.inventory.gold==20000000,"simultaneous requests cannot bypass click cooldown or double-spend");now.addAndGet(500);check(exchange.execute(spam,cost,List.of(item(890,1)))==Result.SUCCESS&&quantity(spam,890)==2,"next valid request succeeds after configured cooldown");
  boolean rejected=false;try{new Cost(Map.of(888,-1),0,0);}catch(IllegalArgumentException expected){rejected=true;}check(rejected,"negative quantities rejected before transaction");
  Player bad=player(item(888,10),new Item());check(exchange.execute(bad,cost,List.of(item(890,100000)))==Result.INVALID_REWARD&&quantity(bad,888)==10,"invalid oversized reward cannot consume inputs");
  System.out.println("PASS: "+checks+" safe exchange checks; no database or server started");
 }
}
