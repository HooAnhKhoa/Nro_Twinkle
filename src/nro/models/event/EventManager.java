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

/** Independent event switches. Boss populations are allocated once, never on each toggle. */
public final class EventManager {
    private static final EventManager INSTANCE = new EventManager();
    private static final Map<String, Boolean> DEFAULTS = Map.of(
        "broly",true,"hung_vuong",true,"halloween",false,"christmas",false,
        "tet",false,"trung_thu",false,"summer",true,"mob_rewards",true);
    private static volatile Map<String, Boolean> switches = DEFAULTS;
    private static volatile boolean initialized;
    private static final Map<Player,NpcVisibility> npcVisibility=Collections.synchronizedMap(new WeakHashMap<>());
    private static final class NpcVisibility {
        final Zone zone; final Map<String,Boolean> switches;
        NpcVisibility(Zone zone,Map<String,Boolean> switches){this.zone=zone;this.switches=switches;}
    }
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
    public static int npcStatus(Npc npc) {return npcEnabled(npc)?npc.status:0;}
    /** Keep NPC definitions stable: the client's -73 packet hides/shows an existing template. */
    public static void syncNpcVisibility(Player player) {
        synchronized(player) {
            Zone zone=player.zone;
            if(zone==null){npcVisibility.remove(player);return;}
            Map<String,Boolean> snapshot=switches;
            NpcVisibility previous=npcVisibility.get(player);
            if(previous!=null && previous.zone==zone && previous.switches==snapshot)return;
            for(Npc npc:NpcManager.getNpcsByMapPlayer(player)) {
                String key=npcEvent(npc.tempId);
                if(player.zone!=zone)return;
                if(key!=null)Service.gI().sendHideNpc(player,npc.tempId,npc.status==0 || !snapshot.getOrDefault(key,false));
            }
            if(player.zone==zone && switches==snapshot)npcVisibility.put(player,new NpcVisibility(zone,snapshot));
        }
    }
    public static void refreshNpcVisibility(Player player) {
        synchronized(player){npcVisibility.remove(player);syncNpcVisibility(player);}
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
        new HungVuong().boss();
        new Halloween().boss();
        new Christmas().boss();
        // Legacy LunarNewYear.npc() points to unrelated NPC 49; create only its bosses.
        new LunarNewYear().boss();
        new TrungThu().boss();
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
