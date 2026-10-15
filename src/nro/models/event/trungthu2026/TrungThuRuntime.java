package nro.models.event.trungthu2026;

import java.util.*;
import nro.models.boss.event_trung_thu.MoonBoss;
import nro.models.consts.ConstMob;
import nro.models.item.Item;
import nro.models.map.*;
import nro.models.mob.Mob;
import nro.models.npc.NpcFactory;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.*;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

public final class TrungThuRuntime {
    private static final Random RANDOM=new Random();
    private static MoonBoss monkey,rabbit;
    private static final BagExchange SUMMON=new BagExchange(number("exchange.cooldown-ms"),System::currentTimeMillis);
    private static final java.util.Map<Player,Boolean> CARROTS=Collections.synchronizedMap(new WeakHashMap<>());
    public static void tickPlayer(Player p){
        if(!p.isPl()||p.id<=0||p.zone==null)return;
        TrungThuCakeBuff.tick(p);
        boolean active=TrungThuStore.carrot(p);Boolean before=CARROTS.put(p,active);
        if(before!=null&&before!=active)Service.gI().Send_Caitrang(p);
    }
    public static boolean goldMaterial(int id){return id==item("flour")||id==item("beans")||id==item("lotus-seed");}
    public static void init()throws Exception{
        TrungThuConfig.validate();
        // The old event's 10 automatic monkeys and Nguyet/Nhat pair are intentionally replaced.
        for(String table:List.of("tails","carrots","lantern","normal-box","vegetarian-box"))TrungThuRewards.table(table);
        for(String key:List.of("moon","rabbit-summon","two-can-lantern","moon-board","omega-costume","normal-box","vegetarian-box","chicken-cake","lotus-cake"))ItemService.gI().getTemplate((short)item(key));
        monkey=new MoonBoss.Monkey();rabbit=new MoonBoss.Rabbit();new MoonBoss.Gogeta();new MoonBoss.Omega();
        for(nro.models.map.Map map:Manager.MAPS)if(map.mapId==5||map.mapId==84){
            if(map.npcs.stream().noneMatch(n->n.tempId==number("npc.event")))map.addNpc(NpcFactory.createNPC(map.mapId,1,Math.min(520,map.mapWidth-80),map.yPhysicInTop(Math.min(520,map.mapWidth-80),0),number("npc.event")));
        }
    }
    public static boolean use(Player p,Item used){
        if(TrungThuCakeBuff.use(p,used))return true;
        int id=used.template.id;
        if(id==item("monkey-tail")){Service.gI().sendThongBao(p,"Gom "+number("exchange.monkey-tails")+" đuôi khỉ, đổi tại NPC Trung Thu.");return true;}
        if(id==item("normal-box")||id==item("vegetarian-box"))return TrungThuCommerce.useBox(p,used);
        if(id!=item("moon")&&id!=item("rabbit-summon"))return false;
        if(!nro.models.event.EventManager.allowNpc(p,"trung_thu"))return true;
        synchronized(p){MoonBoss boss=id==item("moon")?monkey:rabbit;
            if(boss==null||p.zone==null||p.isDie()||nro.models.server.Maintenance.isRunning)return true;
            synchronized(boss){
                if(!boss.available()){Service.gI().sendThongBao(p,"Boss này đang xuất hiện hoặc chuẩn bị xuất hiện. Vật phẩm được giữ nguyên.");return true;}
                int[] maps=numbers(id==item("moon")?"map.monkey":"map.rabbit");
                List<Zone> zones=new ArrayList<>();for(nro.models.map.Map map:Manager.MAPS)if(Arrays.stream(maps).anyMatch(m->m==map.mapId))for(Zone zone:map.zones)if(zone.getNumOfPlayers()<zone.maxPlayer)zones.add(zone);
                if(zones.isEmpty()){Service.gI().sendThongBao(p,"Chưa có khu phù hợp. Vật phẩm được giữ nguyên.");return true;}
                // Pick map first, then zone, so maps with more zones do not gain extra weight.
                List<Integer> available=zones.stream().map(z->z.map.mapId).distinct().toList();int map=available.get(RANDOM.nextInt(available.size()));List<Zone> target=zones.stream().filter(z->z.map.mapId==map).toList();Zone zone=target.get(RANDOM.nextInt(target.size()));
                if(!p.inventory.itemsBag.contains(used)||used.quantity<1)return true;
                BagExchange.Result result=SUMMON.execute(p,TrungThuCommerce.cost(java.util.Map.of(id,1),0,0),new BagExchange.Gain(List.of(),0,0,0),TrungThuStore::persistBag);
                if(result!=BagExchange.Result.SUCCESS){TrungThuCommerce.notifyResult(p,result);return true;}
                boss.arm(zone);InventoryService.gI().sendItemBags(p);Service.gI().sendThongBao(p,"Boss sẽ xuất hiện tại "+zone.map.mapName+", khu "+zone.zoneId+".");
            }
        }return true;
    }
    public static void monkeyReward(Player killer){
        long now=System.currentTimeMillis();String race=killer.gender==1?"namec":killer.gender==0?"earth":"xayda";
        java.util.Map<Long,Long> grants=new HashMap<>();
        if(killer.clan!=null)for(var member:new ArrayList<>(killer.clan.members))grants.put((long)member.id,now+number("buff.monkey.clan-minutes")*60000L);
        grants.merge(killer.id,now+number("buff.monkey."+race+"-minutes")*60000L,Math::max);
        TrungThuStore.grantBatch("monkey_until",grants);
        Service.gI().sendThongBao(killer,"Bạn nhận X3 EXP Trung Thu. Thời gian vẫn trôi khi offline; không cộng với bùa trí tuệ/X2.");
    }
    public static void touchRabbit(Player p){
        Item costume=p.inventory.itemsBody.size()>5?p.inventory.itemsBody.get(5):null;
        if(costume!=null&&costume.isNotNullItem()&&Arrays.stream(numbers("item.rabbit-costumes")).anyMatch(id->id==costume.template.id))return;
        if(TrungThuStore.carrot(p))return;
        TrungThuStore.grant(p.id,"carrot_until",System.currentTimeMillis()+number("buff.carrot-seconds")*1000L);
        Service.gI().Send_Caitrang(p);Service.gI().sendThongBao(p,"Bạn bị hóa cà rốt trong 5 phút, giảm "+number("buff.carrot-damage-reduction-percent")+"% sức đánh.");
    }
    public static void drops(Mob mob,Player attacker,List<ItemMap> items,int x,int y){
        Player owner=attacker instanceof nro.models.player.Pet pet?pet.master:attacker;
        if(!enabled()||!nro.models.event.EventManager.enabled("mob_rewards")||mob.isBigBoss()||mob.tempId==ConstMob.MOC_NHAN||mob.tempId==ConstMob.BU_NHIN_MA_QUAI||!owner.isPl())return;
        int type=mob.zone.map.type;
        if(type==0||type==1){roll(mob,owner,items,x,y,"salted-egg","egg");roll(mob,owner,items,x,y,"chicken","chicken");}
        else if(type==2||type==4||type==6||type==7){roll(mob,owner,items,x,y,"flour","flour");roll(mob,owner,items,x,y,"beans","beans");}
    }
    private static void roll(Mob mob,Player p,List<ItemMap> items,int x,int y,String item,String rate){if(nro.models.server.PanelBridge.rollDrop("trung_thu","trung_thu."+rate,number("drop."+rate+"-percent"),100))items.add(new ItemMap(mob.zone,TrungThuConfig.item(item),1,x,y,p.id));}
    private TrungThuRuntime(){}
}
