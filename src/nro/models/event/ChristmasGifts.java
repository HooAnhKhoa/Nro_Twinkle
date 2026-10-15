package nro.models.event;
import java.nio.file.*;
import java.sql.*;
import java.util.Random;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.*;

/** Existing Noel contract: one stocking per collected gift; owned gifts remain usable after EV ends. */
public final class ChristmasGifts {
    public static final int MENU=202610150;
    public static void ensureData(Connection connection){
        try(Statement statement=connection.createStatement()){
            for(String sql:Files.readString(Path.of("web/noel_schema.sql")).split(";"))if(!sql.isBlank())statement.execute(sql);
        }catch(Exception error){throw new IllegalStateException("Cannot initialize Noel stocking shop",error);}
    }
    public static void pickup(Player player,ItemMap gift){
        synchronized(gift){synchronized(player){
            if(gift.isPickedUp||gift.zone!=player.zone||!gift.zone.items.contains(gift)||gift.itemTemplate.id!=648)return;
            if(gift.playerId!=-1&&gift.playerId!=player.id){Service.gI().sendThongBao(player,"Không thể nhặt vật phẩm của người khác.");return;}
            int quantity=gift.quantity;
            Item socks=InventoryService.gI().findItemBag(player,649);
            if(quantity<1||quantity>99999||socks==null||socks.quantity<quantity){Service.gI().sendThongBao(player,"Cần 1 Tất, vớ Giáng sinh cho mỗi hộp quà. Mua tại Santa → Giáng sinh.");return;}
            if(InventoryService.gI().getCountEmptyBag(player)==0){Service.gI().sendThongBao(player,"Cần chỗ trống để nhận hộp quà Giáng sinh.");return;}
            Item reward=ItemService.gI().createItemFromItemMap(gift);
            if(!InventoryService.gI().addItemBag(player,reward))return;
            InventoryService.gI().subQuantityItemsBag(player,socks,quantity);
            // Commit ownership before network writes, so retries cannot duplicate gifts or charge socks twice.
            gift.isPickedUp=true;gift.zone.removeItemMap(gift);
            InventoryService.gI().sendItemBags(player);
            Message packet=new Message(-20);
            try{packet.writer().writeShort(gift.itemMapId);packet.writer().writeShort(Math.min(quantity,Short.MAX_VALUE));player.sendMessage(packet);}
            catch(java.io.IOException error){throw new IllegalStateException(error);}finally{packet.cleanup();}
            Service.gI().sendToAntherMePickItem(player,gift.itemMapId);
            TaskService.gI().checkDoneTaskPickItem(player,gift);
            TaskService.gI().checkDoneSideTaskPickItem(player,gift);
            TaskService.gI().checkDoneClanTaskPickItem(player,gift);
        }}
    }
    public static void open(Player player,Item gift){open(player,gift,new Random());}
    static void open(Player player,Item gift,Random random){
        synchronized(player){
            if(gift==null||!gift.isNotNullItem()||gift.template.id!=648||gift.quantity<1||!player.inventory.itemsBag.contains(gift))return;
            if(InventoryService.gI().getCountEmptyBag(player)==0){Service.gI().sendThongBao(player,"Hành trang đã đầy. Hộp quà được giữ nguyên.");return;}
            if(random.nextInt(90)<5){
                int gems=10+random.nextInt(11);
                if((long)player.inventory.gem+gems>Integer.MAX_VALUE){Service.gI().sendThongBao(player,"Ngọc đã đạt giới hạn. Hộp quà được giữ nguyên.");return;}
                player.inventory.gem+=gems;
                InventoryService.gI().subQuantityItemsBag(player,gift,1);
                Service.gI().sendMoney(player);InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player,"Bạn nhận được "+gems+" ngọc.");return;
            }
            int bucket=random.nextInt(7),id=switch(bucket){case 0->441+random.nextInt(5);case 1->381+random.nextInt(4);case 2->17+random.nextInt(4);case 3->925+random.nextInt(7);case 4->1066+random.nextInt(5);case 5->533;default->380;};
            Item reward=ItemService.gI().createNewItem((short)id);
            if(bucket==0){reward.itemOptions.add(new ItemOption(id-346,id<=443?5:3));reward.quantity=1+random.nextInt(5);}
            else if(bucket==1||bucket==4)reward.quantity=1+random.nextInt(5);
            else reward.itemOptions.add(new ItemOption(73,0));
            if(!InventoryService.gI().addItemBag(player,reward))return;
            InventoryService.gI().subQuantityItemsBag(player,gift,1);InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player,"Bạn nhận được "+reward.template.name+".");
        }
    }
}
