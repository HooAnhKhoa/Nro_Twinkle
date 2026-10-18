package nro.models.event.october20;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.consts.ConstMob;
import nro.models.event.EventManager;
import nro.models.event.trungthu2026.BagExchange;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.server.Maintenance;
import nro.models.server.PanelBridge;

/** October 20: scheduled purchase ranking; legacy gifts remain redeemable. */
public final class October20 {
    public static final int MENU=19101,CONFIRM=19102;
    public static final int[] ACCESSORIES={1515,1520,954};
    private static final int[] MATERIALS={1508,1505,1507,1509};
    private static final String[] MATERIAL_KEYS={"roses","paper","chocolate","bow"};
    private record Quote(October20Config.Snapshot config,int action){}
    private static final Map<Player,Quote> QUOTES=Collections.synchronizedMap(new WeakHashMap<>());
    private static final BagExchange EXCHANGE=new BagExchange(500,System::currentTimeMillis);
    public static boolean enabled(){return EventManager.enabled("october20");}
    public static Item item(int id,int quantity){return ItemService.gI().createNewItem((short)id,quantity);}
    public static BagExchange.Cost recipe(October20Config.Snapshot c,String kind){
        Map<Integer,Integer> costs=new LinkedHashMap<>();for(int i=0;i<MATERIALS.length;i++)costs.put(MATERIALS[i],c.n(kind+"."+MATERIAL_KEYS[i]));
        return new BagExchange.Cost(costs,c.n(kind+".gold"),0);
    }
    public static long count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id&&i.quantity>0).mapToLong(i->i.quantity).sum();}
    public static Item accessory(October20Config.Snapshot c,int id,int days,Random random){
        if(Arrays.stream(ACCESSORIES).noneMatch(v->v==id))throw new IllegalArgumentException("Phụ kiện 20/10 không hợp lệ");
        Item reward=item(id,1);int[] options={50,5,14,226};String[] names={"sd","sdcm","cm","cute"};
        for(int i=0;i<options.length;i++)reward.itemOptions.add(new Item.ItemOption(options[i],c.n(names[i]+".min")+random.nextInt(c.n(names[i]+".max")-c.n(names[i]+".min")+1)));
        reward.itemOptions.add(new Item.ItemOption(30,0));if(days>0)reward.itemOptions.add(new Item.ItemOption(93,days));return reward;
    }
    public static Item reward(October20Config.Snapshot c,String kind,Random random){
        int draw=random.nextInt(100);
        if(draw<c.n(kind+".accessory"))return accessory(c,ACCESSORIES[random.nextInt(ACCESSORIES.length)],random.nextInt(100)<c.n("permanent")?0:c.n(kind+".days"),random);
        if(draw<c.n(kind+".accessory")+c.n(kind+".stones"))return item(220+random.nextInt(5),c.n(kind+".quantity"));
        Item support=item(381+random.nextInt(3),c.n(kind+".quantity"));support.itemOptions.add(new Item.ItemOption(30,0));return support;
    }
    public static void open(Npc npc,Player p){October20Competition.openNpc(p,npc);}
    private static void legacyOpen(Npc npc,Player p){
        if(!EventManager.allowNpc(p,"october20"))return;
        QUOTES.remove(p);
        try{
            var c=October20Config.current();long points=October20Store.points(p);
            npc.createOtherMenu(p,MENU,"|2|MỪNG NGÀY PHỤ NỮ VIỆT NAM 20/10\n|1|Gói quà bằng nguyên liệu rơi từ quái, sau đó mở trong hành trang.\nĐiểm 20/10: "+points+"\nMỗi quà thường: +"+c.n("normal.points")+" điểm; quà đặc biệt: +"+c.n("special.points")+" điểm.\nĐổi phụ kiện "+c.n("redeem.days")+" ngày: "+c.n("redeem.points")+" điểm.\nKhông có đua top.","Gói quà\nthường","Gói quà\nđặc biệt","Đổi chậu\nhoa hồng","Đổi bóng\nhoa hồng","Đổi bó\nhoa hồng","Đóng");
        }catch(Exception e){failure(p,e);}
    }
    public static boolean menu(Npc npc,Player p,int select){
        if(October20Competition.confirm(p,npc,select))return true;
        int menu=p.idMark.getIndexMenu();if(menu!=MENU&&menu!=CONFIRM)return false;
        synchronized(p){
            if(!EventManager.allowNpc(p,"october20")){QUOTES.remove(p);return true;}
            if(menu==CONFIRM){Quote quote=QUOTES.remove(p);if(select==0&&quote!=null)execute(p,quote.config,quote.action);return true;}
            if(select<0||select>4){QUOTES.remove(p);return true;}
            var c=October20Config.current();String text;
            if(select<2){String kind=select==0?"normal":"special";var cost=recipe(c,kind);
                StringBuilder s=new StringBuilder("|2|GÓI QUÀ 20/10 "+(select==0?"THƯỜNG":"ĐẶC BIỆT"));
                for(int id:MATERIALS){long owned=count(p,id);int needed=cost.items().get(id);s.append(owned>=needed?"\n|1|":"\n|7|").append(ItemService.gI().getTemplate((short)id).name).append(" ").append(owned).append("/").append(needed);}
                s.append(p.inventory.gold>=cost.gold()?"\n|1|Giá vàng: ":"\n|7|Giá vàng: ").append(String.format(Locale.US,"%,d",cost.gold()));
                s.append("\n|0|Mở quà: ").append(c.n(kind+".accessory")).append("% phụ kiện; ").append(c.n(kind+".stones")).append("% đá nâng cấp; ").append(100-c.n(kind+".accessory")-c.n(kind+".stones")).append("% vật phẩm hỗ trợ.");
                s.append("\nPhụ kiện: ").append(c.n("permanent")).append("% vĩnh viễn, còn lại hạn ").append(c.n(kind+".days")).append(" ngày.");text=s.toString();
            }else{text="|2|ĐỔI "+ItemService.gI().getTemplate((short)ACCESSORIES[select-2]).name+"\n|1|Cần "+c.n("redeem.points")+" điểm 20/10.\nHạn dùng "+c.n("redeem.days")+" ngày.\n"+stats(c);}
            QUOTES.put(p,new Quote(c,select));npc.createOtherMenu(p,CONFIRM,text,"Đồng ý","Từ chối");return true;
        }
    }
    public static String stats(October20Config.Snapshot c){return "SD "+c.n("sd.min")+"–"+c.n("sd.max")+"%, SĐCM "+c.n("sdcm.min")+"–"+c.n("sdcm.max")+"%\nChí mạng "+c.n("cm.min")+"–"+c.n("cm.max")+"%, Cute "+c.n("cute.min")+"–"+c.n("cute.max")+"%.";}
    private static boolean ready(Player p){return p.isPl()&&p.id>0&&!p.isDie()&&!Maintenance.isRunning;}
    public static BagExchange.Result execute(Player p,October20Config.Snapshot c,int action){
        synchronized(p){
            if(!ready(p)||!enabled()||action<0||action>4)return BagExchange.Result.INVALID_REWARD;
            if(c.revision()!=October20Config.current().revision()){Service.gI().sendThongBao(p,"Cấu hình vừa thay đổi. Hãy mở lại menu.");return BagExchange.Result.SAVE_FAILED;}
            int delta=0;BagExchange.Cost cost;Item reward;
            if(action<2){cost=recipe(c,action==0?"normal":"special");reward=item(action==0?1719:1720,1);}
            else{cost=new BagExchange.Cost(Map.of(),0,0);reward=accessory(c,ACCESSORIES[action-2],c.n("redeem.days"),ThreadLocalRandom.current());delta=-c.n("redeem.points");}
            final int points=delta;var result=EXCHANGE.execute(p,cost,new BagExchange.Gain(List.of(reward),0,0,0),who->October20Store.commit(who,c,true,points,action<2?"craft."+action:"redeem."+reward.template.id,List.of(reward)));
            notify(p,result,result==BagExchange.Result.SUCCESS?"Đã nhận "+reward.template.name+". "+(action<2?"Mở trong hành trang để nhận thưởng.":"Đã trừ "+(-points)+" điểm 20/10."):null);return result;
        }
    }
    public static boolean use(Player p,Item used){
        int id=used.template.id;
        if(id==1718||id==2101){synchronized(p){if(!ready(p)||!p.inventory.itemsBag.contains(used)||used.quantity<1)return true;var c=October20Config.current();Item reward=reward(c,id==1718?"special":"normal",ThreadLocalRandom.current());var result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(id,1),0,0),new BagExchange.Gain(List.of(reward),0,0,0),who->October20Store.commit(who,c,false,0,"open.purchased",List.of(reward)));notify(p,result,"Bạn nhận "+reward.template.name+" x"+reward.quantity+". Mở hộp không cộng điểm đua top.");}return true;}
        if(enabled()&&Arrays.stream(MATERIALS).anyMatch(v->v==id)){Service.gI().sendThongBao(p,"Nguyên liệu 20/10: đến Chi Chi ở Đảo Kame để gói quà.");return true;}if(id!=1719&&id!=1720)return false;
        synchronized(p){
            if(!ready(p)||!p.inventory.itemsBag.contains(used)||used.quantity<1)return true;
            var c=October20Config.current();String kind=id==1719?"normal":"special";Item reward=reward(c,kind,ThreadLocalRandom.current());
            var result=EXCHANGE.execute(p,new BagExchange.Cost(Map.of(id,1),0,0),new BagExchange.Gain(List.of(reward),0,0,0),who->October20Store.commit(who,c,false,c.n(kind+".points"),"open."+kind,List.of(reward)));
            notify(p,result,"Bạn nhận "+reward.template.name+" x"+reward.quantity+" và "+c.n(kind+".points")+" điểm 20/10.");return true;
        }
    }
    private static void notify(Player p,BagExchange.Result result,String success){
        if(result==BagExchange.Result.SUCCESS){InventoryService.gI().sendItemBags(p);Service.gI().sendMoney(p);Service.gI().sendThongBao(p,success);return;}
        String text=switch(result){case MISSING_ITEMS->"Không đủ nguyên liệu.";case MISSING_GOLD->"Không đủ vàng.";case BAG_FULL->"Hành trang đầy. Nguyên liệu và quà được giữ nguyên.";case COOLDOWN->"Thao tác quá nhanh, hãy thử lại.";case SAVE_FAILED->"Chưa đổi được: kiểm tra điểm, trạng thái sự kiện hoặc tải lại cấu hình. Vật phẩm được giữ nguyên.";default->"Chưa thể thực hiện giao dịch.";};Service.gI().sendThongBao(p,text);
    }
    private static void failure(Player p,Exception e){nro.models.utils.Logger.logException(October20.class,e);Service.gI().sendThongBao(p,"Chưa đọc được dữ liệu 20/10, vui lòng thử lại.");}
    public static void drops(Mob mob,Player attacker,List<ItemMap> drops,int x,int y){
        Player owner=attacker instanceof nro.models.player.Pet pet?pet.master:attacker;
        if(!enabled()||!EventManager.enabled("mob_rewards")||!owner.isPl()||mob.zone==null||mob.isBigBoss()||mob.tempId==ConstMob.MOC_NHAN||mob.tempId==ConstMob.BU_NHIN_MA_QUAI)return;
        // Independent rolls; no fall-through into unrelated map-specific reward branches.
        int[] rates={5,5,3,1};
        for(int i=0;i<MATERIALS.length;i++)if(nro.models.mob.MobDropBonuses.roll(owner,"october20","october20."+MATERIAL_KEYS[i],rates[i],100))drops.add(new ItemMap(mob.zone,MATERIALS[i],1,x,y,owner.id));
    }
    private October20(){}
}
