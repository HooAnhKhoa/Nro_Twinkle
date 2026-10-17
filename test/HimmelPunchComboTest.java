package nro.models.event.trungthu2026;
import java.sql.*;import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
import nro.models.data.LocalManager;import nro.models.server.Manager;import nro.models.player_system.Template.*;import nro.models.item.Item;import nro.models.shop.ItemShop;import nro.models.services.*;import nro.models.skill.*;import nro.models.mob.Mob;import com.google.gson.*;
public class HimmelPunchComboTest {
 static int checks;static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS: "+s);}
 static TrungThuRuntimeTest.User user(int id){var p=new StatUser(id);p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;while(p.inventory.itemsBody.size()<11)p.inventory.itemsBody.add(new Item());p.zone=Manager.MAPS.get(0).zones.get(0);p.zone.addPlayer(p);return p;}
 static class StatUser extends TrungThuRuntimeTest.User {
  int wireHp,wireSd,wireCrit;
  StatUser(int id){super(id);}
  @Override public void sendMessage(nro.models.network.Message m){super.sendMessage(m);if(m.command==-42)try{var in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(m.getData()));in.skipBytes(12);wireHp=in.readInt();in.skipBytes(16);wireSd=in.readInt();in.readInt();wireCrit=in.readUnsignedByte();}catch(Exception e){throw new AssertionError(e);}}
 }
 static Item costume(int cap){var i=ItemService.gI().createNewItem((short)1632);i.itemOptions.add(new Item.ItemOption(50,25));i.itemOptions.add(new Item.ItemOption(77,25));i.itemOptions.add(new Item.ItemOption(14,20));i.itemOptions.add(new Item.ItemOption(226,25));i.itemOptions.add(new Item.ItemOption(156,cap));return i;}
 static void skill(TrungThuRuntimeTest.User p,int id){var t=new SkillTemplate();t.id=(byte)id;var s=new Skill();s.template=t;s.skillId=(short)id;s.point=1;s.damage=100;p.playerSkill.skillSelect=s;}
 static class TargetMob extends Mob{TargetMob(){super();point.hp=100000;point.maxHp=100000;}@Override public void injured(nro.models.player.Player p,long damage,boolean full){point.hp-=Math.min(point.hp,(int)damage);}}
 static class TargetPlayer extends TrungThuRuntimeTest.User{TargetPlayer(){super(2026102603);playerTask.taskMain=new nro.models.task.TaskMain();while(inventory.itemsBody.size()<11)inventory.itemsBody.add(new Item());nPoint.hp=100000;nPoint.hpMax=100000;}@Override public synchronized int injured(nro.models.player.Player p,long damage,boolean piercing,boolean mob){int d=(int)Math.min(nPoint.hp,Math.max(0,damage));nPoint.hp-=d;return d;}}
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection()){
   check(java.util.Set.of("nro_panel_qa_20261013","nro_teamobi_qa_20261017_v2","nro_teamobi_qa_20261017_v3").contains(c.getCatalog()),"QA database guard");
   try(var q=c.createStatement();var r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)r.getInt("id"),r.getByte("TYPE"),r.getByte("gender"),r.getString("NAME"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),false,0));}
   try(var q=c.createStatement();var r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
  }
  for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());TrungThuRuntimeTest.map(5,(byte)0);
  var p=user(2026102601);p.nPoint.calPoint();check(p.nPoint.dame==1000&&p.nPoint.hpMax==10000,"unmodified baseline");
  p.inventory.itemsBody.set(5,costume(25));p.nPoint.calPoint();int base=p.nPoint.dame;
  check(base==1562&&p.nPoint.hpMax==12500&&p.nPoint.mpMax==10000&&p.nPoint.crit==20,"Himmel SD25/Cute25 once, HP25 and crit20; no invented KI bonus");
  check(p.nPoint.tlSubSD==0&&p.nPoint.tlGold==0,"option156 no longer reduces SD or grants unrelated gold");
  check(!p.nPoint.punchCombo.hit(p,Skill.GALICK,0)&&p.nPoint.punchCombo.percent(p)==0,"miss/zero damage earns no stacks");
  for(int n=1;n<=25;n++){check(p.nPoint.punchCombo.hit(p,Skill.GALICK,1),"successful punch stack "+n);p.nPoint.calPoint();check(p.nPoint.dame==base+(long)base*n/100,"real SD reflects stack "+n);}
  check(!p.nPoint.punchCombo.hit(p,Skill.GALICK,1)&&p.nPoint.punchCombo.percent(p)==25,"stack cannot exceed item cap");
  p.nPoint.calPoint();check(p.nPoint.dame==1952,"25 percent cap yields exact final SD");
  check(p.nPoint.punchCombo.useSkill(p,Skill.KAMEJOKO),"using another skill resets stack");p.nPoint.calPoint();check(p.nPoint.dame==base,"reset removes accumulated SD");
  for(int id:new int[]{Skill.DRAGON,Skill.DEMON,Skill.GALICK})check(p.nPoint.punchCombo.hit(p,id,1),"all three basic punches accepted");
  check(p.nPoint.punchCombo.hit(p,Skill.LIEN_HOAN,1)&&p.nPoint.punchCombo.percent(p)==0,"other attack types clear rather than earn stacks");
  p.nPoint.punchCombo.hit(p,Skill.GALICK,1);p.inventory.itemsBody.set(5,costume(25));p.nPoint.calPoint();check(p.nPoint.punchCombo.percent(p)==0,"swapping to a different copy resets chain");
  p.nPoint.punchCombo.hit(p,Skill.GALICK,1);p.inventory.itemsBody.set(5,new Item());p.nPoint.calPoint();check(p.nPoint.dame==1000&&p.nPoint.hpMax==10000&&p.nPoint.crit==0,"unequipping removes stats and chain");
  p.inventory.itemsBody.set(5,costume(2));p.nPoint.punchCombo.hit(p,Skill.GALICK,1);p.nPoint.hp=0;check(p.nPoint.punchCombo.percent(p)==0,"death clears chain");p.nPoint.hp=100;
  var reconnect=user(2026102602);reconnect.inventory.itemsBody.set(5,p.inventory.itemsBody.get(5));check(reconnect.nPoint.punchCombo.percent(reconnect)==0,"new session with same gear starts empty");
  // Exercise the actual private mob attack route; no direct call to the combo state.
  p.inventory.itemsBody.set(5,costume(25));p.nPoint.calPoint();skill(p,Skill.GALICK);
  var method=SkillService.class.getDeclaredMethod("playerAttackMob",nro.models.player.Player.class,Mob.class,boolean.class,boolean.class);method.setAccessible(true);
  TargetMob mob=new TargetMob();mob.zone=p.zone;
  method.invoke(SkillService.gI(),p,mob,false,false);check(p.nPoint.punchCombo.percent(p)==1,"actual PvE hit routes into combo");
  method.invoke(SkillService.gI(),p,mob,true,false);check(p.nPoint.punchCombo.percent(p)==1,"actual PvE miss does not stack");
  skill(p,Skill.KAMEJOKO);method.invoke(SkillService.gI(),p,mob,false,false);check(p.nPoint.punchCombo.percent(p)==0,"actual non-punch attack resets before damage");
  var pvp=SkillService.class.getDeclaredMethod("playerAttackPlayer",nro.models.player.Player.class,nro.models.player.Player.class,boolean.class);pvp.setAccessible(true);var victim=new TargetPlayer();victim.zone=p.zone;p.zone.addPlayer(victim);skill(p,Skill.GALICK);
  pvp.invoke(SkillService.gI(),p,victim,false);check(p.nPoint.punchCombo.percent(p)==1,"actual PvP hit routes into combo");
  pvp.invoke(SkillService.gI(),p,victim,true);check(p.nPoint.punchCombo.percent(p)==1,"actual PvP miss does not stack");
  skill(p,Skill.ANTOMIC);pvp.invoke(SkillService.gI(),p,victim,false);check(p.nPoint.punchCombo.percent(p)==0,"actual PvP blast resets chain");
  // Native equipment route also sends the newly calculated stats to the client.
  p.inventory.itemsBody.set(5,new Item());p.inventory.itemsBag.add(costume(25));InventoryService.gI().itemBagToBody(p,0);
  check(p.inventory.itemsBody.get(5).template.id==1632&&p.nPoint.hpMax==12500&&p.nPoint.crit==20,"native bag-to-body route equips Himmel and recalculates stats");
  check(((StatUser)p).wireHp==12500&&((StatUser)p).wireSd==1562&&((StatUser)p).wireCrit==20,"native point packet contains updated HP/SD/crit for client");
  InventoryService.gI().itemBodyToBag(p,5);check(!p.inventory.itemsBody.get(5).isNotNullItem()&&p.nPoint.hpMax==10000&&p.nPoint.crit==0,"native unequip removes HP and crit bonus");
  p.inventory.itemsBody.set(5,costume(2));p.nPoint.calPoint();p.nPoint.punchCombo.hit(p,Skill.GALICK,1);p.nPoint.punchCombo.hit(p,Skill.GALICK,1);check(!p.nPoint.punchCombo.hit(p,Skill.GALICK,1)&&p.nPoint.punchCombo.percent(p)==2,"custom item cap is respected");
  p.inventory.itemsBody.set(10,costume(3));check(p.nPoint.punchCombo.percent(p)==0,"changing strongest combo source resets it");for(int n=0;n<5;n++)p.nPoint.punchCombo.hit(p,Skill.GALICK,1);check(p.nPoint.punchCombo.percent(p)==3,"multiple sources use strongest cap rather than adding caps");
  if(args.length>0){
  // Replay jhgyo's saved base stats/equipment without modifying the live player.
  JsonObject saved=new JsonParser().parse(Files.readString(Path.of(args[0]))).getAsJsonObject();var j=user(2026102604);var pts=new JsonParser().parse(saved.get("data_point").getAsString()).getAsJsonArray();j.gender=saved.get("gender").getAsByte();j.nPoint.hpg=pts.get(5).getAsInt();j.nPoint.mpg=pts.get(6).getAsInt();j.nPoint.dameg=pts.get(7).getAsInt();j.nPoint.critg=pts.get(9).getAsInt();
  var rows=new JsonParser().parse(saved.get("items_body").getAsString()).getAsJsonArray();
  for(int slot=0;slot<rows.size();slot++){var row=new JsonParser().parse(rows.get(slot).getAsString()).getAsJsonArray();int id=row.get(0).getAsInt();if(id<0)continue;var item=ItemService.gI().createNewItem((short)id);for(var text:new JsonParser().parse(row.get(2).getAsString()).getAsJsonArray()){var o=new JsonParser().parse(text.getAsString()).getAsJsonArray();item.itemOptions.add(new Item.ItemOption(o.get(0).getAsInt(),o.get(1).getAsInt()));}j.inventory.itemsBody.set(slot,item);}
  Item himmel=j.inventory.itemsBody.get(5);j.inventory.itemsBody.set(5,new Item());j.nPoint.calPoint();int sd=j.nPoint.dame,hp=j.nPoint.hpMax,crit=j.nPoint.crit;j.inventory.itemsBody.set(5,himmel);j.nPoint.calPoint();check(j.nPoint.dame>sd&&j.nPoint.hpMax>hp&&j.nPoint.crit==crit+20,"jhgyo exact saved equipment gains real SD, HP and crit from Himmel");
  System.out.println("jhgyo replay: SD "+sd+" -> "+j.nPoint.dame+", HP "+hp+" -> "+j.nPoint.hpMax+", crit "+crit+" -> "+j.nPoint.crit);
  }
  System.out.println("CHECKS "+checks);System.exit(0);
 }
}