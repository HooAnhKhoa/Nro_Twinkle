package nro.models.event.october20;

import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.event.trungthu2026.TrungThuStore;
import nro.models.player.Player;
import nro.models.item.Item;
import org.json.simple.JSONArray;

/** Points, bag, currency and exchange receipt are committed in one InnoDB transaction. */
public final class October20Store {
    public static long points(Player p)throws SQLException {
        try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("SELECT points FROM october20_player WHERE player_id=?")){
            q.setLong(1,p.id);try(ResultSet r=q.executeQuery()){return r.next()?r.getLong(1):0;}
        }
    }
    public static void commit(Player p,October20Config.Snapshot config,boolean requireEnabled,int delta,String operation,List<Item> rewards)throws SQLException {
        try(Connection c=LocalManager.getConnection()){
            c.setAutoCommit(false);
            try {
                if(requireEnabled)try(PreparedStatement q=c.prepareStatement("SELECT enabled FROM panel_event_config WHERE event_key='october20' FOR UPDATE");ResultSet r=q.executeQuery()){
                    if(!r.next()||!r.getBoolean(1))throw new SQLException("Sự kiện đã tắt");
                }
                try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT revision FROM october20_config WHERE id=1 FOR UPDATE")){
                    if(!r.next()||r.getLong(1)!=config.revision())throw new SQLException("Cấu hình vừa thay đổi, vui lòng thử lại");
                }
                try(PreparedStatement q=c.prepareStatement("INSERT IGNORE INTO october20_player(player_id) VALUES(?)")){q.setLong(1,p.id);q.executeUpdate();}
                try(PreparedStatement q=c.prepareStatement("UPDATE october20_player SET points=points+? WHERE player_id=? AND points>=? AND points<=?")){
                    q.setInt(1,delta);q.setLong(2,p.id);q.setLong(3,Math.max(0,-(long)delta));q.setLong(4,Long.MAX_VALUE-Math.max(0,delta));if(q.executeUpdate()!=1)throw new SQLException("Không đủ điểm 20/10");
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=? WHERE id=?")){
                    q.setString(1,TrungThuStore.bag(p));q.setString(2,TrungThuStore.money(p));q.setLong(3,p.id);if(q.executeUpdate()!=1)throw new SQLException("Không tìm thấy nhân vật");
                }
                JSONArray list=new JSONArray();for(Item item:rewards){JSONArray row=new JSONArray();row.add((int)item.template.id);row.add(item.quantity);JSONArray options=new JSONArray();for(Item.ItemOption option:item.itemOptions)options.add(List.of(option.optionTemplate.id,option.param));row.add(options);list.add(row);}
                try(PreparedStatement q=c.prepareStatement("INSERT INTO october20_exchange(player_id,operation,points_delta,config_revision,rewards) VALUES(?,?,?,?,?)")){
                    q.setLong(1,p.id);q.setString(2,operation);q.setInt(3,delta);q.setLong(4,config.revision());q.setString(5,list.toJSONString());q.executeUpdate();
                }
                c.commit();
            }catch(SQLException|RuntimeException error){c.rollback();throw error;}
        }
    }
    private October20Store(){}
}
