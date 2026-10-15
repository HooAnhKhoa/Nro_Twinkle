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
public class BagInventory100Test {
    static int checks;
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
    static class MemorySocket extends java.net.Socket {
        @Override public void setSendBufferSize(int n){} @Override public void setReceiveBufferSize(int n){}
        @Override public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("80.0.0.1",1);}
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
        Client p=new Client(100);InventoryService.gI().sendItemBags(p);List<Integer> ids=bagPacket(p);
        check(ids.size()==100&&ids.get(0)==1800&&ids.get(80)==1880&&ids.get(90)==1890&&ids.get(99)==1899,"one native packet exposes all 100 slots in original order");
        check(BagProtocol.index(p,80)==80&&BagProtocol.index(p,90)==90&&BagProtocol.index(p,99)==99&&BagProtocol.index(p,100)==-1&&BagProtocol.index(p,255)==-1,"full unsigned-byte slot range validated without pages");
        Service.gI().player(p);DataInputStream login=p.packet(-30);login.readByte();login.readInt();login.readByte();login.readByte();login.readShort();login.readUTF();login.readByte();login.readByte();login.readLong();login.readShort();login.readShort();login.readByte();for(int n=login.readUnsignedByte();n>0;n--)login.readShort();login.readLong();login.readInt();login.readInt();for(int n=login.readUnsignedByte();n>0;n--)if(login.readShort()!=-1)throw new AssertionError("Fixture body");check(login.readUnsignedByte()==100&&login.readShort()==1800,"initial login sends 100 slots together");
        p.inventory.itemsBag.set(99,new Item());InventoryService.gI().sendItemBags(p);ids=bagPacket(p);check(ids.size()==100&&ids.get(99)==-1,"empty final slot preserves full packet encoding");
        Client small=new Client(80);InventoryService.gI().sendItemBags(small);check(bagPacket(small).size()==80,"existing smaller inventories keep their owned capacity");
        for(int slot:new int[]{80,90,99}){
            Client use=new Client(100);Item boost=item(381,2);use.inventory.itemsBag.set(slot,boost);UseItem.gI().doItem(use,input(0,1,slot));check(boost.quantity==1&&use.itemTime.isUseCuongNo&&use.inventory.itemsBag.get(5).template.id==1805,"native use reads unsigned slot "+slot+" correctly");
        }
        Client byId=new Client(100);Item boost=item(381,2);byId.inventory.itemsBag.set(99,boost);UseItem.gI().doItem(byId,input(0,1,255,1,125));check(boost.quantity==1&&byId.itemTime.isUseCuongNo,"use-by-template 255 sentinel still works");
        Client move=new Client(100);UseItem.gI().getItem(move.session,input(1,99));check(move.inventory.itemsBox.get(0).template.id==1899&&move.inventory.itemsBox.get(0).quantity==2&&move.inventory.itemsBag.get(5).template.id==1805,"bag-to-box reads unsigned final slot99");
        Manager.ITEM_TEMPLATES.get(1890).type=0;Client equip=new Client(100);Item equipped=equip.inventory.itemsBag.get(90);UseItem.gI().getItem(equip.session,input(4,90));check(equip.inventory.itemsBody.get(0)==equipped&&equip.inventory.itemsBag.get(5).template.id==1805,"equip reads unsigned slot90");Manager.ITEM_TEMPLATES.get(1890).type=27;
        Client throwing=new Client(100);Item thrown=throwing.inventory.itemsBag.get(99);UseItem.gI().doItem(throwing,input(1,1,99));DataInputStream prompt=throwing.packet(-43);check(prompt.readUnsignedByte()==1&&prompt.readUnsignedByte()==1&&prompt.readUnsignedByte()==99&&prompt.readUTF().contains("1899"),"throw confirmation retains slot99 on wire");UseItem.gI().doItem(throwing,input(2,1,99));check(!throwing.inventory.itemsBag.contains(thrown)&&throwing.inventory.itemsBag.get(5).template.id==1805,"confirmed throw removes final-slot item only");
        Client book=new Client(100);Manager.ITEM_TEMPLATES.get(1890).type=7;UseItem.gI().doItem(book,input(0,1,90));prompt=book.packet(-43);check(prompt.readUnsignedByte()==0&&prompt.readUnsignedByte()==1&&prompt.readUnsignedByte()==90&&prompt.readUTF().contains("1890"),"skill-book confirmation supports slot90");Manager.ITEM_TEMPLATES.get(1890).type=27;
        Client seller=new Client(100);Shop shop=new Shop();shop.tagName="QA";seller.idMark.setShopOpen(shop);seller.idMark.setTagNameShop("QA");Manager.ITEM_TEMPLATES.get(1899).gold=400;Item sold=seller.inventory.itemsBag.get(99);ShopService.gI().showConfirmSellItem(seller,1,99);prompt=seller.packet(7);check(prompt.readByte()==1&&prompt.readShort()==99&&prompt.readUTF().contains("1899"),"sale confirmation retains full slot99");ShopService.gI().sellItem(seller,1,99);check(seller.inventory.gold==200&&!seller.inventory.itemsBag.contains(sold)&&seller.inventory.itemsBag.get(5).template.id==1805,"sale consumes final-slot item and pays exactly once");ShopService.gI().sellItem(seller,1,99);check(seller.inventory.gold==200,"replayed sale does not pay twice");
        Client trader=new Client(100),other=new Client(100);Trade trade=new Trade(trader,other);trade.addItemTrade(trader,(byte)99,1);List<?> offered=(List<?>)field(trade,"itemsTrade1");check(offered.size()==1&&((Item)offered.get(0)).template.id==1899&&((List<?>)field(trade,"itemsBag1")).size()==100,"trade accepts native slot99");trade.addItemTrade(trader,(byte)-1,1234);check((Integer)field(trade,"goldTrade1")==1234,"gold trade keeps -1 sentinel");trade.cancelTrade();
        Client combine=new Client(100);combine.combineNew.itemsCombine.add(combine.inventory.itemsBag.get(99));CombineService.gI().reOpenItemCombine(combine);DataInputStream packet=combine.packet(-81);check(packet.readByte()==1&&packet.readByte()==1&&packet.readUnsignedByte()==99&&packet.available()==0,"combine sends unsigned final-slot index99");
        Client consign=new Client(100);consign.inventory.itemsBag.get(99).itemOptions.add(new Item.ItemOption(86,0));var listing=nro.models.shop_ky_gui.ConsignShopService.gI().getItemCanKiGui(consign);check(listing.size()==1&&listing.get(0).id==99&&listing.get(0).itemId==1899,"consignment identifier99 stays positive in short field");
        Client cap=new Client(99);check(InventoryService.gI().addItemBag(cap,item(517,1))&&cap.inventory.itemsBag.size()==100&&!InventoryService.gI().addItemBag(cap,item(517,1))&&cap.inventory.itemsBag.size()==100&&Inventory.MAX_ITEMS_BAG==100,"capacity expands99 to100 and refuses further slots");
        boolean refused=false;try{BagProtocol.items(new Client(101));}catch(IllegalStateException expected){refused=true;}check(refused,"oversized unmigrated bag refused without truncating items");
        check(Arrays.stream(Inventory.class.getDeclaredFields()).noneMatch(f->f.getName().equals("bagPage")),"inventory has no page state");
        System.out.println("PASS: "+checks+" single-bag100 legacy packet and inventory checks; no database or live server");
    }catch(Throwable error){error.printStackTrace();System.exit(1);}System.exit(0);}
}