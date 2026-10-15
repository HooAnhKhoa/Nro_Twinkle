package nro.models.services;

import java.util.*;
import java.sql.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services_func.TransactionService;

/** Keep the existing byte protocol: client slots 0..99 map onto one of three server pages. */
public final class BagPages {
    public static final int PAGE_SIZE = 100;
    public static final int MENU = 202610180;
    public static final int SELL = 100;

    // An old confirmation must never act on a different item occupying the same client slot.
    private record Confirmation(Item item, int action, int where, int wire) {}
    private static final Map<Player, Confirmation> CONFIRMATIONS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** A full 300-item bag can exceed the old TEXT limit of 65,535 bytes. */
    public static void ensureStorage(Connection connection) throws SQLException {
        String alter;
        try (Statement query = connection.createStatement();
                ResultSet column = query.executeQuery("SHOW FULL COLUMNS FROM player LIKE 'items_bag'")) {
            if (!column.next()) throw new SQLException("Missing player.items_bag");
            String type = column.getString("Type").toLowerCase(Locale.ROOT);
            if (type.equals("longtext") || type.equals("mediumtext")) return;
            if (!type.equals("text") || column.getString("Default") != null
                    || !column.getString("Extra").isEmpty()) {
                throw new SQLException("Unsupported player.items_bag schema; migration refused");
            }
            String collation = column.getString("Collation");
            if (collation == null || !collation.matches("[a-zA-Z0-9_]+")) {
                throw new SQLException("Unexpected bag collation");
            }
            String charset = collation.substring(0, collation.indexOf('_'));
            String comment = column.getString("Comment").replace("'", "''");
            alter = "ALTER TABLE player MODIFY COLUMN items_bag LONGTEXT CHARACTER SET "
                    + charset + " COLLATE " + collation
                    + ("YES".equals(column.getString("Null")) ? " NULL DEFAULT NULL" : " NOT NULL")
                    + " COMMENT '" + comment + "'";
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(alter);
        }
    }

    public static int start(Player player) {
        int pages = Math.max(1, (player.inventory.itemsBag.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        player.inventory.bagPage = Math.max(0, Math.min(player.inventory.bagPage, pages - 1));
        return player.inventory.bagPage * PAGE_SIZE;
    }

    public static int count(Player player) {
        return Math.min(PAGE_SIZE, player.inventory.itemsBag.size() - start(player));
    }

    public static List<Item> visible(Player player) {
        int start = start(player);
        return new ArrayList<>(player.inventory.itemsBag.subList(start, start + count(player)));
    }

    public static int global(Player player, int wire) {
        return wire < 0 || wire >= count(player) ? -1 : start(player) + wire;
    }

    public static int local(Player player, int global) {
        int start = start(player);
        return global < start || global >= start + count(player) ? -1 : global - start;
    }

    public static void remember(Player player, Item item, int action, int where, int wire) {
        CONFIRMATIONS.put(player, new Confirmation(item, action, where, wire));
    }

    public static boolean confirm(Player player, Item item, int action, int where, int wire) {
        Confirmation pending = CONFIRMATIONS.remove(player);
        return pending != null && pending.item == item && pending.action == action
                && pending.where == where && pending.wire == wire;
    }

    public static void menu(Player player) {
        int size = player.inventory.itemsBag.size();
        String[] choices = new String[4];
        for (int i = 0; i < 3; i++) {
            int slots = Math.max(0, Math.min(PAGE_SIZE, size - i * PAGE_SIZE));
            choices[i] = "Trang " + (i + 1) + "\n" + (slots > 0 ? slots + " ô" : "Chưa mở");
        }
        choices[3] = "Đóng";
        NpcService.gI().createMenuConMeo(player, MENU, -1,
                "Túi đồ: " + size + "/300 ô. Trang hiện tại: " + (start(player) / PAGE_SIZE + 1)
                + ".\nChat tui 1, tui 2 hoặc tui 3 để chuyển nhanh. Mua mở rộng hành trang tại Santa nếu cần thêm ô.", choices);
    }

    public static boolean select(Player player, int page) {
        synchronized (player) {
            if (page < 1 || page > 3 || (page - 1) * PAGE_SIZE >= player.inventory.itemsBag.size()) {
                Service.gI().sendThongBao(player, "Trang này chưa được mở. Hãy mở rộng hành trang tại Santa.");
                return false;
            }
            if (TransactionService.hasTrade(player)) {
                Service.gI().sendThongBao(player, "Hãy kết thúc giao dịch trước khi chuyển trang túi.");
                return false;
            }
            CONFIRMATIONS.remove(player);
            player.inventory.bagPage = page - 1;
            if (player.combineNew != null && !player.combineNew.itemsCombine.isEmpty()) {
                player.combineNew.clearItemCombine();
                nro.models.combine.CombineService.gI().reOpenItemCombine(player);
            }
            InventoryService.gI().sendItemBags(player);
            int pages = Math.max(1, (player.inventory.itemsBag.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            Service.gI().sendThongBao(player, "Túi đồ — trang " + page + "/" + pages
                    + ", " + count(player) + " ô đang mở.");
            return true;
        }
    }

    public static boolean command(Player player, String text) {
        String command = text.trim().toLowerCase(Locale.ROOT);
        if (command.equals("tui") || command.equals("/tui")) {
            menu(player);
            return true;
        }
        if (command.matches("/?tui ?[123]")) {
            select(player, command.charAt(command.length() - 1) - '0');
            return true;
        }
        return false;
    }

    public static boolean showItemPage(Player player, Item item) {
        int index = player.inventory.itemsBag.indexOf(item);
        return index >= 0 && (local(player, index) >= 0 || select(player, index / PAGE_SIZE + 1));
    }

    private BagPages() {}
}
