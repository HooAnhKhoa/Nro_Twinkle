package nro.models.npc_list;

import java.util.ArrayList;
import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.event.october20.October20;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.shop.ShopService;

public class ChiChi extends Npc {
    private final java.util.Map<Player,List<Integer>> selections=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    public ChiChi(int mapId,int status,int cx,int cy,int tempId,int avatar){
        super(mapId,status,cx,cy,tempId,avatar);
    }
    @Override public void openBaseMenu(Player player){
        if(!canOpenNpc(player))return;
        List<String> menu=new ArrayList<>();
        List<Integer> actions=new ArrayList<>();menu.add("Cửa hàng");actions.add(0);
        if(nro.models.event.EventManager.enabled("october20")){menu.add("Sự kiện\n20/10");actions.add(1);}
        else if(nro.models.shop.EventPointShop.pending(player).contains("october20")){menu.add("Nhận quà top\n20/10");actions.add(2);}
        menu.add("Đóng");actions.add(3);selections.put(player,List.copyOf(actions));
        createOtherMenu(player,ConstNpc.BASE_MENU,"Bạn muốn hỏi chi?",menu.toArray(new String[0]));
    }
    @Override public void confirmMenu(Player player,int select){
        if(!canOpenNpc(player))return;
        if(October20.menu(this,player,select))return;
        if(mapId!=5||!player.idMark.isBaseMenu())return;
        var actions=selections.remove(player);if(actions==null||select<0||select>=actions.size())return;select=actions.get(select);
        if(select==0)ShopService.gI().opendShop(player,"SHOP_CHI_CHI",false);
        else if(select==1&&nro.models.event.EventManager.enabled("october20"))October20.open(this,player);
        else if(select==2)nro.models.event.october20.October20Competition.claim(player);
    }
}
