package nro.models.combine;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;

/** Porata materials support normal quantity stacks and legacy option-31 totals. */
final class PorataMaterials {
    private PorataMaterials() {}
    private static Item.ItemOption legacy(Item item) {
        for (var option : item.itemOptions) if (option.optionTemplate.id == 31) return option;
        return null;
    }
    private static int amount(Item item) {
        var option = legacy(item);
        return Math.max(0, option == null ? item.quantity : option.param);
    }
    static long count(Player player, int id) {
        long total = 0;
        for (Item item : player.inventory.itemsBag)
            if (item != null && item.isNotNullItem() && item.template.id == id && item.quantity > 0) total += amount(item);
        return total;
    }
    static boolean consume(Player player, int id, int count) {
        synchronized (player) {
            if (count < 1 || count(player, id) < count) return false;
            int remaining = count;
            for (int slot = 0; slot < player.inventory.itemsBag.size() && remaining > 0; slot++) {
                Item item = player.inventory.itemsBag.get(slot);
                if (item == null || !item.isNotNullItem() || item.template.id != id || item.quantity < 1) continue;
                int take = Math.min(remaining, amount(item));
                var option = legacy(item);
                if (option == null) InventoryService.gI().subQuantityItemsBag(player, item, take);
                else {
                    option.param -= take;
                    if (option.param == 0) player.inventory.itemsBag.set(slot, new Item());
                }
                remaining -= take;
            }
            return true;
        }
    }
    static void refreshSelection(Player player) {
        player.combineNew.itemsCombine.removeIf(item -> item == null || !item.isNotNullItem()
            || item.quantity <= 0 || !player.inventory.itemsBag.contains(item));
    }
    static boolean selected(Player player, int... ids) {
        if (player.combineNew.itemsCombine.size() != ids.length) return false;
        for (int id : ids) {
            long matches = player.combineNew.itemsCombine.stream().filter(item -> item != null && item.isNotNullItem()
                && item.template.id == id && item.quantity > 0 && player.inventory.itemsBag.contains(item)).count();
            if (matches != 1) return false;
        }
        return true;
    }
}
