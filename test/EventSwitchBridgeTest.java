import java.sql.*;
import java.nio.file.*;
import nro.models.event.EventManager;
import nro.models.data.LocalManager;
import nro.models.server.PanelBridge;
import nro.models.boss.Boss;
import nro.models.boss.event.Halloween.BiMa;
import nro.models.consts.BossStatus;
import nro.models.boss.Boss_Manager.*;
public class EventSwitchBridgeTest {
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("PASS: "+text);}
 public static void main(String[] args)throws Exception {
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
   for(String sql:Files.readString(Path.of("event_schema.sql")).split(";"))if(!sql.isBlank())s.execute(sql);
   s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key IN ('halloween','mob_rewards')");
   EventManager.reload(c);
   check(!EventManager.enabled("halloween"),"game receives OFF switch");
   check(PanelBridge.dropChance("event","event.item1798",5,100)==0,"OFF blocks regional event drop");
   check(PanelBridge.dropChance("event",5,100)==0,"OFF blocks legacy event drop");
   check(PanelBridge.dropChance("stone",2,100)==2,"other drop categories unchanged");
   check("tet".equals(EventManager.bossEvent("nro.models.boss.event_tet.LanCon")),"random-ID LanCon classified by class");
   check("hung_vuong".equals(EventManager.bossEvent("nro.models.boss.event_hung_vuong.SonTinh")),"linked companion event classified");
   check(EventManager.bossEvent("nro.models.boss.Frieza.Fide")==null,"ordinary boss not gated");
   Boss boss=new BiMa();boss.currentLevel=0;boss.bossStatus=BossStatus.ACTIVE;
   nro.models.map.Map map=new nro.models.map.Map(1,"QA EV",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[][]{{0}},new int[]{0},0,10,new java.util.ArrayList<>());
   nro.models.map.Zone zone=new nro.models.map.Zone(map,0,10);boss.zone=zone;zone.addPlayer(boss);
   boss.update();check(boss.currentLevel==-1&&boss.bossStatus==BossStatus.REST,"OFF suspends boss before normal update without death reward");
   check(boss.zone==null&&!zone.getBosses().contains(boss),"OFF removes live boss from zone");
   check(boss.injured(null,999999,false,false)==0,"OFF blocks attack/reward path");
   boss.update();check(boss.bossStatus==BossStatus.REST,"repeated OFF ticks stable");
   s.executeUpdate("UPDATE panel_event_config SET enabled=1 WHERE event_key IN ('halloween','mob_rewards')");
   s.executeUpdate("UPDATE panel_event_runtime SET revision=revision+1 WHERE id=1");EventManager.reload(c);
   check(EventManager.bossEnabled(boss),"ON resumes same boss object");
   check(PanelBridge.dropChance("event","event.item1798",5,100)==5,"ON restores configured event drop path");
   EventManager.gI().init();
   BossManager[] managers={BrolyManager.gI(),HungVuongEventManager.gI(),HalloweenEventManager.gI(),ChristmasEventManager.gI(),LunarNewYearEventManager.gI(),TrungThuEventManager.gI()};
   int total=0;for(BossManager manager:managers){check(manager.getBosses().size()>=10,"event population initialized: "+manager.getClass().getSimpleName());total+=manager.getBosses().size();}
   EventManager.gI().init();
   int again=0;for(BossManager manager:managers)again+=manager.getBosses().size();
   check(total==again,"reinitialization cannot duplicate event populations");
   EventManager.reload(c);
   try(ResultSet rows=s.executeQuery("SELECT revision,applied_revision,heartbeat FROM panel_event_runtime WHERE id=1")){rows.next();check(rows.getLong(1)==rows.getLong(2)&&rows.getTimestamp(3)!=null,"runtime acknowledges applied revision after initialization");}
   s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key='halloween'");EventManager.reload(c);
   again=0;for(BossManager manager:managers)again+=manager.getBosses().size();check(total==again,"live toggle reuses existing boss populations");
  }catch(Throwable error){error.printStackTrace();System.exit(1);}System.exit(0);
 }
}
