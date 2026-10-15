package nro.models.shop;

import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.event.trungthu2026.BagExchange;
import nro.models.event.trungthu2026.TrungThuStore;
import nro.models.player.Player;
import nro.models.server.Maintenance;
import nro.models.services.*;
import nro.models.utils.Logger;

/** Uses the same configured cost for the native shop packet and the purchase. */
public final class PointShop {
    private static final BagExchange EXCHANGE=new BagExchange(0,System::currentTimeMillis);
    public record Result(boolean success,String message){}
    public static Result buy(Player p,ItemShop sale){
        synchronized(p){
            int price=sale.cost;
            if(price<0)return new Result(false,"Giá điểm không hợp lệ. Vui lòng báo quản trị viên.");
            if(Maintenance.isRunning)return new Result(false,"Máy chủ đang bảo trì. Chưa thực hiện đổi điểm.");
            if(p.event.getEventPoint()<price)return new Result(false,"Không đủ điểm sự kiện để mua vật phẩm này!");
            int after=p.event.getEventPoint()-price;
            try(Connection c=LocalManager.getConnection()){
                c.setAutoCommit(false);boolean committed=false;
                try{
                    var result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(),0,0),
                        new BagExchange.Gain(List.of(ItemService.gI().createItemFromItemShop(sale)),0,0,0),player->{
                            try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=?,event_point=? WHERE id=? AND account_id=?")){
                                q.setString(1,TrungThuStore.bag(p));q.setString(2,TrungThuStore.money(p));q.setInt(3,after);q.setLong(4,p.id);q.setInt(5,p.getSession().userId);
                                if(q.executeUpdate()!=1)throw new SQLException("Missing player or account mismatch");
                            }
                            c.commit();
                        });
                    if(result!=BagExchange.Result.SUCCESS)return new Result(false,result==BagExchange.Result.BAG_FULL?
                        "Hành trang không đủ chỗ. Chưa trừ điểm.":"Chưa đổi được vật phẩm. Điểm và hành trang được giữ nguyên.");
                    committed=true;p.event.setEventPoint(after);
                    return new Result(true,"Đã đổi "+sale.temp.name+" bằng "+price+" điểm sự kiện.");
                }finally{if(!committed)c.rollback();}
            }catch(Exception error){Logger.logException(PointShop.class,error);return new Result(false,"Chưa đổi được vật phẩm. Điểm và hành trang được giữ nguyên.");}
        }
    }
    private PointShop(){}
}
