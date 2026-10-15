package nro.models.npc_list;
import java.time.*;import java.time.format.*;import java.util.*;
import nro.models.npc.Npc;import nro.models.player.Player;import nro.models.server.Manager;import nro.models.services.*;import nro.models.vip.*;
public class ToriBot extends Npc {
 private record Quote(int level,long revision){}
 private static final Map<Player,Quote> QUOTES=Collections.synchronizedMap(new WeakHashMap<>());
 private static String date(long value){return Instant.ofEpochMilli(value).atZone(ToriVipConfig.ZONE).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));}
 public ToriBot(int mapId,int status,int cx,int cy,int tempId,int avatar){super(mapId,status,cx,cy,tempId,avatar);}
 @Override public void openBaseMenu(Player p){
  QUOTES.remove(p);var c=ToriVipConfig.get();long now=System.currentTimeMillis();
  if(c==null||!c.enabled()||!c.inSeason(now)){createOtherMenu(p,0,"Mùa VIP chưa mở hoặc đã kết thúc."+(c==null?"":"\nTừ "+date(c.start())+" đến "+date(c.end())),"Đóng");return;}
  ToriVipService.syncSeason(p);
  createOtherMenu(p,0,"Mùa VIP: "+c.season()+"\nTừ "+date(c.start())+" đến "+date(c.end())+"\nĐã mua: "+p.vipPurchaseCount+"/"+c.limit()+" lượt.\nGiá trừ từ số dư VNĐ. Thời hạn VIP tối đa đến cuối mùa.","VIP 1","VIP 2","VIP 3","VIP 4","Đóng");
 }
 @Override public void confirmMenu(Player p,int select){
  if(!canOpenNpc(p))return;var c=ToriVipConfig.get();if(c==null)return;
  if(p.idMark.getIndexMenu()==0){if(select<0||select>3)return;var t=c.tier(select+1);if(!t.enabled()){Service.gI().sendThongBao(p,"Gói này đang tạm tắt.");return;}
   StringBuilder text=new StringBuilder("VIP "+t.level()+"\nGiá: "+String.format("%,d",t.cost())+" VNĐ\nThời hạn: "+t.vipDays()+" ngày, không quá cuối mùa\nThưởng thêm "+t.expBonus()+"% SM/TN trong hạn VIP\n");
   if(t.normalPet())text.append("Tặng đệ tử thường nếu chưa có\n");
   for(var row:t.items()){text.append(Manager.ITEM_TEMPLATES.get(row.id()).name).append(" x").append(row.quantity());for(var o:row.options())if(o.id()==93)text.append(" (HSD ").append(o.param()).append(" ngày)");text.append('\n');}
   QUOTES.put(p,new Quote(t.level(),c.revision()));createOtherMenu(p,10+t.level(),text.toString(),"Mua\n"+String.format("%,d",t.cost())+" VNĐ","Đóng");return;
  }
  Quote q=QUOTES.remove(p);if(select!=0||q==null||p.idMark.getIndexMenu()!=10+q.level())return;
  var result=ToriVipService.buy(p,q.level(),q.revision());if(result.success()){InventoryService.gI().sendItemBags(p);Service.gI().sendHavePet(p);}Service.gI().sendThongBao(p,result.message());
 }
}