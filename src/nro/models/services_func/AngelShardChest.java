package nro.models.services_func;

import java.util.*;
import nro.models.event.trungthu2026.BagExchange;
import nro.models.event.trungthu2026.TrungThuStore;
import nro.models.item.Item;
import nro.models.map.service.NpcService;
import nro.models.player.Player;
import nro.models.services.*;

/** Standard angel chest grants one selected crafting fragment. */
public final class AngelShardChest {
    public static final int BOX=1173;
    public static final int MENU=202610405;
    private static final Map<Player,Item> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    private static final BagExchange EXCHANGE=new BagExchange(0,System::currentTimeMillis);

    public static void open(Player p,Item box){
        synchronized(p){
            if(!p.isPl()||nro.models.server.Maintenance.isRunning||box==null||!box.isNotNullItem()||box.template.id!=BOX||box.quantity<1||!p.inventory.itemsBag.contains(box))return;
            PENDING.put(p,box);
            NpcService.gI().createMenuConMeo(p,MENU,-1,"RƯƠNG MẢNH THIÊN SỨ\nChọn một mảnh muốn nhận. Mỗi lần dùng một rương.","Mảnh áo","Mảnh quần","Mảnh giày","Mảnh nhẫn","Mảnh găng","Đóng");
        }
    }
    public static boolean confirm(Player p,int select){
        synchronized(p){
            if(p.idMark.getIndexMenu()!=MENU)return false;
            p.idMark.setIndexMenu(-1);Item box=PENDING.remove(p);
            if(select<0||select>4||box==null||!p.isPl()||nro.models.server.Maintenance.isRunning)return true;
            if(!p.inventory.itemsBag.contains(box)||!box.isNotNullItem()||box.template.id!=BOX||box.quantity<1){Service.gI().sendThongBao(p,"Hộp đã thay đổi hoặc không còn trong hành trang. Hãy mở lại.");return true;}
            int id=1066+select;
            if(!CatalogAssets.check(p,id))return true;
            Item reward=ItemService.gI().createNewItem((short)id,1);
            BagExchange.Result result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(BOX,1),0,0),new BagExchange.Gain(List.of(reward),0,0,0),TrungThuStore::persistBag);
            if(result==BagExchange.Result.SUCCESS){InventoryService.gI().sendItemBags(p);Service.gI().sendThongBao(p,"Bạn nhận được "+reward.template.name+".");}
            else {InventoryService.gI().sendItemBags(p);Service.gI().sendThongBao(p,result==BagExchange.Result.BAG_FULL?"Hành trang không đủ chỗ. Hộp được giữ nguyên.":"Chưa mở được rương mảnh thiên sứ. Hộp được giữ nguyên, hãy thử lại.");}
            return true;
        }
    }
    private AngelShardChest(){}
}
