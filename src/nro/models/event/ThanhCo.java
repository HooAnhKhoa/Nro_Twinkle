package nro.models.event;

import java.nio.file.*;
import java.sql.*;
import java.util.*;
import nro.models.consts.ConstMap;
import nro.models.item.Item;
import nro.models.map.Map;
import nro.models.map.WayPoint;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.server.Maintenance;
import nro.models.services.InventoryService;
import nro.models.services.Service;

/** A paid entry to the castle; moving between castle maps and leaving are free. */
public final class ThanhCo {
    public static final int TICKET=1876;
    public static boolean isCastle(int id){return id>=183&&id<=185;}

    /** Runs before loading item templates and shops, so a fresh checkout can use the new ID. */
    public static void ensureData(Connection connection){
        try(Statement statement=connection.createStatement()){
            try(ResultSet row=statement.executeQuery("SELECT NAME FROM item_template WHERE id=1876")){
                if(row.next()&&!"Vé vàng".equals(row.getString(1)))throw new IllegalStateException("Item 1876 already belongs to another item; refusing to reuse it");
            }
            for(String sql:Files.readString(Path.of("web/thanh_co_schema.sql")).split(";"))if(!sql.isBlank())statement.execute(sql);
            try(ResultSet row=statement.executeQuery("SELECT NAME FROM item_template WHERE id=1876")){
                if(!row.next())throw new IllegalStateException("Missing ticket template canonical item 1876");
            }
        }catch(Exception error){throw new IllegalStateException("Cannot initialize Thanh Co ticket",error);}
    }

    public static boolean useTicket(Player player,Item ticket){
        synchronized(player){
            if(ticket==null||!ticket.isNotNullItem()||ticket.template.id!=TICKET||ticket.quantity<1||!player.inventory.itemsBag.contains(ticket))return false;
            if(player.zone==null||player.isDie()||Maintenance.isRunning){Service.gI().sendThongBao(player,"Hiện chưa thể dùng vé Thành Cổ.");return false;}
            if(!EventManager.allowNpc(player,"hung_vuong"))return false;
            if(isCastle(player.zone.map.mapId)){Service.gI().sendThongBao(player,"Bạn đang ở Thành Cổ. Có thể đi qua cổng để sang map khác.");return false;}
            if(player.zone.map.type!=ConstMap.MAP_NORMAL&&player.zone.map.type!=ConstMap.MAP_OFFLINE){Service.gI().sendThongBao(player,"Hãy ra khỏi phó bản rồi dùng vé Thành Cổ.");return false;}
            Zone target=null;
            for(Map map:Manager.MAPS)if(map.mapId==183)for(Zone zone:map.zones)if(zone.getNumOfPlayers()<zone.maxPlayer){target=zone;break;}
            if(target==null){Service.gI().sendThongBao(player,"Thành Cổ đang đầy hoặc chưa sẵn sàng. Vé được giữ nguyên.");return false;}
            ChangeMapService.gI().changeMap(player,target,120,target.map.yPhysicInTop(120,0));
            if(player.zone!=target)return false;
            InventoryService.gI().subQuantityItemsBag(player,ticket,1);
            InventoryService.gI().sendItemBags(player);
            return true;
        }
    }

    public static void seedRoutes(){
        for(Map map:Manager.MAPS)if(isCastle(map.mapId)){
            addRoute(map,false,map.mapId==183?5:map.mapId-1);
            addRoute(map,true,map.mapId==185?5:map.mapId+1);
        }
    }
    private static void addRoute(Map from,boolean right,int destination){
        if(from.wayPoints.stream().anyMatch(wp->wp.goMap==destination))return;
        Map to=Manager.MAPS.stream().filter(map->map.mapId==destination).findFirst().orElse(null);
        if(to==null)return;
        WayPoint gate=new WayPoint();
        gate.minX=(short)(right?from.mapWidth-48:0);gate.maxX=(short)(right?from.mapWidth:48);
        gate.minY=0;gate.maxY=(short)from.mapHeight;gate.isEnter=false;gate.isOffline=false;
        gate.name=to.mapName;gate.goMap=destination;
        gate.goX=(short)(destination==5?240:right?120:Math.max(120,to.mapWidth-120));
        gate.goY=(short)to.yPhysicInTop(gate.goX,0);
        from.wayPoints.add(gate);
    }
}
