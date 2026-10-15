package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.server.Maintenance;
import nro.models.utils.Logger;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

/** One persisted cake tier; lower tiers cannot consume or extend a stronger tier. */
public final class TrungThuCakeBuff {
    public record Kind(int id,int percent,int gems){}
    public record State(int cake,long until){}
    private record View(int cake,long refreshed){}
    private static final State EMPTY=new State(0,0);
    private static final Map<Player,State> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player,View> VIEWS=Collections.synchronizedMap(new WeakHashMap<>());
    private static final BagExchange EXCHANGE=new BagExchange(number("exchange.cooldown-ms"),System::currentTimeMillis);
    public static List<Kind> kinds(){return List.of(kind("one"),kind("two"),kind("special"));}
    private static Kind kind(String tier){return new Kind(item("buff-cake-"+tier),number("cake."+tier+"-percent"),number("price.buff-cake-"+tier+"-gems"));}
    public static Kind kind(int id){return kinds().stream().filter(k->k.id==id).findFirst().orElse(null);}
    public static void invalidate(long id){synchronized(CACHE){CACHE.keySet().removeIf(p->p.id==id);}}
    private static State read(Connection c,long id,boolean lock)throws SQLException{
        try(PreparedStatement q=c.prepareStatement("SELECT cake_id,until_ms FROM trung_thu_2026_cake WHERE player_id=?"+(lock?" FOR UPDATE":""))){q.setLong(1,id);try(ResultSet r=q.executeQuery()){return r.next()?new State(r.getInt(1),r.getLong(2)):EMPTY;}}
    }
    public static State state(Player p){
        synchronized(p){State state=CACHE.get(p);if(state!=null)return state;
            try(Connection c=LocalManager.getConnection()){state=read(c,p.id,false);CACHE.put(p,state);return state;}
            catch(SQLException error){Logger.logException(TrungThuCakeBuff.class,error);return EMPTY;}
        }
    }
    public static int percent(Player p){
        if(!p.isPl()||p.id<=0||!enabled())return 0;
        State state=state(p);Kind kind=kind(state.cake);return kind!=null&&state.until>System.currentTimeMillis()?kind.percent:0;
    }
    public static boolean use(Player p,Item used){
        Kind cake=kind(used.template.id);if(cake==null)return false;
        if(!nro.models.event.EventManager.allowNpc(p,"trung_thu"))return true;
        synchronized(p){
            if(Maintenance.isRunning||!p.isPl()||p.id<=0||p.isDie()||!p.inventory.itemsBag.contains(used)||used.quantity<1)return true;
            String failure=consume(p,cake);
            if(failure!=null){Service.gI().sendThongBao(p,failure);return true;}
            refresh(p);sendTimers(p);InventoryService.gI().sendItemBags(p);
            Service.gI().sendThongBao(p,"Bánh Trung Thu: SD, HP và KI +"+cake.percent+"%. Thêm 10 phút; chỉ một cấp bánh có hiệu lực.");
        }return true;
    }
    private static String consume(Player p,Kind cake){
        try(Connection c=LocalManager.getConnection()){
            c.setAutoCommit(false);boolean committed=false;
            try{
                State before=read(c,p.id,true);Kind current=kind(before.cake);long now=System.currentTimeMillis();boolean active=before.until>now&&current!=null;
                if(active&&current.percent>cake.percent)return "Bánh đang dùng mạnh hơn. Vật phẩm được giữ nguyên.";
                long until=Math.addExact(active&&before.cake==cake.id?before.until:now,number("cake.duration-seconds")*1000L);
                var result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(cake.id,1),0,0),new BagExchange.Gain(List.of(),0,0,0),player->{
                    try{
                        try(PreparedStatement q=c.prepareStatement("INSERT INTO trung_thu_2026_cake(player_id,cake_id,until_ms) VALUES(?,?,?) ON DUPLICATE KEY UPDATE cake_id=VALUES(cake_id),until_ms=VALUES(until_ms)")){q.setLong(1,p.id);q.setInt(2,cake.id);q.setLong(3,until);q.executeUpdate();}
                        TrungThuStore.persistBag(c,p);c.commit();
                    }catch(Exception error){c.rollback();throw error;}
                });
                if(result!=BagExchange.Result.SUCCESS)return result==BagExchange.Result.COOLDOWN?"Bạn thao tác quá nhanh, vui lòng chờ một chút.":"Chưa dùng được bánh. Vật phẩm và thời gian buff được giữ nguyên.";
                committed=true;CACHE.put(p,new State(cake.id,until));return null;
            }finally{if(!committed)c.rollback();}
        }catch(Exception error){Logger.logException(TrungThuCakeBuff.class,error);return "Chưa dùng được bánh. Vật phẩm và thời gian buff được giữ nguyên.";}
    }
    private static void refresh(Player p){
        p.nPoint.calPoint();p.nPoint.hp=Math.min(p.nPoint.hp,p.nPoint.hpMax);p.nPoint.mp=Math.min(p.nPoint.mp,p.nPoint.mpMax);
        Service.gI().point(p);PlayerService.gI().sendInfoHpMpMoney(p);
    }
    public static void sendTimers(Player p){
        if(!p.isPl()||p.id<=0)return;
        State state=enabled()?state(p):EMPTY;long now=System.currentTimeMillis();int active=state.until>now?state.cake:0;
        for(Kind kind:kinds())ItemTimeService.gI().sendItemTime(p,ItemService.gI().getTemplate(kind.id).iconID,kind.id==active?(int)Math.min(32767,(state.until-now+999)/1000):0);
        VIEWS.put(p,new View(active,now));
    }
    public static void tick(Player p){
        if(!p.isPl()||p.id<=0||p.zone==null)return;
        State state=enabled()?state(p):EMPTY;long now=System.currentTimeMillis();int active=state.until>now?state.cake:0;View before=VIEWS.get(p);
        if((before==null&&active!=0)||(before!=null&&before.cake!=active)){refresh(p);sendTimers(p);}
        else if(before!=null&&active!=0&&now-before.refreshed>=60000)sendTimers(p);
    }
    private TrungThuCakeBuff(){}
}