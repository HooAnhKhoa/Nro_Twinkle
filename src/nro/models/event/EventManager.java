package nro.models.event;

import java.sql.*;
import java.util.*;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.*;
import nro.models.event_list.*;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.server.ServerManager;
import nro.models.utils.Logger;
import nro.models.consts.ConstNpc;
import nro.models.map.Zone;
import nro.models.map.service.NpcManager;
import nro.models.npc.Npc;
import nro.models.mob.Mob;

/** Independent event switches. Boss populations are allocated once, never on each toggle. */
public final class EventManager {
    private static final EventManager INSTANCE = new EventManager();
    private static final Map<String, Boolean> DEFAULTS = Map.of(
        "broly",true,"hung_vuong",true,"halloween",false,"christmas",false,
        "tet",false,"trung_thu",false,"summer",true,"mob_rewards",true,"october20",false);
    private static volatile Map<String, Boolean> switches = DEFAULTS;
    private static volatile boolean initialized;
    private static final Map<Player,NpcVisibility> npcVisibility=Collections.synchronizedMap(new WeakHashMap<>());
    private static final class NpcVisibility {
        final Zone zone; final Set<Integer> ids;
        NpcVisibility(Zone zone,Set<Integer> ids){this.zone=zone;this.ids=ids;}
    }
    private static final Map<Player,Zone> npcRefreshes=Collections.synchronizedMap(new WeakHashMap<>());
    public static String npcEvent(int npcId) {
        return switch(npcId) {
            case ConstNpc.HUNG_VUONG,ConstNpc.NOI_BANH,ConstNpc.DUA_HAU -> "hung_vuong";
            case ConstNpc.XE_NUOC_MIA -> "summer";
            case ConstNpc.TRUNG_THU -> "trung_thu";
            case ConstNpc.CAY_NEU -> "tet";
            default -> null;
        };
    }
    public static boolean npcEnabled(Npc npc) {
        String key=npcEvent(npc.tempId);
        return key==null || enabled(key);
    }
    private static Set<Integer> eventNpcIds(List<Npc> npcs) {
        Set<Integer> ids=new HashSet<>();
        for(Npc npc:npcs)if(npcEvent(npc.tempId)!=null)ids.add(npc.tempId);
        return ids;
    }
    /** Record the actual transmitted list, not just the latest config (which can change mid-send). */
    public static void npcMapSent(Player player,Zone zone,List<Npc> npcs,List<Mob> mobs) {
        synchronized(player) {
            Set<Integer> ids=eventNpcIds(npcs);
            for(Mob mob:mobs)if(OfferingMobs.isOffering(mob))ids.add(-1000-mob.id);
            npcVisibility.put(player,new NpcVisibility(zone,ids));
            if(npcRefreshes.get(player)!=zone)npcRefreshes.remove(player);
        }
    }
    public static boolean clientKnowsMob(Player player,Mob mob) {
        NpcVisibility state=npcVisibility.get(player);
        return state!=null&&state.zone==mob.zone&&state.ids.contains(-1000-mob.id);
    }
    public static boolean consumeNpcRefresh(Player player) {
        synchronized(player){Zone refreshed=npcRefreshes.remove(player);return refreshed!=null && refreshed==player.zone;}
    }
    /** Rebuild the current map's NPC list. Disabled NPCs are absent, regardless of client status handling. */
    public static void syncNpcVisibility(Player player) {
        synchronized(player) {
            Zone zone=player.zone;
            if(zone==null){npcVisibility.remove(player);npcRefreshes.remove(player);return;}
            Set<Integer> ids=eventNpcIds(NpcManager.getNpcsByMapPlayer(player));
            for(Mob mob:zone.mobs)if(OfferingMobs.isOffering(mob)&&OfferingMobs.visible(mob))ids.add(-1000-mob.id);
            NpcVisibility previous=npcVisibility.get(player);
            if(previous==null || previous.zone!=zone){npcVisibility.put(player,new NpcVisibility(zone,ids));return;}
            if(previous.ids.equals(ids) || player.zone!=zone)return;
            npcRefreshes.put(player,zone);
            Service.gI().clearMap(player);
            zone.mapInfo(player);
        }
    }
    public static EventManager gI() { return INSTANCE; }
    public static boolean enabled(String key) { return switches.getOrDefault(key, false); }
    public static String bossEvent(String className) {
        if(className.startsWith("nro.models.boss.Broly."))return "broly";
        if(className.startsWith("nro.models.boss.event_hung_vuong."))return "hung_vuong";
        if(className.startsWith("nro.models.boss.event.Halloween."))return "halloween";
        if(className.startsWith("nro.models.boss.event_noel."))return "christmas";
        if(className.startsWith("nro.models.boss.event_tet."))return "tet";
        if(className.startsWith("nro.models.boss.event_trung_thu."))return "trung_thu";
        return null;
    }
    public static boolean bossEnabled(Boss boss) {
        String key=bossEvent(boss.getClass().getName());
        return key==null || enabled(key);
    }
    public static boolean allowNpc(Player player,String key) {
        if(enabled(key))return true;
        Service.gI().sendThongBao(player,"Sự kiện hiện đang tắt.");
        return false;
    }
    public static void reload(Connection connection) throws SQLException {
        nro.models.event.october20.October20Config.reload(connection);
        Map<String,Boolean> next=new HashMap<>(DEFAULTS);
        long revision;
        boolean auto=connection.getAutoCommit();
        if(!auto)throw new SQLException("Event reload requires an independent transaction");
        connection.setAutoCommit(false);
        try(Statement statement=connection.createStatement()) {
            try(ResultSet rows=statement.executeQuery("SELECT revision FROM panel_event_runtime WHERE id=1 FOR UPDATE")) {
                if(!rows.next())throw new SQLException("Missing event runtime");
                revision=rows.getLong(1);
            }
            try(ResultSet rows=statement.executeQuery("SELECT event_key,enabled FROM panel_event_config")) {
                while(rows.next())if(DEFAULTS.containsKey(rows.getString(1)))next.put(rows.getString(1),rows.getBoolean(2));
            }
            // Preserve snapshot identity on unchanged polls so player ticks do not resend NPC packets.
            if(!switches.equals(next))switches=Collections.unmodifiableMap(next);
            if(initialized)OfferingMobs.publish(connection);
            if(initialized)try(PreparedStatement update=connection.prepareStatement("UPDATE panel_event_runtime SET heartbeat=NOW(),applied_revision=? WHERE id=1")) {
                update.setLong(1,revision);update.executeUpdate();
            }
            connection.commit();
        }catch(SQLException error){connection.rollback();throw error;}
        finally{connection.setAutoCommit(auto);}
    }
    public synchronized void init() {
        if(initialized)return;
        new nro.models.event_list.Default().boss();
        new HungVuong().npc();
        ThanhCo.seedRoutes();
        OfferingMobs.seed();
        new HungVuong().boss();
        new Halloween().boss();
        new Christmas().boss();
        // Legacy LunarNewYear.npc() points to unrelated NPC 49; create only its bosses.
        new LunarNewYear().boss();
        try{nro.models.event.trungthu2026.TrungThuRuntime.init();}
        catch(Exception error){throw new IllegalStateException("Không khởi tạo được Trung Thu 2026",error);}
        BossManager[] managers={HungVuongEventManager.gI(),HalloweenEventManager.gI(),
            ChristmasEventManager.gI(),LunarNewYearEventManager.gI(),TrungThuEventManager.gI()};
        initialized=true;
        Thread worker=new Thread(()->{
            while(ServerManager.isRunning){
                for(BossManager manager:managers)for(Boss boss:manager.getBosses())try{
                    boss.update();
                }catch(Exception error){Logger.logException(EventManager.class,error);}
                try{Thread.sleep(1000);}catch(InterruptedException stop){Thread.currentThread().interrupt();return;}
            }
        },"Seasonal Event Bosses");
        worker.setDaemon(true);worker.start();
    }
}
