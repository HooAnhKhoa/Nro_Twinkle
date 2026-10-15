package nro.models.boss.event_noel;
import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import nro.models.event.*;
import nro.models.boss.*;
import nro.models.consts.BossStatus;
import nro.models.map.*;
import nro.models.map.service.ChangeMapService;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.npc_list.Santa;
import nro.models.skill.*;
import nro.models.item.Item;
import nro.models.shop.*;

public class ChristmasLogicTest {
 static int checks;
 static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static void enabled(boolean on)throws Exception{Field f=EventManager.class.getDeclaredField("switches");f.setAccessible(true);f.set(null,java.util.Map.of("christmas",on));}
 static void field(Object o,String key,Object value)throws Exception{Field f=OngGiaNoel.class.getDeclaredField(key);f.setAccessible(true);f.set(o,value);}
 static class FixedSanta extends OngGiaNoel {boolean win;FixedSanta()throws Exception{super();currentLevel=0;}@Override protected boolean rollGift(int n){return win;}}
 static class MemorySocket extends java.net.Socket {
  @Override public void setSendBufferSize(int n){} @Override public void setReceiveBufferSize(int n){}
  @Override public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("127.0.0.1",1);}
  @Override public java.net.InetAddress getInetAddress(){return java.net.InetAddress.getLoopbackAddress();}
  @Override public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(new byte[0]);}
  @Override public java.io.OutputStream getOutputStream(){return new java.io.ByteArrayOutputStream();}
 }
 static class Client extends Player {Client(){isPlayer=true;id=123;nPoint.hp=nPoint.hpMax=100;session=new nro.models.network.MySession(new MemorySocket());session.version=220;}@Override public void sendMessage(Message message){}}
 static class MenuSanta extends Santa {String[] choices;MenuSanta(){super(5,1,100,0,39,0);}@Override public void createOtherMenu(Player p,int index,String text,String... choices){this.choices=choices;super.createOtherMenu(p,index,text,choices);}}
 static class FixedRandom extends Random {int[] values;int index;FixedRandom(int... values){this.values=values;}@Override public int nextInt(int bound){return values[index++%values.length]%bound;}}
 static Client client(Zone zone,int slots){Client p=new Client();p.zone=zone;zone.addPlayer(p);for(int i=0;i<slots;i++)p.inventory.itemsBag.add(ItemService.gI().createItemNull());return p;}
 static Item put(Player p,int id,int qty){Item item=ItemService.gI().createNewItem((short)id,qty);InventoryService.gI().addItemBag(p,item);return InventoryService.gI().findItemBag(p,id);}
 static int qty(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 public static void main(String[] args)throws Exception{try{
  for(int id=0;id<=2000;id++)Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)id,(byte)27,(byte)3,"Item "+id,"",(short)0,(short)0,true,0));
  for(int id=0;id<300;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
  for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());SkillTemplate template=new SkillTemplate();template.id=(byte)Skill.TAI_TAO_NANG_LUONG;template.name="QA heal";Skill skill=new Skill();skill.template=template;skill.point=7;skill.coolDown=5000;template.skillss.add(skill);Manager.NCLASS.get(0).skillTemplatess.add(template);
  nro.models.map.Map map=new nro.models.map.Map(5,"QA Noel",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[24][60],new int[]{1},3,20,new ArrayList<>());Manager.MAPS.add(map);
  enabled(true);FixedSanta first=new FixedSanta();first.zoneFinal=map.zones.get(0);first.joinMap();check(first.zone==map.zones.get(0),"manual spawn registers Noel in requested zone");first.autoLeaveMap();check(first.bossStatus==BossStatus.CHAT_S,"manual spawn initializes leave timer");
  FixedSanta second=new FixedSanta();second.zoneFinal=map.zones.get(0);second.joinMap();check(second.zone!=first.zone,"event-manager Santa cannot duplicate an occupied zone");
  FixedSanta crowded=new FixedSanta();crowded.zoneFinal=map.zones.get(0);for(Zone z:map.zones)z.maxPlayer=0;crowded.joinMap();check(crowded.zone==null&&crowded.bossStatus==BossStatus.REST,"all unavailable zones return to cooldown without hanging");for(Zone z:map.zones)z.maxPlayer=20;
  Method gift=OngGiaNoel.class.getDeclaredMethod("giftBox");gift.setAccessible(true);field(first,"lastTimeDrop",0L);int items=first.zone.items.size();first.win=false;gift.invoke(first);check(first.zone.items.size()==items,"failed rolls do not register invisible phantom gifts");
  first.win=true;field(first,"lastTimeDrop",0L);gift.invoke(first);check(first.zone.items.size()==items+3,"three successful independent rolls register exactly three visible gifts");gift.invoke(first);check(first.zone.items.size()==items+3,"gift interval throttles repeated calls");
  field(second,"st",System.currentTimeMillis()-400000);second.autoLeaveMap();check(second.bossStatus==BossStatus.LEAVE_MAP,"expired visit enters normal leave state");second.update();check(second.zone==null&&second.bossStatus==BossStatus.REST,"real update removes expired Santa and starts cooldown");second.update();check(second.bossStatus==BossStatus.REST,"normal cooldown does not respawn immediately");
  Client far=client(first.zone,4);far.location.x=first.location.x+200;Client close=client(first.zone,4);close.id=124;close.location.x=first.location.x+10;close.location.y=first.location.y;check(first.getPlayerAttack()==close,"friendly Santa chooses nearest living player");close.nPoint.hp=0;check(first.getPlayerAttack()==far,"dead players are ignored");close.nPoint.hp=100;
  enabled(false);field(first,"lastTimeDrop",0L);gift.invoke(first);check(first.zone.items.size()==items+3,"OFF prevents direct gift calls");check(first.injured(far,9999,false,false)==0,"Noel remains invulnerable and has no kill reward");first.update();check(first.zone==null,"OFF removes Santa through normal event suspension");enabled(true);
  Zone zone=map.zones.get(2);Client p=client(zone,4);ItemMap ground=new ItemMap(zone,648,1,100,0,-1);zone.pickItem(p,ground.itemMapId);check(!ground.isPickedUp&&qty(p,648)==0,"gift pickup requires a stocking");put(p,649,2);zone.pickItem(p,ground.itemMapId);check(ground.isPickedUp&&!zone.items.contains(ground)&&qty(p,648)==1&&qty(p,649)==1,"actual pickup consumes one sock and commits one owned gift");zone.pickItem(p,ground.itemMapId);check(qty(p,648)==1&&qty(p,649)==1,"pickup retry cannot duplicate gifts or consume another sock");
  Client full=client(zone,1);put(full,649,2);ItemMap blocked=new ItemMap(zone,648,1,100,0,-1);zone.pickItem(full,blocked.itemMapId);check(!blocked.isPickedUp&&qty(full,649)==2,"full bag keeps sock and ground gift intact");
  Client a=client(zone,3),b=client(zone,3);a.id=201;b.id=202;put(a,649,1);put(b,649,1);ItemMap contested=new ItemMap(zone,648,1,100,0,-1);CountDownLatch start=new CountDownLatch(1);Thread one=new Thread(()->{try{start.await();zone.pickItem(a,contested.itemMapId);}catch(Exception e){throw new RuntimeException(e);}}),two=new Thread(()->{try{start.await();zone.pickItem(b,contested.itemMapId);}catch(Exception e){throw new RuntimeException(e);}});one.start();two.start();start.countDown();one.join();two.join();check(qty(a,648)+qty(b,648)==1&&qty(a,649)+qty(b,649)==1,"simultaneous pickup gives one box and charges one sock total");
  Method open=ChristmasGifts.class.getDeclaredMethod("open",Player.class,Item.class,Random.class);open.setAccessible(true);
  int[] expected={441,381,17,925,1066,533,380};for(int bucket=0;bucket<7;bucket++){Client winner=client(zone,3);Item box=put(winner,648,2);open.invoke(null,winner,box,new FixedRandom(89,bucket,0,0));check(qty(winner,648)==1&&qty(winner,expected[bucket])>=1,"existing gift reward bucket "+bucket+" reserves output before consuming box");}
  Client gems=client(zone,3);Item box=put(gems,648,2);open.invoke(null,gems,box,new FixedRandom(0,10));check(gems.inventory.gem==20&&qty(gems,648)==1,"currency branch grants green gems and consumes exactly one box");gems.inventory.gem=Integer.MAX_VALUE;open.invoke(null,gems,box,new FixedRandom(0,10));check(gems.inventory.gem==Integer.MAX_VALUE&&qty(gems,648)==1,"gem overflow preserves box");
  enabled(false);Client stored=client(zone,3);Item storedBox=put(stored,648,2);open.invoke(null,stored,storedBox,new FixedRandom(89,5));check(qty(stored,533)==1&&qty(stored,648)==1,"earned gifts remain openable after EV is OFF");
  MenuSanta npc=new MenuSanta();Client menu=client(zone,4);npc.openBaseMenu(menu);check(!Arrays.asList(npc.choices).contains("Giáng sinh"),"OFF hides only Noel menu and retains ordinary Santa");enabled(true);npc.openBaseMenu(menu);check(npc.choices.length==8&&npc.choices[6].equals("Giáng sinh"),"ON appends Noel entry without shifting ordinary menus");put(menu,459,1);npc.openBaseMenu(menu);check(npc.choices.length==9&&npc.choices[7].equals("Giáng sinh"),"coupon menu keeps correct Noel index");
  Shop shop=new Shop();shop.tagName="SANTA_NOEL";shop.npcId=39;shop.typeShop=0;TabShop tab=new TabShop();tab.id=99;tab.shop=shop;shop.tabShops.add(tab);ItemShop sale=new ItemShop();sale.tabShop=tab;sale.temp=Manager.ITEM_TEMPLATES.get(649);sale.cost=20;sale.typeSell=1;tab.itemShops.add(sale);Manager.SHOPS.add(shop);
  npc.confirmMenu(menu,7);check(menu.idMark.getIndexMenu()==ChristmasGifts.MENU,"coupon Noel button opens event instructions");enabled(false);npc.confirmMenu(menu,0);check(menu.idMark.getShopOpen()==null,"stale Noel menu cannot open shop after OFF");
  Client buyer=client(zone,3);buyer.inventory.gem=40;buyer.idMark.setShopOpen(shop);ShopService.gI().buyItem(buyer,649);check(buyer.inventory.gem==40&&qty(buyer,649)==0,"stale shop cannot charge gems or sell socks after OFF");enabled(true);ShopService.gI().buyItem(buyer,649);check(buyer.inventory.gem==20&&qty(buyer,649)==1,"Noel shop grants sock for exactly twenty gems");
  Client packed=client(zone,1);Item packedBox=put(packed,648,2);open.invoke(null,packed,packedBox,new FixedRandom(89,5));check(qty(packed,648)==2,"full bag does not consume owned gift");
  Client fakeOwner=client(zone,3);Item unowned=ItemService.gI().createNewItem((short)648);open.invoke(null,fakeOwner,unowned,new FixedRandom(0,0));check(fakeOwner.inventory.gem==0&&qty(fakeOwner,648)==0,"unowned box cannot grant reward");
  System.out.println("PASS: "+checks+" Noel lifecycle/gift/pickup/reward/menu checks");
 }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);}
}
