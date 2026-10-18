package nro.models.shop;
import java.sql.*;import java.util.*;
import nro.models.player.Player;import nro.models.item.Item;import nro.models.data.LocalManager;
import nro.models.event.EventManager;import nro.models.event.trungthu2026.*;
import nro.models.services.*;import nro.models.server.Manager;
/** Native point shop selected by the active event. Snapshots are validated again inside the exchange transaction. */
public final class EventPointShop {
 public record Binding(String event,int shop,long revision){}
 public static final String PREFIX="EVENT_POINTS:";
 public static final List<String> KEYS=List.of("october20","trung_thu","hung_vuong","christmas","halloween","tet","summer");
 private static volatile Map<String,Binding> bindings=Map.of();
 private static final BagExchange EXCHANGE=new BagExchange(500,System::currentTimeMillis);
 public static void reload(Connection c)throws SQLException{Map<String,Binding> next=new LinkedHashMap<>();try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM panel_event_shop ORDER BY event_key")){while(r.next()){String key=r.getString("event_key");if(KEYS.contains(key))next.put(key,new Binding(key,r.getInt("shop_id"),r.getLong("revision")));}}bindings=Map.copyOf(next);}
 public static String name(String key){return switch(key){case "october20"->"20/10";case "trung_thu"->"Trung Thu";case "hung_vuong"->"Giỗ Tổ";case "christmas"->"Noel";case "halloween"->"Halloween";case "tet"->"Tết";default->"Hè";};}
 public static List<String> active(){return KEYS.stream().filter(k->bindings.containsKey(k)&&EventManager.enabled(k)).toList();}
 public static boolean open(Player p,String key){
  Binding b=bindings.get(key);if(b==null||!EventManager.enabled(key)){Service.gI().sendThongBao(p,"Sự kiện chưa mở hoặc đã hết giờ.");return false;}
  Shop source=Manager.SHOPS.stream().filter(s->s.id==b.shop&&s.npcId==13&&s.typeShop==3).findFirst().orElse(null);
  if(source==null){Service.gI().sendThongBao(p,"Chưa cấu hình shop đổi điểm cho sự kiện.");return false;}
  Shop shop=new Shop(source);shop.tagName=PREFIX+key+":"+b.revision;
  for(TabShop tab:shop.tabShops){tab.itemShops.removeIf(i->i.cost<=0||!CatalogAssets.available(i.temp.id));for(ItemShop i:tab.itemShops)i.iconSpec=14116;}
  ShopService.gI().openEventPointShop(p,shop);return true;
 }
 private static String signature(List<Item.ItemOption> options){return options.stream().map(o->o.optionTemplate.id+":"+o.param).sorted().collect(java.util.stream.Collectors.joining(","));}
 public static void buy(Player p,Shop shop,int item){synchronized(p){
  if(nro.models.server.Maintenance.isRunning||!p.isPl())return;
  ItemShop sale=shop.getItemShop(item);if(sale==null||sale.cost<=0||!CatalogAssets.check(p,item))return;
  String[] tag=shop.tagName.split(":");Binding binding=tag.length==3?bindings.get(tag[1]):null;
  if(binding==null||!shop.tagName.equals(PREFIX+binding.event+":"+binding.revision)||shop.id!=binding.shop||!EventManager.enabled(binding.event)){Service.gI().sendThongBao(p,"Sự kiện hoặc shop đã thay đổi. Hãy mở lại.");return;}
  int balance=p.event.getEventPoint();if(balance<sale.cost){Service.gI().sendThongBao(p,"Không đủ điểm sự kiện. Cần "+sale.cost+", đang có "+balance+" điểm.");return;}
  try(Connection c=LocalManager.getConnection()){
   c.setAutoCommit(false);boolean committed=false;try{
    var result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(),0,0),new BagExchange.Gain(List.of(ItemService.gI().createItemFromItemShop(sale)),0,0,0),who->{
     try(Statement gate=c.createStatement();ResultSet ignored=gate.executeQuery("SELECT revision FROM panel_event_runtime WHERE id=1 FOR UPDATE")){if(!ignored.next())throw new SQLException("Missing event runtime");}
     try(PreparedStatement q=c.prepareStatement("SELECT shop_id,revision FROM panel_event_shop WHERE event_key=? FOR UPDATE")){q.setString(1,binding.event);try(ResultSet r=q.executeQuery()){if(!r.next()||r.getInt(1)!=binding.shop||r.getLong(2)!=binding.revision)throw new SQLException("Shop binding changed");}}
     try(PreparedStatement q=c.prepareStatement("SELECT i.temp_id,i.cost FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id WHERE i.id=? AND t.shop_id=? AND i.is_sell=1 FOR UPDATE")){q.setInt(1,sale.id);q.setInt(2,shop.id);try(ResultSet r=q.executeQuery()){if(!r.next()||r.getInt(1)!=item||r.getInt(2)!=sale.cost)throw new SQLException("Shop price changed");}}
     List<Item.ItemOption> options=new ArrayList<>();try(PreparedStatement q=c.prepareStatement("SELECT option_id,param FROM item_shop_option WHERE item_shop_id=? ORDER BY option_id,param FOR UPDATE")){q.setInt(1,sale.id);try(ResultSet r=q.executeQuery()){while(r.next())options.add(new Item.ItemOption(r.getInt(1),r.getInt(2)));}}
     if(!signature(options).equals(signature(sale.options)))throw new SQLException("Shop stats changed");
     long[] season=ensureActive(c,binding.event);boolean counted=season[0]>0&&season[1]>System.currentTimeMillis()&&season[2]==0;
     if(counted)try(PreparedStatement q=c.prepareStatement("INSERT INTO october20_score(season_id,board,player_id,score,reached_ms) VALUES(?,'roses',?,1,?) ON DUPLICATE KEY UPDATE score=score+1,reached_ms=VALUES(reached_ms)")){q.setLong(1,season[0]);q.setLong(2,p.id);q.setLong(3,System.currentTimeMillis());q.executeUpdate();}
     try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=?,event_point=? WHERE id=? AND account_id=?")){q.setString(1,TrungThuStore.bag(p));q.setString(2,TrungThuStore.money(p));q.setInt(3,balance-sale.cost);q.setLong(4,p.id);q.setInt(5,p.getSession().userId);if(q.executeUpdate()!=1)throw new SQLException("Player ownership changed");}
     try(PreparedStatement q=c.prepareStatement("INSERT INTO panel_event_shop_audit(event_key,player_id,item_id,points_cost,season_id,counted) VALUES(?,?,?,?,?,?)")){q.setString(1,binding.event);q.setLong(2,p.id);q.setInt(3,item);q.setInt(4,sale.cost);if(season[0]>0)q.setLong(5,season[0]);else q.setNull(5,Types.BIGINT);q.setBoolean(6,counted);q.executeUpdate();}
     c.commit();
    });
    if(result==BagExchange.Result.SUCCESS){committed=true;p.event.setEventPoint(balance-sale.cost);InventoryService.gI().sendItemBags(p);Service.gI().sendThongBao(p,"Đã đổi "+sale.temp.name+" bằng "+sale.cost+" điểm. Còn "+p.event.getEventPoint()+" điểm sự kiện.");}
    else Service.gI().sendThongBao(p,result==BagExchange.Result.BAG_FULL?"Hành trang không đủ chỗ. Chưa trừ điểm.":"Chưa đổi được quà: kiểm tra lịch, điểm hoặc mở lại shop. Điểm và túi được giữ nguyên.");
   }finally{if(!committed)c.rollback();}
  }catch(Exception e){nro.models.utils.Logger.logException(EventPointShop.class,e);Service.gI().sendThongBao(p,"Chưa đổi được quà. Điểm và túi được giữ nguyên.");}
 }}
 private static long[] ensureActive(Connection c,String key)throws SQLException{
  long now=System.currentTimeMillis();
  if(key.equals("october20")||key.equals("trung_thu")){
   String table=key.equals("october20")?"october20_season":"trung_thu_season";
   try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT * FROM "+table+" ORDER BY id DESC LIMIT 1 FOR UPDATE")){if(r.next()&&r.getBoolean("scheduled")){if(now<r.getLong("start_ms")||now>=r.getLong("end_ms")||(key.equals("october20")&&r.getBoolean("paused"))||(!key.equals("october20")&&r.getTimestamp("closed_at")!=null))throw new SQLException("Event inactive");return key.equals("october20")?new long[]{r.getLong("id"),r.getLong("points_end_ms"),r.getTimestamp("closed_at")==null?0:1}:new long[3];}}
  }
  try(PreparedStatement q=c.prepareStatement("SELECT enabled FROM panel_event_config WHERE event_key=? FOR UPDATE")){q.setString(1,key);try(ResultSet r=q.executeQuery()){if(!r.next()||!r.getBoolean(1))throw new SQLException("Event disabled");}}return new long[3];
 }
 public static Set<String> pending(Player p){Set<String> result=new HashSet<>();try(Connection c=LocalManager.getConnection()){
  for(String key:List.of("october20","trung_thu")){String table=key.equals("october20")?"october20_prize":"trung_thu_prize";try(PreparedStatement q=c.prepareStatement("SELECT 1 FROM "+table+" WHERE player_id=? AND claimed_at IS NULL LIMIT 1")){q.setLong(1,p.id);try(ResultSet r=q.executeQuery()){if(r.next())result.add(key);}}}
 }catch(SQLException e){nro.models.utils.Logger.logException(EventPointShop.class,e);}return result;}
 private EventPointShop(){}
}