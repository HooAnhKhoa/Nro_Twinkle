package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_badges.*;
import nro.models.services.*;
import org.json.simple.JSONValue;

/** Redeem the exact title item and persist its stats with the badge in one transaction. */
public final class TrungThuTopTitles {
    public static boolean use(Player p,Item item){
        if(item.template.id!=1885&&item.template.id!=2076)return false;
        synchronized(p){
            if(nro.models.server.Maintenance.isRunning)return true;
            int slot=p.inventory.itemsBag.indexOf(item);
            if(slot<0||item.quantity<1)return true;
            int days=item.itemOptions.stream().filter(o->o.optionTemplate.id==93).mapToInt(o->o.param).findFirst().orElse(0);
            long until=days>0?System.currentTimeMillis()+days*86400000L:Long.MAX_VALUE;
            List<BadgesData> badges=new ArrayList<>();
            for(BadgesData old:p.dataBadges){
                if(old.timeofUseBadges<=System.currentTimeMillis())continue;
                if(old.idBadGes==item.template.part){Service.gI().sendThongBao(p,"Bạn đã sở hữu danh hiệu này. Vật phẩm được giữ nguyên.");return true;}
                BadgesData copy=new BadgesData(old.idBadGes,old.timeofUseBadges,false);copy.rewardOptions=old.rewardOptions;badges.add(copy);
            }
            BadgesData badge=new BadgesData(item.template.part,until,true);
            badge.rewardOptions=new ArrayList<>();
            for(Item.ItemOption o:item.itemOptions)if(o.optionTemplate.id!=93)badge.rewardOptions.add(new Item.ItemOption(o.optionTemplate.id,o.param));
            badges.add(badge);
            Item before=ItemService.gI().copyItem(item);before.expire=item.expire;
            InventoryService.gI().subQuantityItemsBag(p,item,1);
            try(Connection c=LocalManager.getConnection()){
                c.setAutoCommit(false);
                try{
                    TrungThuStore.persistBag(c,p);
                    try(PreparedStatement q=c.prepareStatement("UPDATE player SET dataBadges=? WHERE id=?")){
                        q.setString(1,JSONValue.toJSONString(badges));q.setLong(2,p.id);if(q.executeUpdate()!=1)throw new SQLException("Missing player");
                    }
                    c.commit();
                }catch(Exception e){c.rollback();throw e;}
            }catch(Exception e){p.inventory.itemsBag.set(slot,before);Service.gI().sendThongBao(p,"Chưa lưu được danh hiệu. Vật phẩm được giữ nguyên.");return true;}
            p.dataBadges.clear();p.dataBadges.addAll(badges);p.badges.idBadges=badge.idBadGes;
            p.nPoint.calPoint();InventoryService.gI().sendItemBags(p);Service.gI().point(p);
            Service.gI().sendBadgesPlayer(p,5,badge.idBadGes);
            Service.gI().sendThongBao(p,"Đã kích hoạt "+before.template.name+" cùng chỉ số của vật phẩm.");
        }
        return true;
    }
    private TrungThuTopTitles(){}
}
