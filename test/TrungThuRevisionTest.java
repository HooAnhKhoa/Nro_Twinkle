package nro.models.event.trungthu2026;
import java.sql.*;import java.util.*;import java.lang.reflect.*;
import nro.models.item.Item;import nro.models.clan.*;import nro.models.map.*;import nro.models.boss.event_trung_thu.MoonBoss;import nro.models.server.Manager;import nro.models.consts.ConstTask;
public final class TrungThuRevisionTest{
 static int checks;
 static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static long scalar(Connection c,String sql)throws Exception{return TrungThuRuntimeTest.scalar(c,sql);}
 static TrungThuRuntimeTest.User user(Connection c,int n,int slots)throws Exception{return TrungThuRuntimeTest.user(c,n,slots);}
 static void put(TrungThuRuntimeTest.User p,int id,int qty){TrungThuRuntimeTest.put(p,id,qty);}
 static int qty(TrungThuRuntimeTest.User p,int id){return TrungThuRuntimeTest.qty(p,id);}
 static ClanMember member(TrungThuRuntimeTest.User p,boolean leader){var m=new ClanMember();m.id=(int)p.id;m.name=p.name;m.role=leader?Clan.LEADER:1;return m;}
 static void lantern(TrungThuRuntimeTest.User p){while(p.inventory.itemsBody.size()<11)p.inventory.itemsBody.add(new Item());p.inventory.itemsBody.set(8,TrungThuRuntimeTest.item(1888,1));}
 static void task(TrungThuRuntimeTest.User p,int level){p.playerTask.sideTask.template=new nro.models.task.SideTaskTemplate();p.playerTask.sideTask.level=(byte)level;p.playerTask.sideTask.count=p.playerTask.sideTask.maxCount=1;}
 public static void run(Connection c)throws Exception{
  check(TrungThuCommerce.boxKind(2078).equals("normal-box")&&TrungThuCommerce.boxKind(1316).equals("normal-box"),"2026 normal box and already-owned legacy box use same rewards");
  check(TrungThuCommerce.boxKind(2080).equals("vegetarian-box")&&TrungThuCommerce.boxKind(1317).equals("vegetarian-box")&&TrungThuCommerce.boxKind(2079)==null,"2026 vegetarian and legacy box recognized; ranking VIP box excluded");
  for(int id:new int[]{2078,2080,1316,1317}){
   var p=user(c,40+(id%10)+(id<2000?10:0),8);put(p,id,1);TrungThuRuntime.use(p,nro.models.services.InventoryService.gI().findItemBag(p,id));check(qty(p,id)==0,"real use path opens box "+id);
  }
  var permanent=TrungThuRewards.reward(TrungThuRewards.select(TrungThuRewards.table("lantern"),9999),new Random(0)).items().get(0);
  check(permanent.template.id==1888&&permanent.itemOptions.stream().noneMatch(o->o.optionTemplate.id==93),"treo den permanent reward is exact canonical two-can lantern");
  var timed=TrungThuRewards.reward(TrungThuRewards.select(TrungThuRewards.table("lantern"),9900),new Random(0)).items().get(0);
  check(timed.template.id==1888&&timed.itemOptions.stream().anyMatch(o->o.optionTemplate.id==93&&o.param==7),"treo den timed lantern retains seven-day expiry");
  check(TrungThuRuntime.goldMaterial(888)&&TrungThuRuntime.goldMaterial(889)&&TrungThuRuntime.goldMaterial(1312),"flour beans lotus retain gold consignment classification");
  var hard=user(c,65,12);task(hard,ConstTask.VERY_HARD);check(TrungThuQuest.pay(hard)&&qty(hard,1311)==2&&qty(hard,1312)==1,"very-hard first daily task pays two lanterns and one lotus");
  Thread.sleep(510);task(hard,ConstTask.VERY_HARD);check(TrungThuQuest.pay(hard)&&qty(hard,1311)==2&&qty(hard,1312)==2,"very-hard repeat task pays lotus but no extra daily lantern");
  var leader=user(c,60,8);var participant=user(c,61,8);var offline=user(c,62,8);
  var clan=new Clan();clan.id=TrungThuRuntimeTest.BASE+80;clan.members.add(member(leader,true));clan.members.add(member(participant,false));clan.members.add(member(offline,false));clan.membersInGame.add(leader);clan.membersInGame.add(participant);leader.clan=participant.clan=offline.clan=clan;
  Zone zone=Manager.MAPS.stream().filter(m->m.mapId==153).findFirst().get().zones.get(1);leader.zone=participant.zone=zone;lantern(leader);lantern(participant);
  check(TrungThuClan.equipped(leader)&&TrungThuClan.equipped(participant),"restored two-can lantern qualifies for parade");
  TrungThuClan.click(participant,"parade");check(scalar(c,"SELECT COUNT(*) FROM trung_thu_2026_daily WHERE owner_id="+clan.id)==0,"non-leader cannot start parade");
  TrungThuClan.click(leader,"parade");check(scalar(c,"SELECT COUNT(*) FROM trung_thu_2026_daily WHERE owner_id="+clan.id)==0,"leader alone cannot complete two-member parade");
  TrungThuClan.click(participant,"parade");
  check(TrungThuStore.multiplier(leader)==2&&TrungThuStore.multiplier(participant)==2,"all checked online participants get X2");
  check(scalar(c,"SELECT COUNT(*) FROM trung_thu_2026_state WHERE player_id="+offline.id)==0,"offline clan member receives no event buff");
  long received=TrungThuStore.state(leader).doubleUntil();TrungThuClan.click(leader,"parade");check(TrungThuStore.state(leader).doubleUntil()==received,"same daily parade cannot extend buff twice");
  TrungThuClan.click(leader,"attendance");TrungThuClan.click(participant,"attendance");check(scalar(c,"SELECT COUNT(*) FROM trung_thu_2026_daily WHERE owner_id="+clan.id)==2,"attendance has separate once-daily receipt");
  var monkey=new MoonBoss.Monkey();var omega=new MoonBoss.Omega();
  monkey.respawn();check("Khỉ Xayda".equals(monkey.name),"monkey uses requested event name");
  check(Arrays.equals(TrungThuConfig.numbers("boss.omega-outfit"),new int[]{1569,1570,1571}),"Omega uses actual canonical costume parts");
  var summon=user(c,66,8);summon.zone=Manager.MAPS.stream().filter(m->m.mapId==126).findFirst().get().zones.get(0);put(summon,1310,1);
  Field random=TrungThuRuntime.class.getDeclaredField("RANDOM");random.setAccessible(true);((Random)random.get(null)).setSeed(0);
  TrungThuRuntime.use(summon,nro.models.services.InventoryService.gI().findItemBag(summon,1310));
  Field rabbitField=TrungThuRuntime.class.getDeclaredField("rabbit");rabbitField.setAccessible(true);MoonBoss rabbit=(MoonBoss)rabbitField.get(null);
  check(qty(summon,1310)==0&&rabbit.zoneFinal!=null&&rabbit.zoneFinal.map.mapId==126,"paid rabbit summon reserves real public Santa zone");
  rabbit.respawn();rabbit.joinMap();check(rabbit.zone!=null&&rabbit.zone.map.mapId==126,"rabbit actually appears at Santa");
  check(Arrays.equals(TrungThuConfig.numbers("map.rabbit"),new int[]{84,126}),"rabbit excludes gas dungeon Santa alias 149");
  String rules=TrungThuRules.text();check(rules.contains("Thành phố Santa")&&rules.contains("Lồng đèn 2 lon")&&rules.contains("20.000.000")&&!rules.contains("X3")&&!rules.toLowerCase().contains("top")&&!rules.contains("nạp"),"player rules show current activities and exact prices without removed promotions");
  check(scalar(c,"SELECT COUNT(*) FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id WHERE s.tag_name='TRUNG_THU_2026' AND i.temp_id IN(2078,2080)")==2,"native shop sells both actual 2026 box templates");
  try(Statement s=c.createStatement()){
   s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id SET i.temp_id=1316,i.cost=37 WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=2078");
   TrungThuShop.ensureData(c);
   check(scalar(c,"SELECT MAX(i.cost) FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=2078")==37,"legacy box row migrates while preserving edited shop price");
   s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop sh ON sh.id=t.shop_id SET i.cost=20 WHERE sh.tag_name='TRUNG_THU_2026' AND i.temp_id=2078");
  }
  System.out.println("PASS: "+checks+" revised Trung Thu rule checks");
 }
}
