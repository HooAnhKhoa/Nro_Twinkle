package nro.models.npc_list;

import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.event.trungthu2026.*;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

public final class TrungThu2026 extends Npc {
    private static final int HOME=202610160,SHOP=HOME+1,CRAFT=HOME+2,PET=HOME+3,SIDE=HOME+4,WEAR=HOME+5;
    public TrungThu2026(int map,int status,int x,int y,int id,int avatar){super(map,status,x,y,id,avatar);}
    @Override public void openBaseMenu(Player p){if(canOpenNpc(p)&&nro.models.event.EventManager.allowNpc(p,"trung_thu"))createOtherMenu(p,HOME,"Trung Thu: săn Khỉ, truy tìm Thỏ, treo đèn, làm bánh và rước đèn bang hội. Không có đua top.\nKhỉ/Thỏ chỉ xuất hiện khi dùng vật phẩm triệu hồi.","Cửa hàng","Làm bánh","Đổi pet","Đổi "+number("exchange.monkey-tails")+" đuôi khỉ","Đổi "+number("exchange.carrots")+" cà rốt","Treo đèn","Chọn phe","Đóng");}
    @Override public void confirmMenu(Player p,int select){
        if(!canOpenNpc(p)||!nro.models.event.EventManager.allowNpc(p,"trung_thu"))return;
        int menu=p.idMark.getIndexMenu();
        if(menu==HOME)switch(select){
            case 0->createOtherMenu(p,SHOP,"Mua mỗi lần 1 món. Dùng đúng tên vật phẩm sẵn có trong game.","Ánh trăng\n"+number("price.moon-gems")+" ngọc","Thỏ ngọc\n"+number("price.rabbit-gold")+" vàng","Đèn treo\n"+number("price.lantern-gems")+" ngọc","Giỏ bánh\n"+number("price.normal-box-gems")+" ngọc","Giỏ chay\n"+number("price.vegetarian-box-gold")+" vàng","Cải trang\nĐèn đeo","Đóng");
            case 1->createOtherMenu(p,CRAFT,"Mỗi bánh cần "+number("recipe.flour")+" bột mì + "+number("recipe.beans")+" đậu xanh + "+number("recipe.eggs")+" trứng.\nGà quay: thêm 1 gà; hạt sen: thêm 1 hạt sen. Công thức ngọc dùng cùng bánh Gà quay, có "+number("recipe.bonus-mixed-percent")+"% thêm Thập Cẩm.","Gà quay\n"+number("price.chicken-cake-gold")+" vàng","Gà quay\n"+number("price.premium-cake-gems")+" ngọc","Hạt sen\n"+number("price.lotus-cake-gems")+" ngọc","Đóng");
            case 2->createOtherMenu(p,PET,"Pet có thể nhận hạn 3/5/7/15 ngày hoặc vĩnh viễn theo bảng thưởng.",number("exchange.grey-rabbit-cakes")+" Gà quay\nThỏ xám",number("exchange.white-rabbit-cakes")+" Thập cẩm\nThỏ trắng",number("exchange.black-cat-cakes")+" Hạt sen\nMèo đen","Đóng");
            case 3->TrungThuCommerce.exchange(p,"tails");case 4->TrungThuCommerce.exchange(p,"carrots");case 5->TrungThuCommerce.exchange(p,"lantern");
            case 6->createOtherMenu(p,SIDE,"Mang kiếm gỗ trong túi, chọn cờ phe và hạ boss đối lập ở Siêu Thị. Cải trang phe Omega dùng Cải trang rồng sẵn có.","Ủng hộ Gogeta","Ủng hộ Omega","Đóng");
        }
        else if(menu==SHOP)switch(select){
            case 0->TrungThuCommerce.buy(p,"moon","price.moon-gems",true);case 1->TrungThuCommerce.buy(p,"rabbit-summon","price.rabbit-gold",false);case 2->TrungThuCommerce.buy(p,"lantern-offering","price.lantern-gems",true);case 3->TrungThuCommerce.buy(p,"normal-box","price.normal-box-gems",true);case 4->TrungThuCommerce.buy(p,"vegetarian-box","price.vegetarian-box-gold",false);
            case 5->createOtherMenu(p,WEAR,"Cải trang thỏ miễn nhiễm hóa cà rốt. Lồng đèn Ông Sao đeo trên người để rước đèn bang hội.","Thỏ đỏ\n"+number("shop.rabbit-costume-gems")+" ngọc","Thỏ đen\n"+number("shop.rabbit-costume-gems")+" ngọc","Ông Sao\n"+number("price.parade-lantern-gems")+" ngọc","Đóng");
        }
        else if(menu==CRAFT&&select>=0&&select<=2)TrungThuCommerce.craft(p,select);
        else if(menu==PET&&select>=0&&select<=2)TrungThuCommerce.pet(p,select);
        else if(menu==SIDE&&select>=0&&select<=1)TrungThuCommerce.chooseSide(p,select+1);
        else if(menu==WEAR){
            if(select==0||select==1)TrungThuCommerce.trade(p,TrungThuCommerce.cost(java.util.Map.of(),0,number("shop.rabbit-costume-gems")),TrungThuCommerce.gain(TrungThuRewards.item(numbers("item.rabbit-costumes")[select],1,0)));
            else if(select==2)TrungThuCommerce.buy(p,"parade-lantern","price.parade-lantern-gems",true);
        }
    }
}
