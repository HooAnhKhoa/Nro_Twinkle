import java.sql.*;
import java.io.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.event.EventManager;
import nro.models.map.Zone;
import nro.models.map.service.NpcManager;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.server.MenuController;

public class EventNpcVisibilityTest {
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);System.out.println("PASS: "+text);}
 static class TestPlayer extends Player {
  int clearPackets,mapPackets,hidePackets;byte[] mapPacket;Runnable mapCallback;
  TestPlayer(){isPlayer=true;}
  @Override public void sendMessage(Message message){
   if(message.command==-73)hidePackets++;
   if(message.command==-22)clearPackets++;
   if(message.command==-24){mapPackets++;mapPacket=message.getData().clone();if(mapCallback!=null){Runnable callback=mapCallback;mapCallback=null;callback.run();}}
  }
 }
 static class TestNpc extends Npc {
  int opened,selected;
  TestNpc(int map,int id){super(map,1,10,0,id,0);}
  @Override public void openBaseMenu(Player player){opened++;}
  @Override public void confirmMenu(Player player,int select){selected++;}
 }
 static Set<Integer> renderedNpcs(byte[] packet)throws Exception{
  // Model the observed client: every transmitted NPC is drawn, even when its status is 0.
  DataInputStream input=new DataInputStream(new ByteArrayInputStream(packet));
  for(int i=0;i<5;i++)input.readByte();input.readUTF();input.readByte();input.readShort();input.readShort();
  if(input.readUnsignedByte()!=0||input.readUnsignedByte()!=0)throw new AssertionError("Nonempty QA waypoint/mob fixture");input.readByte();
  int count=input.readUnsignedByte();Set<Integer> ids=new HashSet<>();
  for(int i=0;i<count;i++){input.readByte();input.readShort();input.readShort();int id=input.readUnsignedByte();input.readShort();ids.add(id);}return ids;
 }
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
   Map<String,Integer> before=new HashMap<>();try(ResultSet rows=s.executeQuery("SELECT event_key,enabled FROM panel_event_config")){while(rows.next())before.put(rows.getString(1),rows.getInt(2));}
   try{
    s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key IN ('hung_vuong','summer','tet','trung_thu')");EventManager.reload(c);
    nro.models.map.Map map=new nro.models.map.Map(1,"QA NPC EV",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[][]{{0}},new int[]{0},0,10,new ArrayList<>());
    Manager.MAPS.add(map);Zone zone=new Zone(map,0,10);TestPlayer player=new TestPlayer();player.zone=zone;
    int[] eventIds={52,66,51,84,41,72};for(int id:eventIds)map.npcs.add(new TestNpc(1,id));map.npcs.add(new TestNpc(1,39));
    int definitions=Manager.NPCS.size();zone.mapInfo(player);Set<Integer> ids=renderedNpcs(player.mapPacket);
    for(int id:eventIds)check(!ids.contains(id),"OFF omits NPC even on client ignoring status: "+id);
    check(ids.contains(39)&&ids.size()==1,"ordinary Santa shop remains visible");
    check(NpcManager.getNpcsByMapPlayer(player).size()==1,"disabled NPCs excluded from outgoing list");
    TestNpc hung=(TestNpc)map.npcs.get(0);MenuController.gI().openMenuNPC(null,52,player);MenuController.gI().doSelectMenu(player,52,0);
    check(hung.opened==0&&hung.selected==0,"hidden NPC rejects new and stale menus");
    int packets=player.mapPackets;EventManager.syncNpcVisibility(player);EventManager.reload(c);EventManager.syncNpcVisibility(player);
    check(player.mapPackets==packets&&player.clearPackets==0,"unchanged polls do not reload map");
    s.executeUpdate("UPDATE panel_event_config SET enabled=1 WHERE event_key IN ('hung_vuong','summer','tet','trung_thu')");EventManager.reload(c);EventManager.syncNpcVisibility(player);
    ids=renderedNpcs(player.mapPacket);for(int id:eventIds)check(ids.contains(id),"ON sends NPC in refreshed current map: "+id);
    check(player.clearPackets==1&&player.mapPackets==packets+1,"live toggle clears stale client list then reloads current map once");
    check(player.zone==zone&&player.location.x==0&&player.location.y==0,"refresh preserves zone and coordinates");
    player.thongBaoChangeMap=true;player.textThongBaoChangeMap="QA normal transition notification";
    nro.models.map.service.ChangeMapService.gI().finishLoadMap(player);
    check(player.thongBaoChangeMap,"NPC-only refresh does not replay normal map-transition actions");
    check(!EventManager.consumeNpcRefresh(player),"actual map-load completion consumes refresh marker once");
    check(map.npcs.size()==7&&Manager.NPCS.size()==definitions,"toggle never duplicates or removes server NPC definitions");
    MenuController.gI().openMenuNPC(null,52,player);MenuController.gI().doSelectMenu(player,52,0);check(hung.opened==1&&hung.selected==1,"ON restores menus");
    player.mapCallback=()->{try{s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key='summer'");EventManager.reload(c);}catch(Exception error){throw new RuntimeException(error);}};
    zone.mapInfo(player);EventManager.syncNpcVisibility(player);
    check(!renderedNpcs(player.mapPacket).contains(84),"toggle racing map send is reconciled by actual transmitted IDs");
    check(player.hidePackets==0,"visibility no longer depends on unsupported -73/status behavior");
    packets=player.mapPackets;EventManager.syncNpcVisibility(player);check(player.mapPackets==packets,"completed refresh does not loop");
    player.zone=new Zone(map,1,10);zone=player.zone;zone.mapInfo(player);EventManager.syncNpcVisibility(player);
    check(!EventManager.consumeNpcRefresh(player),"normal zone change retains normal completion and tasks");
    check(!renderedNpcs(player.mapPacket).contains(84),"new zone also excludes disabled sugarcane NPC");
    map.mapId=21;player.DuaHauEgg=null;zone.mapInfo(player);
    check(!renderedNpcs(player.mapPacket).contains(51),"personal fruit stays absent without owned tree");
   }finally{for(Map.Entry<String,Integer> row:before.entrySet())try(PreparedStatement restore=c.prepareStatement("UPDATE panel_event_config SET enabled=? WHERE event_key=?")){restore.setInt(1,row.getValue());restore.setString(2,row.getKey());restore.executeUpdate();}EventManager.reload(c);}
  }catch(Throwable error){error.printStackTrace();System.exit(1);}System.exit(0);
 }
}
