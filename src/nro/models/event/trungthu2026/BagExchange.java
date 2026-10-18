package nro.models.event.trungthu2026;

import java.util.*;
import java.util.function.LongSupplier;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;

/** In-memory bag/currency exchange. Callers own event gating, persistence and network notifications. */
public final class BagExchange {
    public enum Result {SUCCESS,COOLDOWN,MISSING_ITEMS,MISSING_GOLD,MISSING_GEMS,BAG_FULL,INVALID_REWARD,CURRENCY_LIMIT,SAVE_FAILED}
    public record Gain(List<Item> items,long gold,int gems,int ruby){
        public Gain{items=List.copyOf(items);if(gold<0||gems<0||ruby<0)throw new IllegalArgumentException("Thưởng không hợp lệ");}
    }
    @FunctionalInterface public interface Commit {void save(Player player)throws Exception;}
    public record Cost(Map<Integer,Integer> items,long gold,int gems,int ruby){
        public Cost(Map<Integer,Integer> items,long gold,int gems){this(items,gold,gems,0);}
        public Cost {
            items=Map.copyOf(items);
            if(gold<0||gems<0||ruby<0||items.entrySet().stream().anyMatch(e->e.getKey()<0||e.getValue()<1))throw new IllegalArgumentException("Chi phí không hợp lệ");
        }
    }
    private final long cooldownMillis;
    private final LongSupplier clock;
    private final Map<Player,Long> lastSuccess=Collections.synchronizedMap(new WeakHashMap<>());
    public BagExchange(long cooldownMillis,LongSupplier clock){
        if(cooldownMillis<0)throw new IllegalArgumentException("Thời gian chống spam không hợp lệ");
        this.cooldownMillis=cooldownMillis;this.clock=Objects.requireNonNull(clock);
    }
    public Result execute(Player player,Cost cost,List<Item> rewards){
        return execute(player,cost,new Gain(rewards,0,0,0),p->{});
    }
    public Result execute(Player player,Cost cost,Gain gain,Commit commit){
        List<Item> rewards=gain.items;
        Objects.requireNonNull(player);Objects.requireNonNull(cost);Objects.requireNonNull(rewards);
        synchronized(player){
            Long previous=lastSuccess.get(player);
            if(previous!=null&&clock.getAsLong()-previous<cooldownMillis)return Result.COOLDOWN;
            if(rewards.isEmpty()&&gain.gold==0&&gain.gems==0&&gain.ruby==0&&cost.items.isEmpty()&&cost.gold==0&&cost.gems==0&&cost.ruby==0)return Result.INVALID_REWARD;
            for(Item reward:rewards)if(reward==null||!reward.isNotNullItem()||reward.quantity<1||reward.quantity>99999
                ||reward.template.type==9||reward.template.type==10||reward.template.type==34||reward.template.id==517||reward.template.id==518)return Result.INVALID_REWARD;
            for(var entry:cost.items.entrySet()){
                long count=player.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==entry.getKey()&&i.quantity>0).mapToLong(i->i.quantity).sum();
                if(count<entry.getValue())return Result.MISSING_ITEMS;
            }
            if(player.inventory.gold<cost.gold)return Result.MISSING_GOLD;
            if(player.inventory.gem<cost.gems||player.inventory.ruby<cost.ruby)return Result.MISSING_GEMS;
            if(gain.gold>nro.models.player.Inventory.LIMIT_GOLD-(player.inventory.gold-cost.gold)
                ||(long)player.inventory.gem-cost.gems+gain.gems>Integer.MAX_VALUE
                ||(long)player.inventory.ruby-cost.ruby+gain.ruby>Integer.MAX_VALUE)return Result.CURRENCY_LIMIT;
            List<Item> snapshot=new ArrayList<>();for(Item item:player.inventory.itemsBag)snapshot.add(copy(item));
            long goldBefore=player.inventory.gold;int gemsBefore=player.inventory.gem,rubyBefore=player.inventory.ruby;
            boolean committed=false;
            try{
                for(var entry:cost.items.entrySet()){
                    int remaining=entry.getValue();
                    for(Item item:player.inventory.itemsBag)if(item.isNotNullItem()&&item.template.id==entry.getKey()&&remaining>0){
                        int take=Math.min(remaining,Math.max(0,item.quantity));
                        InventoryService.gI().subQuantityItemsBag(player,item,take);remaining-=take;
                    }
                }
                player.inventory.gold=player.inventory.gold-cost.gold+gain.gold;player.inventory.gem=player.inventory.gem-cost.gems+gain.gems;player.inventory.ruby=player.inventory.ruby-cost.ruby+gain.ruby;
                // Plain bag items only: avoid special-item services with external side effects.
                for(Item reward:rewards)if(!InventoryService.gI().addItemList(player.inventory.itemsBag,copy(reward)))return Result.BAG_FULL;
                try{commit.save(player);}catch(Exception error){nro.models.utils.Logger.logException(BagExchange.class,error);return Result.SAVE_FAILED;}
                committed=true;lastSuccess.put(player,clock.getAsLong());return Result.SUCCESS;
            }finally{
                if(!committed){player.inventory.itemsBag.clear();player.inventory.itemsBag.addAll(snapshot);player.inventory.gold=goldBefore;player.inventory.gem=gemsBefore;player.inventory.ruby=rubyBefore;}
            }
        }
    }
    private static Item copy(Item source){
        Item target=ItemService.gI().copyItem(source);
        target.quantityGD=source.quantityGD;target.id=source.id;target.text=source.text;target.expire=source.expire;
        target.options=source.options==source.itemOptions?target.itemOptions:source.options;
        return target;
    }
}
