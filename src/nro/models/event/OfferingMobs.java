package nro.models.event;

import java.util.*;
import java.sql.*;
import java.util.concurrent.CopyOnWriteArrayList;
import nro.models.consts.ConstMob;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.mob_bigboss.*;
import nro.models.player_system.Template.MobTemplate;
import nro.models.server.Manager;
import nro.models.server.Maintenance;
import nro.models.network.Message;
import nro.models.player.Player;

/** Stable mob slots: three fixed offerings in each castle zone and six random world map/zone groups. */
public final class OfferingMobs {
    private static final List<Mob> population=new CopyOnWriteArrayList<>();
    private static boolean seeded;
    public static boolean isOffering(Mob mob){return EventDrops.offeringItem(mob.tempId)!=-1;}
    public static boolean visible(Mob mob){return !isOffering(mob)||EventManager.enabled("hung_vuong");}
    public static synchronized void seed() {
        if(seeded)return;
        List<nro.models.map.Map> world=new ArrayList<>();
        for(nro.models.map.Map map:Manager.MAPS){
            int id=map.mapId;
            if(id>=183 && id<=185)for(Zone zone:map.zones)addTriplet(zone);
            else if((id>=1&&id<=38&&id!=7&&id!=14&&(id<21||id>26))||(id>=63&&id<=83)||(id>=105&&id<=110))
                if(!map.zones.isEmpty())world.add(map);
        }
        Collections.shuffle(world);
        for(nro.models.map.Map map:world.subList(0,Math.min(6,world.size())))addTriplet(map.zones.get(new Random().nextInt(map.zones.size())));
        seeded=true;
    }
    private static void addTriplet(Zone zone) {
        // All three form a tail block. OFF omits the whole block, preserving client mob indices.
        if(zone.mobs.size()>124 || zone.mobs.stream().anyMatch(OfferingMobs::isOffering))return;
        for(int id:new int[]{ConstMob.VOI_CHIN_NGA,ConstMob.GA_CHIN_CUA,ConstMob.NGUA_CHIN_LMAO}) {
            MobTemplate template=Manager.getMobTemplateByTemp(id);
            if(template==null)throw new IllegalStateException("Missing offering mob template "+id);
        }
        for(int id:new int[]{ConstMob.VOI_CHIN_NGA,ConstMob.GA_CHIN_CUA,ConstMob.NGUA_CHIN_LMAO}) {
            MobTemplate template=Manager.getMobTemplateByTemp(id);
            Mob base=new Mob();base.id=zone.mobs.size();base.tempId=id;base.level=21;base.type=template.type;
            base.pDame=template.percentDame;base.pTiemNang=template.percentTiemNang;
            base.point.setHpFull(template.hp);base.point.sethp(template.hp);base.setTiemNang();
            int index=id-ConstMob.VOI_CHIN_NGA;
            base.location.x=Math.max(48,zone.map.mapWidth*(index+1)/4);
            base.location.y=zone.map.yPhysicInTop(base.location.x,0);
            Mob mob=switch(id){case ConstMob.VOI_CHIN_NGA->new VoiChinNga(base);case ConstMob.GA_CHIN_CUA->new GaChinCua(base);default->new NguaChinLmao(base);};
            mob.zone=zone;mob.eventOfferingSuspended=!EventManager.enabled("hung_vuong");
            if(mob.eventOfferingSuspended){mob.point.hp=0;mob.status=0;}
            zone.mobs.add(mob);population.add(mob);
        }
    }
    /** Called only by each zone's existing update thread; no second attacker/update thread. */
    public static boolean tick(Mob mob) {
        if(!isOffering(mob))return false;
        if(!EventManager.enabled("hung_vuong")){
            mob.eventOfferingSuspended=true;mob.point.hp=0;mob.status=0;return true;
        }
        if(mob.eventOfferingSuspended){mob.eventOfferingSuspended=false;revive(mob);}
        if(mob.isDie()){
            if(!Maintenance.isRunning && System.currentTimeMillis()-mob.lastTimeDie>=60000)revive(mob);
            return true;
        }
        return false;
    }
    private static void revive(Mob mob) {
        mob.hoiSinh();
        // An OFF client has no event slots yet; let its map refresh introduce them first.
        for(Player player:new ArrayList<>(mob.zone.getPlayers()))synchronized(player){
            if(!EventManager.clientKnowsMob(player,mob))continue;
            Message message=new Message(-13);
            try{message.writer().writeByte(mob.id);message.writer().writeByte(mob.tempId);message.writer().writeByte(0);message.writer().writeInt(mob.point.hp);player.sendMessage(message);}
            catch(java.io.IOException error){throw new IllegalStateException(error);}finally{message.cleanup();}
        }
    }
    public static void publish(Connection connection)throws SQLException {
        List<Map<String,Object>> entries=new ArrayList<>();
        for(Mob mob:population){Map<String,Object> row=new LinkedHashMap<>();row.put("map_id",mob.zone.map.mapId);row.put("map_name",mob.zone.map.mapName);row.put("zone_id",mob.zone.zoneId);row.put("mob_id",mob.tempId);row.put("name",Manager.getMobTemplateByTemp(mob.tempId).name);row.put("alive",visible(mob)&&!mob.isDie());entries.add(row);}
        try(PreparedStatement update=connection.prepareStatement("UPDATE panel_event_mob_runtime SET locations=?,heartbeat=NOW() WHERE id=1")){update.setString(1,new com.google.gson.Gson().toJson(entries));update.executeUpdate();}
    }
}
