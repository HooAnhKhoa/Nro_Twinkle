package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.*;

/** One season lock serializes counted exchanges, closing and payout receipts. No live bag writes offline. */
public final class TrungThuCompetition {
    public record Schedule(long id,long start,long end,boolean scheduled,String basis,long gold,int gems,boolean closed){}
    private static volatile Schedule schedule;
    private static final int MENU=202610300;
    private static final int VIP_MENU=202610301;
    private static final int[] VIP_QUANTITIES={1,10,50,100};
    private static final BagExchange CLAIM=new BagExchange(500,System::currentTimeMillis);
    public static Boolean scheduledEnabled(){Schedule s=schedule;return s==null||!s.scheduled?null:!s.closed&&System.currentTimeMillis()>=s.start&&System.currentTimeMillis()<s.end;}
    public static void publish(Schedule s){schedule=s;}
    public static String boardName(String board){return switch(board){case "vip"->"Hộp VIP";case "normal"->"Hộp ngọc";case "vegetarian"->"Hộp chay";default->"Mốc 25.000 VIP";};}
    public static Schedule reload(Connection c)throws SQLException{
        Schedule s=null;String rewards=null;
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM trung_thu_season ORDER BY id DESC LIMIT 1 FOR UPDATE")){
            if(r.next()){s=new Schedule(r.getLong("id"),r.getLong("start_ms"),r.getLong("end_ms"),r.getBoolean("scheduled"),r.getString("basis"),r.getLong("vip_gold"),r.getInt("vip_gems"),r.getTimestamp("closed_at")!=null);rewards=r.getString("rewards");}
        }
        if(s!=null&&s.scheduled&&!s.closed&&System.currentTimeMillis()>=s.end){
            close(c,s.id,rewards);
            s=new Schedule(s.id,s.start,s.end,true,s.basis,s.gold,s.gems,true);
        }
        return s;
    }
    static void close(Connection c,long season,String rewards)throws SQLException{
        try{
            JSONObject tables=(JSONObject)new JSONParser().parse(rewards);
            for(String board:List.of("vip","normal","vegetarian")){
                int rank=0;
                try(PreparedStatement q=c.prepareStatement("SELECT player_id,score FROM trung_thu_score WHERE season_id=? AND board=? AND score>0 ORDER BY score DESC,reached_ms,player_id LIMIT 100")){
                    q.setLong(1,season);q.setString(2,board);
                    try(ResultSet r=q.executeQuery()){while(r.next()){
                        rank++;JSONArray items=prizes((JSONArray)tables.get(board),rank);
                        enqueue(c,season,board,r.getLong(1),rank,r.getLong(2),items);
                    }}
                }
            }
            try(PreparedStatement q=c.prepareStatement("SELECT player_id,score FROM trung_thu_score WHERE season_id=? AND board='vip' AND score>=25000")){
                q.setLong(1,season);try(ResultSet r=q.executeQuery()){while(r.next())enqueue(c,season,"milestone",r.getLong(1),0,r.getLong(2),(JSONArray)tables.get("milestone"));}
            }
            try(PreparedStatement q=c.prepareStatement("UPDATE trung_thu_season SET closed_at=NOW() WHERE id=? AND closed_at IS NULL")){q.setLong(1,season);q.executeUpdate();}
        }catch(Exception e){if(e instanceof SQLException x)throw x;throw new SQLException("Không chốt được thưởng Trung Thu",e);}
    }
    static JSONArray prizes(JSONArray tiers,int rank){
        if(tiers==null)throw new IllegalArgumentException("Thiếu bảng thưởng");
        for(Object value:tiers){JSONObject tier=(JSONObject)value;if(rank>=((Number)tier.get("from")).intValue()&&rank<=((Number)tier.get("to")).intValue())return (JSONArray)tier.get("items");}
        throw new IllegalArgumentException("Thiếu phần thưởng hạng "+rank);
    }
    static void enqueue(Connection c,long season,String board,long player,int rank,long score,JSONArray items)throws SQLException{
        if(items==null||items.isEmpty())throw new SQLException("Thưởng rỗng");
        // Validate all IDs before closing; a bad configuration leaves the season retryable.
        decode(items.toJSONString());
        try(PreparedStatement q=c.prepareStatement("INSERT IGNORE INTO trung_thu_prize(season_id,board,player_id,place,score,items) VALUES(?,?,?,?,?,?)")){
            q.setLong(1,season);q.setString(2,board);q.setLong(3,player);q.setInt(4,rank);q.setLong(5,score);q.setString(6,items.toJSONString());q.executeUpdate();
        }
    }
    public static String board(int id){if(id==2079)return "vip";if(id==2078||id==1316)return "normal";if(id==2080||id==1317)return "vegetarian";return null;}
    /** Called only inside the SAME transaction as the player's consumed/gained items and money. */
    static void count(Connection c,Player p,int item,String basis)throws SQLException{count(c,p,item,basis,1);}
    static void count(Connection c,Player p,int item,String basis,int quantity)throws SQLException{
        if(quantity<1||quantity>100)throw new SQLException("Invalid counted quantity");
        String board=board(item);if(board==null)return;
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM trung_thu_season ORDER BY id DESC LIMIT 1 FOR UPDATE")){
            if(!r.next())return;long now=System.currentTimeMillis();
            if(!r.getBoolean("scheduled")||r.getTimestamp("closed_at")!=null||!basis.equals(r.getString("basis"))||now<r.getLong("start_ms")||now>=r.getLong("end_ms"))return;
            try(PreparedStatement add=c.prepareStatement("INSERT INTO trung_thu_score(season_id,board,player_id,score,reached_ms) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE score=score+VALUES(score),reached_ms=VALUES(reached_ms)")){
                add.setLong(1,r.getLong("id"));add.setString(2,board);add.setLong(3,p.id);add.setInt(4,quantity);add.setLong(5,now);add.executeUpdate();
            }
        }
    }
    static void persist(Player p,int item,String basis)throws SQLException{persist(p,item,basis,1);}
    static void persist(Player p,int item,String basis,int quantity)throws SQLException{
        try(Connection c=LocalManager.getConnection()){
            c.setAutoCommit(false);try{count(c,p,item,basis,quantity);TrungThuStore.persistBag(c,p);c.commit();}catch(SQLException e){c.rollback();throw e;}
        }
    }
    public static BagExchange.Cost vipCost(){Schedule s=schedule;return s!=null&&!s.closed&&s.scheduled&&s.gold+s.gems>0?TrungThuCommerce.cost(Map.of(),s.gold,s.gems):null;}
    public static BagExchange.Cost vipCost(int quantity){
        if(Arrays.stream(VIP_QUANTITIES).noneMatch(q->q==quantity))return null;
        BagExchange.Cost cost=vipCost();if(cost==null)return null;
        try{return TrungThuCommerce.cost(Map.of(),Math.multiplyExact(cost.gold(),quantity),Math.multiplyExact(cost.gems(),quantity));}
        catch(ArithmeticException e){return null;}
    }
    public static void buyVip(Player p){buyVip(p,1);}
    public static void buyVip(Player p,int quantity){
        BagExchange.Cost cost=vipCost(quantity);
        if(cost==null||!Boolean.TRUE.equals(scheduledEnabled())){Service.gI().sendThongBao(p,"Chưa mở đổi hộp VIP. Xem lịch đua top tại Quy Lão.");return;}
        TrungThuCommerce.tradeCounted(p,cost,TrungThuCommerce.gain(TrungThuRewards.item(2079,quantity,0)),2079,"acquired",quantity);
    }
    private static void openVip(Player p,Npc npc){
        BagExchange.Cost unit=vipCost();
        if(unit==null||!Boolean.TRUE.equals(scheduledEnabled())){Service.gI().sendThongBao(p,"Đợt Trung Thu đã kết thúc hoặc chưa mở đổi hộp VIP.");return;}
        var format=java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("vi-VN"));
        StringBuilder text=new StringBuilder("ĐỔI HỘP TRUNG THU VIP\nChọn số hộp cần đổi:\n");
        for(int quantity:VIP_QUANTITIES){BagExchange.Cost cost=vipCost(quantity);if(cost==null){Service.gI().sendThongBao(p,"Giá hộp vượt giới hạn, cần chỉnh lại cấu hình.");return;}text.append(quantity).append(" hộp: ").append(format.format(cost.gold())).append(" vàng + ").append(format.format(cost.gems())).append(" ngọc\n");}
        npc.createOtherMenu(p,VIP_MENU,text.toString(),"Đổi 1 hộp","Đổi 10 hộp","Đổi 50 hộp","Đổi 100 hộp","Đóng");
    }
    static List<Item> decode(String json)throws SQLException{
        try{
            List<Item> items=new ArrayList<>();
            for(Object value:(JSONArray)new JSONParser().parse(json)){
                JSONObject row=(JSONObject)value;int id=((Number)row.get("id")).intValue(),qty=((Number)row.get("quantity")).intValue(),days=((Number)row.get("days")).intValue();
                if(id<0||!CatalogAssets.available(id)||ItemService.gI().getTemplate(id)==null||qty<1||qty>99999||days<0||days>3650)throw new IllegalArgumentException("Thưởng sai ID/số lượng/hạn");
                Item item=TrungThuRewards.item(id,qty,days);
                for(Object opt:(JSONArray)row.getOrDefault("options",new JSONArray())){JSONObject o=(JSONObject)opt;int option=((Number)o.get("id")).intValue(),param=((Number)o.get("param")).intValue();if(option==93||param<0)throw new IllegalArgumentException("Option thưởng");item.itemOptions.add(new Item.ItemOption(option,param));}
                items.add(item);
            }return items;
        }catch(Exception e){throw new SQLException("Bảng thưởng không hợp lệ",e);}
    }
    public static void claim(Player p){
        if(nro.models.server.Maintenance.isRunning)return;
        synchronized(p){
            long id=0;String json=null;
            try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("SELECT id,items FROM trung_thu_prize WHERE player_id=? AND claimed_at IS NULL ORDER BY id LIMIT 1")){
                q.setLong(1,p.id);try(ResultSet r=q.executeQuery()){if(r.next()){id=r.getLong(1);json=r.getString(2);}}
            }catch(SQLException e){nro.models.utils.Logger.logException(TrungThuCompetition.class,e);Service.gI().sendThongBao(p,"Chưa đọc được quà. Thử lại sau.");return;}
            if(id==0){Service.gI().sendThongBao(p,"Bạn chưa có quà đua top chưa nhận.");return;}
            final long prize=id;
            try{
                var result=CLAIM.execute(p,TrungThuCommerce.cost(Map.of(),0,0),new BagExchange.Gain(decode(json),0,0,0),player->{
                    try(Connection c=LocalManager.getConnection()){
                        c.setAutoCommit(false);try{
                            try(PreparedStatement q=c.prepareStatement("UPDATE trung_thu_prize SET claimed_at=NOW() WHERE id=? AND player_id=? AND claimed_at IS NULL")){
                                q.setLong(1,prize);q.setLong(2,p.id);if(q.executeUpdate()!=1)throw new SQLException("Quà đã nhận");
                            }
                            TrungThuStore.persistBag(c,p);c.commit();
                        }catch(SQLException e){c.rollback();throw e;}
                    }
                });TrungThuCommerce.notifyResult(p,result);
            }catch(SQLException e){nro.models.utils.Logger.logException(TrungThuCompetition.class,e);Service.gI().sendThongBao(p,"Chưa nhận được quà. Quà vẫn được giữ để nhận lại sau.");}
        }
    }
    public static String standings(Player p){
        StringBuilder text=new StringBuilder("ĐUA TOP TRUNG THU\nGiờ Việt Nam. Hòa điểm: người đạt điểm trước xếp trên.\n");
        try(Connection c=LocalManager.getConnection()){
            long season=0;
            try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM trung_thu_season ORDER BY id DESC LIMIT 1")){
                if(!r.next())return "Chưa có lịch đua top Trung Thu.";season=r.getLong("id");
                var fmt=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
                text.append(r.getString("name")).append('\n').append(fmt.format(java.time.Instant.ofEpochMilli(r.getLong("start_ms")))).append(" → ").append(fmt.format(java.time.Instant.ofEpochMilli(r.getLong("end_ms")))).append('\n');
                text.append(r.getTimestamp("closed_at")!=null?"Đã kết thúc; nhận quà tại Quy Lão.":!r.getBoolean("scheduled")?"Chưa bật lịch.":System.currentTimeMillis()<r.getLong("start_ms")?"Chưa bắt đầu.":System.currentTimeMillis()<r.getLong("end_ms")?"Đang đua top.":"Đang chốt thưởng.").append('\n');
                text.append("Tính theo: ").append("opened".equals(r.getString("basis"))?"hộp đã mở":"hộp mua/đổi tại NPC").append(". Mỗi hộp: 1 điểm.\n");
            }
            for(String board:List.of("vip","normal","vegetarian")){
                text.append('\n').append(boardName(board)).append('\n');
                try(PreparedStatement q=c.prepareStatement("SELECT s.score,p.name FROM trung_thu_score s LEFT JOIN player p ON p.id=s.player_id WHERE season_id=? AND board=? ORDER BY score DESC,reached_ms,player_id LIMIT 10")){
                    q.setLong(1,season);q.setString(2,board);int rank=0;try(ResultSet r=q.executeQuery()){while(r.next())text.append(++rank).append(". ").append(r.getString(2)).append(": ").append(r.getLong(1)).append('\n');}
                }
                try(PreparedStatement q=c.prepareStatement("SELECT score FROM trung_thu_score WHERE season_id=? AND board=? AND player_id=?")){
                    q.setLong(1,season);q.setString(2,board);q.setLong(3,p.id);try(ResultSet r=q.executeQuery()){text.append("Điểm của bạn: ").append(r.next()?r.getLong(1):0).append('\n');}
                }
            }
        }catch(SQLException e){nro.models.utils.Logger.logException(TrungThuCompetition.class,e);return "Chưa tải được bảng top.";}return text.toString();
    }
    public static void openNpc(Player p,Npc npc){
        BagExchange.Cost cost=vipCost();
        npc.createOtherMenu(p,MENU,"Đua top Trung Thu. Quà được chốt tự động khi hết lịch; có thể nhận sau khi sự kiện tắt.\n"+(cost==null?"Chưa cấu hình giá hộp VIP.":"Hộp VIP: "+cost.gold()+" vàng + "+cost.gems()+" ngọc."),"Xem top","Đổi hộp VIP","Nhận quà top","Đóng");
    }
    public static boolean confirm(Player p,Npc npc,int select){
        int menu=p.idMark.getIndexMenu();if(menu!=MENU&&menu!=VIP_MENU)return false;p.idMark.setIndexMenu(-1);
        if(menu==VIP_MENU){if(select>=0&&select<VIP_QUANTITIES.length)buyVip(p,VIP_QUANTITIES[select]);return true;}
        switch(select){case 0->Service.gI().sendThongBaoOK(p,standings(p));case 1->openVip(p,npc);case 2->claim(p);}return true;
    }
    private TrungThuCompetition(){}
}
