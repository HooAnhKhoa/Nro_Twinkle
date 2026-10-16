package nro.models.npc_list;
import java.time.LocalDateTime;
import java.util.*;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.task.TaskMain;

/** No database or live sessions: exercise the actual NPC menu and stale selection handler. */
public class BoMongCheckInRemovalTest {
 static int checks;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class User extends Player {User(){id=2026103101;isPlayer=true;name="NPC QA";inventory.gem=123;inventory.gold=456;lastCheckIn=LocalDateTime.of(2020,1,1,0,0);playerTask.taskMain=new TaskMain();}@Override public void sendMessage(Message message){}}
 static class Npc extends BoMong {String[] buttons;String text;Npc(int map){super(map,1,0,0,ConstNpc.BO_MONG,0);}@Override public boolean canOpenNpc(Player p){return true;}@Override public void createOtherMenu(Player p,int index,String text,String... buttons){p.idMark.setIndexMenu(index);this.text=text;this.buttons=buttons;}}
 public static void main(String[] args){
  for(int map:new int[]{47,84}){
   var p=new User();var npc=new Npc(map);npc.openBaseMenu(p);
   check(npc.buttons.length==4&&Arrays.stream(npc.buttons).noneMatch(s->s.contains("Điểm danh")),"map "+map+" has no free check-in menu");
   check(npc.buttons[0].contains("hàng ngày")&&npc.buttons[1].contains("thành tích")&&npc.buttons[2].equals("Nạp Ngọc")&&npc.buttons[3].equals("Từ chối"),"map "+map+" preserves task/achievement/paid exchange indices");
   LocalDateTime before=p.lastCheckIn;
   for(int selected:new int[]{3,3,4,-1,99}){p.idMark.setIndexMenu(ConstNpc.BASE_MENU);npc.confirmMenu(p,selected);}
   check(p.inventory.gem==123&&p.inventory.gold==456&&p.inventory.itemsBag.stream().noneMatch(Item::isNotNullItem),"map "+map+" stale and repeated reward selections cannot mint gems or bars");
   check(p.lastCheckIn.equals(before),"map "+map+" no longer changes check-in timestamp");
   p.idMark.setIndexMenu(ConstNpc.BASE_MENU);npc.confirmMenu(p,0);check(p.idMark.getIndexMenu()==ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,"map "+map+" daily task menu still opens");
  }
  for(int map:new int[]{21,22,23}){var p=new User();var npc=new Npc(map);npc.openBaseMenu(p);check(npc.buttons.length==4&&npc.buttons[0].contains("thỏi vàng")&&npc.buttons[1].contains("ngọc xanh"),"home "+map+" preserves VND exchanges");npc.confirmMenu(p,2);check(p.idMark.getTypeInput()==nro.models.services_func.Input.GIFT_CODE,"home "+map+" routes input to the existing gift-code service");npc.confirmMenu(p,3);check(p.inventory.gem==123,"home close does not grant free gems");}
  System.out.println("PASS: "+checks+" Bo Mong checks");
 }
}
