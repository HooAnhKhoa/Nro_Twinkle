package nro.models.boss;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.consts.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;

/** Real map items and native reward entrypoint, with QA-only persisted policies. */
public class BossDropsTest {
 static int checks;static final String KEY=Sample.class.getName();
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class User extends Player{User(){id=2026102300;isPlayer=true;name="Boss drop QA";}@Override public void sendMessage(Message m){}}
 static class Sample extends Boss {
  int nativeRewards;
  Sample()throws Exception{super(BossType.TRUNGTHU_EVENT,-202610230,false,false,new BossData("Drop QA",(byte)0,new short[]{0,0,0,-1,-1,-1},1,new int[]{100},new int[]{1},new int[][]{},new String[]{},new String[]{},new String[]{},60));var map=new nro.models.map.Map(1,"QA",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[24][60],new int[]{0},2,20,new ArrayList<>());zone=map.zones.get(0);nPoint.hp=0;currentLevel=0;}
  @Override public void reward(Player p){nativeRewards++;p.event.addEventPoint(5);ItemMap nativeItem=new ItemMap(zone,190,100,0,0,p.id);Service.gI().dropItemMap(zone,nativeItem);}
 }
 static void policy(Connection c,String mode,String items)throws Exception{try(var q=c.prepareStatement("INSERT INTO boss_web_drop_config(boss_key,mode,items,revision) VALUES(?,?,?,1) ON DUPLICATE KEY UPDATE mode=VALUES(mode),items=VALUES(items),revision=revision+1")){q.setString(1,KEY);q.setString(2,mode);q.setString(3,items);q.executeUpdate();}BossDrops.reload(c);}
 static long count(Sample b,int id){return b.zone.items.stream().filter(i->i.itemTemplate.id==id).count();}
 static String entry(int chance){return "[{\"id\":1632,\"min\":2,\"max\":4,\"chance\":"+chance+",\"options\":[{\"id\":50,\"param\":25},{\"id\":93,\"param\":7}]}]";}
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
   try(var q=c.createStatement()){
    for(String sql:java.nio.file.Files.readString(java.nio.file.Path.of("../../web/boss_web.sql")).split(";"))if(!sql.isBlank())q.execute(sql);
    try(var r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate(r.getShort("id"),r.getByte("TYPE"),r.getByte("gender"),r.getString("NAME"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),false,0));}
    try(var r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   }
   User p=new User();policy(c,"original",entry(100));Sample original=new Sample();original.giveReward(p);check(original.nativeRewards==1&&count(original,190)==1&&count(original,1632)==0&&p.event.getEventPoint()==5,"original mode preserves native item and points");
   policy(c,"add",entry(100));Sample added=new Sample();added.giveReward(p);check(count(added,190)==1&&count(added,1632)==1&&added.nativeRewards==1,"add mode keeps native item and adds configured reward");
   ItemMap costume=added.zone.items.stream().filter(i->i.itemTemplate.id==1632).findFirst().orElseThrow();check(costume.quantity>=2&&costume.quantity<=4&&costume.playerId==p.id&&costume.options.get(0).optionTemplate.id==50&&costume.options.get(0).param==25&&costume.options.get(1).param==7,"custom quantity, owner and option values reach real map item");
   added.giveReward(p);check(count(added,1632)==1&&added.nativeRewards==1,"repeat death callback cannot duplicate items or points");
   policy(c,"replace",entry(100));Sample replaced=new Sample();replaced.die(p);check(count(replaced,190)==0&&count(replaced,1632)==1&&replaced.nativeRewards==1&&p.event.getEventPoint()==15,"real die route replaces items while native points still execute");
   policy(c,"off",entry(100));Sample off=new Sample();off.giveReward(p);check(off.zone.items.isEmpty()&&off.nativeRewards==1&&p.event.getEventPoint()==20,"off mode has no ghost items and preserves native points");
   policy(c,"replace",entry(0));Sample zero=new Sample();zero.giveReward(p);check(zero.zone.items.isEmpty(),"zero percent never creates a map item");
   policy(c,"replace","[]");Sample empty=new Sample();empty.giveReward(p);check(empty.zone.items.isEmpty()&&empty.nativeRewards==1,"empty replacement table explicitly disables item rewards only");
   policy(c,"off","[]");Sample delayed=new Sample();BossDrops.nativeScope(delayed,()->{new ItemMap(delayed.zone,421,1,0,0,p.id);});check(delayed.zone.items.isEmpty(),"delayed AFK reward scope suppresses registration as well as packets");
   ItemMap unrelated=new ItemMap(delayed.zone,421,1,0,0,p.id);check(delayed.zone.items.contains(unrelated)&&!unrelated.panelSuppressed,"unrelated drops after reward scope are preserved");
   try{BossDrops.nativeScope(delayed,()->{throw new IllegalStateException("Expected scoped exception");});}catch(IllegalStateException expected){}
   ItemMap afterError=new ItemMap(delayed.zone,421,1,0,0,p.id);check(!afterError.panelSuppressed,"exception cannot leak suppression into later drops");
   var pool=Executors.newFixedThreadPool(2);CountDownLatch inside=new CountDownLatch(1),release=new CountDownLatch(1);
   try{var task=pool.submit(()->BossDrops.nativeScope(delayed,()->{inside.countDown();try{release.await(5,TimeUnit.SECONDS);}catch(InterruptedException error){throw new RuntimeException(error);}}));inside.await(5,TimeUnit.SECONDS);var parallel=pool.submit(()->new ItemMap(delayed.zone,421,1,0,0,p.id));check(!parallel.get().panelSuppressed,"another thread's map drop is never suppressed");release.countDown();task.get();}finally{release.countDown();pool.shutdownNow();}
   policy(c,"replace",entry(100));Sample publicBoss=new Sample();BossDrops.rewardPublic(publicBoss,()->new ItemMap(publicBoss.zone,648,1,0,0,-1));check(count(publicBoss,648)==0&&count(publicBoss,1632)==1&&publicBoss.zone.items.get(0).playerId==-1,"Noel-style public emission replaces gifts and keeps public ownership");
   BossDrops.rewardPublic(publicBoss,()->{});check(count(publicBoss,1632)==2,"periodic public gifts can repeat on their native interval");
   var pet=new nro.models.player.Pet(p);pet.id=2026102301;Sample petBoss=new Sample();petBoss.giveReward(pet);check(petBoss.nativeRewards==1&&petBoss.zone.items.stream().anyMatch(i->i.itemTemplate.id==1632&&i.playerId==p.id),"pet kills preserve native rewards and assign custom items to master");
   User bot=new User();bot.isBot=true;Sample blocked=new Sample();blocked.giveReward(bot);check(blocked.zone.items.isEmpty()&&blocked.nativeRewards==0,"bot kills cannot mint custom or native rewards");
   try(var q=c.createStatement();var r=q.executeQuery("SELECT revision,applied_revision FROM boss_web_drop_config WHERE boss_key='"+KEY+"'")){r.next();check(r.getLong(1)==r.getLong(2),"server reload acknowledges only the loaded revision");}
   try(var q=c.createStatement()){q.executeUpdate("DELETE FROM boss_web_drop_config WHERE boss_key='"+KEY+"'");}BossDrops.reload(c);check(BossDrops.policy(original).mode().equals("original"),"removed policy returns to native defaults");
   System.out.println("PASS: "+checks+" boss drop checks");
  }catch(Throwable error){error.printStackTrace();System.exit(1);}finally{LocalManager.close();}System.exit(0);
 }
}
