package nro.models.services_func;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.event.trungthu2026.*;
/** Five exclusive teacher rewards; identical pools for ordinary and VIP boxes. */
public final class TeachersDayBox {
 private static final BagExchange EXCHANGE=new BagExchange(500,System::currentTimeMillis);
 public static boolean handles(int id){return id==1739||id==1944;}
 public static Item reward(int choice,int gender){
  if(choice<0||choice>=5||gender<0||gender>2)throw new IllegalArgumentException("Teacher reward selection");
  int id=switch(choice){case 0->1362;case 1->1937;case 2->1389+gender;case 3->1384;default->1383;};
  Item item=ItemService.gI().createNewItem((short)id,1);
  int[][] stats=switch(choice){
   case 0->new int[][]{{50,22},{77,22},{103,22},{94,5},{222,10},{93,15}};
   case 1->new int[][]{{50,25},{5,20},{204,25},{117,20},{30,0}};
   case 2->new int[][]{{50,24},{77,23},{103,23},{101,25},{93,15}};
   case 3->new int[][]{{50,23},{77,23},{103,23},{94,7},{108,10},{24,0},{93,7}};
   default->new int[][]{{50,22},{77,23},{103,22},{97,5},{108,10},{160,25},{93,7}};
  };
  for(int[] stat:stats)item.itemOptions.add(new Item.ItemOption(stat[0],stat[1]));return item;
 }
 public static boolean use(Player p,Item box){
  if(box==null||!box.isNotNullItem()||!handles(box.template.id))return false;
  synchronized(p){
   if(!p.isPl()||nro.models.server.Maintenance.isRunning||box.quantity<1||!p.inventory.itemsBag.contains(box))return true;
   Item gift=reward(ThreadLocalRandom.current().nextInt(5),p.gender);if(!CatalogAssets.check(p,gift.template.id))return true;
   var result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of((int)box.template.id,1),0,0),new BagExchange.Gain(List.of(gift),0,0,0),TrungThuStore::persistBag);
   InventoryService.gI().sendItemBags(p);
   Service.gI().sendThongBao(p,result==BagExchange.Result.SUCCESS?"Bạn nhận được "+gift.template.name+".":result==BagExchange.Result.BAG_FULL?"Hành trang đầy. Hộp được giữ nguyên.":"Chưa mở được hộp. Hộp được giữ nguyên, hãy thử lại.");
   return true;
  }
 }
 private TeachersDayBox(){}
}