package nro.models.database;
import nro.models.item.Item;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;
import nro.models.services.ItemService;
import nro.models.utils.Logger;
import nro.models.data.LocalManager;
import nro.models.server.Manager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ShopDAO {

    private static long lastReloadId;

    public static void pollReloadRequests() {
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT id FROM shop_reload_request WHERE id > ? ORDER BY id DESC LIMIT 1")) {
            ps.setLong(1, lastReloadId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                long requestId = rs.getLong(1);
                List<Shop> shops = loadShopsStrict(con);
                Manager.SHOPS = shops;
                try (PreparedStatement ack = con.prepareStatement("UPDATE shop_reload_request SET loaded_at = NOW(), shop_count = ? WHERE id <= ? AND loaded_at IS NULL")) {
                    ack.setInt(1, shops.size());
                    ack.setLong(2, requestId);
                    ack.executeUpdate();
                }
                lastReloadId = requestId;
            }
        } catch (SQLException e) {
            if (!"42S02".equals(e.getSQLState())) Logger.logException(ShopDAO.class, e);
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }

    private static List<Shop> loadShopsStrict(Connection con) throws SQLException {
        List<Shop> shops = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM shop ORDER BY npc_id ASC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Shop shop = new Shop();
                shop.id = rs.getInt("id");
                shop.npcId = rs.getByte("npc_id");
                shop.tagName = rs.getString("tag_name");
                shop.typeShop = rs.getByte("type_shop");
                loadTabsStrict(con, shop);
                shops.add(shop);
            }
        }
        return shops;
    }

    private static void loadTabsStrict(Connection con, Shop shop) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM tab_shop WHERE shop_id = ? ORDER BY id")) {
            ps.setInt(1, shop.id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TabShop tab = new TabShop();
                    tab.shop = shop;
                    tab.id = rs.getInt("id");
                    tab.name = rs.getString("name").replace("<>", "\n");
                    loadItemsStrict(con, tab);
                    shop.tabShops.add(tab);
                }
            }
        }
    }

    private static void loadItemsStrict(Connection con, TabShop tab) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM item_shop WHERE is_sell = 1 AND tab_id = ? ORDER BY sort_order ASC, id ASC")) {
            int tabId = tab.id;
            if (tabId >= 41 && tabId <= 43) tabId -= 31;
            ps.setInt(1, tabId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ItemShop item = new ItemShop();
                    item.tabShop = tab;
                    item.id = rs.getInt("id");
                    item.temp = ItemService.gI().getTemplate(rs.getShort("temp_id"));
                    if (item.temp == null) throw new SQLException("Unknown item template: " + rs.getInt("temp_id"));
                    item.isNew = rs.getBoolean("is_new");
                    item.cost = rs.getInt("cost");
                    item.iconSpec = rs.getInt("icon_spec");
                    item.typeSell = rs.getByte("type_sell");
                    try (PreparedStatement optionPs = con.prepareStatement("SELECT * FROM item_shop_option WHERE item_shop_id = ?")) {
                        optionPs.setInt(1, item.id);
                        try (ResultSet options = optionPs.executeQuery()) {
                            while (options.next()) item.options.add(new Item.ItemOption(options.getInt("option_id"), options.getInt("param")));
                        }
                    }
                    tab.itemShops.add(item);
                }
            }
        }
    }

    public static List<Shop> getShops(Connection con) {
        List<Shop> list = new ArrayList<>();
        try {
            PreparedStatement ps = con.prepareStatement("select * from shop order by npc_id asc");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Shop shop = new Shop();
                shop.id = rs.getInt("id");
                shop.npcId = rs.getByte("npc_id");
                shop.tagName = rs.getString("tag_name");
                shop.typeShop = rs.getByte("type_shop");
                loadShopTab(con, shop);
                list.add(shop);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
        return list;
    }

    private static void loadShopTab(Connection con, Shop shop) {
        try {
            PreparedStatement ps = con.prepareStatement("select * from tab_shop where shop_id = ? order by id");
            ps.setInt(1, shop.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TabShop tab = new TabShop();
                tab.shop = shop;
                tab.id = rs.getInt("id");
                tab.name = rs.getString("name").replaceAll("<>", "\n");
                loadItemShop(con, tab);
                shop.tabShops.add(tab);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }

    private static void loadItemShop(Connection con, TabShop tabShop) {
        try {
            PreparedStatement ps = con.prepareStatement("select * from item_shop where is_sell = 1 and tab_id = ? "
                    + "order by sort_order asc, id asc");
            int id = tabShop.id;
            if (id >= 41 && id <= 43){//10,11,12
                id -= 31;
            }
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ItemShop itemShop = new ItemShop();
                itemShop.tabShop = tabShop;
                itemShop.id = rs.getInt("id");
                itemShop.temp = ItemService.gI().getTemplate(rs.getShort("temp_id"));
                itemShop.isNew = rs.getBoolean("is_new");
                itemShop.cost = rs.getInt("cost");
                itemShop.iconSpec = rs.getInt("icon_spec");
                itemShop.typeSell = rs.getByte("type_sell");
                loadItemShopOption(con, itemShop);
                tabShop.itemShops.add(itemShop);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }

    private static void loadItemShopOption(Connection con, ItemShop itemShop) {
        try {
            PreparedStatement ps = con.prepareStatement("select * from item_shop_option where item_shop_id = ?");
            ps.setInt(1, itemShop.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                itemShop.options.add(new Item.ItemOption(rs.getInt("option_id"), rs.getInt("param")));
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }
}
