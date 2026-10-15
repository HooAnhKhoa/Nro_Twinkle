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
  final List<byte[]> visibility=new ArrayList<>();byte[] mapPacket;Runnable mapCallback;
  TestPlayer(){isPlayer=true;}
  @Override public void sendMessage(Message message){
   if(message.command==-73)visibility.add(message.getData().clone());
   if(message.command==-24){mapPacket=message.getData().clone();if(mapCallback!=null){Runnable callback=mapCallback;mapCallback=null;callback.run();}}
  }
  int status(int id){for(int i=visibility.size()-1;i>=0;i--){byte[] packet=visibility.get(i);if((packet[0]&255)==id)return packet[1];}return -1;}
 }
 static class TestNpc extends Npc {
  int opened,selected;
  TestNpc(int map,int id){super(map,1,10,0,id,0);}
  @Override public void openBaseMenu(Player player){opened++;}
  @Override public void confirmMenu(Player player,int select){selected++;}
 }
 static Map<Integer,Integer> initialStatuses(byte[] packet)throws Exception{
  DataInputStream input=new DataInputStream(new ByteArrayInputStream(packet));
  for(int i=0;i<5;i++)input.readByte();input.readUTF();input.readByte();input.readShort();input.readShort();
  check(input.readUnsignedByte()==0,"map fixture has no waypoints");check(input.readUnsignedByte()==0,"map fixture has no mobs");input.readByte();
  int count=input.readUnsignedByte();Map<Integer,Integer> statuses=new HashMap<>();
  for(int i=0;i<count;i++){int status=input.readUnsignedByte();input.readShort();input.readShort();int id=input.readUnsignedByte();input.readShort();statuses.put(id,status);}return statuses;
 }
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
   Map<String,Integer> before=new HashMap<>();try(ResultSet rows=s.executeQuery("SELECT event_key,enabled FROM panel_event_config")){while(rows.next())before.put(rows.getString(1),rows.getInt(2));}
   try{
    s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key IN ('hung_vuong','summer','tet','trung_thu')");EventManager.reload(c);
    nro.models.map.Map map=new nro.models.map.Map(1,"QA NPC EV",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[][]{{0}},new int[]{0},0,10,new ArrayList<>());
    Manager.MAPS.add(map);Zone zone=new Zone(map,0,10);TestPlayer player=new TestPlayer();player.zone=zone;
    int[] eventIds={52,66,51,84,41,72};for(int id:eventIds)map.npcs.add(new TestNpc(1,id));TestNpc santa=new TestNpc(1,39);map.npcs.add(santa);
    int definitions=Manager.NPCS.size();zone.mapInfo(player);Map<Integer,Integer> statuses=initialStatuses(player.mapPacket);
    for(int id:eventIds)check(statuses.get(id)==0&&player.status(id)==0,"OFF hides NPC in initial map and live packets: "+id);
    check(statuses.get(39)==1&&player.status(39)==-1,"ordinary Santa shop remains visible");
    check(NpcManager.getNpcsByMapPlayer(player).size()==7,"hidden definitions retained for in-place show");
    TestNpc hung=(TestNpc)map.npcs.get(0);MenuController.gI().openMenuNPC(null,52,player);MenuController.gI().doSelectMenu(player,52,0);
    check(hung.opened==0&&hung.selected==0,"hidden NPC rejects new and previously-open menus");
    int packets=player.visibility.size();EventManager.syncNpcVisibility(player);EventManager.reload(c);EventManager.syncNpcVisibility(player);
    check(player.visibility.size()==packets,"unchanged polls and player ticks do not spam packets");
    s.executeUpdate("UPDATE panel_event_config SET enabled=1 WHERE event_key IN ('hung_vuong','summer','tet','trung_thu')");EventManager.reload(c);EventManager.syncNpcVisibility(player);
    for(int id:eventIds)check(player.status(id)==1,"ON shows existing NPC without map change: "+id);
    check(map.npcs.size()==7&&Manager.NPCS.size()==definitions,"toggle never duplicates NPCs or alters map definitions");
    MenuController.gI().openMenuNPC(null,52,player);MenuController.gI().doSelectMenu(player,52,0);check(hung.opened==1&&hung.selected==1,"ON restores NPC menu interaction");
    // Simulate a switch changing after the initial map packet was built but before it completed sending.
    player.mapCallback=()->{try{s.executeUpdate("UPDATE panel_event_config SET enabled=0 WHERE event_key='summer'");EventManager.reload(c);}catch(Exception error){throw new RuntimeException(error);}};
    zone.mapInfo(player);check(player.status(84)==0,"post-map refresh reconciles toggle racing with map entry");
    player.zone=new Zone(map,1,10);packets=player.visibility.size();EventManager.syncNpcVisibility(player);
    check(player.visibility.size()>packets&&player.status(84)==0,"zone change resynchronizes hidden NPCs");
    player.zone=null;EventManager.syncNpcVisibility(player);player.zone=zone;packets=player.visibility.size();EventManager.syncNpcVisibility(player);
    check(player.visibility.size()>packets,"leaving and returning to same zone resynchronizes");
    map.mapId=21;player.DuaHauEgg=null;player.visibility.clear();EventManager.refreshNpcVisibility(player);
    check(player.status(51)==-1,"personal fruit NPC is not revealed to players without a tree");
   }finally{for(Map.Entry<String,Integer> row:before.entrySet())try(PreparedStatement restore=c.prepareStatement("UPDATE panel_event_config SET enabled=? WHERE event_key=?")){restore.setInt(1,row.getValue());restore.setString(2,row.getKey());restore.executeUpdate();}EventManager.reload(c);}
  }catch(Throwable error){error.printStackTrace();System.exit(1);}System.exit(0);
 }
}
