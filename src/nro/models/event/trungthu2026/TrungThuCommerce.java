package nro.models.event.trungthu2026;

import java.util.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

public final class TrungThuCommerce {
    private static final BagExchange EXCHANGE=new BagExchange(number("exchange.cooldown-ms"),System::currentTimeMillis);
    private static final Random RANDOM=new Random();
    public static boolean trade(Player p,BagExchange.Cost cost,BagExchange.Gain gain){
        if(!nro.models.event.EventManager.allowNpc(p,"trung_thu"))return false;
        if(nro.models.server.Maintenance.isRunning){Service.gI().sendThongBao(p,"Máy chủ đang bảo trì. Giao dịch chưa thực hiện.");return false;}
        return tradeCounted(p,cost,gain,-1,"");
    }
    public static boolean tradeCounted(Player p,BagExchange.Cost cost,BagExchange.Gain gain,int item,String basis){return tradeCounted(p,cost,gain,item,basis,1);}
    public static boolean tradeCounted(Player p,BagExchange.Cost cost,BagExchange.Gain gain,int item,String basis,int quantity){
        if(!nro.models.event.EventManager.allowNpc(p,"trung_thu")||nro.models.server.Maintenance.isRunning)return false;
        BagExchange.Result result=EXCHANGE.execute(p,cost,gain,player->TrungThuCompetition.persist(player,item,basis,quantity));
        notifyResult(p,result);return result==BagExchange.Result.SUCCESS;
    }
    public static void notifyResult(Player p,BagExchange.Result result){
        if(result==BagExchange.Result.SUCCESS){InventoryService.gI().sendItemBags(p);Service.gI().sendMoney(p);Service.gI().sendThongBao(p,"Giao dịch Trung Thu thành công.");}
        else Service.gI().sendThongBao(p,switch(result){case COOLDOWN->"Bạn thao tác quá nhanh, vui lòng chờ một chút.";case MISSING_ITEMS->"Chưa đủ nguyên liệu.";case MISSING_GOLD->"Chưa đủ vàng.";case MISSING_GEMS->"Chưa đủ ngọc xanh.";case BAG_FULL->"Hành trang không đủ chỗ. Nguyên liệu và tiền được giữ nguyên.";case CURRENCY_LIMIT->"Tiền/ngọc đạt giới hạn. Vật phẩm được giữ nguyên.";default->"Chưa thể hoàn tất giao dịch. Vật phẩm và tiền được giữ nguyên.";});
    }
    public static BagExchange.Cost cost(Map<Integer,Integer> items,long gold,int gems){return new BagExchange.Cost(items,gold,gems);}
    public static BagExchange.Gain gain(Item... items){return new BagExchange.Gain(Arrays.asList(items),0,0,0);}
    public static void buy(Player p,String key,String price,boolean gems){trade(p,cost(Map.of(),gems?0:number(price),gems?number(price):0),gain(TrungThuRewards.item(key,1,0)));}
    public static BagExchange.Cost craftCost(int recipe){
        if(recipe<0||recipe>2)throw new IllegalArgumentException("Công thức không hợp lệ");
        Map<Integer,Integer> ingredients=new HashMap<>();ingredients.put(item("flour"),number("recipe.flour"));ingredients.put(item("beans"),number("recipe.beans"));ingredients.put(item("salted-egg"),number("recipe.eggs"));
        ingredients.put(item(recipe==2?"lotus-seed":"chicken"),number(recipe==2?"recipe.lotus-seed":"recipe.chicken"));
        return cost(ingredients,recipe==0?number("price.chicken-cake-gold"):0,recipe==0?0:number(recipe==1?"price.premium-cake-gems":"price.lotus-cake-gems"));
    }
    public static void craft(Player p,int recipe){
        BagExchange.Cost cost=craftCost(recipe);
        List<Item> rewards=new ArrayList<>();rewards.add(TrungThuRewards.item(recipe==2?"lotus-cake":recipe==1?"premium-chicken-cake":"chicken-cake",1,0));
        if(recipe==1&&RANDOM.nextInt(100)<number("recipe.bonus-mixed-percent"))rewards.add(TrungThuRewards.item("mixed-cake",1,0));
        trade(p,cost,new BagExchange.Gain(rewards,0,0,0));
    }
    public static BagExchange.Cost exchangeCost(String kind){
        if(!Set.of("tails","carrots","lantern").contains(kind))throw new IllegalArgumentException("Đổi thưởng không hợp lệ");
        String ingredient=kind.equals("tails")?"monkey-tail":kind.equals("carrots")?"carrot":"lantern-offering";
        int count=kind.equals("lantern")?1:number(kind.equals("tails")?"exchange.monkey-tails":"exchange.carrots");
        return cost(Map.of(item(ingredient),count),0,0);
    }
    public static void exchange(Player p,String kind){trade(p,exchangeCost(kind),TrungThuRewards.roll(kind,RANDOM));}
    public static BagExchange.Cost petCost(int select){
        if(select<0||select>2)throw new IllegalArgumentException("Pet không hợp lệ");
        String[] cakes={"chicken-cake","mixed-cake","lotus-cake"},counts={"exchange.grey-rabbit-cakes","exchange.white-rabbit-cakes","exchange.black-cat-cakes"};
        return cost(Map.of(item(cakes[select]),number(counts[select])),0,0);
    }
    public static void pet(Player p,int select){String[] pets={"grey-rabbit","white-rabbit","black-cat"};if(select<0||select>2)return;
        trade(p,petCost(select),gain(TrungThuRewards.item(pets[select],1,TrungThuRewards.petDays(RANDOM))));
    }
    public static String boxKind(int id){
        if(id==2079)return "normal-box";
        if(id==item("normal-box")||id==item("legacy-normal-box"))return "normal-box";
        if(id==item("vegetarian-box")||id==item("legacy-vegetarian-box"))return "vegetarian-box";
        return null;
    }
    public static boolean useBox(Player p,Item box){
        String key=boxKind(box.template.id);
        if(key==null)return false;
        synchronized(p){if(!p.inventory.itemsBag.contains(box)||box.quantity<1)return true;tradeCounted(p,cost(Map.of((int)box.template.id,1),0,0),TrungThuRewards.roll(key,RANDOM),box.template.id,"opened");}return true;
    }
    public static void chooseSide(Player p,int side){
        if(side!=1&&side!=2)return;if(!nro.models.event.EventManager.allowNpc(p,"trung_thu"))return;
        synchronized(p){try{
            Item sword=InventoryService.gI().findItemBag(p,item("wooden-sword"));
            if(sword==null){Item token=TrungThuRewards.item("wooden-sword",1,0);token.itemOptions.add(new Item.ItemOption(30,0));if(!trade(p,cost(Map.of(),0,0),gain(token)))return;}
            TrungThuStore.side(p,side);Service.gI().changeFlag(p,side);Service.gI().sendThongBao(p,"Bạn ủng hộ phe "+(side==1?"Gogeta":"Omega")+". Giữ kiếm gỗ trong hành trang và hạ boss phe đối lập.");
        }catch(Exception error){nro.models.utils.Logger.logException(TrungThuCommerce.class,error);Service.gI().sendThongBao(p,"Chưa lưu được lựa chọn phe.");}}
    }
    private TrungThuCommerce(){}
}
