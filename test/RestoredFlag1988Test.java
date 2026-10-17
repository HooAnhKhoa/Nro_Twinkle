import java.io.*;import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;
import nro.models.data.LocalManager;import nro.models.server.Manager;import nro.models.services.*;import nro.models.player.Player;import nro.models.item.Item;import nro.models.network.Message;
public class RestoredFlag1988Test{
 static int checks;static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class Recorder extends Player{int command;byte[] bytes;@Override public void sendMessage(Message msg){command=msg.command;bytes=msg.getData().clone();}}
 public static void main(String[] args)throws Exception{
  try{
   try(var c=LocalManager.getConnection()){check("nro_teamobi_qa_20261017_v3".equals(c.getCatalog()),"isolated canonical QA guard");}
   Manager.gI();
   var template=ItemService.gI().getTemplate(1988);check(template.id==1988&&template.type==11&&template.part==176&&template.iconID==16758,"canonical item IDs and type preserved");
   var flag=FlagBagService.gI().getFlagBag(176);check(flag!=null&&flag.iconId==16758&&Arrays.equals(flag.iconEffect,new short[]{16754,16755,16756,16757}),"Manager loads four recovered original frames");
   check(Manager.FLAGS_BAGS.stream().filter(f->f.id==176).count()==1,"flag 176 exists exactly once");
   Set<String> fingerprints=new HashSet<>();
   for(int id=16754;id<=16757;id++){
    for(int zoom=1;zoom<=4;zoom++){
     Path path=Path.of(args[0],"data/icon/x"+zoom+"/"+id+".png");var image=ImageIO.read(path.toFile());
     check(image!=null&&image.getWidth()==165*zoom&&image.getHeight()==100*zoom&&image.getColorModel().hasAlpha(),"transparent original frame "+id+" x"+zoom+" correct canvas");
    }
    fingerprints.add(Base64.getEncoder().encodeToString(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(Path.of(args[0],"data/icon/x4/"+id+".png")))));
   }
   check(fingerprints.size()==4,"animation has four distinct original frames");
   check(CatalogAssets.available(1988)&&!CatalogAssets.available(2082),"restored item enabled while unrelated incomplete costume stays blocked");
   Recorder p=new Recorder();p.gender=0;p.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();p.nPoint.power=60_000_000_000L;p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;while(p.inventory.itemsBody.size()<11)p.inventory.itemsBody.add(new Item());while(p.inventory.itemsBag.size()<100)p.inventory.itemsBag.add(new Item());
   Item item=ItemService.gI().createNewItem((short)1988);var result=InventoryService.gI().putItemBody(p,item);
   check(result!=item&&p.inventory.itemsBody.get(8)==item&&p.getFlagBag()==176,"actual equipment selects accessory slot 8 and flag 176");
   FlagBagService.gI().sendIconEffectFlag(p,176);
   check(p.command==-63,"native flag animation response");
   var in=new DataInputStream(new ByteArrayInputStream(p.bytes));
   check(in.readUnsignedByte()==176&&in.readUnsignedByte()==4,"unsigned flag 176 and frame count decoded correctly");
   for(int id=16754;id<=16757;id++)if(in.readUnsignedShort()!=id)throw new AssertionError("Frame changed");
   check(in.available()==0,"native animation packet has all four frames and no trailing bytes");
   FlagBagService.gI().sendIconFlagChoose(p,176);in=new DataInputStream(new ByteArrayInputStream(p.bytes));
   check(p.command==-62&&in.readUnsignedByte()==176&&in.readUnsignedByte()==5&&in.readUnsignedShort()==16758,"native preview response includes correct inventory icon");
   System.out.println("PASS: "+checks+" restored flag checks");
  }finally{LocalManager.close();}
  System.exit(0);
 }
}
