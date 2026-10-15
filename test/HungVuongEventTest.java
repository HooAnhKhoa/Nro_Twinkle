import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.server.Manager;
import nro.models.server.PanelBridge;
import nro.models.skill.*;
import nro.models.player_system.Template.*;
import nro.models.event.EventManager;
import nro.models.event_list.HungVuong;
import nro.models.boss.*;
import nro.models.boss.Boss_Manager.*;
import nro.models.boss.event_hung_vuong.*;
import nro.models.consts.BossStatus;
import nro.models.player.Player;
import nro.models.map.service.NpcManager;
public class HungVuongEventTest {
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("PASS: "+text);}
 static nro.models.map.Map map(int id){nro.models.map.Map map=new nro.models.map.Map(id,"QA map "+id,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[][]{{0}},new int[]{0},1,10,new ArrayList<>());Manager.MAPS.add(map);return map;}
 static void ticks(){for(int i=0;i<6;i++)for(Boss boss:HungVuongEventManager.gI().getBosses())boss.update();}
 public static void main(String[] args)throws Exception{
 try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
  check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
  int before;try(ResultSet row=s.executeQuery("SELECT enabled FROM panel_event_config WHERE event_key='hung_vuong'")){row.next();before=row.getInt(1);}
  try{
   for(int i=0;i<85;i++){NpcTemplate template=new NpcTemplate();template.id=(byte)i;Manager.NPC_TEMPLATES.add(template);}
   for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());
   for(int id:new int[]{Skill.GALICK,Skill.ANTOMIC}){SkillTemplate template=new SkillTemplate();template.id=(byte)id;template.name="QA";Skill skill=new Skill();skill.template=template;skill.point=1;skill.coolDown=2000;template.skillss.add(skill);Manager.NCLASS.get(0).skillTemplatess.add(template);}
   List<nro.models.map.Map> villages=List.of(map(0),map(7),map(14));nro.models.map.Map arena=map(1);
   s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key='hung_vuong'");EventManager.reload(c);
   HungVuong event=new HungVuong();event.npc();int count=Manager.NPCS.size();event.npc();
   check(Manager.NPCS.size()==count&&count==6,"repeated event initialization seeds exactly six village NPCs");
   for(nro.models.map.Map village:villages){Player player=new Player();player.zone=village.zones.get(0);check(village.npcs.stream().filter(npc->npc.tempId==52).count()==1&&village.npcs.stream().filter(npc->npc.tempId==66).count()==1,"village has Vua Hung and Noi Banh: "+village.mapId);check(NpcManager.getNpcsByMapPlayer(player).isEmpty(),"OFF hides newly seeded NPCs: "+village.mapId);}
   ThuyTinh parent=new ThuyTinh();parent.data[0].setMapJoin(new int[]{1});
   for(Boss boss:HungVuongEventManager.gI().getBosses())boss.update();
   Boss child=HungVuongEventManager.gI().getBosses().stream().filter(boss->boss instanceof SonTinh).findFirst().orElseThrow();
   check(child.bossStatus==BossStatus.REST&&child.zone==null,"linked Son Tinh waits for parent spawn");
   s.executeUpdate("UPDATE panel_event_config SET enabled=1 WHERE event_key='hung_vuong'");EventManager.reload(c);
   for(nro.models.map.Map village:villages){Player player=new Player();player.zone=village.zones.get(0);check(NpcManager.getNpcsByMapPlayer(player).size()==2,"ON reveals both event NPCs: "+village.mapId);}
   ticks();check(parent.zone==arena.zones.get(0)&&child.zone==parent.zone,"real boss lifecycle spawns Thuy Tinh and linked Son Tinh together");
   check(parent.bossStatus==BossStatus.ACTIVE&&child.bossStatus==BossStatus.ACTIVE,"both event bosses reach active state");
   check(arena.zones.get(0).getBosses().contains(parent)&&arena.zones.get(0).getBosses().contains(child),"spawned bosses registered in zone");
   check(!parent.isNotifyDisabled,"parent appearance sends normal boss announcement");
   check(PanelBridge.canSpawn(parent)&&!PanelBridge.canSpawn(child),"web can summon event root without spawning companion independently");
   for(int id=0;id<2000;id++)Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)id,(byte)27,(byte)3,"QA item "+id,"",(short)0,(short)0,false,0));
   Player winner=new Player();winner.id=321;winner.isPlayer=true;winner.zone=parent.zone;
   parent.reward(winner);child.reward(winner);
   check(arena.zones.get(0).items.stream().filter(item->item.itemTemplate.id==1558&&item.quantity==1&&item.playerId==321).count()==2,"both actual boss reward paths drop one owned Mai An Tiem stamp");
   int population=HungVuongEventManager.gI().getBosses().size();
   s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key='hung_vuong'");EventManager.reload(c);ticks();
   check(parent.zone==null&&child.zone==null,"OFF removes both linked bosses");
   s.executeUpdate("UPDATE panel_event_config SET enabled=1 WHERE event_key='hung_vuong'");EventManager.reload(c);ticks();
   check(parent.zone!=null&&child.zone==parent.zone,"OFF to ON respawns existing pair within normal update ticks");
   check(HungVuongEventManager.gI().getBosses().size()==population,"repeated toggles do not duplicate bosses");
   parent.leaveMap();parent.update();check(parent.bossStatus==BossStatus.REST&&parent.data[0].getSecondsRest()==900,"ordinary 15-minute respawn cooldown retained after leaving");
  }finally{s.executeUpdate("UPDATE panel_event_config SET enabled="+before+" WHERE event_key='hung_vuong'");EventManager.reload(c);}
 }catch(Throwable error){error.printStackTrace();System.exit(1);}System.exit(0);
 }
}
