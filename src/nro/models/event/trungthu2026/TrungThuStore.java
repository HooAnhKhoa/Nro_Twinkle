package nro.models.event.trungthu2026;

import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.server.Client;
import org.json.simple.JSONArray;

/** Buff timestamps survive offline time. Inventory exchanges commit before network acknowledgements. */
public final class TrungThuStore {
    public record State(long monkeyUntil,long doubleUntil,long carrotUntil,int side){}
    private static final Map<Player,State> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
    public static String today(){return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString();}
    public static void ensureData(Connection connection){
        try(Statement s=connection.createStatement()){
            try(ResultSet r=s.executeQuery("SELECT TABLE_NAME,ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME IN ('player','shop_ky_gui')")){
                while(r.next())if(!"InnoDB".equalsIgnoreCase(r.getString(2)))throw new SQLException("Bảng "+r.getString(1)+" phải dùng InnoDB để đổi thưởng an toàn");
            }
            for(String sql:Files.readString(Path.of("web/trung_thu_schema.sql")).split(";"))if(!sql.isBlank())s.execute(sql);
            TrungThuShop.ensureData(connection);
        }catch(Exception error){throw new IllegalStateException("Không tạo được dữ liệu Trung Thu",error);}
    }
    public static State state(Player player){
        if(player.id<=0)return new State(0,0,0,0);
        synchronized(CACHE){
        State state=CACHE.get(player);if(state!=null)return state;
        try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("SELECT monkey_until,double_until,carrot_until,side FROM trung_thu_2026_state WHERE player_id=?")){
            q.setLong(1,player.id);try(ResultSet r=q.executeQuery()){state=r.next()?new State(r.getLong(1),r.getLong(2),r.getLong(3),r.getInt(4)):new State(0,0,0,0);}
            CACHE.put(player,state);return state;
        }catch(SQLException error){throw new IllegalStateException("Không đọc được trạng thái Trung Thu",error);}
        }
    }
    public static int multiplier(Player player){
        if(!TrungThuConfig.enabled()||!player.isPl())return 1;
        State state=state(player);long now=System.currentTimeMillis();return state.monkeyUntil>now?3:state.doubleUntil>now?2:1;
    }
    public static boolean carrot(Player p){return TrungThuConfig.enabled()&&p.isPl()&&state(p).carrotUntil>System.currentTimeMillis();}
    public static void invalidate(long id){synchronized(CACHE){CACHE.keySet().removeIf(p->p.id==id);}}
    public static void grant(long id,String field,long until){
        grantBatch(field,Map.of(id,until));
    }
    public static void grantBatch(String field,Map<Long,Long> grants){
        if(!Set.of("monkey_until","double_until","carrot_until").contains(field))throw new IllegalArgumentException(field);
        try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("INSERT INTO trung_thu_2026_state(player_id,"+field+") VALUES(?,?) ON DUPLICATE KEY UPDATE "+field+"=GREATEST("+field+",VALUES("+field+"))")){
            c.setAutoCommit(false);try{
                for(var grant:grants.entrySet()){q.setLong(1,grant.getKey());q.setLong(2,grant.getValue());q.addBatch();}q.executeBatch();c.commit();
            }catch(SQLException error){c.rollback();throw error;}
            for(long id:grants.keySet())invalidate(id);
        }catch(SQLException error){throw new IllegalStateException("Không lưu được buff Trung Thu",error);}
    }
    public static void side(Player p,int side)throws SQLException{
        try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("INSERT INTO trung_thu_2026_state(player_id,side) VALUES(?,?) ON DUPLICATE KEY UPDATE side=VALUES(side)")){
            q.setLong(1,p.id);q.setInt(2,side);q.executeUpdate();CACHE.remove(p);
        }
    }
    public static String bag(Player p){
        JSONArray bag=new JSONArray();
        for(Item item:p.inventory.itemsBag){JSONArray row=new JSONArray(),options=new JSONArray();
            if(item.isNotNullItem())for(Item.ItemOption option:item.itemOptions){JSONArray pair=new JSONArray();pair.add(option.optionTemplate.id);pair.add(option.param);options.add(pair.toJSONString());}
            row.add(item.isNotNullItem()?item.template.id:-1);row.add(item.isNotNullItem()?item.quantity:0);row.add(options.toJSONString());row.add(item.createTime);bag.add(row.toJSONString());
        }return bag.toJSONString();
    }
    public static String money(Player p){JSONArray row=new JSONArray();row.add(p.inventory.gold);row.add(p.inventory.gem);row.add(p.inventory.ruby);row.add(p.inventory.coupon);row.add(p.inventory.event);return row.toJSONString();}
    public static void persistBag(Player player)throws SQLException{
        try(Connection c=LocalManager.getConnection()){
            c.setAutoCommit(false);try{persistBag(c,player);c.commit();}catch(SQLException error){c.rollback();throw error;}
        }
    }
    static void persistBag(Connection c,Player p)throws SQLException{
        try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=? WHERE id=?")){
            q.setString(1,bag(p));q.setString(2,money(p));q.setLong(3,p.id);if(q.executeUpdate()!=1)throw new SQLException("Không tìm thấy nhân vật");
        }
    }
    public static boolean claimed(Connection c,long owner,String kind)throws SQLException{
        try(PreparedStatement q=c.prepareStatement("SELECT 1 FROM trung_thu_2026_daily WHERE owner_id=? AND reward_day=? AND reward_kind=?")){
            q.setLong(1,owner);q.setString(2,today());q.setString(3,kind);try(ResultSet r=q.executeQuery()){return r.next();}
        }
    }
    public static void receipt(Connection c,long owner,String kind)throws SQLException{
        try(PreparedStatement q=c.prepareStatement("INSERT INTO trung_thu_2026_daily(owner_id,reward_day,reward_kind) VALUES(?,?,?)")){
            q.setLong(1,owner);q.setString(2,today());q.setString(3,kind);q.executeUpdate();
        }
    }
    private TrungThuStore(){}
}
