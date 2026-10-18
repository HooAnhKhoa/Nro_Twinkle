package nro.models.event.october20;
import java.sql.*;import java.util.*;
import nro.models.data.LocalManager;import nro.models.item.Item;import nro.models.player.*;import nro.models.services.*;import nro.models.services_func.*;import nro.models.player_system.Template.*;import nro.models.server.Manager;
public class TeachersDayBoxTest extends October20CompetitionTest {
 static Map<Integer,Integer> opts(Item i){Map<Integer,Integer> m=new HashMap<>();for(var o:i.itemOptions){if(m.put(o.optionTemplate.id,o.param)!=null)throw new AssertionError("duplicate option");}return m;}
 public static void main(String[] args)throws Exception{try{
  try(Connection c=LocalManager.getConnection();Statement q=c.createStatement()){
   ok("nro_october20_top_20261018".equals(c.getCatalog()),"isolated QA guard");q.executeUpdate("DELETE FROM player WHERE id BETWEEN "+(BASE+50)+" AND "+(BASE+59));
   try(ResultSet r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),r.getBoolean("is_up_to_up"),r.getInt("power_require")));}
   try(ResultSet r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
  }
  int[] ids={1362,1937,1389,1384,1383};
  for(int g=0;g<3;g++)for(int n=0;n<5;n++){Item gift=TeachersDayBox.reward(n,g);ok(gift.template.id==(n==2?1389+g:ids[n])&&gift.quantity==1&&CatalogAssets.available(gift.template.id),"exclusive available reward "+n+" gender "+g);}
  ok(opts(TeachersDayBox.reward(0,0)).equals(Map.of(50,22,77,22,103,22,94,5,222,10,93,15)),"Ca Lich exact stats and expiry");
  ok(opts(TeachersDayBox.reward(1,0)).equals(Map.of(50,25,5,20,204,25,117,20,30,0)),"Bunma permanent bound with boss and aura stats");
  ok(opts(TeachersDayBox.reward(2,0)).equals(Map.of(50,24,77,23,103,23,101,25,93,15)),"graduate exact stats and expiry");
  ok(opts(TeachersDayBox.reward(3,0)).equals(Map.of(50,23,77,23,103,23,94,7,108,10,24,0,93,7)),"Goku exact stats and gravity");
  ok(opts(TeachersDayBox.reward(4,0)).equals(Map.of(50,22,77,23,103,22,97,5,108,10,160,25,93,7)),"Raddit exact stats and disciple bonus");
  TeacherRage rage=new TeacherRage();Item ca=TeachersDayBox.reward(0,0);ok(rage.percent(ca,0)==0&&rage.percent(ca,119999)==0,"rage waits full 120 seconds");ok(rage.percent(ca,120000)==10&&rage.percent(ca,149999)==10,"rage active 30 seconds");ok(rage.percent(ca,150000)==0&&rage.percent(ca,240000)==10,"rage ends and repeats at 120 seconds");ok(rage.percent((Item)null,241000)==0&&rage.percent(ca,241001)==0,"unequip resets rage");ca.createTime=0;ok(rage.percent(ca,16L*86400000)==0,"expired costume cannot proc rage");
  User buyer=user(50),target=user(51);for(int i=0;i<7;i++)buyer.inventory.itemsBody.add(new Item());buyer.inventory.itemsBody.set(5,TeachersDayBox.reward(1,0));target.isBoss=true;
  ok(TeacherRage.bossDamage(buyer,target,1000)==1250,"Bunma increases actual boss damage by 25 percent");target.isBoss=false;ok(TeacherRage.bossDamage(buyer,target,1000)==1000,"Bunma does not increase PvP damage");buyer.inventory.itemsBody.set(5,new Item());target.isBoss=true;ok(TeacherRage.bossDamage(buyer,target,1000)==1000,"boss bonus stops when costume removed");
    buyer.inventory.itemsBody.set(5,TeachersDayBox.reward(1,0));buyer.nPoint.calPoint();buyer.zone=new nro.models.map.Zone(null,0,20);target.zone=buyer.zone;target.isBoss=false;buyer.zone.getNotBosses().add(buyer);buyer.zone.getNotBosses().add(target);
  var aura=EffectSkin.class.getDeclaredMethod("updateThoBulma");aura.setAccessible(true);aura.invoke(buyer.effectSkin);ok(target.effectSkill.isDameBuff&&target.effectSkill.tileDameBuff==20,"Bunma aura buffs nearby recipient");buyer.zone=null;target.zone=null;
  buyer.inventory.itemsBody.set(5,TeachersDayBox.reward(3,0));buyer.nPoint.calPoint();ok(buyer.nPoint.isLamCham&&buyer.nPoint.tlNeDon==10&&buyer.nPoint.tlGiap==7,"Goku real gravity dodge and damage reduction");
  buyer.inventory.itemsBody.set(5,TeachersDayBox.reward(4,0));buyer.nPoint.calPoint();ok(buyer.nPoint.tlTNSMPet==25&&buyer.nPoint.tlPST==5&&buyer.nPoint.tlNeDon==10,"Raddit real disciple reflect and dodge stats");
  buyer.inventory.itemsBody.set(5,TeachersDayBox.reward(2,0));buyer.nPoint.calPoint();ok(buyer.nPoint.tlTNSM.contains(25),"graduate real experience stat");buyer.inventory.itemsBag.set(0,item(1739,1));ok(TeachersDayBox.use(buyer,buyer.inventory.itemsBag.get(0))&&qty(buyer,1739)==0&&buyer.inventory.itemsBag.stream().anyMatch(i->i.isNotNullItem()&&i.template.type==5),"ordinary box consumes one and persists costume");
  pause();buyer.inventory.itemsBag.set(1,item(1944,1));TeachersDayBox.use(buyer,buyer.inventory.itemsBag.get(1));ok(qty(buyer,1944)==0,"VIP box uses same exclusive pool");
  pause();User full=user(52);full.inventory.itemsBag.clear();full.inventory.itemsBag.add(item(1739,2));full.inventory.itemsBag.add(item(0,1));String before=nro.models.event.trungthu2026.TrungThuStore.bag(full);TeachersDayBox.use(full,full.inventory.itemsBag.get(0));ok(before.equals(nro.models.event.trungthu2026.TrungThuStore.bag(full)),"full bag keeps box and all inventory");
  pause();User failed=user(53);failed.inventory.itemsBag.set(0,item(1739,1));before=nro.models.event.trungthu2026.TrungThuStore.bag(failed);sql("DROP TRIGGER IF EXISTS qa_teacher_fail");sql("CREATE TRIGGER qa_teacher_fail BEFORE UPDATE ON player FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='QA teachers rollback'");try{TeachersDayBox.use(failed,failed.inventory.itemsBag.get(0));ok(before.equals(nro.models.event.trungthu2026.TrungThuStore.bag(failed)),"SQL failure restores box without granting reward");}finally{sql("DROP TRIGGER qa_teacher_fail");}
  System.out.println("PASS "+checks+" teachers day checks");
 }finally{LocalManager.close();}System.exit(0);}
}