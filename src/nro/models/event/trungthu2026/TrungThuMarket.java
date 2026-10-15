package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.shop_ky_gui.*;

/** New event ingredients pay actual gold, matching the existing buyer's gold debit. */
public final class TrungThuMarket {
    private static final BagExchange EXCHANGE=new BagExchange(0,System::currentTimeMillis);
    public static void claim(Player p,ConsignItem item){
        synchronized(p){synchronized(ConsignShopManager.gI()){
            if(!TrungThuRuntime.goldMaterial(item.itemId)||!item.isBuy||item.player_sell!=p.id||item.goldSell<=0||!ConsignShopManager.gI().listItem.contains(item))return;
            long net=item.goldSell-(long)item.goldSell*TrungThuConfig.number("market.tax-percent")/100;
            var result=EXCHANGE.execute(p,TrungThuCommerce.cost(Map.of(),0,0),new BagExchange.Gain(List.of(),net,0,0),player->{
                try(Connection c=LocalManager.getConnection()){
                    c.setAutoCommit(false);try{
                        try(PreparedStatement q=c.prepareStatement("DELETE FROM shop_ky_gui WHERE id=? AND player_id=? AND item_id=? AND isBuy=1 AND gold=?")){
                            q.setInt(1,item.id);q.setLong(2,p.id);q.setInt(3,item.itemId);q.setInt(4,item.goldSell);if(q.executeUpdate()!=1)throw new SQLException("Giao dịch ký gửi đã thay đổi");
                        }TrungThuStore.persistBag(c,p);c.commit();
                    }catch(SQLException error){c.rollback();throw error;}
                }
            });
            if(result==BagExchange.Result.SUCCESS)ConsignShopManager.gI().listItem.remove(item);
            TrungThuCommerce.notifyResult(p,result);
        }}
    }
    private TrungThuMarket(){}
}
