package nro.models.services;

import java.util.*;
import java.sql.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player.Inventory;

/** Native one-byte bag indices, with a single bag of up to 150 slots. */
public final class BagProtocol {
    public static final int SELL = 100;
    private record Confirmation(Item item, int action, int where, int wire) {}
    private static final Map<Player, Confirmation> CONFIRMATIONS = Collections.synchronizedMap(new WeakHashMap<>());
    public static List<Item> items(Player player) {
        if (player.inventory.itemsBag.size() > Inventory.MAX_ITEMS_BAG) {
            throw new IllegalStateException("Inventory must be compacted before loading this runtime");
        }
        return player.inventory.itemsBag;
    }
    public static int index(Player player, int index) {
        return index >= 0 && index < player.inventory.itemsBag.size() && index < Inventory.MAX_ITEMS_BAG ? index : -1;
    }
    public static void remember(Player player, Item item, int action, int where, int wire) {
        CONFIRMATIONS.put(player, new Confirmation(item, action, where, wire));
    }
    public static boolean confirm(Player player, Item item, int action, int where, int wire) {
        Confirmation pending = CONFIRMATIONS.remove(player);
        return pending != null && pending.item == item && pending.action == action && pending.where == where && pending.wire == wire;
    }
    /** A heavily optioned bag can exceed the old TEXT limit of 65,535 bytes. */
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

    private BagProtocol() {}
}
