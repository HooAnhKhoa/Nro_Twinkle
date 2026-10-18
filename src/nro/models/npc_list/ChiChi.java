package nro.models.npc_list;
import java.util.*;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.shop.*;
import nro.models.event.EventManager;
import nro.models.event.october20.*;
import nro.models.event.trungthu2026.*;
import nro.models.services.Service;
/** Compact event boards and prize collection live at Chi Chi. */
public class ChiChi extends Npc {
 private static final int TOP_MENU=202610407,STORE_MENU=202610408;
 private record Action(String event,String board){}
 private final Map<Player,List<Action>> selections=Collections.synchronizedMap(new WeakHashMap<>());
 private final Map<Player,Action> selectedTop=Collections.synchronizedMap(new WeakHashMap<>());
 public ChiChi(int mapId,int status,int cx,int cy,int tempId,int avatar){super(mapId,status,cx,cy,tempId,avatar);}
 private String label(Action a){return "Top\n"+(a.event.equals("october20")?switch(a.board){case "roses"->"Đổi điểm";case "cards"->"Thiệp VIP";default->"Hộp quà";}:TrungThuCompetition.boardName(a.board))+"\n"+EventPointShop.name(a.event);}
 private boolean available(Player p,Action a){return EventManager.enabled(a.event)||EventPointShop.pendingBoards(p).getOrDefault(a.event,Set.of()).contains(a.board);}
 @Override public void openBaseMenu(Player p){
  if(!canOpenNpc(p))return;List<String> menu=new ArrayList<>();List<Action> actions=new ArrayList<>();var pending=EventPointShop.pendingBoards(p);
  for(String key:List.of("october20","trung_thu"))for(String board:key.equals("october20")?List.of("roses","cards","gifts"):List.of("vip","normal","vegetarian")){
   Action a=new Action(key,board);if(EventManager.enabled(key)||pending.getOrDefault(key,Set.of()).contains(board)){menu.add(label(a));actions.add(a);}
  }
  menu.add("Cửa hàng");actions.add(new Action("store","choose"));menu.add("Đóng");actions.add(new Action("close",""));
  selections.put(p,List.copyOf(actions));createOtherMenu(p,ConstNpc.BASE_MENU,"Bạn muốn hỏi chi?",menu.toArray(String[]::new));
 }
 private void showTop(Player p,Action a){
  if(!available(p,a)){Service.gI().sendThongBao(p,"Bảng top chưa mở hoặc đã hết giờ, không có quà chưa nhận.");return;}
  selectedTop.put(p,a);String text=a.event.equals("october20")?October20Competition.standings(p,a.board):TrungThuCompetition.standings(p,a.board);
  createOtherMenu(p,TOP_MENU,text+"\nNhận quà của bảng này tại Chi Chi. Nếu còn quà mốc, mở lại để nhận tiếp.","Nhận quà","Đóng");
 }
 private void openStore(Player p){
  List<String> labels=new ArrayList<>();List<Action> actions=new ArrayList<>();labels.add("Cửa hàng thường");actions.add(new Action("store","ordinary"));
  if(EventManager.enabled("october20")){labels.add("Quà 20/10");actions.add(new Action("store","october20"));}
  if(EventManager.enabled("trung_thu")){labels.add("Shop Trung Thu");actions.add(new Action("store","trung_thu"));if(TrungThuCompetition.vipCost()!=null){labels.add("Hộp VIP Trung Thu");actions.add(new Action("store","trung_thu_vip"));}}
  if(actions.size()==1){ShopService.gI().opendShop(p,"SHOP_CHI_CHI",false);return;}
  labels.add("Đóng");actions.add(new Action("close",""));selections.put(p,List.copyOf(actions));createOtherMenu(p,STORE_MENU,"Chọn cửa hàng",labels.toArray(String[]::new));
 }
 @Override public void confirmMenu(Player p,int select){
  if(!canOpenNpc(p)||mapId!=5)return;int menu=p.idMark.getIndexMenu();
  if(menu==TOP_MENU){Action a=selectedTop.remove(p);p.idMark.setIndexMenu(-1);if(select==0&&a!=null&&available(p,a)){if(a.event.equals("october20"))October20Competition.claim(p,a.board);else TrungThuCompetition.claim(p,a.board);}return;}
  if(TrungThuCompetition.confirm(p,this,select))return;if(October20.menu(this,p,select))return;
  if(menu!=ConstNpc.BASE_MENU&&menu!=STORE_MENU)return;var actions=selections.remove(p);p.idMark.setIndexMenu(-1);if(actions==null||select<0||select>=actions.size())return;Action a=actions.get(select);
  if(a.event.equals("close"))return;if(!a.event.equals("store")){showTop(p,a);return;}
  switch(a.board){case "choose"->openStore(p);case "ordinary"->ShopService.gI().opendShop(p,"SHOP_CHI_CHI",false);case "october20"->October20Shop.open(p);case "trung_thu"->TrungThuShop.open(p);case "trung_thu_vip"->TrungThuCompetition.openVip(p,this);}
 }
}