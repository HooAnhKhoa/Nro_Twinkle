package nro.models.npc_list;

import java.text.NumberFormat;
import java.util.Locale;
import nro.models.consts.ConstFont;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.event.trungthu2026.*;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

public final class TrungThu2026 extends Npc {
    private static final int HOME=202610160,CRAFT=HOME+2,PET=HOME+3,SIDE=HOME+4,CONFIRM=HOME+10;
    public TrungThu2026(int map,int status,int x,int y,int id,int avatar){super(map,status,x,y,id,avatar);}
    @Override protected int menuAvatar(){return avartar;}

    @Override public void openBaseMenu(Player p){
        if(canOpenNpc(p)&&nro.models.event.EventManager.allowNpc(p,"trung_thu"))
            createOtherMenu(p,HOME,"Trung Thu: săn Khỉ, truy tìm Thỏ, treo đèn, làm bánh và rước đèn bang hội.\nKhỉ/Thỏ xuất hiện khi dùng vật phẩm triệu hồi.","Cửa hàng","Làm bánh","Đổi pet","Đổi đuôi khỉ","Đổi cà rốt","Treo đèn","Chọn phe","Đóng");
    }

    private static String format(long value){return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);}

    /** The displayed costs are the exact objects used by the transaction code. */
    private void preview(Player p,int action){
        BagExchange.Cost cost;
        String title;
        if(action<3){
            cost=TrungThuCommerce.craftCost(action);
            title=action==2?"Làm bánh Hạt sen?":action==1?"Làm bánh Gà quay bằng ngọc?":"Làm bánh Gà quay bằng vàng?";
        }else if(action<6){
            int pet=action-3;cost=TrungThuCommerce.petCost(pet);
            title="Đổi "+new String[]{"Thỏ xám","Thỏ trắng","Mèo đen"}[pet]+"?";
        }else{
            String kind=new String[]{"tails","carrots","lantern"}[action-6];
            cost=TrungThuCommerce.exchangeCost(kind);
            title=action==8?"Treo đèn nhận thưởng?":"Đổi "+(action==6?"đuôi khỉ":"cà rốt")+" nhận thưởng?";
        }
        StringBuilder text=new StringBuilder(ConstFont.BOLD_GREEN).append(title).append('\n');
        cost.items().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).forEach(entry->{
            long owned=p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==entry.getKey()&&i.quantity>0).mapToLong(i->i.quantity).sum();
            text.append(owned>=entry.getValue()?ConstFont.BOLD_BLUE:ConstFont.BOLD_RED)
                .append(ItemService.gI().getTemplate(entry.getKey()).name).append(' ')
                .append(format(owned)).append('/').append(format(entry.getValue())).append('\n');
        });
        if(cost.gold()>0)text.append(ConstFont.BOLD_RED).append("Giá vàng: ").append(format(cost.gold())).append('\n');
        if(cost.gems()>0)text.append(ConstFont.BOLD_RED).append("Giá ngọc xanh: ").append(format(cost.gems())).append('\n');
        if(action==1)text.append(ConstFont.DEFAULT).append("Thêm ").append(number("recipe.bonus-mixed-percent")).append("% nhận bánh Thập cẩm.\n");
        if(action>=3&&action<6)text.append(ConstFont.DEFAULT).append("Hạn ngẫu nhiên: 3/5/7/15 ngày hoặc vĩnh viễn.\n");
        createOtherMenu(p,CONFIRM+action,text.toString(),"Đồng ý","Từ chối");
    }

    @Override public void confirmMenu(Player p,int select){
        if(!canOpenNpc(p)||!nro.models.event.EventManager.allowNpc(p,"trung_thu"))return;
        int menu=p.idMark.getIndexMenu();
        if(menu>=CONFIRM&&menu<CONFIRM+9){
            p.idMark.setIndexMenu(-1);
            if(select!=0)return;
            int action=menu-CONFIRM;
            if(action<3)TrungThuCommerce.craft(p,action);
            else if(action<6)TrungThuCommerce.pet(p,action-3);
            else TrungThuCommerce.exchange(p,new String[]{"tails","carrots","lantern"}[action-6]);
            return;
        }
        if(menu==HOME)switch(select){
            case 0->TrungThuShop.open(p);
            case 1->createOtherMenu(p,CRAFT,"Chọn loại bánh muốn làm:","Gà quay\nBằng vàng","Gà quay\nBằng ngọc","Hạt sen\nBằng ngọc","Đóng");
            case 2->createOtherMenu(p,PET,"Chọn pet muốn đổi:","Thỏ xám","Thỏ trắng","Mèo đen","Đóng");
            case 3->preview(p,6);
            case 4->preview(p,7);
            case 5->preview(p,8);
            case 6->createOtherMenu(p,SIDE,"Mang kiếm gỗ trong túi, chọn cờ phe và hạ boss đối lập ở Siêu Thị.","Ủng hộ Gogeta","Ủng hộ Omega","Đóng");
        }
        else if(menu==CRAFT&&select>=0&&select<=2)preview(p,select);
        else if(menu==PET&&select>=0&&select<=2)preview(p,select+3);
        else if(menu==SIDE&&select>=0&&select<=1)TrungThuCommerce.chooseSide(p,select+1);
    }
}