package nro.models.vip;
import java.sql.*;import java.util.*;import org.json.simple.JSONArray;
import nro.models.data.LocalManager;import nro.models.event.trungthu2026.*;import nro.models.item.Item;import nro.models.player.*;import nro.models.services.*;import nro.models.server.Maintenance;import nro.models.utils.Logger;
public final class ToriVipService {
 private static final BagExchange EXCHANGE=new BagExchange(0,System::currentTimeMillis);
 public record Result(boolean success,String message){}
 public static void syncSeason(Player p){var cfg=ToriVipConfig.get();if(cfg==null)return;if(p.toriVipSeason.isEmpty())p.toriVipSeason=cfg.season();else if(!p.toriVipSeason.equals(cfg.season())){p.toriVipSeason=cfg.season();p.vipPurchaseCount=0;p.vip=0;p.timevip=0;p.toriVipExpBonus=300;}}
 public static int expBonus(Player p){var cfg=ToriVipConfig.get();return cfg!=null&&(p.toriVipSeason.isEmpty()||p.toriVipSeason.equals(cfg.season()))&&cfg.inSeason(System.currentTimeMillis())&&p.timevip>System.currentTimeMillis()?p.toriVipExpBonus:0;}
 public static String vipData(Player p,int level,long until,int count,String season,int exp){JSONArray a=new JSONArray();Collections.addAll(a,p.timesPerDayCuuSat,p.lastTimeCuuSat,p.nhanDeTuNangVIP,p.nhanVangNangVIP,p.nhanSKHVIP,level,until,count,season,exp);return a.toJSONString();}
 static String array(Object... values){JSONArray a=new JSONArray();Collections.addAll(a,values);return a.toJSONString();}
 private static String petData(Player p,Pet pet){
  var pt=pet.nPoint;String info=array(pet.typePet,pet.gender,pet.name,0,0,pet.status),point=array(pt.limitPower,pt.power,pt.tiemNang,pt.stamina,pt.maxStamina,pt.hpg,pt.mpg,pt.dameg,pt.defg,pt.critg,pt.hp,pt.mp);JSONArray body=new JSONArray(),skills=new JSONArray();for(Item item:pet.inventory.itemsBody)body.add(array(-1,0,"[]",item.createTime));for(var skill:pet.playerSkill.skills)skills.add(skill.skillId==-1?array(-1,0,0,0):array(skill.template.id,skill.point,skill.lastTimeUseThisSkill,skill.currLevel));return array(info,point,body.toJSONString(),skills.toJSONString());
 }
 public static Result buy(Player p,int level,long revision){
  synchronized(p){
   var cfg=ToriVipConfig.get();long now=System.currentTimeMillis();if(cfg==null||revision!=cfg.revision())return new Result(false,"Cấu hình VIP đã đổi. Hãy mở lại Tori Bot.");
   var tier=cfg.tier(level);if(tier==null||!tier.enabled()||!cfg.enabled()||!cfg.inSeason(now))return new Result(false,"Gói VIP đang tắt hoặc ngoài thời gian mở mùa.");
   if(Maintenance.isRunning)return new Result(false,"Máy chủ đang bảo trì. Chưa trừ VNĐ.");syncSeason(p);if(p.vipPurchaseCount>=cfg.limit())return new Result(false,"Bạn đã dùng hết lượt mua VIP mùa này.");
   int nextCount=p.vipPurchaseCount+1,nextLevel=p.timevip>now?Math.max(p.vip,level):level,nextExp=p.timevip>now?Math.max(p.toriVipExpBonus,tier.expBonus()):tier.expBonus();long until=Math.min(cfg.end(),Math.addExact(Math.max(now,p.timevip),tier.vipDays()*86400000L));
   Pet rewardPet=null;int after=0;boolean done=false;
   try{
    // Prepare an unregistered pet; publish it only after the transaction commits.
    if(tier.normalPet()&&p.pet==null)rewardPet=PetService.gI().prepareVipNormalPet(p);
    final Pet created=rewardPet;
    try(Connection c=LocalManager.getConnection()){
     c.setAutoCommit(false);boolean committed=false;
     try{
      try(var q=c.prepareStatement("SELECT revision FROM tori_vip_config WHERE id=1 FOR UPDATE");var r=q.executeQuery()){if(!r.next()||r.getLong(1)!=revision)return new Result(false,"Cấu hình vừa thay đổi. Mở lại Tori Bot để xác nhận.");}
      int balance;try(var q=c.prepareStatement("SELECT vnd FROM account WHERE id=? FOR UPDATE")){q.setInt(1,p.getSession().userId);try(var r=q.executeQuery()){if(!r.next())return new Result(false,"Không tìm thấy tài khoản.");balance=r.getInt(1);}}
      try(var q=c.prepareStatement("SELECT data_vip FROM player WHERE id=? AND account_id=? FOR UPDATE")){q.setLong(1,p.id);q.setInt(2,p.getSession().userId);try(var r=q.executeQuery()){if(!r.next())return new Result(false,"Không tìm thấy nhân vật thuộc tài khoản.");String raw=r.getString(1);var stored=raw==null||raw.isBlank()?new com.google.gson.JsonArray():new com.google.gson.JsonParser().parse(raw).getAsJsonArray();String season=stored.size()>8?stored.get(8).getAsString():cfg.season();if(season.isEmpty())season=cfg.season();int count=stored.size()>7&&season.equals(cfg.season())?stored.get(7).getAsInt():0;if(count!=p.vipPurchaseCount)return new Result(false,"Dữ liệu VIP đã thay đổi. Hãy đăng nhập lại.");}}
      p.getSession().vnd=balance;if(balance<tier.cost())return new Result(false,"Không đủ số dư VNĐ.");after=balance-tier.cost();final int remaining=after;
      List<Item> rewards=new ArrayList<>();for(var entry:tier.items()){Item item=ItemService.gI().createNewItem((short)entry.id(),entry.quantity());for(var opt:entry.options())item.itemOptions.add(new Item.ItemOption(opt.id(),opt.param()));rewards.add(item);}
      BagExchange.Commit save=player->{
       try(var q=c.prepareStatement("UPDATE account SET vnd=? WHERE id=?")){q.setInt(1,remaining);q.setInt(2,p.getSession().userId);if(q.executeUpdate()!=1)throw new SQLException("Account missing");}
       String sql="UPDATE player SET items_bag=?,data_inventory=?,data_vip=?"+(created!=null?",pet=?":"")+" WHERE id=? AND account_id=?";
       try(var q=c.prepareStatement(sql)){q.setString(1,TrungThuStore.bag(p));q.setString(2,TrungThuStore.money(p));q.setString(3,vipData(p,nextLevel,until,nextCount,cfg.season(),nextExp));int n=4;if(created!=null)q.setString(n++,petData(p,created));q.setLong(n++,p.id);q.setInt(n,p.getSession().userId);if(q.executeUpdate()!=1)throw new SQLException("Player missing");}
       try(var q=c.prepareStatement("INSERT INTO tori_vip_purchase(player_id,account_id,season_key,vip_level,cost,until_ms,revision,rewards) VALUES(?,?,?,?,?,?,?,?)")){q.setLong(1,p.id);q.setInt(2,p.getSession().userId);q.setString(3,cfg.season());q.setInt(4,level);q.setInt(5,tier.cost());q.setLong(6,until);q.setLong(7,revision);q.setString(8,new com.google.gson.Gson().toJson(tier));q.executeUpdate();}c.commit();
      };
      var result=BagExchange.Result.SUCCESS;if(rewards.isEmpty())save.save(p);else result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(),0,0),new BagExchange.Gain(rewards,0,0,0),save);
      if(result!=BagExchange.Result.SUCCESS)return new Result(false,result==BagExchange.Result.BAG_FULL?"Hành trang không đủ chỗ. Chưa trừ VNĐ.":"Không lưu được gói VIP. VNĐ và vật phẩm được giữ nguyên.");
      committed=true;
     }finally{if(!committed)c.rollback();}
    }
    p.getSession().vnd=after;p.vip=(byte)nextLevel;p.timevip=until;p.vipPurchaseCount=nextCount;p.toriVipExpBonus=nextExp;p.toriVipSeason=cfg.season();if(rewardPet!=null)p.pet=rewardPet;done=true;
    return new Result(true,"Đã mua VIP "+level+". Lượt mùa: "+nextCount+"/"+cfg.limit()+".");
   }catch(Exception e){Logger.logException(ToriVipService.class,e);return new Result(false,"Không thể lưu giao dịch VIP. Chưa trừ VNĐ hoặc trao thưởng.");}
   finally{if(!done&&rewardPet!=null)rewardPet.dispose();}
  }
 }
 private ToriVipService(){}
}