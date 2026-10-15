package nro.models.boss.event_noel;
import java.util.*;
import nro.models.boss.*;
import nro.models.consts.BossStatus;
import static nro.models.consts.BossType.CHRISTMAS_EVENT;
import nro.models.event.EventManager;
import nro.models.map.Zone;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Util;

/** Friendly, invulnerable gift giver. Each zone holds at most one. */
public class OngGiaNoel extends Boss {
    private long lastTimeDrop,st;
    private int timeLeave;
    public OngGiaNoel()throws Exception{super(CHRISTMAS_EVENT,BossID.ONG_GIA_NOEL,BossesData.ONG_GIA_NOEL);this.panelIndependent=true;}
    @Override public void joinMap(){
        if(!EventManager.bossEnabled(this))return;
        Zone requested=zoneFinal!=null?zoneFinal:getMapJoin();
        if(requested==null){retryLater();return;}
        List<Zone> candidates=new ArrayList<>(requested.map.zones);candidates.remove(requested);candidates.add(0,requested);
        Zone chosen=null;
        for(Zone candidate:candidates){
            if(candidate.getNumOfPlayers()>10||candidate.getNumOfPlayers()>=candidate.maxPlayer)continue;
            if(candidate.getBosses().stream().anyMatch(boss->boss!=this&&boss instanceof OngGiaNoel))continue;
            chosen=candidate;break;
        }
        if(chosen==null){retryLater();return;}
        this.zone=chosen;int x=Util.nextInt(100,Math.max(100,Math.min(500,chosen.map.mapWidth-100)));
        ChangeMapService.gI().changeMap(this,chosen,x,chosen.map.yPhysicInTop(x,0));
        st=lastTimeDrop=System.currentTimeMillis();timeLeave=Util.nextInt(100000,300000);
        changeStatus(BossStatus.CHAT_S);notifyJoinMap();
    }
    private void retryLater(){zone=null;lastZone=null;lastTimeRest=System.currentTimeMillis();changeStatus(BossStatus.REST);}
    @Override public Player getPlayerAttack(){
        if(zone==null)return null;
        return new ArrayList<>(zone.getNotBosses()).stream().filter(player->player.isPl()&&!player.isDie()&&player.zone==zone)
            .min(Comparator.comparingInt(player->Util.getDistance(this,player))).orElse(null);
    }
    @Override public void chatM(){
        if(!EventManager.bossEnabled(this)||currentLevel<0)return;
        if(Util.canDoWithTime(lastTimeChatM,timeChatM)){
            String[] lines=data[currentLevel].getTextM();if(lines.length>0){String line=lines[Util.nextInt(lines.length)];chat(line.substring(line.lastIndexOf('|')+1));}
            lastTimeChatM=System.currentTimeMillis();timeChatM=Util.nextInt(3000,20000);
        }
    }
    protected boolean rollGift(int denominator){return Util.isTrue(1,denominator);}
    private void giftBox(){
        if(!EventManager.bossEnabled(this)||zone==null||!Util.canDoWithTime(lastTimeDrop,60000))return;
        lastTimeDrop=System.currentTimeMillis();chat("Hô hô hô! Giáng sinh vui vẻ!");
        int[] denominators={3,5,7};
        for(int i=0;i<denominators.length;i++)if(rollGift(denominators[i])){
            int x=Math.max(24,Math.min(zone.map.mapWidth-24,location.x+(i-1)*40));
            // ItemMap registers itself: create it only after the probability check.
            Service.gI().dropItemMap(zone,new ItemMap(zone,648,1,x,zone.map.yPhysicInTop(x,0),-1));
        }
    }
    @Override public void active(){attack();}
    @Override public void attack(){
        if(!EventManager.bossEnabled(this)||zone==null||!Util.canDoWithTime(lastTimeAttack,1000))return;
        lastTimeAttack=System.currentTimeMillis();Player player=getPlayerAttack();if(player==null)return;
        if(Util.getDistance(this,player)<=50)giftBox();else moveToPlayer(player);
    }
    @Override public void autoLeaveMap(){if(st>0&&Util.canDoWithTime(st,timeLeave))leaveMapNew();}
    @Override public void leaveMap(){ChangeMapService.gI().exitMap(this);lastZone=null;lastTimeRest=System.currentTimeMillis();st=0;changeStatus(BossStatus.REST);}
    @Override public synchronized int injured(Player attacker,long damage,boolean piercing,boolean mobAttack){return 0;}
    @Override public void reward(Player player){}
}
