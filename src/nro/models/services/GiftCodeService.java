package nro.models.services;

import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.event.trungthu2026.BagExchange;
import nro.models.event.trungthu2026.TrungThuStore;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.server.Maintenance;
import nro.models.utils.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/** Resolve current web data on every redemption; save rewards, receipt and stock atomically. */
public class GiftCodeService {
    private static final GiftCodeService INSTANCE=new GiftCodeService();
    private static final BagExchange EXCHANGE=new BagExchange(0,System::currentTimeMillis);
    public record Result(boolean success,String message){}
    public static GiftCodeService gI(){return INSTANCE;}
    private static Result fail(String message){return new Result(false,message);}
    private static String normalize(String code){return code==null?"":code.strip().toLowerCase(Locale.ROOT);}

    public void giftCode(Player player,String code){
        Result result=redeem(player,code);
        if(result.success()){
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
        }
        Service.gI().sendThongBao(player,result.message());
    }

    public Result redeem(Player player,String entered){
        String code=normalize(entered);
        if(code.isEmpty()||code.length()>100)return fail("Vui lòng nhập đúng mã quà tặng.");
        synchronized(player){
            if(Maintenance.isRunning)return fail("Máy chủ đang bảo trì, vui lòng thử lại sau.");
            try(Connection c=LocalManager.getConnection()){
                c.setAutoCommit(false);boolean committed=false;
                try{
                    int id,count;String stored,detail;Timestamp start,end;
                    try(PreparedStatement q=c.prepareStatement("SELECT id,code,count_left,detail,datecreate,expired FROM giftcode WHERE LOWER(TRIM(code))=? FOR UPDATE")){
                        q.setString(1,code);
                        try(ResultSet r=q.executeQuery()){
                            if(!r.next())return fail("Code không chính xác!");
                            id=r.getInt("id");stored=r.getString("code");count=r.getInt("count_left");detail=r.getString("detail");start=r.getTimestamp("datecreate");end=r.getTimestamp("expired");
                            if(r.next())return fail("Mã quà tặng bị trùng, vui lòng báo quản trị viên.");
                        }
                    }
                    long now=System.currentTimeMillis();
                    if(end!=null&&now>=end.getTime())return fail("Code đã hết hạn.");
                    if(start!=null&&now<start.getTime())return fail("Code chưa đến thời gian sử dụng.");
                    if(count!=-1&&count<=0)return fail("Giftcode đã hết lượt.");
                    LinkedHashSet<String> receipts=new LinkedHashSet<>();
                    try(PreparedStatement q=c.prepareStatement("SELECT giftcode FROM player WHERE id=? FOR UPDATE")){
                        q.setLong(1,player.id);
                        try(ResultSet r=q.executeQuery()){
                            if(!r.next())return fail("Không tìm thấy dữ liệu nhân vật.");
                            String json=r.getString(1);Object parsed=json==null?null:JSONValue.parseWithException(json);
                            if(parsed!=null&&!(parsed instanceof JSONArray))throw new IllegalArgumentException("Invalid giftcode receipt");
                            if(parsed instanceof JSONArray list)for(Object value:list)receipts.add(value.toString());
                        }
                    }
                    receipts.addAll(player.giftCode.rewards);
                    if(receipts.stream().anyMatch(value->normalize(value).equals(code)))return fail("Bạn đã nhận giftcode này rồi.");
                    BagExchange.Gain gain=parseRewards(detail);
                    receipts.add(stored);JSONArray history=new JSONArray();history.addAll(receipts);
                    BagExchange.Result result=EXCHANGE.execute(player,new BagExchange.Cost(Map.of(),0,0),gain,p->{
                        try{
                            try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=?,data_inventory=?,giftcode=? WHERE id=?")){
                                q.setString(1,TrungThuStore.bag(p));q.setString(2,TrungThuStore.money(p));q.setString(3,history.toJSONString());q.setLong(4,p.id);
                                if(q.executeUpdate()!=1)throw new SQLException("Missing player");
                            }
                            if(count!=-1)try(PreparedStatement q=c.prepareStatement("UPDATE giftcode SET count_left=count_left-1 WHERE id=? AND count_left>0")){
                                q.setInt(1,id);if(q.executeUpdate()!=1)throw new SQLException("Giftcode stock changed");
                            }
                            c.commit();
                        }catch(Exception error){c.rollback();throw error;}
                    });
                    if(result!=BagExchange.Result.SUCCESS)return fail(switch(result){
                        case BAG_FULL->"Hành trang không đủ chỗ. Giftcode chưa bị sử dụng.";
                        case CURRENCY_LIMIT->"Tiền/ngọc sẽ vượt giới hạn. Giftcode chưa bị sử dụng.";
                        case INVALID_REWARD->"Phần thưởng giftcode chưa hợp lệ, vui lòng báo quản trị viên.";
                        default->"Chưa thể nhận giftcode. Vui lòng thử lại, mã chưa bị sử dụng.";
                    });
                    committed=true;player.giftCode.rewards.clear();player.giftCode.rewards.addAll(receipts);
                    return new Result(true,"Nhận giftcode "+stored+" thành công!");
                }finally{if(!committed)c.rollback();}
            }catch(Exception error){Logger.logException(GiftCodeService.class,error);return fail("Chưa thể nhận giftcode. Vui lòng báo quản trị viên; mã chưa bị sử dụng.");}
        }
    }

    private static BagExchange.Gain parseRewards(String json)throws Exception{
        Object parsed=JSONValue.parseWithException(json);
        if(!(parsed instanceof JSONArray rows)||rows.isEmpty())throw new IllegalArgumentException("Empty giftcode reward");
        List<Item> items=new ArrayList<>();long gold=0;int gems=0,ruby=0;
        for(Object value:rows){
            JSONObject row=(JSONObject)value;int id=Integer.parseInt(row.get("id").toString()),qty=Integer.parseInt(row.get("quantity").toString());
            if(qty<=0)throw new IllegalArgumentException("Invalid giftcode quantity");
            if(id==-1){gold=Math.addExact(gold,qty);continue;}
            if(id==-2){gems=Math.addExact(gems,qty);continue;}
            if(id==-3){ruby=Math.addExact(ruby,qty);continue;}
            if(id<0||id>Short.MAX_VALUE)throw new IllegalArgumentException("Invalid giftcode item");
            Item item=ItemService.gI().createNewItem((short)id,qty);
            if(item==null||!item.isNotNullItem())throw new IllegalArgumentException("Unknown giftcode item");
            if(item.template.type==9){gold=Math.addExact(gold,qty);continue;}
            if(item.template.type==10){gems=Math.addExact(gems,qty);continue;}
            if(item.template.type==34){ruby=Math.addExact(ruby,qty);continue;}
            item.itemOptions.clear();
            if(row.get("options") instanceof JSONArray options)for(Object raw:options){
                JSONObject option=(JSONObject)raw;Item.ItemOption io=new Item.ItemOption(Integer.parseInt(option.get("id").toString()),Integer.parseInt(option.get("param").toString()));
                if(io.optionTemplate==null)throw new IllegalArgumentException("Unknown giftcode option");item.itemOptions.add(io);
            }
            items.add(item);
        }
        return new BagExchange.Gain(items,gold,gems,ruby);
    }
}