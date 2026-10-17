package nro.models.event.trungthu2026;
import java.util.*;import java.util.concurrent.*;import nro.models.player.Player;import nro.models.data.LocalManager;
public class TrungThuCacheConcurrencyTest{
 public static void main(String[] args)throws Exception{
  var pool=Executors.newFixedThreadPool(6,r->{Thread t=new Thread(r);t.setDaemon(true);return t;});
  try{
   try(var c=LocalManager.getConnection()){
    if(!"nro_trungthu_rules_20261017".equals(c.getCatalog()))throw new AssertionError("QA guard");
    try(var q=c.createStatement()){q.executeUpdate("DELETE FROM trung_thu_2026_state WHERE player_id BETWEEN 2026101690 AND 2026101695");}
   }
   List<Callable<Integer>> jobs=new ArrayList<>();
   for(int n=0;n<6;n++){Player p=new Player();p.id=2026101690+n;jobs.add(()->{
    for(int round=0;round<30;round++){
     long until=System.currentTimeMillis()+300000+round;
     TrungThuStore.grant(p.id,"carrot_until",until);
     int side=round%2+1;TrungThuStore.side(p,side);
     var state=TrungThuStore.state(p);
     if(state.carrotUntil()<until||state.side()!=side)throw new AssertionError("Stale state cache");
    }return 30;
   });}
   List<Future<Integer>> results=new ArrayList<>();for(var job:jobs)results.add(pool.submit(job));
   int operations=0;for(var result:results)operations+=result.get(20,TimeUnit.SECONDS);
   if(operations!=180)throw new AssertionError("Incomplete updates");
   System.out.println("PASS: 180 concurrent grant/side/read cycles complete with one SQL connection, without stale cache or deadlock");
  }finally{
   pool.shutdownNow();
   try(var c=LocalManager.getConnection();var s=c.createStatement()){s.executeUpdate("DELETE FROM trung_thu_2026_state WHERE player_id BETWEEN 2026101690 AND 2026101695");}
   LocalManager.close();
  }
  System.exit(0);
 }
}
