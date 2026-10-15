import java.util.*;
import java.lang.reflect.*;
import nro.models.event.*;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.map.WayPoint;
import nro.models.map.service.ChangeMapService;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.shop.*;

public class ThanhCoTicketTest {
 static int checks;
 static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static void enabled(boolean on)throws Exception{Field f=EventManager.class.getDeclaredField("switches");f.setAccessible(true);f.set(null,Map.of("hung_vuong",on));}
 static class Client extends Player {Client(){isPlayer=true;id=123;nPoint.hp=100;nPoint.hpMax=100;}@Override public void sendMessage(Message message){}}
 static nro.models.map.Map map(int id){nro.models.map.Map m=new nro.models.map.Map(id,"Map "+id,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[30][60],new int[]{1},2,10,new ArrayList<>());Manager.MAPS.add(m);return m;}
 static Item ticket(Client p,int quantity){Item t=ItemService.gI().createNewItem((short)2000,quantity);p.inventory.itemsBag.add(t);return t;}
 static void walk(Client p,int mapId){WayPoint wp=p.zone.map.wayPoints.stream().filter(w->w.goMap==mapId).findFirst().orElseThrow();p.location.x=(wp.minX+wp.maxX)/2;p.location.y=(wp.minY+wp.maxY)/2;ChangeMapService.gI().changeMapWaypoint(p);}
 public static void main(String[] args)throws Exception{
  try{
   for(int id=0;id<=2000;id++)Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)id,(byte)75,(byte)3,"Item "+id,"",(short)0,(short)0,true,0));
   for(int id=0;id<300;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
   nro.models.map.Map home=map(5),one=map(183),two=map(184),arena=map(185);ThanhCo.seedRoutes();ThanhCo.seedRoutes();
   check(home.wayPoints.isEmpty(),"no free entry gate bypasses ticket");for(nro.models.map.Map m:List.of(one,two,arena))check(m.wayPoints.size()==2,"exactly two routes on castle map "+m.mapId);
   Client p=new Client();p.zone=home.zones.get(0);p.zone.addPlayer(p);Item t=ticket(p,2);
   enabled(false);check(!ThanhCo.useTicket(p,t)&&t.quantity==2&&p.zone.map.mapId==5,"OFF preserves ticket and location");
   enabled(true);p.nPoint.hp=0;check(!ThanhCo.useTicket(p,t)&&t.quantity==2,"dead player cannot spend ticket");p.nPoint.hp=100;
   for(Zone z:one.zones)z.maxPlayer=0;check(!ThanhCo.useTicket(p,t)&&t.quantity==2,"all zones full cannot consume ticket");one.zones.get(1).maxPlayer=10;
   p.idNRNM=353;p.lastTimePickNRNM=System.currentTimeMillis();check(!ThanhCo.useTicket(p,t)&&t.quantity==2&&p.zone.map.mapId==5,"base map-transition rejection preserves ticket");p.idNRNM=-1;
   Method use=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);use.setAccessible(true);use.invoke(UseItem.gI(),p,t,0);
   check(p.zone==one.zones.get(1)&&t.quantity==1,"real item dispatcher selects available zone and spends exactly one ticket");
   check(!ThanhCo.useTicket(p,t)&&t.quantity==1,"using again inside castle does not waste ticket");
   walk(p,184);check(p.zone.map.mapId==184&&t.quantity==1,"castle one to two through real waypoint handler is free");
   walk(p,185);check(p.zone.map.mapId==185,"castle two connects to arena");
   walk(p,5);check(p.zone.map.mapId==5&&t.quantity==1,"arena exit returns to Chi Chi map without ticket");
   Item fake=ItemService.gI().createNewItem((short)2000);check(!ThanhCo.useTicket(p,fake),"stale item not owned by player rejected");
   p.zone.map.type=2;check(!ThanhCo.useTicket(p,t)&&t.quantity==1,"dungeon blocks ticket use without loss");p.zone.map.type=0;
   ChangeMapService.gI().goToMap(p,one.zones.get(1));enabled(false);walk(p,5);check(p.zone.map.mapId==5,"exit remains available when event is turned OFF");enabled(true);
   Shop shop=new Shop();shop.tagName="SHOP_CHI_CHI";shop.npcId=81;shop.typeShop=0;TabShop tab=new TabShop();tab.id=58;tab.shop=shop;shop.tabShops.add(tab);ItemShop sale=new ItemShop();sale.tabShop=tab;sale.temp=Manager.ITEM_TEMPLATES.get(2000);sale.typeSell=1;sale.cost=5;tab.itemShops.add(sale);
   Client buyer=new Client();buyer.zone=home.zones.get(0);buyer.idMark.setShopOpen(shop);buyer.inventory.gem=10;buyer.inventory.itemsBag.add(ItemService.gI().createItemNull());
   ShopService.gI().buyItem(buyer,2000);check(buyer.inventory.gem==5&&InventoryService.gI().findItemBag(buyer,2000)!=null,"normal Chi Chi purchase costs exactly five green gems and grants ticket");
   Client poor=new Client();poor.zone=home.zones.get(0);poor.idMark.setShopOpen(shop);poor.inventory.gem=4;poor.inventory.itemsBag.add(ItemService.gI().createItemNull());ShopService.gI().buyItem(poor,2000);check(poor.inventory.gem==4&&InventoryService.gI().findItemBag(poor,2000)==null,"insufficient gems do not charge or give ticket");
   check(nro.models.data.DataGame.vsItem==10,"client item cache version advances for new ticket template");
   System.out.println("PASS: "+checks+" castle ticket/purchase/route checks");
  }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);
 }
}
