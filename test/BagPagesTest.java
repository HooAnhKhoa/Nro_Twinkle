import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player.Inventory;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.network.*;
import nro.models.services.*;
import nro.models.services_func.*;
import nro.models.shop.*;
import nro.models.combine.CombineService;
import nro.models.skill.NClass;

/** Actual legacy-client packets and mutation entry points; no database or live server. */
public class BagPagesTest {
    static int checks;
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
    static class MemorySocket extends java.net.Socket {
        @Override public void setSendBufferSize(int n){} @Override public void setReceiveBufferSize(int n){}
        @Override public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("127.0.0.1",1);}
        @Override public java.net.InetAddress getInetAddress(){return java.net.InetAddress.getLoopbackAddress();}
        @Override public InputStream getInputStream(){return new ByteArrayInputStream(new byte[0]);}
        @Override public OutputStream getOutputStream(){return new ByteArrayOutputStream();}
    }
    static class Client extends Player {
        Map<Integer,byte[]> packets=new HashMap<>();
        Client(int slots){isPlayer=true;id=123;name="QA";playerTask.taskMain=new nro.models.task.TaskMain();session=new MySession(new MemorySocket());session.player=this;session.version=220;session.actived=true;nPoint.hp=nPoint.hpMax=100;
            for(int i=0;i<slots;i++)inventory.itemsBag.add(item(1800+i,2));
            for(int i=0;i<14;i++)inventory.itemsBody.add(new Item());
            for(int i=0;i<10;i++)inventory.itemsBox.add(new Item());
            zone=new nro.models.map.Map(5,"QA",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[24][60],new int[]{1},1,20,new ArrayList<>()).zones.get(0);
        }
        @Override public void sendMessage(Message m){packets.put((int)m.command,m.getData());}
        DataInputStream packet(int command){byte[] raw=packets.get(command);if(raw==null)throw new AssertionError("Missing packet "+command);return new DataInputStream(new ByteArrayInputStream(raw));}
    }
    static Item item(int id,int qty){return ItemService.gI().createNewItem((short)id,qty);}
    static Message input(int... bytes)throws Exception {ByteArrayOutputStream out=new ByteArrayOutputStream();for(int value:bytes)out.write(value);return new Message((byte)0,out.toByteArray());}
    static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
    static List<Integer> bagPacket(Client p)throws Exception{
        DataInputStream in=p.packet(-36);check(in.readByte()==0,"bag update has native client command");int slots=in.readUnsignedByte();List<Integer> ids=new ArrayList<>();
        for(int i=0;i<slots;i++){int id=in.readShort();ids.add(id);if(id<0)continue;in.readInt();in.readUTF();in.readUTF();for(int n=in.readUnsignedByte();n>0;n--){in.readUnsignedByte();in.readShort();}}
        check(in.available()==0,"native decoder consumes complete bag packet without byte overflow");return ids;
    }
    public static void main(String[] args){try{
        for(int id=0;id<=2200;id++)Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)id,(byte)27,(byte)3,"QA "+id,"",(short)0,(short)0,true,0));
        for(int id=0;id<300;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
        for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());
        Client p=new Client(300);Set<Integer> visited=new HashSet<>();
        for(int page=1;page<=3;page++){
            check(BagPages.select(p,page),"select owned page "+page);
            List<Integer> ids=bagPacket(p);check(ids.size()==100&&ids.get(0)==1800+(page-1)*100&&ids.get(99)==1799+page*100,"page "+page+" sends exactly its 100 real items");visited.addAll(ids);
            check(BagPages.global(p,0)==(page-1)*100&&BagPages.global(p,99)==page*100-1,"wire boundaries map correctly on page "+page);
            check(BagPages.global(p,-1)==-1&&BagPages.global(p,100)==-1&&BagPages.local(p,300)==-1,"invalid wire and global indices rejected");
        }
        check(visited.size()==300,"all 300 items reachable exactly once");
        Service.gI().player(p);DataInputStream login=p.packet(-30);login.readByte();login.readInt();login.readByte();login.readByte();login.readShort();login.readUTF();login.readByte();login.readByte();login.readLong();login.readShort();login.readShort();login.readByte();for(int n=login.readUnsignedByte();n>0;n--)login.readShort();login.readLong();login.readInt();login.readInt();for(int n=login.readUnsignedByte();n>0;n--)check(login.readShort()==-1,"login fixture body slot empty");check(login.readUnsignedByte()==100&&login.readShort()==2000,"initial login sends current page with safe 100-slot count");
        nro.models.npc.NpcFactory.createNpcConMeo();check(BagPages.command(p,"tui")&&p.idMark.getIndexMenu()==BagPages.MENU,"chat opens native page-selection menu");DataInputStream menu=p.packet(32);menu.readShort();check(menu.readUTF().contains("300/300")&&menu.readByte()==4&&menu.readUTF().contains("Trang 1"),"page menu shows total capacity and three choices plus close");nro.models.server.Manager.NPCS.stream().filter(n->n.tempId==nro.models.consts.ConstNpc.CON_MEO).findFirst().orElseThrow().confirmMenu(p,1);check(p.inventory.bagPage==1,"native NPC menu routes selection to second page");BagPages.select(p,3);
        Client small=new Client(80);check(!BagPages.select(small,2)&&small.inventory.bagPage==0,"unowned page remains locked");
        Client partial=new Client(205);BagPages.select(partial,3);check(bagPacket(partial).size()==5,"partly expanded page sends only owned slots");
        p.inventory.itemsBag.set(299,new Item());InventoryService.gI().sendItemBags(p);List<Integer> ids=bagPacket(p);check(ids.size()==100&&ids.get(99)==-1,"empty page slots explicitly encoded without packet truncation");
        check(BagPages.command(p,"/tui 2")&&p.inventory.bagPage==1&&BagPages.command(p,"tui3")&&p.inventory.bagPage==2&&!BagPages.command(p,"hello"),"all-player chat commands select pages and preserve ordinary chat");
        Item third=p.inventory.itemsBag.get(205);BagPages.remember(p,third,3,1,5);check(BagPages.confirm(p,third,3,1,5)&&!BagPages.confirm(p,third,3,1,5),"confirmation is valid once only");
        BagPages.remember(p,third,3,1,5);BagPages.select(p,2);check(!BagPages.confirm(p,third,3,1,5),"page change invalidates old confirmation");
        BagPages.remember(p,third,3,1,5);check(!BagPages.confirm(p,item(2005,2),3,1,5),"same-template replacement cannot impersonate confirmed item");
        Client use=new Client(300);use.inventory.bagPage=2;Item boost=item(381,2);use.inventory.itemsBag.set(205,boost);UseItem.gI().doItem(use,input(0,1,5));check(boost.quantity==1&&use.itemTime.isUseCuongNo&&use.inventory.itemsBag.get(5).template.id==1805,"using slot 5 on third page consumes actual slot 205 only");
        Client move=new Client(300);move.inventory.bagPage=2;Item moved=move.inventory.itemsBag.get(205);UseItem.gI().getItem(move.session,input(1,5));check(move.inventory.itemsBox.get(0).template.id==2005&&move.inventory.itemsBox.get(0).quantity==2&&move.inventory.itemsBag.get(5).template.id==1805&&!move.inventory.itemsBag.contains(moved),"bag-to-box moves third-page item instead of first-page item");
        Manager.ITEM_TEMPLATES.get(2006).type=0;Client equip=new Client(300);equip.inventory.bagPage=2;Item equipped=equip.inventory.itemsBag.get(206);UseItem.gI().getItem(equip.session,input(4,6));check(equip.inventory.itemsBody.get(0)==equipped&&equip.inventory.itemsBag.get(6).template.id==1806,"equip operation resolves third-page slot before moving to body");Manager.ITEM_TEMPLATES.get(2006).type=27;
        Client throwing=new Client(300);throwing.inventory.bagPage=2;Item thrown=throwing.inventory.itemsBag.get(205);UseItem.gI().doItem(throwing,input(1,1,5));DataInputStream prompt=throwing.packet(-43);check(prompt.readByte()==1&&prompt.readByte()==1&&prompt.readByte()==5&&prompt.readUTF().contains("2005"),"throw prompt keeps native local index and correct item name");UseItem.gI().doItem(throwing,input(2,1,5));check(!throwing.inventory.itemsBag.contains(thrown)&&throwing.inventory.itemsBag.get(5).template.id==1805,"confirmed throw removes actual third-page item only");
        Client stale=new Client(300);stale.inventory.bagPage=2;UseItem.gI().doItem(stale,input(1,1,5));BagPages.select(stale,1);UseItem.gI().doItem(stale,input(2,1,5));check(stale.inventory.itemsBag.get(5).template.id==1805&&stale.inventory.itemsBag.get(205).template.id==2005,"late throw confirmation after page switch removes nothing");
        Manager.ITEM_TEMPLATES.get(1905).type=7;stale.inventory.bagPage=1;UseItem.gI().doItem(stale,input(0,1,5));prompt=stale.packet(-43);check(prompt.readByte()==0&&prompt.readByte()==1&&prompt.readByte()==5&&prompt.readUTF().contains("1905"),"skill-book confirmation on second page uses local index");
        Client seller=new Client(300);seller.inventory.bagPage=2;Shop shop=new Shop();shop.tagName="QA";seller.idMark.setShopOpen(shop);seller.idMark.setTagNameShop("QA");Manager.ITEM_TEMPLATES.get(2005).gold=400;Item sold=seller.inventory.itemsBag.get(205);ShopService.gI().showConfirmSellItem(seller,1,5);prompt=seller.packet(7);check(prompt.readByte()==1&&prompt.readShort()==5&&prompt.readUTF().contains("2005"),"sale confirmation identifies third-page item and local slot");ShopService.gI().sellItem(seller,1,5);check(seller.inventory.gold==200&&!seller.inventory.itemsBag.contains(sold)&&seller.inventory.itemsBag.get(5).template.id==1805,"confirmed sale pays once for actual third-page item");ShopService.gI().sellItem(seller,1,5);check(seller.inventory.gold==200,"replayed sale confirmation cannot pay twice");
        ShopService.gI().showConfirmSellItem(seller,1,5);BagPages.select(seller,1);ShopService.gI().sellItem(seller,1,5);check(seller.inventory.gold==200&&seller.inventory.itemsBag.get(5).template.id==1805,"late sale after page switch cannot sell first-page item");
        Client trader=new Client(300),other=new Client(300);trader.inventory.bagPage=2;Trade trade=new Trade(trader,other);trade.addItemTrade(trader,(byte)5,1);List<?> offered=(List<?>)field(trade,"itemsTrade1");check(offered.size()==1&&((Item)offered.get(0)).template.id==2005&&((List<?>)field(trade,"itemsBag1")).size()==300,"trade translates third-page input while preserving complete 300-slot snapshot");check(!BagPages.select(trader,1)&&trader.inventory.bagPage==2,"active trade prevents page changes");trade.addItemTrade(trader,(byte)-1,1234);check((Integer)field(trade,"goldTrade1")==1234,"gold-trade sentinel remains unchanged");trade.cancelTrade();check(BagPages.select(trader,1),"page changes resume when trade closes");
        Client combine=new Client(300);combine.inventory.bagPage=2;combine.combineNew.itemsCombine.add(combine.inventory.itemsBag.get(295));CombineService.gI().reOpenItemCombine(combine);DataInputStream packet=combine.packet(-81);check(packet.readByte()==1&&packet.readByte()==1&&packet.readByte()==95&&packet.available()==0,"combine output converts slot 295 to client slot 95");BagPages.select(combine,2);check(combine.combineNew.itemsCombine.isEmpty(),"page changes clear old combine selection");
        Client cap=new Client(299);check(InventoryService.gI().addItemBag(cap,item(517,1))&&cap.inventory.itemsBag.size()==300&&!InventoryService.gI().addItemBag(cap,item(517,1))&&cap.inventory.itemsBag.size()==300&&Inventory.MAX_ITEMS_BAG==300,"capacity item expands 299 to 300 and refuses overflow");
        Client consign=new Client(300);consign.inventory.bagPage=2;consign.inventory.itemsBag.get(5).itemOptions.add(new Item.ItemOption(86,0));consign.inventory.itemsBag.get(205).itemOptions.add(new Item.ItemOption(86,0));var listing=nro.models.shop_ky_gui.ConsignShopService.gI().getItemCanKiGui(consign);check(listing.size()==1&&listing.get(0).id==5&&listing.get(0).itemId==2005,"consignment lists only current-page eligible item with local slot");
        System.out.println("PASS: "+checks+" bag paging, legacy packet and inventory action checks; no database or live server");
    }catch(Throwable error){error.printStackTrace();System.exit(1);}System.exit(0);}
}
