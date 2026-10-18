package nro.models.services_func;

import java.util.*;
import nro.models.event.trungthu2026.BagExchange;
import nro.models.event.trungthu2026.TrungThuStore;
import nro.models.item.Item;
import nro.models.map.service.NpcService;
import nro.models.player.Player;
import nro.models.services.*;

/** Standard box 2071 grants exactly one of the seven standard green dragon balls. */
public final class GreenDragonBox {
    public static final int BOX=2071;
    public static final int MENU=202610302;
    private static final Map<Player,Item> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    private static final BagExchange EXCHANGE=new BagExchange(0,System::currentTimeMillis);

    public static void open(Player p,Item box){
        synchronized(p){
            if(!p.isPl()||nro.models.server.Maintenance.isRunning||box==null||!box.isNotNullItem()||box.template.id!=BOX||box.quantity<1||!p.inventory.itemsBag.contains(box))return;
            PENDING.put(p,box);
            NpcService.gI().createMenuConMeo(p,MENU,-1,"HỘP NGỌC RỒNG XANH\nChọn 1 viên muốn nhận. Mỗi lần đổi dùng 1 hộp.","1 sao","2 sao","3 sao","4 sao","5 sao","6 sao","7 sao","Đóng");
        }
    }
    public static boolean confirm(Player p,int select){
        synchronized(p){
            if(p.idMark.getIndexMenu()!=MENU)return false;
            p.idMark.setIndexMenu(-1);Item box=PENDING.remove(p);
            if(select<0||select>6||box==null||!p.isPl()||nro.models.server.Maintenance.isRunning)return true;
            if(!p.inventory.itemsBag.contains(box)||!box.isNotNullItem()||box.template.id!=BOX||box.quantity<1){Service.gI().sendThongBao(p,"Hộp đã thay đổi hoặc không còn trong hành trang. Hãy mở lại.");return true;}
            int id=2064+select;
            if(!CatalogAssets.check(p,id))return true;
            Item reward=ItemService.gI().createNewItem((short)id,1);
            BagExchange.Result result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(BOX,1),0,0),new BagExchange.Gain(List.of(reward),0,0,0),TrungThuStore::persistBag);
            if(result==BagExchange.Result.SUCCESS){InventoryService.gI().sendItemBags(p);Service.gI().sendThongBao(p,"Bạn nhận được "+reward.template.name+".");}
            else {InventoryService.gI().sendItemBags(p);Service.gI().sendThongBao(p,result==BagExchange.Result.BAG_FULL?"Hành trang không đủ chỗ. Hộp được giữ nguyên.":"Chưa đổi được ngọc rồng xanh. Hộp được giữ nguyên, hãy thử lại.");}
            return true;
        }
    }
    private GreenDragonBox(){}
}
