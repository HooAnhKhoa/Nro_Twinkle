package nro.models.event.october20;
import java.util.*;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.shop.*;
public final class October20Shop {
 public static final String TAG="OCTOBER20_2026";
 public static void open(Player p){
  var s=October20Competition.current();if(s==null||!Boolean.TRUE.equals(October20Competition.scheduledEnabled())){Service.gI().sendThongBao(p,"Shop 20/10 đang đóng.");return;}
  Shop shop=new Shop();shop.id=Math.toIntExact(s.id());shop.tagName=TAG;shop.npcId=nro.models.consts.ConstNpc.CHI_CHI;shop.typeShop=0;
  TabShop tab=new TabShop();tab.shop=shop;tab.name="Quà 20/10";tab.id=1;
  for(int id:new int[]{1718,1719}){if(!CatalogAssets.available(id))continue;ItemShop sale=new ItemShop();sale.tabShop=tab;sale.temp=ItemService.gI().getTemplate(id);sale.id=id;sale.cost=id==1718?s.cardGems():Math.toIntExact(s.giftGold());sale.typeSell=(byte)(id==1718?1:0);tab.itemShops.add(sale);}
  shop.tabShops.add(tab);nro.models.shop.ShopService.gI().openOctober20Shop(p,shop);
 }
 public static void buy(Player p,Shop shop,int id){
  var s=October20Competition.current();ItemShop sale=shop.getItemShop(id);
  if(s==null||shop.id!=s.id()||sale==null||(id!=1718&&id!=1719)||sale.cost!=(id==1718?s.cardGems():s.giftGold())){Service.gI().sendThongBao(p,"Shop đã đổi cấu hình. Hãy mở lại.");return;}
  October20Competition.buy(p,id,1);
 }
 private October20Shop(){}
}
