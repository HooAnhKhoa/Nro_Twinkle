package nro.models.vip;
import java.sql.*;import java.time.*;import java.util.*;import com.google.gson.*;import nro.models.server.Manager;
public final class ToriVipConfig {
 public static final ZoneId ZONE=ZoneId.of("Asia/Ho_Chi_Minh");
 public record Opt(int id,int param){}
 public record Loot(int id,int quantity,List<Opt> options){}
 public record Tier(int level,boolean enabled,int cost,int vipDays,int expBonus,boolean normalPet,List<Loot> items){}
 public record Settings(long revision,boolean enabled,String season,long start,long end,int limit,List<Tier> tiers){
  public boolean inSeason(long now){return now>=start&&now<=end;}
  public Tier tier(int level){return level>=1&&level<=4?tiers.get(level-1):null;}
 }
 private static volatile Settings current;
 public static Settings get(){return current;}
 static int integer(JsonObject o,String key,int min,int max){var x=o.get(key);if(x==null||!x.isJsonPrimitive()||!x.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);int v=x.getAsBigDecimal().intValueExact();if(v<min||v>max)throw new IllegalArgumentException(key);return v;}
 static boolean bool(JsonObject o,String key){var x=o.get(key);if(x==null||!x.isJsonPrimitive()||!x.getAsJsonPrimitive().isBoolean())throw new IllegalArgumentException(key);return x.getAsBoolean();}
 public static Settings parse(String json,long revision){
  var o=new JsonParser().parse(json).getAsJsonObject();String season=o.get("season_key").getAsString();if(!season.matches("[A-Za-z0-9_-]{1,32}"))throw new IllegalArgumentException("season");
  long start=LocalDateTime.parse(o.get("starts_at").getAsString()).atZone(ZONE).toInstant().toEpochMilli(),end=LocalDateTime.parse(o.get("ends_at").getAsString()).atZone(ZONE).toInstant().toEpochMilli();if(start>=end)throw new IllegalArgumentException("dates");
  var tiers=new ArrayList<Tier>();var raw=o.getAsJsonArray("tiers");if(raw.size()!=4)throw new IllegalArgumentException("tiers");
  for(int n=0;n<4;n++){var t=raw.get(n).getAsJsonObject();int level=integer(t,"level",1,4);if(level!=n+1)throw new IllegalArgumentException("order");var items=new ArrayList<Loot>();if(t.getAsJsonArray("items").size()>30)throw new IllegalArgumentException("items");
   for(var row:t.getAsJsonArray("items")){var r=row.getAsJsonObject();int id=integer(r,"id",0,32767),qty=integer(r,"quantity",1,99999);var template=Manager.ITEM_TEMPLATES.get(id);if(template.id!=id||template.type==9||template.type==10||template.type==34||id==517||id==518)throw new IllegalArgumentException("reward");var options=new ArrayList<Opt>();var seen=new HashSet<Integer>();if(r.getAsJsonArray("options").size()>15)throw new IllegalArgumentException("options");for(var value:r.getAsJsonArray("options")){var opt=value.getAsJsonObject();int oid=integer(opt,"id",0,255);if(!seen.add(oid)||Manager.ITEM_OPTION_TEMPLATES.get(oid).id!=oid)throw new IllegalArgumentException("option");options.add(new Opt(oid,integer(opt,"param",0,32767)));}items.add(new Loot(id,qty,List.copyOf(options)));}
   tiers.add(new Tier(level,bool(t,"enabled"),integer(t,"cost",0,2000000000),integer(t,"vip_days",1,3650),integer(t,"exp_bonus",0,10000),bool(t,"normal_pet"),List.copyOf(items)));
  }
  return new Settings(revision,bool(o,"enabled"),season,start,end,integer(o,"max_purchases",1,100),List.copyOf(tiers));
 }
 public static void reload(Connection c)throws Exception{
  Settings next=current;try(var s=c.createStatement();var r=s.executeQuery("SELECT config,revision FROM tori_vip_config WHERE id=1")){if(!r.next())return;long rev=r.getLong(2);if(next==null||next.revision()!=rev)next=parse(r.getString(1),rev);}
  current=next;try(var s=c.prepareStatement("UPDATE tori_vip_config SET heartbeat=NOW(),applied_revision=? WHERE id=1 AND revision=?")){s.setLong(1,next.revision());s.setLong(2,next.revision());s.executeUpdate();}
 }
 private ToriVipConfig(){}
}