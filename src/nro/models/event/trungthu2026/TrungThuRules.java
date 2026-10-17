package nro.models.event.trungthu2026;
import java.text.NumberFormat;import java.util.Locale;
import static nro.models.event.trungthu2026.TrungThuConfig.*;
/** Player-facing rules use the same prices and quantities as actual transactions. */
public final class TrungThuRules{
 private static String n(long value){return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value);}
 public static String text(){
  return "TRUNG THU 2026\n"
   +"1. Săn Khỉ Xayda: mua Ánh trăng tròn "+n(number("price.moon-gems"))+" ngọc, dùng để gọi Khỉ ngẫu nhiên ở Thung lũng đen, Vách núi đen hoặc Thành phố Vegeta. Chỉ dùng đấm/chưởng cơ bản; Khỉ miễn khống chế. Gom "+number("exchange.monkey-tails")+" đuôi khỉ để đổi quà.\n"
   +"2. Thỏ Đại Ca: Thỏ ngọc giá "+n(number("price.rabbit-gold"))+" vàng, gọi Thỏ ở Siêu Thị hoặc Thành phố Santa. Đến gần có thể hóa cà rốt 5 phút, giảm 15% sức đánh. Mặc cải trang Thỏ của sự kiện để miễn hóa. Đổi "+number("exchange.carrots")+" cà rốt tại đây.\n"
   +"3. Treo đèn: nhiệm vụ khó cho "+number("quest.hard-lanterns")+" đèn/ngày, siêu khó cho "+number("quest.very-hard-lanterns")+" đèn/ngày. Mua đèn treo "+n(number("price.lantern-gems"))+" ngọc. Chọn Treo đèn để nhận quà, có cơ hội Lồng đèn 2 lon hạn dùng hoặc vĩnh viễn.\n"
   +"4. Gogeta–Omega: chọn phe để nhận kiếm gỗ; giữ kiếm trong túi và cờ đúng phe, hạ boss đối lập để có cơ hội nhận cải trang 3/5/7/15 ngày và thú cưỡi hạn dùng hoặc vĩnh viễn.\n"
   +"5. Nguyên liệu: trứng/thịt gà từ quái thường; bột mì/đậu xanh từ phó bản; hạt sen mỗi lần hoàn thành nhiệm vụ khó/siêu khó. Bột mì, đậu xanh và hạt sen được ký gửi bằng vàng.\n"
   +"6. Công thức: "+number("recipe.flour")+" bột mì + "+number("recipe.beans")+" đậu xanh + "+number("recipe.eggs")+" trứng + "+number("recipe.chicken")+" thịt gà + "+n(number("price.chicken-cake-gold"))+" vàng = bánh Gà quay. Đổi sang "+number("price.premium-cake-gems")+" ngọc có thêm "+number("recipe.bonus-mixed-percent")+"% nhận bánh Thập cẩm. Thay thịt gà bằng "+number("recipe.lotus-seed")+" hạt sen, dùng "+number("price.lotus-cake-gems")+" ngọc để làm bánh Hạt sen.\n"
   +"7. Đổi pet: "+number("exchange.grey-rabbit-cakes")+" bánh Gà quay đổi Thỏ xám; "+number("exchange.white-rabbit-cakes")+" bánh Thập cẩm đổi Thỏ trắng; "+number("exchange.black-cat-cakes")+" bánh Hạt sen đổi Mèo đen đuôi vàng. Hạn 3/5/7/15 ngày, có cơ hội vĩnh viễn.\n"
   +"8. Rước đèn/Điểm danh: Quy Lão → khu vực bang → Giu-ma (Ngưu Ma Vương). Bang chủ bắt đầu; thành viên online cùng khu, đeo lồng đèn và xác nhận đủ lượt. Người tham gia nhận X2 TNSM "+number("buff.parade-minutes")+" phút, mỗi hoạt động một lần/ngày.\n"
   +"9. Cửa hàng: hộp Trung Thu 2026 "+number("price.normal-box-gems")+" ngọc; hộp Chay 2026 "+n(number("price.vegetarian-box-gold"))+" vàng. Mở hộp nhận vàng, hồng ngọc hoặc vật phẩm hiếm.";
 }
 private TrungThuRules(){}
}
