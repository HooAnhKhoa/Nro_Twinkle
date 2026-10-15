package nro.models.services;

import java.sql.*;
import java.util.*;
import nro.models.consts.ConstTaskBadges;
import nro.models.data.LocalManager;
import nro.models.event.trungthu2026.BagExchange;
import nro.models.event.trungthu2026.TrungThuStore;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.server.Maintenance;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Logger;

/** Debits the current database balance and saves all rewards in the same transaction. */
public final class VndExchangeService {
    public static final int POINTS_PER_10000 = 5000;
    private static final BagExchange EXCHANGE=new BagExchange(0,System::currentTimeMillis);
    public record Result(boolean success,String message){}
    private static Result fail(String message){return new Result(false,message);}

    public static void exchange(Player p,String raw,boolean gold){
        int amount;
        try{amount=Integer.parseInt(raw.strip());}catch(Exception error){Service.gI().sendThongBao(p,"Vui lòng nhập số VNĐ hợp lệ.");return;}
        Result result=redeem(p,amount,gold);
        if(result.success()){
            InventoryService.gI().sendItemBags(p);Service.gI().sendMoney(p);
            BadgesTaskService.updateCountBagesTask(p,ConstTaskBadges.DAI_GIA_MOI_NHU,amount);
            BadgesTaskService.updateCountBagesTask(p,ConstTaskBadges.EM_XINH_EM_DEP,amount);
        }
        Service.gI().sendThongBao(p,result.message());
    }

    public static Result redeem(Player p,int amount,boolean gold){
        if(amount<10000||amount>5000000)return fail("Tối thiểu 10.000 VNĐ và tối đa 5.000.000 VNĐ.");
        synchronized(p){
            if(Maintenance.isRunning)return fail("Máy chủ đang bảo trì. Chưa thực hiện đổi VNĐ.");
            try(Connection c=LocalManager.getConnection()){
                c.setAutoCommit(false);boolean committed=false;
                try{
                    int balance;boolean active;
                    try(PreparedStatement q=c.prepareStatement("SELECT vnd,active FROM account WHERE id=? FOR UPDATE")){
                        q.setInt(1,p.getSession().userId);
                        try(ResultSet r=q.executeQuery()){if(!r.next())return fail("Không tìm thấy tài khoản.");balance=r.getInt(1);active=r.getBoolean(2);}
                    }
                    p.getSession().vnd=balance;
                    if(gold&&!active)return fail("Vui lòng kích hoạt tài khoản để đổi thỏi vàng.");
                    if(balance<amount)return fail("Số dư VNĐ không đủ, vui lòng nạp thêm.");
                    int points=(amount/10000)*POINTS_PER_10000,tickets=(amount/10000)*10,bars=(amount/1000)*4;
                    long pointAfter=(long)p.event.getEventPoint()+points;
                    if(pointAfter>Integer.MAX_VALUE)return fail("Điểm sự kiện sẽ vượt giới hạn. Chưa trừ VNĐ.");
                    List<Item> items=new ArrayList<>();
                    if(gold)items.add(ItemService.gI().createNewItem((short)457,bars));
                    items.add(ItemService.gI().createNewItem((short)718,tickets));
                    var result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(),0,0),new BagExchange.Gain(items,0,gold?0:amount,0),player->{
                        try{
                            try(PreparedStatement q=c.prepareStatement("UPDATE account SET vnd=vnd-? WHERE id=? AND vnd>=?")){
                                q.setInt(1,amount);q.setInt(2,p.getSession().userId);q.setInt(3,amount);if(q.executeUpdate()!=1)throw new SQLException("Balance changed");
                            }
                            try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=?,event_point=? WHERE id=? AND account_id=?")){
                                q.setString(1,TrungThuStore.bag(p));q.setString(2,TrungThuStore.money(p));q.setInt(3,(int)pointAfter);q.setLong(4,p.id);q.setInt(5,p.getSession().userId);
                                if(q.executeUpdate()!=1)throw new SQLException("Missing player or account mismatch");
                            }
                            c.commit();
                        }catch(Exception error){c.rollback();throw error;}
                    });
                    if(result!=BagExchange.Result.SUCCESS)return fail(switch(result){
                        case BAG_FULL->"Hành trang không đủ chỗ. Chưa trừ VNĐ.";
                        case CURRENCY_LIMIT->"Ngọc sẽ vượt giới hạn. Chưa trừ VNĐ.";
                        default->"Chưa đổi được. Số dư VNĐ và vật phẩm được giữ nguyên.";
                    });
                    committed=true;p.getSession().vnd=balance-amount;p.event.setEventPoint((int)pointAfter);
                    return new Result(true,"Đổi thành công: "+(gold?bars+" thỏi vàng":amount+" ngọc xanh")+", "+tickets+" vé tặng ngọc và "+points+" điểm sự kiện.");
                }finally{if(!committed)c.rollback();}
            }catch(Exception error){Logger.logException(VndExchangeService.class,error);return fail("Chưa đổi được. Số dư VNĐ và vật phẩm được giữ nguyên.");}
        }
    }
    private VndExchangeService(){}
}