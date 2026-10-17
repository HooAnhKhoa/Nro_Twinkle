package nro.models.services;
import java.nio.file.*;import java.util.*;import nro.models.player.Player;
/** Canonical templates stay visible; incomplete visual assets cannot be purchased or consumed. */
public final class CatalogAssets {
 private static final Map<Integer,String> UNAVAILABLE=load();
 private static Map<Integer,String> load(){try{var json=new com.google.gson.JsonParser().parse(Files.readString(Path.of("config/teamobi-assets.json"))).getAsJsonObject().getAsJsonObject("unavailable");Map<Integer,String> rows=new HashMap<>();for(var e:json.entrySet())rows.put(Integer.parseInt(e.getKey()),e.getValue().getAsString());return Map.copyOf(rows);}catch(Exception e){throw new IllegalStateException("Cannot read canonical asset availability",e);}}
 public static boolean available(int id){return !UNAVAILABLE.containsKey(id);}
 public static boolean check(Player player,int id){String reason=UNAVAILABLE.get(id);if(reason==null)return true;Service.gI().sendThongBao(player,"Vật phẩm chưa sẵn sàng: "+reason+". Chưa trừ tiền hoặc vật phẩm.");return false;}
 private CatalogAssets(){}
}
