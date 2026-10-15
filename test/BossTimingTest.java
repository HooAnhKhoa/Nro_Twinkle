package nro.models.boss;

import nro.models.consts.*;
import nro.models.map.Map;
import java.util.ArrayList;

/** Real Boss lifecycle hooks without listeners or production database writes. */
public class BossTimingTest {
 static int checks;
 static void check(boolean value,String text){if(!value)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class Sample extends Boss {
  int leaves,rewards;
  Sample()throws Exception{super(BossType.TRUNGTHU_EVENT,-202610222,false,false,new BossData("Timing QA",(byte)0,new short[]{0,0,0,-1,-1,-1},1,new int[]{100},new int[]{1},new int[][]{},new String[]{},new String[]{},new String[]{},60));panelIndependent=true;nPoint.hp=100;}
  @Override public void leaveMap(){leaves++;zone=null;lastTimeRest=System.currentTimeMillis();changeStatus(BossStatus.REST);}
  void appeared(){var map=new Map(1,"QA",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[][]{{0}},new int[]{0},2,20,new ArrayList<>());zone=map.zones.get(0);changeStatus(BossStatus.CHAT_S);}
 }
 static class Conditional extends Sample {Conditional()throws Exception{}@Override public void rest(){}}
 public static void main(String[] args)throws Exception{
  try{
   Sample b=new Sample();b.lastTimeRest=System.currentTimeMillis();long now=System.currentTimeMillis();
   check((Long)b.panelTiming(now).get("spawn_at")>=now+59000,"ordinary rest yields actual respawn deadline");
   b.panelSetRest(120);check((Long)b.panelTiming(now).get("spawn_at")>=now+119000,"rest edit changes current countdown");
   b.panelScheduleSpawn(2);check(b.panelWaitingSpawn()&&(Long)b.panelTiming(now).get("spawn_at")<now+3000,"scheduled summon overrides natural rest countdown");
   b.panelScheduleSpawn(0);check(!b.panelWaitingSpawn()&&b.bossStatus==BossStatus.RESPAWN,"zero delay wakes boss immediately");
   b.appeared();check(b.panelTiming(now).get("appeared_at")!=null&&b.panelTiming(now).get("spawn_at")==null,"map entry starts elapsed timer and hides spawn countdown");
   b.panelSetLifetime(1);check(b.panelTiming(now).get("expires_at")!=null&&!b.panelExpire(),"positive lifetime exposes actual deadline without early expiry");
   Thread.sleep(1050);check(b.panelExpire()&&b.leaves==1&&b.zone==null,"lifetime expires through boss leave hook");
   check(b.panelTiming(System.currentTimeMillis()).get("appeared_at")==null&&b.bossStatus==BossStatus.REST,"expiry clears elapsed time and returns to rest");
   b.appeared();b.panelSetLifetime(0);check(!b.panelExpire()&&b.panelTiming(now).get("expires_at")==null,"zero lifetime disables auto timeout");
   b.panelSetLifetime(-1);check(!b.panelExpire()&&b.panelTiming(now).get("expires_at")==null,"unset lifetime retains native behavior");
   b.panelScheduleSpawn(10);b.suspendEvent();check(!b.panelWaitingSpawn()&&b.zone==null,"event OFF clears pending summon and existing map presence");
   Conditional special=new Conditional();check(special.panelTiming(now).get("spawn_at")==null,"special rest condition never invents a countdown");
   System.out.println("PASS: "+checks+" boss timing checks");
  }catch(Throwable error){error.printStackTrace();System.exit(1);}System.exit(0);
 }
}
