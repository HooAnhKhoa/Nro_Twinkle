package nro.models.event;

import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

public class NoiBanh extends Npc {
    private static final int[][] DAY={{1546,99},{1547,10},{1545,10},{1544,1}};
    private static final int[][] CHUNG={{1546,99},{1548,99},{1549,99}};
    public NoiBanh(int mapId,int status,int cx,int cy,int tempId,int avatar){super(mapId,status,cx,cy,tempId,avatar);}
    @Override public void openBaseMenu(Player player){
        if(!EventManager.allowNpc(player,"hung_vuong"))return;
        createOtherMenu(player,0,"Nấu Bánh dầy: 99 Cơm nếp + 10 Bột gạo + 10 Muối tiêu + 1 Chả lụa.\nNấu Bánh chưng: 99 Cơm nếp + 99 Đậu xanh + 99 Thịt tươi.","Tự nấu\nbánh","Từ chối");
    }
    @Override public void confirmMenu(Player player,int select){
        if(!EventManager.allowNpc(player,"hung_vuong")||!canOpenNpc(player))return;
        if(player.idMark.getIndexMenu()==0&&select==0)createOtherMenu(player,1,"Chọn loại bánh muốn nấu:","Nấu\nBánh Dầy","Nấu\nBánh Chưng","Từ chối");
        else if(player.idMark.getIndexMenu()==1&&(select==0||select==1))cook(player,select==0);
    }
    private void cook(Player player,boolean day){
        synchronized(player){
            int[][] recipe=day?DAY:CHUNG;
            Item[] ingredients=new Item[recipe.length];
            for(int i=0;i<recipe.length;i++){
                ingredients[i]=InventoryService.gI().findItemBag(player,recipe[i][0]);
                if(ingredients[i]==null||ingredients[i].quantity<recipe[i][1]){Service.gI().sendThongBao(player,"Không đủ số lượng nguyên liệu để nấu bánh.");return;}
            }
            Item output=ItemService.gI().createNewItem((short)(day?1542:1556));output.itemOptions.add(new ItemOption(93,30));
            // Reserve the result before consuming anything; a full bag cannot eat the ingredients.
            if(!InventoryService.gI().addItemBag(player,output)){Service.gI().sendThongBao(player,"Cần chỗ trống trong hành trang để nhận bánh.");return;}
            for(int i=0;i<recipe.length;i++)InventoryService.gI().subQuantityItemsBag(player,ingredients[i],recipe[i][1]);
            InventoryService.gI().sendItemBags(player);Service.gI().sendThongBao(player,day?"Bạn nhận được Bánh dầy":"Bạn nhận được Bánh chưng Lang Liêu");
        }
    }
}
