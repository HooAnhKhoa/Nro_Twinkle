package nro.models.boss;

import com.google.gson.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.services.Service;

/** A reward scope changes ground items only; native tasks, points and encounter logic still execute. */
public final class BossDrops {
    public record Option(int id,int param){}
    public record Entry(int id,int min,int max,double chance,List<Option> options){
        public Entry{options=List.copyOf(options);if(id<0||id>32767||min<1||max<min||max>99999||!Double.isFinite(chance)||chance<0||chance>100||options.size()>15)throw new IllegalArgumentException("Invalid boss drop");
            for(Option option:options)if(option.id()<0||option.id()>255||option.param()<0||option.param()>32767)throw new IllegalArgumentException("Invalid drop option");}
    }
    public record Policy(String mode,List<Entry> items){
        public Policy{items=List.copyOf(items);if(!Set.of("original","add","replace","off").contains(mode)||items.size()>30)throw new IllegalArgumentException("Invalid boss drop policy");}
    }
    private record Scope(Boss boss,Policy policy){}
    private static final Policy ORIGINAL=new Policy("original",List.of());
    private static volatile Map<String,Policy> policies=Map.of();
    private static final ThreadLocal<Scope> scope=new ThreadLocal<>();
    public static Policy policy(Boss b){return policies.getOrDefault(b.getClass().getName(),ORIGINAL);}
    public static boolean suppress(ItemMap item){Scope s=scope.get();return s!=null&&item.zone==s.boss().zone&&(s.policy().mode().equals("replace")||s.policy().mode().equals("off"));}
    public static void nativeScope(Boss boss,Runnable original){
        Scope previous=scope.get();scope.set(new Scope(boss,policy(boss)));
        try{original.run();}finally{if(previous==null)scope.remove();else scope.set(previous);}
    }
    public static void reward(Boss b,Player p,Runnable original){
        if(p==null||p.isBot||b.zone==null||!nro.models.server.PanelBridge.enabled(b))return;
        Player owner=p instanceof nro.models.player.Pet pet?pet.master:p;
        perform(b,owner!=null&&owner.isPl()?owner.id:Long.MIN_VALUE,original);
    }
    public static void rewardPublic(Boss b,Runnable original){
        if(b.zone!=null&&nro.models.server.PanelBridge.enabled(b))perform(b,-1,original);
    }
    private static void perform(Boss b,long owner,Runnable original){
        Policy selected=policy(b);Scope previous=scope.get();scope.set(new Scope(b,selected));
        try{original.run();}finally{if(previous==null)scope.remove();else scope.set(previous);}
        if(owner==Long.MIN_VALUE||!selected.mode().equals("add")&&!selected.mode().equals("replace"))return;
        if(b.zone==null||!nro.models.server.PanelBridge.enabled(b))return;
        // Emission bypasses any enclosing native scope, including delayed encounter callbacks.
        scope.remove();
        try{
            for(Entry row:selected.items())if(ThreadLocalRandom.current().nextDouble()*100<row.chance()){
                int count=ThreadLocalRandom.current().nextInt(row.min(),row.max()+1);
                ItemMap item=new ItemMap(b.zone,row.id(),count,b.location.x,b.zone.map.yPhysicInTop(b.location.x,b.location.y-24),owner);
                for(Option option:row.options())item.options.add(new Item.ItemOption(option.id(),option.param()));
                Service.gI().dropItemMap(b.zone,item);
            }
        }finally{if(previous!=null)scope.set(previous);}
    }
    public static void reload(Connection c)throws SQLException{
        Map<String,Policy> next=new HashMap<>();Map<String,Long> revisions=new HashMap<>();
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT boss_key,mode,items,revision FROM boss_web_drop_config")){
            while(r.next()){
                List<Entry> items=new ArrayList<>();
                for(JsonElement value:new JsonParser().parse(r.getString("items")).getAsJsonArray()){
                    JsonObject row=value.getAsJsonObject();List<Option> options=new ArrayList<>();
                    for(JsonElement raw:row.getAsJsonArray("options")){
                        JsonObject option=raw.getAsJsonObject();int oid=option.get("id").getAsInt();
                        if(oid<0||oid>=nro.models.server.Manager.ITEM_OPTION_TEMPLATES.size()||nro.models.server.Manager.ITEM_OPTION_TEMPLATES.get(oid)==null)throw new SQLException("Unknown drop option "+oid);
                        options.add(new Option(oid,option.get("param").getAsInt()));
                    }
                    int id=row.get("id").getAsInt();
                    if(nro.models.services.ItemService.gI().getTemplate((short)id)==null)throw new SQLException("Unknown boss drop item "+id);
                    items.add(new Entry(id,row.get("min").getAsInt(),row.get("max").getAsInt(),row.get("chance").getAsDouble(),options));
                }
                next.put(r.getString("boss_key"),new Policy(r.getString("mode"),items));revisions.put(r.getString("boss_key"),r.getLong("revision"));
            }
        }catch(RuntimeException error){throw new SQLException("Invalid boss drop configuration",error);}
        policies=Map.copyOf(next);
        try(PreparedStatement q=c.prepareStatement("UPDATE boss_web_drop_config SET applied_revision=?,applied_at=NOW() WHERE boss_key=? AND revision=?")){
            for(var revision:revisions.entrySet()){q.setLong(1,revision.getValue());q.setString(2,revision.getKey());q.setLong(3,revision.getValue());q.addBatch();}q.executeBatch();
        }
    }
    private BossDrops(){}
}
