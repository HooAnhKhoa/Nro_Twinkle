package nro.models.event.october20;

import java.sql.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/** Immutable configuration snapshots. Confirmation and commit must use the same revision. */
public final class October20Config {
    public record Snapshot(long revision,Map<String,Integer> values){public int n(String key){return values.get(key);}}
    private static volatile Snapshot current;
    public static Snapshot current(){Snapshot c=current;if(c==null)throw new IllegalStateException("Chưa nạp cấu hình 20/10");return c;}
    public static Set<String> keys(){return Set.of("normal.roses","normal.paper","normal.chocolate","normal.bow","normal.gold","special.roses","special.paper","special.chocolate","special.bow","special.gold","normal.accessory","special.accessory","normal.stones","special.stones","permanent","normal.days","special.days","normal.quantity","special.quantity","normal.points","special.points","redeem.points","redeem.days","sd.min","sd.max","sdcm.min","sdcm.max","cm.min","cm.max","cute.min","cute.max");}
    public static Snapshot parse(long revision,String json)throws Exception {
        Object parsed=new JSONParser().parse(json);
        if(!(parsed instanceof JSONObject object)||!object.keySet().equals(keys())||revision<1)throw new IllegalArgumentException("Cấu hình 20/10 không hợp lệ");
        Map<String,Integer> values=new HashMap<>();
        for(String key:keys()){
            Object raw=object.get(key);if(!(raw instanceof Long number))throw new IllegalArgumentException(key);
            long value=number;int max=key.endsWith(".gold")?100000000:key.endsWith(".roses")||key.endsWith(".paper")||key.endsWith(".chocolate")||key.endsWith(".bow")||key.endsWith(".quantity")?9999:key.endsWith(".points")?1000000:key.endsWith(".days")?365:100;
            int min=key.endsWith(".accessory")||key.endsWith(".stones")||key.equals("permanent")||key.endsWith(".gold")||key.endsWith(".min")||key.endsWith(".max")?0:1;
            if(value<min||value>max)throw new IllegalArgumentException(key);values.put(key,(int)value);
        }
        for(String kind:List.of("normal","special"))if(values.get(kind+".accessory")+values.get(kind+".stones")>100)throw new IllegalArgumentException("Tổng tỷ lệ vượt 100%");
        for(String stat:List.of("sd","sdcm","cm","cute"))if(values.get(stat+".min")>values.get(stat+".max"))throw new IllegalArgumentException("Khoảng chỉ số không hợp lệ");
        return new Snapshot(revision,Map.copyOf(values));
    }
    public static void reload(Connection c)throws SQLException {
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT revision,settings FROM october20_config WHERE id=1")){
            if(!r.next())throw new SQLException("Thiếu cấu hình 20/10");
            if(current==null||current.revision()!=r.getLong(1))try{current=parse(r.getLong(1),r.getString(2));}catch(Exception e){throw new SQLException("Cấu hình 20/10 không hợp lệ",e);}
        }
    }
    private October20Config(){}
}
