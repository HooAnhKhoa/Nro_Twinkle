package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import nro.models.consts.*;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.task.*;
import org.json.simple.JSONArray;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

public final class TrungThuQuest {
    private static final BagExchange EXCHANGE=new BagExchange(number("exchange.cooldown-ms"),System::currentTimeMillis);
    /** True means the EV path handled this request, including failures; do not also pay the legacy path. */
    public static boolean pay(Player p){synchronized(p){
        var task=p.playerTask.sideTask;
        if(!enabled()||task.template==null||!task.isDone()||(task.level!=ConstTask.HARD&&task.level!=ConstTask.VERY_HARD))return false;
        String kind="quest_"+task.level;
        boolean lanternEligible;
        try(Connection c=LocalManager.getConnection()){
            lanternEligible=!TrungThuStore.claimed(c,p.id,kind);
        }catch(SQLException error){Service.gI().sendThongBao(p,"Chưa đọc được thưởng Trung Thu. Nhiệm vụ được giữ nguyên.");return true;}
        boolean hard=task.level==ConstTask.HARD;
        List<nro.models.item.Item> rewards=new ArrayList<>();
        rewards.add(TrungThuRewards.item(hard?706:705,1,30));
        rewards.add(TrungThuRewards.item("lotus-seed",number("quest.lotus-seeds"),0));
        if(lanternEligible)rewards.add(TrungThuRewards.item("lantern-offering",number(hard?"quest.hard-lanterns":"quest.very-hard-lanterns"),0));
        var result=EXCHANGE.execute(p,TrungThuCommerce.cost(Map.of(),0,0),new BagExchange.Gain(rewards,hard?ConstTask.GOLD_HARD:ConstTask.GOLD_VERY_HARD,0,0),player->{
            try(Connection c=LocalManager.getConnection()){
                c.setAutoCommit(false);try{
                    if(lanternEligible)TrungThuStore.receipt(c,p.id,kind);TrungThuStore.persistBag(c,p);
                    JSONArray reset=new JSONArray();reset.add(-1);reset.add(task.receivedTime);reset.add(0);reset.add(task.maxCount);reset.add(task.leftTask);reset.add(0);
                    try(PreparedStatement q=c.prepareStatement("UPDATE player SET data_side_task=? WHERE id=?")){q.setString(1,reset.toJSONString());q.setLong(2,p.id);q.executeUpdate();}c.commit();
                }catch(SQLException error){c.rollback();throw error;}
            }
        });
        if(result==BagExchange.Result.SUCCESS){task.reset();BadgesTaskService.updateCountBagesTask(p,ConstTaskBadges.NONG_DAN_CHAM_CHI,1);}
        TrungThuCommerce.notifyResult(p,result);return true;
    }}
    private TrungThuQuest(){}
}
