package nro.models.event.october20;
import nro.models.event.trungthu2026.*;

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
public final class October20Competition {
    public record Schedule(long id,long start,long pointsEnd,long end,boolean scheduled,boolean paused,int roseGems,int cardGems,long giftGold,boolean closed){}
    private static volatile Schedule schedule;
    private static final int MENU=202610400;
    private static final int VIP_MENU=202610401;
    private static final int[] VIP_QUANTITIES={1,10,50,100};
    private static final BagExchange CLAIM=new BagExchange(500,System::currentTimeMillis);
    public static Boolean scheduledEnabled(){Schedule s=schedule;return s==null||!s.scheduled?null:!s.paused&&System.currentTimeMillis()>=s.start&&System.currentTimeMillis()<s.end;}
    public static void publish(Schedule s){schedule=s;}
    public static String boardName(String board){return switch(board){case "roses"->"Hộp Quà 20/10 (ngọc)";case "cards"->"Thiệp Chúc VIP";case "gifts"->"Hộp Quà 20/10";case "rose_bonus"->"Mốc top 1 Hộp ngọc";default->"Mốc top 1 Thiệp VIP";};}
    public static Schedule reload(Connection c)throws SQLException{
        Schedule s=null;String rewards=null;
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM october20_season ORDER BY id DESC LIMIT 1 FOR UPDATE")){
            if(r.next()){s=readSchedule(r);rewards=r.getString("rewards");}
        }
        if(s!=null&&s.scheduled&&!s.closed&&System.currentTimeMillis()>=s.pointsEnd){
            close(c,s.id,rewards);
            s=new Schedule(s.id,s.start,s.pointsEnd,s.end,s.scheduled,s.paused,s.roseGems,s.cardGems,s.giftGold,true);
        }
        return s;
    }
    static void close(Connection c,long season,String rewards)throws SQLException{
        try{
            JSONObject tables=(JSONObject)new JSONParser().parse(rewards);
            for(String board:List.of("roses","cards","gifts")){
                int rank=0;
                try(PreparedStatement q=c.prepareStatement("SELECT player_id,score FROM october20_score WHERE season_id=? AND board=? AND score>0 ORDER BY score DESC,reached_ms,player_id LIMIT 100")){
                    q.setLong(1,season);q.setString(2,board);
                    try(ResultSet r=q.executeQuery()){while(r.next()){
                        rank++;JSONArray items=prizes((JSONArray)tables.get(board),rank);
                        enqueue(c,season,board,r.getLong(1),rank,r.getLong(2),items);
                    }}
                }
            }
            for(String board:List.of("roses","cards")){
                try(PreparedStatement q=c.prepareStatement("SELECT player_id,score FROM october20_score WHERE season_id=? AND board=? AND score>0 ORDER BY score DESC,reached_ms,player_id LIMIT 1")){
                    q.setLong(1,season);q.setString(2,board);try(ResultSet r=q.executeQuery()){if(r.next()&&r.getLong(2)>=(board.equals("roses")?21000:41250)){
                        String bonus=board.equals("roses")?"rose_bonus":"card_bonus";
                        enqueue(c,season,bonus,r.getLong(1),1,r.getLong(2),(JSONArray)tables.get(bonus));
                    }}
                }
            }
            try(PreparedStatement q=c.prepareStatement("UPDATE october20_season SET closed_at=NOW() WHERE id=? AND closed_at IS NULL")){q.setLong(1,season);q.executeUpdate();}
        }catch(Exception e){if(e instanceof SQLException x)throw x;throw new SQLException("Không chốt được thưởng 20/10",e);}
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
        try(PreparedStatement q=c.prepareStatement("INSERT IGNORE INTO october20_prize(season_id,board,player_id,place,score,items) VALUES(?,?,?,?,?,?)")){
            q.setLong(1,season);q.setString(2,board);q.setLong(3,player);q.setInt(4,rank);q.setLong(5,score);q.setString(6,items.toJSONString());q.executeUpdate();
        }
    }
    private static final BagExchange BUY=new BagExchange(500,System::currentTimeMillis);
    public static String board(int id){return switch(id){case 1720->"roses";case 1718->"cards";case 1719->"gifts";default->null;};}
    static Schedule readSchedule(ResultSet r)throws SQLException{return new Schedule(r.getLong("id"),r.getLong("start_ms"),r.getLong("points_end_ms"),r.getLong("end_ms"),r.getBoolean("scheduled"),r.getBoolean("paused"),r.getInt("rose_gems"),r.getInt("card_gems"),r.getLong("gift_gold"),r.getTimestamp("closed_at")!=null);}
    public static Schedule current(){return schedule;}
    public static void buy(Player p,int id,int quantity){
        synchronized(p){
            Schedule s=schedule;String board=board(id);
            if(s==null||board==null||Arrays.stream(VIP_QUANTITIES).noneMatch(q->q==quantity)||!Boolean.TRUE.equals(scheduledEnabled())||nro.models.server.Maintenance.isRunning||!p.isPl()){
                Service.gI().sendThongBao(p,"Sự kiện chưa mở hoặc đang tạm tắt. Chưa trừ tiền.");return;
            }
            if(!CatalogAssets.check(p,id))return;
            long totalGold=id==1719?Math.multiplyExact(s.giftGold,quantity):0;
            int totalGems=id==1719?0:Math.multiplyExact(id==1720?s.roseGems:s.cardGems,quantity);
            int rubies=id==1718?Math.min(Math.max(0,p.inventory.ruby),totalGems):0;
            BagExchange.Cost cost=new BagExchange.Cost(Map.of(),totalGold,totalGems-rubies,rubies);
            var result=BUY.execute(p,cost,new BagExchange.Gain(List.of(TrungThuRewards.item(id,quantity,0)),0,0,0),who->persistPurchase(who,s,board,quantity,cost));
            if(result==BagExchange.Result.SUCCESS){InventoryService.gI().sendItemBags(p);Service.gI().sendMoney(p);Service.gI().sendThongBao(p,"Đã nhận "+quantity+" "+ItemService.gI().getTemplate(id).name+". "+(System.currentTimeMillis()<s.pointsEnd?"Mỗi món mua được tính 1 điểm đua top.":"Đã hết giờ tính điểm đua top."));}
            else if(result==BagExchange.Result.MISSING_GEMS)Service.gI().sendThongBao(p,"Không đủ ngọc để mua. Chưa trừ tiền.");else TrungThuCommerce.notifyResult(p,result);
        }
    }
    static void persistPurchase(Player p,Schedule expected,String board,int quantity,BagExchange.Cost cost)throws SQLException{
        try(Connection c=LocalManager.getConnection()){
            c.setAutoCommit(false);try{
                Schedule actual;try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM october20_season ORDER BY id DESC LIMIT 1 FOR UPDATE")){
                    if(!r.next())throw new SQLException("No season");actual=readSchedule(r);
                }
                long now=System.currentTimeMillis();
                if(!actual.equals(expected)||!actual.scheduled||actual.paused||now<actual.start||now>=actual.end)throw new SQLException("Lịch hoặc giá sự kiện vừa thay đổi");
                boolean counted=!actual.closed&&now<actual.pointsEnd;
                if(counted)try(PreparedStatement q=c.prepareStatement("INSERT INTO october20_score(season_id,board,player_id,score,reached_ms) VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE score=score+VALUES(score),reached_ms=VALUES(reached_ms)")){
                    q.setLong(1,actual.id);q.setString(2,board);q.setLong(3,p.id);q.setInt(4,quantity);q.setLong(5,now);q.executeUpdate();
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=? WHERE id=?")){q.setString(1,TrungThuStore.bag(p));q.setString(2,TrungThuStore.money(p));q.setLong(3,p.id);if(q.executeUpdate()!=1)throw new SQLException("Missing player");}
                try(PreparedStatement q=c.prepareStatement("INSERT INTO october20_purchase(season_id,player_id,board,quantity,gold,gems,ruby,counted) VALUES(?,?,?,?,?,?,?,?)")){
                    q.setLong(1,actual.id);q.setLong(2,p.id);q.setString(3,board);q.setInt(4,quantity);q.setLong(5,cost.gold());q.setInt(6,cost.gems());q.setInt(7,cost.ruby());q.setBoolean(8,counted);q.executeUpdate();
                }
                c.commit();
            }catch(Exception e){c.rollback();if(e instanceof SQLException x)throw x;throw new SQLException(e);}
        }
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
            try(Connection c=LocalManager.getConnection();PreparedStatement q=c.prepareStatement("SELECT id,items FROM october20_prize WHERE player_id=? AND claimed_at IS NULL ORDER BY id LIMIT 1")){
                q.setLong(1,p.id);try(ResultSet r=q.executeQuery()){if(r.next()){id=r.getLong(1);json=r.getString(2);}}
            }catch(SQLException e){nro.models.utils.Logger.logException(October20Competition.class,e);Service.gI().sendThongBao(p,"Chưa đọc được quà. Thử lại sau.");return;}
            if(id==0){Service.gI().sendThongBao(p,"Bạn chưa có quà đua top chưa nhận.");return;}
            final long prize=id;
            try{
                var result=CLAIM.execute(p,TrungThuCommerce.cost(Map.of(),0,0),new BagExchange.Gain(decode(json),0,0,0),player->{
                    try(Connection c=LocalManager.getConnection()){
                        c.setAutoCommit(false);try{
                            try(PreparedStatement q=c.prepareStatement("UPDATE october20_prize SET claimed_at=NOW() WHERE id=? AND player_id=? AND claimed_at IS NULL")){
                                q.setLong(1,prize);q.setLong(2,p.id);if(q.executeUpdate()!=1)throw new SQLException("Quà đã nhận");
                            }
                            try(PreparedStatement save=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=? WHERE id=?")){save.setString(1,TrungThuStore.bag(p));save.setString(2,TrungThuStore.money(p));save.setLong(3,p.id);if(save.executeUpdate()!=1)throw new SQLException("Missing player");}c.commit();
                        }catch(SQLException e){c.rollback();throw e;}
                    }
                });if(result==BagExchange.Result.SUCCESS){InventoryService.gI().sendItemBags(p);Service.gI().sendMoney(p);Service.gI().sendThongBao(p,"Đã nhận quà đua top 20/10.");}else TrungThuCommerce.notifyResult(p,result);
            }catch(SQLException e){nro.models.utils.Logger.logException(October20Competition.class,e);Service.gI().sendThongBao(p,"Chưa nhận được quà. Quà vẫn được giữ để nhận lại sau.");}
        }
    }
    public static String standings(Player p){
        StringBuilder text=new StringBuilder("ĐUA TOP 20/10\nGiờ Việt Nam. Hòa điểm: người đạt điểm trước xếp trên.\n");
        try(Connection c=LocalManager.getConnection()){
            long season=0;
            try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM october20_season ORDER BY id DESC LIMIT 1")){
                if(!r.next())return "Chưa có lịch đua top 20/10.";season=r.getLong("id");
                var fmt=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
                text.append(r.getString("name")).append('\n').append(fmt.format(java.time.Instant.ofEpochMilli(r.getLong("start_ms")))).append(" → ").append(fmt.format(java.time.Instant.ofEpochMilli(r.getLong("end_ms")))).append('\n');
                text.append(r.getTimestamp("closed_at")!=null?"Đã chốt top; nhận quà tại Quy Lão hoặc Chi Chi.":!r.getBoolean("scheduled")?"Chưa bật lịch.":System.currentTimeMillis()<r.getLong("start_ms")?"Chưa bắt đầu.":System.currentTimeMillis()<r.getLong("points_end_ms")?"Đang đua top.":"Đang chốt thưởng.").append('\n');
                text.append("Chốt điểm: ").append(fmt.format(java.time.Instant.ofEpochMilli(r.getLong("points_end_ms")))).append("\nMỗi món mua tại NPC: 1 điểm. Đồ được cấp, giao dịch hoặc mở hộp không tính điểm.\n");
            }
            for(String board:List.of("roses","cards","gifts")){
                text.append('\n').append(boardName(board)).append('\n');
                try(PreparedStatement q=c.prepareStatement("SELECT s.score,p.name FROM october20_score s LEFT JOIN player p ON p.id=s.player_id WHERE season_id=? AND board=? ORDER BY score DESC,reached_ms,player_id LIMIT 10")){
                    q.setLong(1,season);q.setString(2,board);int rank=0;try(ResultSet r=q.executeQuery()){while(r.next())text.append(++rank).append(". ").append(r.getString(2)).append(": ").append(r.getLong(1)).append('\n');}
                }
                try(PreparedStatement q=c.prepareStatement("SELECT score FROM october20_score WHERE season_id=? AND board=? AND player_id=?")){
                    q.setLong(1,season);q.setString(2,board);q.setLong(3,p.id);try(ResultSet r=q.executeQuery()){text.append("Điểm của bạn: ").append(r.next()?r.getLong(1):0).append('\n');}
                }
            }
        }catch(SQLException e){nro.models.utils.Logger.logException(October20Competition.class,e);return "Chưa tải được bảng top.";}return text.toString();
    }
    private static final int CARD_MENU=202610402,GIFT_MENU=202610403;
    public static void openNpc(Player p,Npc npc){
        Schedule s=schedule;String info=s==null?"Chưa cấu hình lịch 20/10.":"Hộp Quà 20/10 (ngọc): "+s.roseGems+" ngọc xanh; Thiệp VIP: "+s.cardGems+" ngọc bất kỳ (ưu tiên hồng ngọc); Hộp Quà: "+s.giftGold+" vàng.\n";
        npc.createOtherMenu(p,MENU,"SỰ KIỆN PHỤ NỮ VIỆT NAM 20/10\n"+info+"Mua 1 món = 1 điểm ở bảng tương ứng, không cộng điểm từ mở hộp. Quà top nhận được sau khi chốt.\nXem bảng top để biết chính xác lịch giờ Việt Nam.","Hộp Quà 20/10 (ngọc)","Shop Chi Chi","Xem top","Nhận quà top","Thể lệ","Đóng");
    }
    private static void quantityMenu(Player p,Npc npc,int item){
        Schedule s=schedule;if(s==null||!Boolean.TRUE.equals(scheduledEnabled())){Service.gI().sendThongBao(p,"Chưa mở sự kiện.");return;}
        int menu=item==1720?VIP_MENU:item==1718?CARD_MENU:GIFT_MENU;
        StringBuilder text=new StringBuilder("Mua "+ItemService.gI().getTemplate(item).name+"\n");
        for(int amount:VIP_QUANTITIES)text.append(amount).append(" món: ").append(item==1719?s.giftGold*amount:(long)(item==1720?s.roseGems:s.cardGems)*amount).append(item==1719?" vàng\n":" ngọc\n");
        npc.createOtherMenu(p,menu,text.toString(),"Mua 1","Mua 10","Mua 50","Mua 100","Đóng");
    }
    public static boolean confirm(Player p,Npc npc,int select){
        int menu=p.idMark.getIndexMenu();if(menu!=MENU&&menu!=VIP_MENU&&menu!=CARD_MENU&&menu!=GIFT_MENU)return false;p.idMark.setIndexMenu(-1);
        if(menu!=MENU){if(select>=0&&select<VIP_QUANTITIES.length)buy(p,menu==VIP_MENU?1720:menu==CARD_MENU?1718:1719,VIP_QUANTITIES[select]);return true;}
        switch(select){
            case 0->{if(npc.tempId!=nro.models.consts.ConstNpc.QUY_LAO_KAME)Service.gI().sendThongBao(p,"Đến Quy Lão ở Đảo Kame để mua Hộp Quà 20/10 (ngọc).");else quantityMenu(p,npc,1720);}
            case 1->{if(npc.tempId!=nro.models.consts.ConstNpc.CHI_CHI)Service.gI().sendThongBao(p,"Đến Chi Chi để mua Thiệp VIP và Hộp Quà 20/10.");else October20Shop.open(p);}
            case 2->Service.gI().sendThongBaoOK(p,standings(p));case 3->claim(p);
            case 4->Service.gI().sendThongBaoOK(p,"Ba bảng đua top: mua Hộp Quà 20/10 (ngọc) tại Quy Lão; mua Thiệp Chúc VIP, Hộp Quà 20/10 tại Chi Chi. Mỗi món mua thành công tính 1 điểm. Chỉ điểm của đợt hiện tại, không tính đồ admin cấp hoặc giao dịch. Hòa điểm: đạt trước xếp trên. Top 1 Hộp ngọc đủ 21.000 điểm, Top 1 Thiệp VIP đủ 41.250 điểm nhận thêm Capsule kích hoạt 1 món tự chọn. Không có X3 nạp. Xem top để biết giờ chốt; phần thưởng giữ cho người offline, nhận từng gói sau khi chốt.");
        }return true;
    }
    private October20Competition(){}
}
