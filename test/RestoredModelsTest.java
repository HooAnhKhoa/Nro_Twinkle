import java.io.*;import java.nio.file.*;import java.util.*;import javax.imageio.ImageIO;
import com.google.gson.*;import nro.models.data.LocalManager;import nro.models.server.Manager;import nro.models.services.*;import nro.models.player.Player;import nro.models.item.Item;import nro.models.network.Message;
public class RestoredModelsTest{
 static int checks;static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class Recorder extends Player{int command;byte[] bytes;@Override public void sendMessage(Message msg){command=msg.command;bytes=msg.getData().clone();}}
 public static void main(String[] args)throws Exception{
  try{
   try(var c=LocalManager.getConnection()){check("nro_teamobi_qa_20261017_v3".equals(c.getCatalog()),"isolated canonical QA guard");}
   Manager.gI();var definitions=new JsonParser().parse(Files.readString(Path.of(args[0],"config/teamobi-restored-flags.json"))).getAsJsonArray();Set<Integer> flags=new HashSet<>();int verifiedImages=0;
   for(var element:definitions){
    var d=element.getAsJsonObject();int id=d.get("item_id").getAsInt(),flagId=d.get("flag_id").getAsInt(),icon=d.get("icon_id").getAsInt();
    var t=ItemService.gI().getTemplate(id);check(t.id==id&&t.type==11&&t.part==flagId&&t.iconID==icon,"canonical item "+id+" has valid recovered appearance reference");
    var flag=FlagBagService.gI().getFlagBag(flagId);check(flag!=null&&flag.iconId==icon&&flag.iconEffect.length==d.getAsJsonArray("frames").size(),"flag frames loaded for "+id);
    check(CatalogAssets.available(id),"restored item "+id+" enabled");
    for(int n=0;n<flag.iconEffect.length;n++){
     if(flag.iconEffect[n]!=d.getAsJsonArray("frames").get(n).getAsInt())throw new AssertionError("Frame sequence mismatch");
     for(int zoom=1;zoom<=4;zoom++){var image=ImageIO.read(Path.of(args[0],"data/icon/x"+zoom+"/"+flag.iconEffect[n]+".png").toFile());if(image==null||image.getWidth()<=1||image.getHeight()<=1)throw new AssertionError("Missing/placeholder frame");verifiedImages++;}
    }
    Recorder p=new Recorder();p.gender=0;p.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();p.nPoint.power=60_000_000_000L;p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;while(p.inventory.itemsBody.size()<11)p.inventory.itemsBody.add(new Item());while(p.inventory.itemsBag.size()<100)p.inventory.itemsBag.add(new Item());
    Item item=ItemService.gI().createNewItem((short)id);Item replaced=InventoryService.gI().putItemBody(p,item);
    check(replaced!=item&&p.inventory.itemsBody.get(8)==item&&p.getFlagBag()==flagId,"actual item "+id+" equips and publishes correct flag");
    FlagBagService.gI().sendIconEffectFlag(p,flagId);var in=new DataInputStream(new ByteArrayInputStream(p.bytes));
    check(p.command==-63&&in.readUnsignedByte()==flagId&&in.readUnsignedByte()==flag.iconEffect.length,"native animation packet header "+id);
    for(short frame:flag.iconEffect)if(in.readUnsignedShort()!=frame)throw new AssertionError("Wire frame mismatch");
    check(in.available()==0,"all native frame bytes consumed for "+id);
    flags.add(flagId);
   }
   for(int f:flags)if(Manager.FLAGS_BAGS.stream().filter(x->x.id==f).count()!=1)throw new AssertionError("Duplicate flag");
   check(flags.size()==22,"shared accessory frames are not duplicated");
   check(verifiedImages>500,"verified original frame files at all zooms: "+verifiedImages);
   check(!CatalogAssets.available(1749)&&!CatalogAssets.available(2062)&&!CatalogAssets.available(2082),"missing mount, blank costume and absent icon remain blocked");
   check(ItemService.gI().getTemplate(1706).part==143&&ItemService.gI().getTemplate(1702).part==143,"scythe reuses existing identical animation instead of truncated flag 300");
   System.out.println("PASS: "+checks+" restored model checks for "+definitions.size()+" items");
  }finally{LocalManager.close();}
  System.exit(0);
 }
}
