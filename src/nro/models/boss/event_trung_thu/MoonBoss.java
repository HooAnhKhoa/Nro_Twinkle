package nro.models.boss.event_trung_thu;

import java.util.*;
import nro.models.boss.*;
import nro.models.consts.*;
import nro.models.event.trungthu2026.*;
import nro.models.item.Item;
import nro.models.map.*;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.skill.Skill;
import nro.models.utils.Util;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

/** Four stable pool entries. Monkey/rabbit require paid summons; rivals respawn while EV is on. */
public class MoonBoss extends Boss {
    public static final class Monkey extends MoonBoss {public Monkey()throws Exception{super(0);}}
    public static final class Rabbit extends MoonBoss {public Rabbit()throws Exception{super(1);}}
    public static final class Gogeta extends MoonBoss {public Gogeta()throws Exception{super(2);}}
    public static final class Omega extends MoonBoss {public Omega()throws Exception{super(3);}}
    public final int kind;
    private boolean armed,rewarded;
    private long appeared;
    public MoonBoss(int kind)throws Exception{
        super(BossType.TRUNGTHU_EVENT,idFor(kind),false,false,definition(kind));this.kind=kind;
    }
    private static int idFor(int kind){return switch(kind){case 0->BossID.KHIDOT;case 1->BossID.NGUYETTHAN;case 2->-202610151;case 3->-202610152;default->throw new IllegalArgumentException();};}
    static BossData definition(int kind){
        String key=switch(kind){case 0->"monkey";case 1->"rabbit";default->"rival";};
        String outfitKey=switch(kind){case 0->"monkey";case 1->"rabbit";case 2->"gogeta";default->"omega";};
        int[] parts=numbers("boss."+outfitKey+"-outfit");short[] outfit={(short)parts[0],(short)parts[1],(short)parts[2],-1,-1,-1};
        return new BossData(new String[]{"Khỉ Xayda","Thỏ Đại Ca","Gogeta Trung Thu","Omega Trung Thu"}[kind],(byte)2,outfit,number("boss."+key+"-damage"),new int[]{number("boss."+key+"-hp")},numbers(kind==0?"map.monkey":kind==1?"map.rabbit":"map.rivals"),new int[][]{{Skill.GALICK,1,1000},{Skill.ANTOMIC,1,3000}},new String[]{},new String[]{},new String[]{},number("boss.rival-rest-seconds"));
    }
    public static boolean immune(Player player){return player instanceof MoonBoss boss&&boss.kind==0;}
    public static boolean basic(Player attacker){if(attacker==null||!attacker.isPl()||attacker.playerSkill.skillSelect==null)return false;int id=attacker.playerSkill.skillSelect.template.id;return id==Skill.DRAGON||id==Skill.DEMON||id==Skill.GALICK||id==Skill.KAMEJOKO||id==Skill.MASENKO||id==Skill.ANTOMIC;}
    @Override public boolean panelCanSpawn(){return kind>=2&&super.panelCanSpawn();}
    public synchronized boolean available(){return !armed&&zone==null&&bossStatus==BossStatus.REST;}
    public synchronized void arm(Zone zone){if(!available())throw new IllegalStateException("Boss đã được gọi");armed=true;zoneFinal=zone;currentLevel=-1;changeStatus(BossStatus.RESPAWN);}
    @Override public void rest(){if(kind>=2)super.rest();}
    @Override protected void resetEventState(){super.resetEventState();armed=false;rewarded=false;}
    @Override public void joinMap(){super.joinMap();if(zone!=null){appeared=System.currentTimeMillis();rewarded=false;}}
    @Override public void leaveMap(){super.leaveMap();zoneFinal=null;armed=false;}
    @Override public void autoLeaveMap(){if(System.currentTimeMillis()-appeared>=number("boss.lifetime-seconds")*1000L)leaveMap();}
    @Override public void active(){
        if(kind==1&&zone!=null)for(Player p:new ArrayList<>(zone.getPlayers()))if(p.isPl()&&!p.isDie()&&Util.getDistance(this,p)<=number("boss.rabbit-touch-distance"))TrungThuRuntime.touchRabbit(p);
        super.active();
    }
    public boolean accepts(Player p){
        if(p==null||!p.isPl()||!enabled())return false;
        if(kind==0)return basic(p);
        if(kind>=2){int side=TrungThuStore.state(p).side();Item sword=InventoryService.gI().findItemBag(p,item("wooden-sword"));return sword!=null&&sword.quantity>0&&p.cFlag==side&&side==(kind==2?2:1);}
        return true;
    }
    @Override public synchronized int injured(Player attacker,long damage,boolean piercing,boolean mobAttack){
        if(!accepts(attacker)||isDie())return 0;
        return super.injured(attacker,damage,piercing,mobAttack);
    }
    @Override public boolean panelDropEligible(Player p){return super.panelDropEligible(p)&&!rewarded&&accepts(p);}
    @Override public synchronized void reward(Player p){
        if(rewarded||!accepts(p)||zone==null)return;rewarded=true;
        if(kind<=1){
            int count=Util.nextInt(number("drop.material-min"),number("drop.material-max"));
            if(nro.models.server.PanelBridge.rollDrop("trung_thu",kind==0?"trung_thu.monkey_tail":"trung_thu.carrot",100,100))
                Service.gI().dropItemMap(zone,new ItemMap(zone,item(kind==0?"monkey-tail":"carrot"),count,location.x,location.y,p.id));

        }else{
            if(nro.models.server.PanelBridge.rollDrop("trung_thu",kind==2?"trung_thu.gogeta_costume":"trung_thu.omega_costume",number("drop.costume-percent"),100)){int[] days=numbers("reward.costume-days");drop(p,TrungThuRewards.item(kind==2?"gogeta-costume":"omega-costume",1,days[Util.nextInt(0,days.length-1)]));}
            if(nro.models.server.PanelBridge.rollDrop("trung_thu",kind==2?"trung_thu.gogeta_board":"trung_thu.omega_board",number("drop.board-percent"),100))drop(p,TrungThuRewards.item("moon-board",1,Util.isTrue(number("drop.board-permanent-percent"),100)?0:number("drop.board-days")));
        }
    }
    private void drop(Player p,Item item){ItemMap ground=new ItemMap(zone,item.template.id,item.quantity,location.x,location.y,p.id);ground.options.addAll(item.itemOptions);Service.gI().dropItemMap(zone,ground);}
}
