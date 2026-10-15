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
 static class User extends Player {User(int id){isPlayer=true;this.id=id;name="TT QA "+id;session=new MySession(new Socket());session.version=220;nPoint.hp=nPoint.hpMax=100;nPoint.power=10000;}@Override public void sendMessage(Message m){}}
 static Item item(int id,int qty){return ItemService.gI().createNewItem((short)id,qty);}
 static User user(Connection c,int n,int slots)throws Exception{User p=new User(BASE+n);p.inventory.gold=100000000;p.inventory.gem=1000;for(int i=0;i<slots;i++)p.inventory.itemsBag.add(new Item());
  try(PreparedStatement q=c.prepareStatement("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task) VALUES(?,?,1,0,'[0,0,0,0]','[]','[0,0,0,0,0]','[-1,0,0,0,20,0]') ON DUPLICATE KEY UPDATE name=VALUES(name)")){q.setLong(1,p.id);q.setString(2,p.name);q.executeUpdate();}return p;}
 static int qty(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 static void put(Player p,int id,int count){InventoryService.gI().addItemList(p.inventory.itemsBag,item(id,count));}
 static nro.models.map.Map map(int id,byte type){var map=new nro.models.map.Map(id,"QA "+id,(byte)0,(byte)0,(byte)0,(byte)0,type,new int[24][60],new int[]{0},2,20,new ArrayList<>());Manager.MAPS.add(map);return map;}
 static long scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 public static void main(String[] args)throws Exception{try(Connection c=LocalManager.getConnection()){
  check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
  try(Statement s=c.createStatement()){
   s.execute("CREATE TABLE IF NOT EXISTS item_option_template LIKE srcnrofree.item_option_template");
   s.executeUpdate("INSERT IGNORE INTO item_option_template SELECT * FROM srcnrofree.item_option_template");
   s.execute("CREATE TABLE IF NOT EXISTS shop_ky_gui LIKE srcnrofree.shop_ky_gui");
   for(String name:List.of("items_bag","data_inventory","data_side_task"))try(ResultSet r=s.executeQuery("SHOW COLUMNS FROM player LIKE '"+name+"'")){if(!r.next())try(Statement a=c.createStatement()){a.execute("ALTER TABLE player ADD "+name+" LONGTEXT NULL");}}
   s.executeUpdate("DELETE FROM player WHERE id BETWEEN "+BASE+" AND "+(BASE+99));
   TrungThuStore.ensureData(c);TrungThuStore.ensureData(c);s.executeUpdate("DELETE FROM trung_thu_2026_state WHERE player_id BETWEEN "+BASE+" AND "+(BASE+99));s.executeUpdate("DELETE FROM trung_thu_2026_daily WHERE owner_id BETWEEN "+BASE+" AND "+(BASE+99));
   try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){ItemTemplate t=new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),false,r.getInt("power_require"));t.head=(short)r.getInt("head");t.body=(short)r.getInt("body");t.leg=(short)r.getInt("leg");Manager.ITEM_TEMPLATES.add(t);}}
   try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   for(int i=0;i<=110;i++){NpcTemplate t=new NpcTemplate();t.id=i;Manager.NPC_TEMPLATES.add(t);}
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
  User menuUser=user(c,9,8);menuUser.zone=Manager.MAPS.stream().filter(m->m.mapId==5).findFirst().get().zones.get(0);var npc=menuUser.zone.map.npcs.stream().filter(n->n.tempId==41).findFirst().get();npc.openBaseMenu(menuUser);check(menuUser.idMark.getIndexMenu()==202610160,"real Trung Thu NPC opens the event menu");npc.confirmMenu(menuUser,0);npc.confirmMenu(menuUser,0);check(qty(menuUser,1305)==1&&menuUser.inventory.gem==990,"real NPC shop grants moon item for exactly ten gems");
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
  System.out.println("PASS: "+checks+" Trung Thu integration checks; QA only");
 }catch(Throwable error){error.printStackTrace();System.exit(1);}finally{LocalManager.close();}System.exit(0);}
}
