package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.shop.*;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

/** Uses the ordinary NPC shop window; purchases retain the event's atomic save/rollback path. */
public final class TrungThuShop {
    public static final String TAG = "TRUNG_THU_2026";

    public static void ensureData(Connection c) throws SQLException {
        int shop = id(c, "SELECT id FROM shop WHERE tag_name=?", TAG);
        if (shop < 0) {
            execute(c, "INSERT INTO shop(npc_id,tag_name,type_shop) VALUES(41,?,0)", TAG);
            shop = id(c, "SELECT id FROM shop WHERE tag_name=?", TAG);
        }
        int summons = tab(c, shop, "Triệu hồi");
        int goods = tab(c, shop, "Đồ sự kiện");
        int outfits = tab(c, shop, "Trang phục");
        seed(c, summons, item("moon"), number("price.moon-gems"), 1, 0);
        seed(c, summons, item("rabbit-summon"), number("price.rabbit-gold"), 0, 1);
        seed(c, goods, item("lantern-offering"), number("price.lantern-gems"), 1, 0);
        seed(c, goods, item("normal-box"), number("price.normal-box-gems"), 1, 1);
        seed(c, goods, item("vegetarian-box"), number("price.vegetarian-box-gold"), 0, 2);
        int[] rabbits = numbers("item.rabbit-costumes");
        for (int i = 0; i < rabbits.length; i++) seed(c, outfits, rabbits[i], number("shop.rabbit-costume-gems"), 1, i);
        seed(c, outfits, item("parade-lantern"), number("price.parade-lantern-gems"), 1, rabbits.length);
    }

    private static int tab(Connection c, int shop, String name) throws SQLException {
        int tab = id(c, "SELECT id FROM tab_shop WHERE shop_id=? AND NAME=?", shop, name);
        if (tab < 0) {
            execute(c, "INSERT INTO tab_shop(shop_id,NAME) VALUES(?,?)", shop, name);
            tab = id(c, "SELECT id FROM tab_shop WHERE shop_id=? AND NAME=?", shop, name);
        }
        return tab;
    }

    private static void seed(Connection c, int tab, int item, int cost, int currency, int order) throws SQLException {
        if (id(c, "SELECT id FROM item_shop WHERE tab_id=? AND temp_id=?", tab, item) >= 0) return;
        execute(c, "INSERT INTO item_shop(tab_id,temp_id,is_new,is_sell,type_sell,cost,icon_spec,sort_order) VALUES(?,?,1,1,?,?,0,?)", tab, item, currency, cost, order);
    }

    private static int id(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement q = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) q.setObject(i + 1, args[i]);
            try (ResultSet r = q.executeQuery()) { return r.next() ? r.getInt(1) : -1; }
        }
    }

    private static void execute(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement q = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) q.setObject(i + 1, args[i]);
            q.executeUpdate();
        }
    }

    public static void open(Player player) {
        if (nro.models.event.EventManager.allowNpc(player, "trung_thu")) ShopService.gI().opendShop(player, TAG, true);
    }

    public static void buy(Player player, Shop shop, int itemId) {
        if (shop == null || !TAG.equals(shop.tagName)) return;
        ItemShop sale = shop.getItemShop(itemId);
        if (sale == null || sale.cost < 0 || (sale.typeSell != 0 && sale.typeSell != 1)) {
            Service.gI().sendThongBao(player, "Vật phẩm hoặc giá bán không hợp lệ.");
            return;
        }
        TrungThuCommerce.trade(player,
                TrungThuCommerce.cost(Map.of(), sale.typeSell == 0 ? sale.cost : 0, sale.typeSell == 1 ? sale.cost : 0),
                TrungThuCommerce.gain(ItemService.gI().createItemFromItemShop(sale)));
    }

    private TrungThuShop() {}
}
