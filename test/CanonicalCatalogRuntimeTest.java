import java.sql.*;import java.util.*;import java.nio.file.*;import nro.models.server.Manager;import nro.models.data.LocalManager;import nro.models.services.*;import nro.models.player.Player;import nro.models.item.Item;import nro.models.intrinsic.Intrinsic;
public class CanonicalCatalogRuntimeTest {
 static int checks;static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static void ShopServiceTestBuy(Player p,int id){nro.models.shop.ShopService.gI().buyItem(p,id);}
 public static void main(String[] args)throws Exception {try{
  try(var c=LocalManager.getConnection()){check("nro_teamobi_qa_20261017_v3".equals(c.getCatalog()),"isolated canonical QA guard");}
  Manager.gI();check(Manager.ITEM_TEMPLATES.size()==2103,"actual Manager loads all 2103 canonical templates");
  for(int i=0;i<2103;i++)if(Manager.ITEM_TEMPLATES.get(i).id!=i)throw new AssertionError("index mismatch "+i);check(true,"template index matches ID at every slot");
  check(Manager.ITEM_OPTION_TEMPLATES.size()<=255,"existing option packet stays within one-byte limit");
  check(ItemService.gI().getTemplate(2000).name.equals("Đá Linh Hồn"),"ID2000 is canonical soul stone, no custom ticket");
  check(nro.models.event.ThanhCo.TICKET==1876&&ItemService.gI().getTemplate(1876).name.equals("Vé vàng"),"castle entry uses real canonical ticket");
  check(ItemService.gI().getTemplate(1884).name.equals("Bông tai Porata")&&ItemService.gI().getTemplate(1868).name.equals("Mảnh vỡ bông tai cấp 3"),"earring and material use canonical IDs");
  Player player=new Player();player.gender=0;player.playerIntrinsic.intrinsic=new Intrinsic();for(int i=0;i<11;i++)player.inventory.itemsBody.add(new Item());for(int i=0;i<100;i++)player.inventory.itemsBag.add(new Item());player.nPoint.limitPower=8;player.nPoint.power=60_000_000_000L;player.nPoint.hpg=10000;player.nPoint.mpg=10000;player.nPoint.dameg=1000;player.nPoint.calPoint();
  Item costume=ItemService.gI().createNewItem((short)2075);player.inventory.itemsBag.set(0,costume);Item previous=InventoryService.gI().putItemBody(player,costume);check(previous!=costume&&player.inventory.itemsBody.get(5)==costume,"actual inventory equips Nappa Aura");
  check(player.getHead()==2053&&player.getBody()==2054&&player.getLeg()==2055,"equipped Nappa sends exact head/body/leg");
  int beforeHp=player.nPoint.hpMax,beforeSd=player.nPoint.dame;costume.itemOptions.add(new Item.ItemOption(50,20));costume.itemOptions.add(new Item.ItemOption(77,20));player.nPoint.calPoint();check(player.nPoint.hpMax>beforeHp&&player.nPoint.dame>beforeSd,"Nappa equipment options actually affect HP and damage");
  var pet=ItemService.gI().createNewItem((short)1813);Item replaced=InventoryService.gI().putItemBody(player,pet);check(player.inventory.itemsBody.get(7)==pet&&replaced!=pet,"canonical type18 pet equips in companion slot");
  check(ItemService.gI().getTemplate(1505).isUpToUp&&ItemService.gI().getTemplate(1509).isUpToUp&&ItemService.gI().getTemplate(1628).isUpToUp,"paper, bow and pet charm stacking survives bootstrap");
  var bytes=Files.readAllBytes(Path.of("data/update_data/part"));try(var in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes))){check(in.readUnsignedShort()==2082,"part packet contains canonical count");for(int i=0;i<2082;i++){int type=in.readUnsignedByte(),count=type==0?3:type==1?17:14;for(int j=0;j<count;j++){in.readUnsignedShort();in.readByte();in.readByte();}}check(in.available()==0,"all part frames decode without protocol drift");}
  nro.models.event.trungthu2026.TrungThuConfig.validate();check(true,"Trung Thu config references valid canonical templates");
  check(CatalogAssets.available(2075)&&!CatalogAssets.available(2082),"asset readiness allows Nappa and blocks missing Beerus frames");
  Item incomplete=ItemService.gI().createNewItem((short)2082);long gold=player.inventory.gold;int gems=player.inventory.gem;
  check(InventoryService.gI().putItemBody(player,incomplete)==incomplete&&player.inventory.itemsBody.get(5)==costume,"missing model cannot replace equipped costume");
  ShopServiceTestBuy(player,2082);check(player.inventory.gold==gold&&player.inventory.gem==gems&&incomplete.quantity==1,"blocked purchase does not spend currency or item");
  System.out.println("PASS: "+checks+" canonical runtime checks");
 }finally{LocalManager.close();}System.exit(0);}
}
