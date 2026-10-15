package nro.models.event_list;


import nro.models.boss.BossID;
import nro.models.event.Event;

public class HungVuong extends Event {

    @Override
    public void npc() {
        // Seed once at boot, including when the event starts OFF; visibility follows the web switch.
        ensureNpc(0, 52, 720, 432);
        ensureNpc(0, 66, 1120, 432);
        ensureNpc(7, 52, 1000, 432);
        ensureNpc(7, 66, 1120, 432);
        ensureNpc(14, 52, 650, 408);
        ensureNpc(14, 66, 780, 408);
    }

    private void ensureNpc(int mapId,int npcId,int x,int y) {
        nro.models.map.Map map=nro.models.map.service.MapService.gI().getMapById(mapId);
        if(map==null || map.npcs.stream().anyMatch(npc->npc!=null && npc.tempId==npcId))return;
        nro.models.npc.Npc npc=nro.models.npc.NpcFactory.createNPC(mapId,1,x,y,npcId);
        if(npc==null)throw new IllegalStateException("Cannot create Hung Vuong NPC "+npcId+" on map "+mapId);
        map.addNpc(npc);
    }

    @Override
    public void boss() {
        createBoss(BossID.THUY_TINH, 10);
    }
}
