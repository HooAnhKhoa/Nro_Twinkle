package nro.models.event.trungthu2026;

import java.util.*;
import nro.models.item.Item;
import nro.models.services.ItemService;

public final class TrungThuRewards {
    public record Entry(int weight,String kind,int quantity,int days){}
    public static List<Entry> table(String key){
        List<Entry> entries=new ArrayList<>();int total=0;
        for(String text:TrungThuConfig.text("loot."+key).split(",")){
            String[] p=text.split(":");if(p.length!=4)throw new IllegalArgumentException(key);
            Entry row=new Entry(Integer.parseInt(p[0]),p[1],Integer.parseInt(p[2]),Integer.parseInt(p[3]));
            if(row.weight<=0||row.quantity<=0||row.days<0)throw new IllegalArgumentException(key);entries.add(row);total+=row.weight;
        }if(total!=10000)throw new IllegalArgumentException("Tổng tỷ lệ không bằng 100%: "+key);return List.copyOf(entries);
    }
    public static BagExchange.Gain roll(String key,Random random){return reward(select(table(key),random.nextInt(10000)),random);}
    public static Entry select(List<Entry> entries,int draw){if(draw<0||draw>=10000)throw new IllegalArgumentException("draw");for(Entry row:entries){draw-=row.weight;if(draw<0)return row;}throw new IllegalArgumentException("weights");}
    public static Item item(String key,int count,int days){return item(TrungThuConfig.item(key),count,days);}
    public static Item item(int id,int count,int days){
        Item item=ItemService.gI().createNewItem((short)id,count);
        if(days>0)item.itemOptions.add(new Item.ItemOption(93,days));
        // Only use existing template capabilities; no invented combat stats.
        return item;
    }
    static BagExchange.Gain reward(Entry entry,Random random){
        List<Item> items=new ArrayList<>();long gold=0;int gems=0,ruby=0;
        switch(entry.kind){
            case "gold"->gold=entry.quantity;
            case "gems"->gems=entry.quantity;
            case "ruby"->ruby=entry.quantity;
            case "stones","stars"->{int[] ids=TrungThuConfig.numbers(entry.kind.equals("stones")?"reward.stone-ids":"reward.star-ids");Item item=item(ids[random.nextInt(ids.length)],entry.quantity,0);if(entry.kind.equals("stars")){int id=item.template.id;item.itemOptions.add(new Item.ItemOption(id-346,id<=443?5:3));}items.add(item);}
            default->items.add(item(entry.kind,entry.quantity,entry.days));
        }return new BagExchange.Gain(items,gold,gems,ruby);
    }
    public static int petDays(Random random){int[] days=TrungThuConfig.numbers("reward.pet-days"),weights=TrungThuConfig.numbers("reward.pet-weights");if(days.length!=weights.length||Arrays.stream(weights).sum()!=100)throw new IllegalArgumentException("pet weights");int draw=random.nextInt(100);for(int i=0;i<days.length;i++){draw-=weights[i];if(draw<0)return days[i];}throw new IllegalStateException();}
    private TrungThuRewards(){}
}
