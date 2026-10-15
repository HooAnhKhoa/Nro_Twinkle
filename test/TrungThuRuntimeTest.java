package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import java.lang.reflect.Field;
import nro.models.data.LocalManager;
import nro.models.event.EventManager;
import nro.models.boss.event_trung_thu.MoonBoss;
import nro.models.consts.*;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.network.*;
import nro.models.player.*;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.skill.*;
import nro.models.clan.*;

/** QA database only. No listener, live JVM attachment or production data writes. */
public class TrungThuRuntimeTest {
 static int checks;static final int BASE=2026101600;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static void enabled(boolean value)throws Exception{Field f=EventManager.class.getDeclaredField("switches");f.setAccessible(true);f.set(null,java.util.Map.of("trung_thu",value,"mob_rewards",true));}
 static class Socket extends java.net.Socket{
  @Override public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("127.0.0.1",1);}
  @Override public java.net.InetAddress getInetAddress(){return java.net.InetAddress.getLoopbackAddress();}
  @Override public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(new byte[0]);}
  @Override public java.io.OutputStream getOutputStream(){return new java.io.ByteArrayOutputStream();}
 }
 static class User extends Player {byte[] shopPacket,menuPacket;User(int id){isPlayer=true;this.id=id;name="TT QA "+id;session=new MySession(new Socket());session.version=220;nPoint.hp=nPoint.hpMax=100;nPoint.power=10000;}@Override public void sendMessage(Message m){if(m.command==-44)shopPacket=m.getData();if(m.command==32)menuPacket=m.getData();}}
 static Item item(int id,int qty){return ItemService.gI().createNewItem((short)id,qty);}
 static User user(Connection c,int n,int slots)throws Exception{User p=new User(BASE+n);p.inventory.gold=100000000;p.inventory.gem=1000;for(int i=0;i<slots;i++)p.inventory.itemsBag.add(new Item());
  try(PreparedStatement q=c.prepareStatement("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task) VALUES(?,?,1,0,'[0,0,0,0]','[]','[0,0,0,0,0]','[-1,0,0,0,20,0]') ON DUPLICATE KEY UPDATE name=VALUES(name)")){q.setLong(1,p.id);q.setString(2,p.name);q.executeUpdate();}return p;}
 static int qty(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 static void put(Player p,int id,int count){InventoryService.gI().addItemList(p.inventory.itemsBag,item(id,count));}
 static nro.models.map.Map map(int id,byte type){var map=new nro.models.map.Map(id,"QA "+id,(byte)0,(byte)0,(byte)0,(byte)0,type,new int[24][60],new int[]{0},2,20,new ArrayList<>());Manager.MAPS.add(map);return map;}
 static long scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void loadShop(Connection c)throws Exception{
  Manager.SHOPS.removeIf(shop->TrungThuShop.TAG.equals(shop.tagName));var shop=new nro.models.shop.Shop();shop.npcId=41;shop.tagName=TrungThuShop.TAG;shop.typeShop=0;
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT t.id,t.NAME FROM tab_shop t JOIN shop s ON s.id=t.shop_id WHERE s.tag_name='TRUNG_THU_2026' ORDER BY t.id")){
   while(r.next()){var tab=new nro.models.shop.TabShop();tab.id=r.getInt(1);tab.name=r.getString(2);tab.shop=shop;
    try(Statement q=c.createStatement();ResultSet a=q.executeQuery("SELECT * FROM item_shop WHERE is_sell=1 AND tab_id="+tab.id+" ORDER BY sort_order,id")){while(a.next()){var sale=new nro.models.shop.ItemShop();sale.id=a.getInt("id");sale.tabShop=tab;sale.temp=Manager.ITEM_TEMPLATES.get(a.getInt("temp_id"));sale.cost=a.getInt("cost");sale.typeSell=a.getByte("type_sell");try(PreparedStatement opts=c.prepareStatement("SELECT option_id,param FROM item_shop_option WHERE item_shop_id=?")){opts.setInt(1,sale.id);try(ResultSet rows=opts.executeQuery()){while(rows.next())sale.options.add(new Item.ItemOption(rows.getInt(1),rows.getInt(2)));}}tab.itemShops.add(sale);}}
    shop.tabShops.add(tab);
   }
  }Manager.SHOPS.add(shop);
 }
 static void checkMenuPortrait(User user,int npc,int avatar)throws Exception{
  var input=new java.io.DataInputStream(new java.io.ByteArrayInputStream(user.menuPacket));
  check(input.readUnsignedShort()==npc,"menu keeps the actual Trung Thu NPC identity");input.readUTF();int count=input.readUnsignedByte();for(int i=0;i<count;i++)input.readUTF();
  check(input.available()==2&&input.readUnsignedShort()==avatar,"menu includes the existing rabbit portrait after its buttons");
 }
 public static void main(String[] args)throws Exception{try(Connection c=LocalManager.getConnection()){
  check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
  try(Statement s=c.createStatement()){
   s.execute("CREATE TABLE IF NOT EXISTS item_option_template LIKE srcnrofree.item_option_template");
   s.executeUpdate("INSERT IGNORE INTO item_option_template SELECT * FROM srcnrofree.item_option_template");
   s.execute("CREATE TABLE IF NOT EXISTS shop_ky_gui LIKE srcnrofree.shop_ky_gui");
   for(String table:List.of("shop","tab_shop","item_shop","item_shop_option","npc_template"))s.execute("CREATE TABLE IF NOT EXISTS "+table+" LIKE srcnrofree."+table);
   s.execute("ALTER TABLE tab_shop AUTO_INCREMENT=1000");
   for(String name:List.of("items_bag","data_inventory","data_side_task"))try(ResultSet r=s.executeQuery("SHOW COLUMNS FROM player LIKE '"+name+"'")){if(!r.next())try(Statement a=c.createStatement()){a.execute("ALTER TABLE player ADD "+name+" LONGTEXT NULL");}}
   s.executeUpdate("DELETE FROM player WHERE id BETWEEN "+BASE+" AND "+(BASE+99));
   s.executeUpdate("REPLACE INTO npc_template SELECT * FROM srcnrofree.npc_template WHERE id=41");s.executeUpdate("UPDATE npc_template SET head=120,body=121,leg=122,avatar=0 WHERE id=41");TrungThuStore.ensureData(c);TrungThuStore.ensureData(c);check(scalar(c,"SELECT COUNT(*) FROM npc_template WHERE id=41 AND head=1098 AND body=1099 AND leg=1100 AND avatar=9976")==1,"placeholder NPC migrates once to existing rabbit Chi Chi appearance");s.executeUpdate("UPDATE npc_template SET avatar=1415 WHERE id=41");TrungThuStore.ensureData(c);check(scalar(c,"SELECT avatar FROM npc_template WHERE id=41")==1415,"custom NPC appearance survives startup");s.executeUpdate("UPDATE npc_template SET avatar=9976 WHERE id=41");s.executeUpdate("DELETE FROM trung_thu_2026_state WHERE player_id BETWEEN "+BASE+" AND "+(BASE+99));s.executeUpdate("DELETE FROM trung_thu_2026_daily WHERE owner_id BETWEEN "+BASE+" AND "+(BASE+99));
   try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){ItemTemplate t=new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),false,r.getInt("power_require"));t.head=(short)r.getInt("head");t.body=(short)r.getInt("body");t.leg=(short)r.getInt("leg");Manager.ITEM_TEMPLATES.add(t);}}
   try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   for(int i=0;i<=110;i++){NpcTemplate t=new NpcTemplate();t.id=i;t.avatar=i==41?9976:0;Manager.NPC_TEMPLATES.add(t);}
  }
  for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());
  for(int id:new int[]{Skill.GALICK,Skill.ANTOMIC,Skill.DRAGON,Skill.KAMEJOKO,Skill.DEMON,Skill.MASENKO,Skill.THAI_DUONG_HA_SAN}){SkillTemplate t=new SkillTemplate();t.id=(byte)id;t.name="QA skill "+id;Skill skill=new Skill();skill.template=t;skill.point=1;skill.coolDown=1000;t.skillss.add(skill);Manager.NCLASS.get(0).skillTemplatess.add(t);}
  for(String key:List.of("tails","carrots","lantern","normal-box","vegetarian-box")){
   var table=TrungThuRewards.table(key);java.util.Map<String,Integer> counts=new HashMap<>();for(int draw=0;draw<10000;draw++){var row=TrungThuRewards.select(table,draw);counts.merge(row.kind()+":"+row.days(),1,Integer::sum);}for(var row:table)check(counts.get(row.kind()+":"+row.days())==row.weight(),key+" exact weight for "+row.kind()+"/"+row.days());
  }
  enabled(true);Manager.RATE_EXP_SERVER=1;
  User cook=user(c,1,12);put(cook,888,10);put(cook,889,10);put(cook,886,2);put(cook,887,1);TrungThuCommerce.craft(cook,0);
  check(qty(cook,890)==1&&qty(cook,888)==0&&cook.inventory.gold==80000000,"cake recipe grants one cake and charges exact ingredients/gold");
  try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT items_bag,data_inventory FROM player WHERE id="+cook.id)){r.next();check(r.getString(1).contains("890")&&r.getString(2).startsWith("[80000000,"),"crafted cake and currency saved together before acknowledgement");}
  User missing=user(c,2,8);put(missing,888,9);long gold=missing.inventory.gold;TrungThuCommerce.craft(missing,0);check(qty(missing,888)==9&&missing.inventory.gold==gold,"missing recipe cannot consume items or gold");
  User full=user(c,3,1);put(full,1316,2);var exchanger=new BagExchange(0,System::currentTimeMillis);check(exchanger.execute(full,TrungThuCommerce.cost(java.util.Map.of(1316,1),0,0),TrungThuCommerce.gain(item(220,1)),TrungThuStore::persistBag)==BagExchange.Result.BAG_FULL&&qty(full,1316)==2,"box stack is restored when reward has no slot");
  User money=user(c,4,1);put(money,1316,1);money.inventory.ruby=Integer.MAX_VALUE;check(exchanger.execute(money,TrungThuCommerce.cost(java.util.Map.of(1316,1),0,0),new BagExchange.Gain(List.of(),0,0,1),TrungThuStore::persistBag)==BagExchange.Result.CURRENCY_LIMIT&&qty(money,1316)==1,"overflow cannot destroy the opened box");
  money.inventory.ruby=0;check(exchanger.execute(money,TrungThuCommerce.cost(java.util.Map.of(1316,1),0,0),new BagExchange.Gain(List.of(),2000000,0,0),TrungThuStore::persistBag)==BagExchange.Result.SUCCESS&&qty(money,1316)==0&&money.inventory.gold==102000000,"currency box reward charges one box and persists without a free slot");
  check(TrungThuStore.multiplier(cook)==1,"new player starts without event EXP");long until=System.currentTimeMillis()+60000;TrungThuStore.grant(cook.id,"monkey_until",until);TrungThuStore.grant(cook.id,"monkey_until",until-1000);check(TrungThuStore.state(cook).monkeyUntil()==until&&TrungThuStore.multiplier(new User((int)cook.id))==3,"absolute buff survives reconnect and shorter grants cannot overwrite it");
  var normal=map(37,(byte)0);var dungeon=map(59,(byte)2);var castle=map(153,(byte)0);map(20,(byte)0);map(19,(byte)0);map(84,(byte)0);map(5,(byte)0);cook.zone=normal.zones.get(0);
  long baseline=cook.nPoint.calSucManhTiemNang(100);check(baseline==300,"active monkey buff yields exactly X3 before unrelated modifiers");cook.charms.tdTriTue4=System.currentTimeMillis()+60000;cook.itemTime.isUseDK=true;check(cook.nPoint.calSucManhTiemNang(100)==baseline,"event EXP does not stack with intellect charms or monkey-tail EXP");
  TrungThuRuntime.init();check(normal.npcs.isEmpty()&&Manager.MAPS.stream().filter(m->m.mapId==5||m.mapId==84).allMatch(m->m.npcs.stream().anyMatch(n->n.tempId==41)),"Trung Thu NPC seeded only at Kame and supermarket");
  MoonBoss.Monkey monkey=new MoonBoss.Monkey();monkey.rest();check(monkey.bossStatus==BossStatus.REST&&!monkey.panelCanSpawn(),"monkey never auto-spawns or accepts free panel summons");
  cook.playerSkill.skillSelect=nro.models.utils.SkillUtil.createSkill(Skill.GALICK,1);check(monkey.accepts(cook),"monkey accepts basic punch");cook.playerSkill.skillSelect=nro.models.utils.SkillUtil.createSkill(Skill.THAI_DUONG_HA_SAN,1);check(!monkey.accepts(cook),"monkey rejects crowd control and non-basic skills");EffectSkillService.gI().setThoiMien(monkey,1,1000);EffectSkillService.gI().setSocola(monkey,1,1000);EffectSkillService.gI().setAnTroi(monkey,cook,1,1000);check(!monkey.effectSkill.isThoiMien&&!monkey.effectSkill.isSocola&&!monkey.effectSkill.anTroi,"monkey remains immune to sleep, chocolate and hold");
  User summon=user(c,5,4);summon.zone=normal.zones.get(0);put(summon,1305,2);TrungThuRuntime.use(summon,InventoryService.gI().findItemBag(summon,1305));check(qty(summon,1305)==1,"paid monkey summon consumes exactly one moon item");TrungThuRuntime.use(summon,InventoryService.gI().findItemBag(summon,1305));check(qty(summon,1305)==1,"second summon cannot charge while boss is reserved");
  User immune=user(c,6,4);for(int i=0;i<6;i++)immune.inventory.itemsBody.add(new Item());immune.inventory.itemsBody.set(5,item(1042,1));TrungThuRuntime.touchRabbit(immune);check(!TrungThuStore.carrot(immune),"rabbit event costume prevents carrot transformation");TrungThuRuntime.touchRabbit(cook);check(TrungThuStore.carrot(cook)&&cook.getBody()==407,"rabbit applies persisted carrot appearance");
  User quest=user(c,7,8);quest.playerTask.sideTask.template=new nro.models.task.SideTaskTemplate();quest.playerTask.sideTask.level=ConstTask.HARD;quest.playerTask.sideTask.count=quest.playerTask.sideTask.maxCount=1;check(TrungThuQuest.pay(quest)&&qty(quest,1311)==1&&qty(quest,1312)==1&&quest.playerTask.sideTask.template==null,"hard quest atomically pays lantern, lotus, base reward and resets task");
  quest.playerTask.sideTask.template=new nro.models.task.SideTaskTemplate();quest.playerTask.sideTask.level=ConstTask.HARD;quest.playerTask.sideTask.count=quest.playerTask.sideTask.maxCount=1;check(!TrungThuQuest.pay(quest)&&qty(quest,1311)==1,"daily quest receipt prevents repeat lantern/lotus rewards");
  Clan clan=new Clan();clan.id=BASE+50;ClanMember leader=new ClanMember();leader.id=(int)cook.id;leader.role=Clan.LEADER;ClanMember offline=new ClanMember();offline.id=BASE+51;clan.members.add(leader);clan.members.add(offline);clan.membersInGame.add(cook);cook.clan=clan;cook.zone=castle.zones.get(0);for(int i=0;i<8;i++)cook.inventory.itemsBody.add(new Item());cook.inventory.itemsBody.set(7,item(1047,1));TrungThuClan.click(cook,"parade");check(scalar(c,"SELECT double_until FROM trung_thu_2026_state WHERE player_id="+(BASE+51))>System.currentTimeMillis(),"clan completion grants EXP to offline members");check(scalar(c,"SELECT COUNT(*) FROM trung_thu_2026_daily WHERE owner_id="+clan.id+" AND reward_kind='parade'")==1,"clan reward has one durable daily receipt");TrungThuClan.click(cook,"parade");check(scalar(c,"SELECT COUNT(*) FROM trung_thu_2026_daily WHERE owner_id="+clan.id+" AND reward_kind='parade'")==1,"repeated clan click cannot claim again");
  check(nro.models.server.PanelBridge.skillDamage(cook,100)==85,"carrot reduces damage through the common skill path by exactly 15 percent");
  MoonBoss.Gogeta gogeta=new MoonBoss.Gogeta();MoonBoss.Omega omega=new MoonBoss.Omega();User fighter=user(c,8,5);put(fighter,1304,1);TrungThuStore.side(fighter,1);fighter.cFlag=1;check(!gogeta.accepts(fighter)&&omega.accepts(fighter),"supporting Gogeta permits only the opposing Omega boss");fighter.cFlag=0;check(!omega.accepts(fighter),"rival fight requires the currently selected team flag");fighter.cFlag=1;InventoryService.gI().subQuantityItemsBag(fighter,InventoryService.gI().findItemBag(fighter,1304),1);check(!omega.accepts(fighter),"rival fight requires a wooden sword");
  monkey.arm(normal.zones.get(0));monkey.respawn();monkey.joinMap();check(monkey.zone==normal.zones.get(0),"reserved monkey really joins its selected map and zone");cook.playerSkill.skillSelect=nro.models.utils.SkillUtil.createSkill(Skill.GALICK,1);int ground=monkey.zone.items.size();monkey.reward(cook);monkey.reward(cook);check(monkey.zone.items.size()==ground+1,"duplicate kill reward cannot drop a second set of tails");check(scalar(c,"SELECT monkey_until FROM trung_thu_2026_state WHERE player_id="+(BASE+51))>System.currentTimeMillis(),"monkey kill grants clan X3 to an offline member");
  User menuUser=user(c,9,8);menuUser.zone=Manager.MAPS.stream().filter(m->m.mapId==5).findFirst().get().zones.get(0);var npc=menuUser.zone.map.npcs.stream().filter(n->n.tempId==41).findFirst().get();npc.openBaseMenu(menuUser);check(menuUser.idMark.getIndexMenu()==202610160,"real Trung Thu NPC opens the event menu");checkMenuPortrait(menuUser,41,9976);npc.confirmMenu(menuUser,1);checkMenuPortrait(menuUser,41,9976);npc.createOtherMenu(menuUser,202610160,"QA object menu",new String[]{"OK"},"QA");checkMenuPortrait(menuUser,41,9976);loadShop(c);npc.confirmMenu(menuUser,0);check(menuUser.shopPacket!=null&&menuUser.shopPacket[0]==0&&menuUser.shopPacket[1]==4&&TrungThuShop.TAG.equals(menuUser.idMark.getShopOpen().tagName),"NPC opens native shop with four tabs");nro.models.shop.ShopService.gI().takeItem(menuUser,(byte)0,1305);check(qty(menuUser,1305)==1&&menuUser.inventory.gem==990,"real NPC shop grants moon item for exactly ten gems");
  try(var packet=new java.io.DataInputStream(new java.io.ByteArrayInputStream(menuUser.shopPacket))){packet.readByte();int tabs=packet.readUnsignedByte(),items=0;Set<Integer> sold=new HashSet<>();for(int t=0;t<tabs;t++){packet.readUTF();for(int n=packet.readUnsignedByte();n>0;n--){int id=packet.readShort();sold.add(id);items++;int goldCost=packet.readInt(),gemCost=packet.readInt();if(id==1305)check(goldCost==0&&gemCost==10,"native moon row displays ten green gems");if(id==1310)check(goldCost==10000000&&gemCost==0,"native rabbit row displays ten million gold");for(int options=packet.readUnsignedByte();options>0;options--){packet.readUnsignedByte();packet.readShort();}packet.readByte();if(packet.readByte()==1)for(int part=0;part<4;part++)packet.readShort();}}check(items==11&&sold.size()==11&&packet.available()==0,"native decoder reads eleven distinct items with valid icons/costumes/prices");}
  for(int action=0;action<9;action++){
   User preview=user(c,20+action,16);preview.zone=menuUser.zone;
   for(int id:new int[]{886,887,888,889,1312,890,891,1313,1045,462,1311})put(preview,id,999);
   String before=TrungThuStore.bag(preview);long beforeGold=preview.inventory.gold;int beforeGems=preview.inventory.gem;
   npc.openBaseMenu(preview);
   if(action<6){npc.confirmMenu(preview,action<3?1:2);npc.confirmMenu(preview,action%3);}else npc.confirmMenu(preview,action-3);
   var reader=new java.io.DataInputStream(new java.io.ByteArrayInputStream(preview.menuPacket));reader.readShort();String text=reader.readUTF();
   var price=action<3?TrungThuCommerce.craftCost(action):action<6?TrungThuCommerce.petCost(action-3):TrungThuCommerce.exchangeCost(new String[]{"tails","carrots","lantern"}[action-6]);
   for(var entry:price.items().entrySet())check(text.contains(Manager.ITEM_TEMPLATES.get(entry.getKey()).name+" 999/"+entry.getValue()),"confirmation "+action+" shows actual item name and owned/needed count");
   check(reader.readUnsignedByte()==2&&reader.readUTF().equals("Đồng ý")&&reader.readUTF().equals("Từ chối")&&reader.readUnsignedShort()==9976&&reader.available()==0,"confirmation "+action+" has two buttons and NPC portrait");
   check(TrungThuStore.bag(preview).equals(before)&&preview.inventory.gold==beforeGold&&preview.inventory.gem==beforeGems,"opening confirmation "+action+" never consumes inventory/payment");
   npc.confirmMenu(preview,1);npc.confirmMenu(preview,0);
   check(TrungThuStore.bag(preview).equals(before)&&preview.inventory.gold==beforeGold&&preview.inventory.gem==beforeGems,"cancelled confirmation "+action+" cannot execute later");
   if(action==0)check(text.contains("Giá vàng: 20.000.000"),"gold price is grouped for readability");
   if(action==1||action==2)check(text.contains("Giá ngọc xanh: 20"),"premium recipe displays green gem cost");
  }
  User confirm=user(c,30,16);confirm.zone=menuUser.zone;confirm.inventory.itemsBag.set(0,item(888,6));confirm.inventory.itemsBag.set(1,item(888,4));put(confirm,889,10);put(confirm,886,2);put(confirm,887,1);
  npc.openBaseMenu(confirm);npc.confirmMenu(confirm,1);npc.confirmMenu(confirm,0);
  var confirmReader=new java.io.DataInputStream(new java.io.ByteArrayInputStream(confirm.menuPacket));confirmReader.readShort();String confirmText=confirmReader.readUTF();
  check(confirmText.contains(Manager.ITEM_TEMPLATES.get(888).name+" 10/10"),"preview sums ingredients split across inventory stacks");
  npc.confirmMenu(confirm,0);npc.confirmMenu(confirm,0);
  check(qty(confirm,890)==1&&qty(confirm,888)==0&&confirm.inventory.gold==80000000,"agree crafts once and repeated confirmation cannot buy again");
  User stale=user(c,31,16);stale.zone=menuUser.zone;put(stale,888,10);put(stale,889,10);put(stale,886,2);put(stale,887,1);
  npc.openBaseMenu(stale);npc.confirmMenu(stale,1);npc.confirmMenu(stale,0);InventoryService.gI().subQuantityItemsBag(stale,InventoryService.gI().findItemBag(stale,888),1);npc.confirmMenu(stale,0);
  check(qty(stale,888)==9&&qty(stale,890)==0&&stale.inventory.gold==100000000,"agree rechecks current ingredients after preview");
  User emptyPreview=user(c,32,16);emptyPreview.zone=menuUser.zone;npc.openBaseMenu(emptyPreview);npc.confirmMenu(emptyPreview,1);npc.confirmMenu(emptyPreview,0);
  var emptyReader=new java.io.DataInputStream(new java.io.ByteArrayInputStream(emptyPreview.menuPacket));emptyReader.readShort();
  check(emptyReader.readUTF().contains(ConstFont.BOLD_RED+Manager.ITEM_TEMPLATES.get(888).name+" 0/10"),"missing ingredient count is red in confirmation");
  User goldBuyer=user(c,14,3);TrungThuShop.open(goldBuyer);nro.models.shop.ShopService.gI().takeItem(goldBuyer,(byte)0,1310);check(qty(goldBuyer,1310)==1&&goldBuyer.inventory.gold==90000000,"native shop gold purchase uses atomic event exchange");
  long gemBalance=menuUser.inventory.gem;nro.models.shop.ShopService.gI().takeItem(menuUser,(byte)0,999);check(menuUser.inventory.gem==gemBalance&&qty(menuUser,999)==0,"unlisted native item cannot be purchased");
  User poorBuyer=user(c,15,3);poorBuyer.inventory.gem=9;TrungThuShop.open(poorBuyer);nro.models.shop.ShopService.gI().takeItem(poorBuyer,(byte)0,1305);check(poorBuyer.inventory.gem==9&&qty(poorBuyer,1305)==0,"native purchase with insufficient funds keeps gems");
  User packedBuyer=user(c,16,1);put(packedBuyer,888,99999);TrungThuShop.open(packedBuyer);nro.models.shop.ShopService.gI().takeItem(packedBuyer,(byte)0,1305);check(packedBuyer.inventory.gem==1000&&qty(packedBuyer,1305)==0,"native purchase with full bag keeps payment");
  try(Statement s=c.createStatement()){s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id SET i.cost=13 WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=1305");TrungThuShop.ensureData(c);check(scalar(c,"SELECT MAX(i.cost) FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=1305")==13,"idempotent shop migration preserves admin-edited prices");s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id SET i.cost=10 WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=1305");}
  check(scalar(c,"SELECT COUNT(*) FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id WHERE sh.tag_name='TRUNG_THU_2026'")==11,"repeated migration never duplicates shop goods");
  enabled(false);nro.models.shop.ShopService.gI().takeItem(poorBuyer,(byte)0,1305);check(qty(poorBuyer,1305)==0&&poorBuyer.inventory.gem==9,"stale native shop cannot purchase after event OFF");enabled(true);
  User seller=user(c,10,1);var consignment=new nro.models.shop_ky_gui.ConsignItem();consignment.id=BASE+60;consignment.player_sell=(int)seller.id;consignment.itemId=888;consignment.quantity=1;consignment.goldSell=10000;consignment.isBuy=true;
  try(Statement s=c.createStatement()){s.executeUpdate("DELETE FROM shop_ky_gui WHERE id="+consignment.id);s.executeUpdate("INSERT INTO shop_ky_gui(id,player_id,tab,item_id,gold,gem,quantity,itemOption,isUpTop,isBuy) VALUES("+consignment.id+","+seller.id+",3,888,10000,-1,1,'[]',0,1)");}
  nro.models.shop_ky_gui.ConsignShopManager.gI().listItem.add(consignment);TrungThuMarket.claim(seller,consignment);check(seller.inventory.gold==100009000&&qty(seller,457)==0,"Trung Thu consignment returns actual gold after the existing ten-percent fee");TrungThuMarket.claim(seller,consignment);check(seller.inventory.gold==100009000&&scalar(c,"SELECT COUNT(*) FROM shop_ky_gui WHERE id="+consignment.id)==0,"consignment claim is durable and cannot pay twice");
  User failed=user(c,11,2);put(failed,888,10);long failedGold=failed.inventory.gold;check(exchanger.execute(failed,TrungThuCommerce.cost(java.util.Map.of(888,10),20000000,0),TrungThuCommerce.gain(item(890,1)),p->{throw new SQLException("Expected QA save failure");})==BagExchange.Result.SAVE_FAILED&&qty(failed,888)==10&&qty(failed,890)==0&&failed.inventory.gold==failedGold,"save failure restores consumed ingredients, reward and money");
  User hard=user(c,12,1);hard.playerTask.sideTask.template=new nro.models.task.SideTaskTemplate();hard.playerTask.sideTask.level=ConstTask.VERY_HARD;hard.playerTask.sideTask.count=hard.playerTask.sideTask.maxCount=1;check(TrungThuQuest.pay(hard)&&hard.playerTask.sideTask.template!=null&&qty(hard,1311)==0,"full quest bag keeps the task unclaimed");
  var mob=new nro.models.mob.Mob();mob.zone=normal.zones.get(0);mob.tempId=1;java.util.List<nro.models.map.ItemMap> drops=new ArrayList<>();Field rng=TrungThuRuntime.class.getDeclaredField("RANDOM");rng.setAccessible(true);((Random)rng.get(null)).setSeed(20261015);
  for(int i=0;i<1000;i++)TrungThuRuntime.drops(mob,cook,drops,100,100);check(drops.size()>0&&drops.stream().allMatch(d->d.itemTemplate.id==886||d.itemTemplate.id==887),"ordinary world mobs drop only the approved egg/chicken ingredients");
  drops.clear();mob.zone=dungeon.zones.get(0);for(int i=0;i<1000;i++)TrungThuRuntime.drops(mob,cook,drops,100,100);check(drops.size()>0&&drops.stream().allMatch(d->d.itemTemplate.id==888||d.itemTemplate.id==889),"dungeon mobs drop flour/beans with a separate loot path");
  drops.clear();mob.tempId=ConstMob.MOC_NHAN;TrungThuRuntime.drops(mob,cook,drops,100,100);check(drops.isEmpty(),"training dummy cannot farm Trung Thu ingredients");
  Field flags=EventManager.class.getDeclaredField("switches");flags.setAccessible(true);flags.set(null,java.util.Map.of("trung_thu",true,"mob_rewards",false));mob.tempId=1;for(int i=0;i<1000;i++)TrungThuRuntime.drops(mob,cook,drops,100,100);check(drops.isEmpty(),"global EV-material switch also stops Trung Thu mob ingredients");
  enabled(false);monkey.update();check(monkey.zone==null&&monkey.bossStatus==BossStatus.REST,"event OFF removes a summoned boss without death rewards");long beforeGold=missing.inventory.gold;TrungThuCommerce.buy(missing,"moon","price.moon-gems",true);check(qty(missing,1305)==0&&missing.inventory.gold==beforeGold&&TrungThuStore.multiplier(cook)==1,"event OFF blocks purchases and event EXP");
  java.lang.reflect.Method poll=nro.models.server.PanelBridge.class.getDeclaredMethod("poll");poll.setAccessible(true);
  try(Statement s=c.createStatement()){
   for(String file:List.of("drop_rates.sql","drop_rules.sql"))for(String sql:java.nio.file.Files.readString(java.nio.file.Path.of(file)).split(";"))if(!sql.isBlank())s.execute(sql);
   s.executeUpdate("UPDATE server_rates SET drop_multiplier=5 WHERE id=1");s.executeUpdate("UPDATE panel_drop_rate SET value=2,mode='multiplier' WHERE category='trung_thu'");s.executeUpdate("UPDATE panel_drop_rule SET percent_override=NULL WHERE category='trung_thu'");poll.invoke(null);enabled(true);
   check(nro.models.server.PanelBridge.dropChance("trung_thu","trung_thu.egg",2,100)==20,"Trung Thu inherits both group and server multipliers");
   s.executeUpdate("UPDATE panel_drop_rule SET percent_override=CASE WHEN rule_key IN ('trung_thu.egg','trung_thu.flour','trung_thu.gogeta_costume') THEN 100 ELSE 0 END WHERE category='trung_thu'");poll.invoke(null);enabled(true);
   check(nro.models.server.PanelBridge.dropChance("trung_thu","trung_thu.egg",2,100)==100&&nro.models.server.PanelBridge.dropChance("trung_thu","trung_thu.chicken",2,100)==0,"per-item percent replaces multipliers independently");
   drops.clear();mob.zone=normal.zones.get(0);for(int i=0;i<20;i++)TrungThuRuntime.drops(mob,cook,drops,100,100);check(drops.size()==20&&drops.stream().allMatch(d->d.itemTemplate.id==886),"real mob path applies egg100/chicken0 after bridge reload");
   drops.clear();mob.zone=dungeon.zones.get(0);for(int i=0;i<20;i++)TrungThuRuntime.drops(mob,cook,drops,100,100);check(drops.size()==20&&drops.stream().allMatch(d->d.itemTemplate.id==888),"real dungeon path applies flour100/beans0 without world ingredients");
   put(fighter,1304,1);TrungThuStore.side(fighter,2);fighter.cFlag=2;gogeta.zone=normal.zones.get(0);int countBefore=gogeta.zone.items.size();gogeta.reward(fighter);gogeta.reward(fighter);check(gogeta.zone.items.size()==countBefore+1&&gogeta.zone.items.get(0).itemTemplate.id==1697,"Gogeta costume100/board0 produces one reward and blocks duplicate kills");
   TrungThuStore.side(fighter,1);fighter.cFlag=1;omega.zone=normal.zones.get(0);countBefore=omega.zone.items.size();omega.reward(fighter);check(omega.zone.items.size()==countBefore,"Omega zero overrides remain independent of Gogeta");
   s.executeUpdate("UPDATE panel_drop_rate SET value=0 WHERE category='trung_thu'");poll.invoke(null);enabled(true);check(!nro.models.server.PanelBridge.rollDrop("trung_thu","trung_thu.egg",2,100),"group x0 still disables explicit100 percent");
   s.executeUpdate("UPDATE panel_drop_rate SET value=1 WHERE category='trung_thu'");s.executeUpdate("UPDATE server_rates SET drop_multiplier=0 WHERE id=1");poll.invoke(null);enabled(true);check(!nro.models.server.PanelBridge.rollDrop("trung_thu","trung_thu.egg",2,100),"server x0 still disables explicit100 percent");
   s.executeUpdate("UPDATE server_rates SET drop_multiplier=1 WHERE id=1");poll.invoke(null);enabled(false);check(!nro.models.server.PanelBridge.rollDrop("trung_thu","trung_thu.egg",2,100),"event OFF gates explicit override");flags.set(null,java.util.Map.of("trung_thu",true,"mob_rewards",false));check(!nro.models.server.PanelBridge.rollDrop("trung_thu","trung_thu.egg",2,100),"EV-material OFF gates explicit override");
   s.executeUpdate("UPDATE panel_drop_rule SET percent_override=NULL WHERE category='trung_thu'");poll.invoke(null);enabled(true);check(nro.models.server.PanelBridge.dropChance("trung_thu","trung_thu.egg",2,100)==2,"restore default removes only custom override");
  }
  System.out.println("PASS: "+checks+" Trung Thu integration checks; QA only");
 }catch(Throwable error){error.printStackTrace();System.exit(1);}finally{LocalManager.close();}System.exit(0);}
}
