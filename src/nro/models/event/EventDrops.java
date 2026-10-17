package nro.models.event;

import java.util.*;
import nro.models.boss.Boss;
import nro.models.consts.ConstMob;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.server.PanelBridge;
import nro.models.services.Service;

/** Material IDs correspond to the actual NPC recipes; outputs are never raw ingredient drops. */
public final class EventDrops {
    public static final int[] COOKING_MATERIALS={1544,1545,1546,1547,1548,1549};
    public static int offeringItem(int mobTemplate) {
        return switch(mobTemplate){case ConstMob.VOI_CHIN_NGA->1220;case ConstMob.GA_CHIN_CUA->1221;case ConstMob.NGUA_CHIN_LMAO->1222;default->-1;};
    }
    public static List<Integer> rollMaterials(int mobTemplate,boolean bigBoss) {
        return rollMaterials(mobTemplate,bigBoss,null);
    }
    public static List<Integer> rollMaterials(int mobTemplate,boolean bigBoss,Player owner) {
        List<Integer> ids=new ArrayList<>();
        int offering=offeringItem(mobTemplate);
        if(offering!=-1){if(PanelBridge.rollDrop("hung_vuong","hung.offering."+offering,100,100))ids.add(offering);}
        else if(!bigBoss && mobTemplate!=ConstMob.MOC_NHAN && mobTemplate!=ConstMob.BU_NHIN_MA_QUAI)
            for(int id:COOKING_MATERIALS)if(nro.models.mob.MobDropBonuses.roll(owner,"hung_vuong","hung.cooking."+id,5,100))ids.add(id);
        return ids;
    }
    public static void appendMobDrops(Mob mob,Player owner,List<ItemMap> items,int x,int y) {
        for(int id:rollMaterials(mob.tempId,mob.isBigBoss(),owner))items.add(new ItemMap(mob.zone,id,1,x,y,owner.id));
    }
    public static void rewardStamp(Boss boss,Player owner) {
        if(owner!=null && boss.zone!=null && PanelBridge.rollDrop("hung_vuong","hung.stamp",100,100))
            Service.gI().dropItemMap(boss.zone,new ItemMap(boss.zone,1558,1,boss.location.x,boss.location.y,owner.id));
    }
}
