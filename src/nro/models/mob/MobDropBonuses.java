package nro.models.mob;

import java.util.concurrent.ThreadLocalRandom;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.server.PanelBridge;
import nro.models.item.ItemTime;

/** Personal bonuses apply after panel rates; zero rates and map eligibility stay intact. */
public final class MobDropBonuses {
    private MobDropBonuses() {}
    public static Player owner(Player attacker) { return attacker instanceof Pet pet ? pet.master : attacker; }
    public static double chance(Player attacker, String category, String rule, double numerator, double denominator) {
        double base = PanelBridge.dropChance(category, rule, numerator, denominator);
        Player p = owner(attacker);
        if (p == null || !p.isPl() || "quest_items".equals(category)) return base;
        long luck = Math.max(0, p.nPoint.tlLuck) + (p.setClothes.gohan == 5 ? 150L : 0L);
        double factor = 1 + luck / 100d;
        if ("skh".equals(category) && p.itemTime.isUseCoBonLa
                && System.currentTimeMillis() - p.itemTime.lastTimeUseCoBonLa < ItemTime.TIME_CO_BON_LA) factor *= 1.2;
        return Math.min(100, Math.max(0, base * factor));
    }
    public static boolean roll(Player player, String category, String rule, double numerator, double denominator) {
        return ThreadLocalRandom.current().nextDouble(100) < chance(player, category, rule, numerator, denominator);
    }
    public static int gold(Player attacker, int base) {
        Player p = owner(attacker);
        if (p == null || !p.isPl() || base <= 0) return base;
        long bonus = Math.max(0, p.nPoint.tlGold) + (p.setClothes.gohan == 5 ? 30L : 0L);
        var intrinsic = p.playerIntrinsic.intrinsic;
        if (intrinsic != null && intrinsic.id == 23) bonus += Math.max(0, intrinsic.param1);
        return (int) Math.min(Integer.MAX_VALUE, base + (long) base * bonus / 100);
    }
}
