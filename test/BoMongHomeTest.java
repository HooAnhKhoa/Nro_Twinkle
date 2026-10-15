package nro.models.event.trungthu2026;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import nro.models.consts.*;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.npc_list.BoMong;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services_func.Input;
import nro.models.task.SideTaskTemplate;

/** No DB, game listener or real player mutations. */
public class BoMongHomeTest {
 static int checks;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class User extends TrungThuRuntimeTest.User {
  byte[] menu;boolean achievement,form;
  User(int id){super(id);inventory.gem=100;for(int i=0;i<8;i++)inventory.itemsBag.add(new Item());}
  @Override public void sendMessage(Message m){if(m.command==32)menu=m.getData();if(m.command==-76)achievement=true;if(m.command==-125)form=true;}
 }
 static String menuText(User p)throws Exception{var r=new DataInputStream(new ByteArrayInputStream(p.menu));r.readShort();return r.readUTF();}
 static int bars(User p){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==457).mapToInt(i->i.quantity).sum();}
 public static void main(String[] args)throws Exception{
  for(int i=0;i<=500;i++)Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)i,(byte)27,(byte)3,"QA "+i,"",(short)0,(short)0,true,0));
  for(int i=0;i<=100;i++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(i,"QA option "+i,0));
  SideTaskTemplate daily=new SideTaskTemplate();daily.id=0;daily.name="Hạ %1 quái";for(int[] range:daily.count){range[0]=1;range[1]=1;}Manager.SIDE_TASKS_TEMPLATE.add(daily);
  for(int id:new int[]{21,22,23,47,84,5})TrungThuRuntimeTest.map(id,(byte)0);
  for(int map:new int[]{21,22,23,47,84}){
   User p=new User(map);p.zone=Manager.MAPS.stream().filter(m->m.mapId==map).findFirst().get().zones.get(0);
   BoMong npc=new BoMong(map,1,0,0,ConstNpc.BO_MONG,0);npc.openBaseMenu(p);
   var packet=new DataInputStream(new ByteArrayInputStream(p.menu));check(packet.readUnsignedShort()==17,"map "+map+" opens Bò Mộng menu");packet.readUTF();int count=packet.readUnsignedByte();List<String> buttons=new ArrayList<>();for(int n=0;n<count;n++)buttons.add(packet.readUTF());
   if(map==21||map==22||map==23){
    check(count==3&&buttons.get(0).contains("thỏi vàng")&&buttons.get(1).contains("ngọc xanh")&&buttons.get(2).equals("Đóng"),"home "+map+" shows only gold bars, green gems and close");
    npc.confirmMenu(p,0);check(p.form&&p.idMark.getTypeInput()==Input.TRADE_GOLD,"home "+map+" routes gold bars to the existing exchange input");
    p.form=false;npc.openBaseMenu(p);npc.confirmMenu(p,1);check(p.form&&p.idMark.getTypeInput()==Input.TRADE_GEM,"home "+map+" routes gems to exchange input");
    p.form=false;npc.openBaseMenu(p);npc.confirmMenu(p,2);check(!p.form&&!p.achievement&&p.lastCheckIn==null&&p.inventory.gem==100&&bars(p)==0,"home "+map+" close never grants old check-in rewards");
    p.idMark.setIndexMenu(ConstNpc.BASE_MENU);npc.confirmMenu(p,3);check(p.lastCheckIn==null&&p.inventory.gem==100,"home "+map+" rejects the old daily check-in selection");
    p.playerTask.sideTask.template=daily;p.idMark.setIndexMenu(ConstNpc.MENU_OPTION_PAY_SIDE_TASK);npc.confirmMenu(p,1);check(p.playerTask.sideTask.template==daily,"home "+map+" rejects old quest actions");
    continue;
   }
   check(count==5&&buttons.get(0).contains("hàng ngày")&&buttons.get(1).contains("thành tích")&&buttons.get(2).equals("Nạp Ngọc")&&buttons.get(3).equals("Điểm danh"),"map "+map+" retains all four existing services");
   npc.confirmMenu(p,0);check(p.idMark.getIndexMenu()==ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,"map "+map+" opens daily difficulty selection");
   npc.confirmMenu(p,2);check(p.playerTask.sideTask.template==daily&&p.playerTask.sideTask.level==ConstTask.HARD&&p.playerTask.sideTask.leftTask==ConstTask.MAX_SIDE_TASK-1,"map "+map+" accepts a hard daily task with the original quota");
   p.playerTask.sideTask.template=new SideTaskTemplate();p.playerTask.sideTask.template.name="Hạ %1 quái";p.playerTask.sideTask.count=2;p.playerTask.sideTask.maxCount=10;p.playerTask.sideTask.level=ConstTask.HARD;
   npc.openBaseMenu(p);npc.confirmMenu(p,0);check(p.idMark.getIndexMenu()==ConstNpc.MENU_OPTION_PAY_SIDE_TASK&&menuText(p).contains("2/10"),"map "+map+" shows active task progress and return/cancel actions");
   npc.confirmMenu(p,1);check(p.playerTask.sideTask.template==null,"map "+map+" cancels a daily task through the original service");
   npc.openBaseMenu(p);npc.confirmMenu(p,1);check(p.achievement,"map "+map+" opens achievement interface");
   npc.openBaseMenu(p);npc.confirmMenu(p,2);check(p.form&&p.idMark.getTypeInput()==Input.TRADE_GEM,"map "+map+" opens the existing VND-to-gem form");
   p.lastCheckIn=LocalDateTime.now().minusDays(1);npc.openBaseMenu(p);npc.confirmMenu(p,3);check(p.inventory.gem==10100&&bars(p)==100,"map "+map+" restores daily 10000 gems and 100 gold bars");
   npc.confirmMenu(p,3);check(p.inventory.gem==10100&&bars(p)==100,"map "+map+" does not grant a second same-day check-in");
  }
  User other=new User(100);other.zone=Manager.MAPS.stream().filter(m->m.mapId==5).findFirst().get().zones.get(0);BoMong unsupported=new BoMong(5,1,0,0,17,0);unsupported.openBaseMenu(other);unsupported.confirmMenu(other,3);check(other.menu==null&&other.inventory.gem==100,"unsupported maps do not gain services");
  BoMong home=new BoMong(21,1,0,0,17,0);home.confirmMenu(other,3);check(other.inventory.gem==100,"NPC in another map cannot grant check-in rewards");
  System.out.println("PASS: "+checks+" Bò Mộng checks; no DB");
 }
}