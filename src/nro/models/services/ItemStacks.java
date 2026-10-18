package nro.models.services;

import java.util.*;
import nro.models.item.Item;

/** Consumables only. Trade locks, stats and expiry are part of the stack's identity. */
public final class ItemStacks {
    public static final int LIMIT=99999;
    private static final Set<Integer> TYPES=Set.of(6,7,8,12,13,14,15,16,22,27,29,30,31,33,35,37);
    private static final Set<Integer> TIMED_BOXES=Set.of(1739,1944,1840,1757,1821);
    private static boolean liveExpiry(Item item){
        for(var o:item.itemOptions)if(o.optionTemplate.id==93){
            if(!TIMED_BOXES.contains((int)item.template.id)||o.param<=0||item.createTime<=0
                ||System.currentTimeMillis()-item.createTime>=o.param*86400000L)return false;
        }
        return true;
    }
    /** Merging boxes never extends the oldest box's expiry. */
    public static void keepOldestExpiry(Item target,Item source){
        if(TIMED_BOXES.contains((int)target.template.id)
            &&target.itemOptions.stream().anyMatch(o->o.optionTemplate.id==93))
            target.createTime=Math.min(target.createTime,source.createTime);
    }
    public static boolean stackable(Item item){
        return item!=null&&item.isNotNullItem()&&item.quantity>0&&item.template.isUpToUp&&TYPES.contains((int)item.template.type)
            &&item.expire==0&&liveExpiry(item);
    }
    private static int counter(Item item){
        for(var o:item.itemOptions)if(o.optionTemplate.id==1||o.optionTemplate.id==31)return o.optionTemplate.id;
        return -1;
    }
    private static Map<Long,Integer> signature(Item item,int ignored){
        Map<Long,Integer> signature=new HashMap<>();
        for(var o:item.itemOptions){int id=o.optionTemplate.id;if(id==ignored||id==73&&o.param==0)continue;
            long key=((long)id<<32)|(o.param&0xffffffffL);signature.merge(key,1,Integer::sum);
        }return signature;
    }
    public static boolean same(Item a,Item b){
        return stackable(a)&&stackable(b)&&a.template.id==b.template.id&&counter(a)==-1&&counter(b)==-1
            &&signature(a,-1).equals(signature(b,-1));
    }
    public static boolean sameCounter(Item a,Item b,int id){
        return stackable(a)&&stackable(b)&&a.template.id==b.template.id&&a.quantity==1&&b.quantity==1
            &&counter(a)==id&&counter(b)==id&&signature(a,id).equals(signature(b,id));
    }
    public static void compact(List<Item> items){
        for(int i=0;i<items.size();i++){
            Item target=items.get(i);if(!stackable(target))continue;
            for(int j=i+1;j<items.size();j++){
                Item source=items.get(j);int counter=counter(target);
                if(counter!=-1&&sameCounter(target,source,counter)){
                    var a=target.itemOptions.stream().filter(o->o.optionTemplate.id==counter).findFirst().orElseThrow();
                    var b=source.itemOptions.stream().filter(o->o.optionTemplate.id==counter).findFirst().orElseThrow();
                    long total=(long)a.param+b.param;if(a.param<0||b.param<0||total>Integer.MAX_VALUE)continue;
                    a.param=(int)total;source.quantity=0;items.set(j,ItemService.gI().createItemNull());
                }else if(target.quantity<LIMIT&&same(target,source)){
                    int moved=Math.min(LIMIT-target.quantity,source.quantity);keepOldestExpiry(target,source);target.quantity+=moved;source.quantity-=moved;
                    if(source.quantity==0)items.set(j,ItemService.gI().createItemNull());
                }
            }
        }
    }
    private ItemStacks(){}
}
