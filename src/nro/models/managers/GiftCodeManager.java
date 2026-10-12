package nro.models.managers;
import nro.models.data.LocalManager;
import nro.models.player_system.GiftCode;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import nro.models.item.Item;
import nro.models.utils.Logger;
import nro.models.services.InventoryService;

public class GiftCodeManager {

    public String name;
    public final ArrayList<GiftCode> listGiftCode = new ArrayList<>();
    private long lastReloadId;

    private static GiftCodeManager instance;

    public static GiftCodeManager gI() {
        if (instance == null) {
            instance = new GiftCodeManager();
        }
        return instance;
    }

    public synchronized GiftCode checkUseGiftCode(Player player, String code) {
        for (GiftCode giftCode : listGiftCode) {
            if (giftCode.code.equals(code)) {
                if (giftCode.countLeft <= 0) {
                    Service.gI().sendThongBaoOK(player, "Giftcode đã hết");
                    return null;
                } else if (giftCode.isUsedGiftCode(player)) {
                    Service.gI().sendThongBaoOK(player, "Tham lam!");
                    return null;
                }
                if (InventoryService.gI().getCountEmptyBag(player) < giftCode.detail.size()) {
                    Service.gI().sendThongBaoOK(player, "Cần tối thiểu " + giftCode.detail.size() + " ô hành trang trống");
                    return null;
                }
                giftCode.countLeft -= 1;
                player.giftCode.add(code);
                updateGiftCode(giftCode);
                return giftCode;
            }
        }
        return null;
    }

    public void updateGiftCode(GiftCode giftcode) {
        try {
            LocalManager.executeUpdate("update giftcode set count_left = ? where id = ?", giftcode.countLeft, giftcode.id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void checkInfomationGiftCode(Player p) {
        StringBuilder sb = new StringBuilder();
        for (GiftCode giftCode : listGiftCode) {
            sb.append("Code: ").append(giftCode.code).append(", Số lượng còn lại: ").append(giftCode.countLeft).append("\b")
                    .append("Ngày tạo: ")
                    .append(giftCode.datecreate).append(", Ngày hết hạn: ").append(giftCode.dateexpired)
                    .append("\n");
        }
        sb.deleteCharAt(sb.length() - 1);
        NpcService.gI().createTutorial(p, 5073, sb.toString());
    }

    public void pollReloadRequests() {
        try {
            long requestId;
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ps = con.prepareStatement("SELECT id FROM giftcode_reload_request WHERE id > ? ORDER BY id DESC LIMIT 1")) {
                ps.setLong(1, lastReloadId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    requestId = rs.getLong(1);
                }
            }
            // Release the pool's only connection before reloadGiftCodes borrows it.
            int count = reloadGiftCodes();
            try (Connection con = LocalManager.getConnection();
                 PreparedStatement ack = con.prepareStatement("UPDATE giftcode_reload_request SET loaded_at = NOW(), code_count = ? WHERE id <= ? AND loaded_at IS NULL")) {
                ack.setInt(1, count);
                ack.setLong(2, requestId);
                ack.executeUpdate();
            }
            lastReloadId = requestId;
        } catch (SQLException e) {
            // The web page creates the signal table on its first update request.
            if (!"42S02".equals(e.getSQLState())) Logger.logException(GiftCodeManager.class, e);
        } catch (Exception e) {
            Logger.logException(GiftCodeManager.class, e);
        }
    }

    public synchronized int reloadGiftCodes() throws Exception {
        List<GiftCode> fresh = new ArrayList<>();
        try (Connection con = LocalManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM giftcode");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.id = rs.getInt("id");
                giftcode.code = rs.getString("code");
                giftcode.countLeft = rs.getInt("count_left");
                if (giftcode.countLeft == -1) giftcode.countLeft = 999999999;
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                JSONArray detail = (JSONArray) JSONValue.parse(rs.getString("detail"));
                if (detail != null) {
                    for (Object value : detail) {
                        JSONObject item = (JSONObject) value;
                        int id = Integer.parseInt(item.get("id").toString());
                        int quantity = Integer.parseInt(item.get("quantity").toString());
                        ArrayList<Item.ItemOption> options = new ArrayList<>();
                        JSONArray rawOptions = (JSONArray) item.get("options");
                        if (rawOptions != null) {
                            for (Object rawOption : rawOptions) {
                                JSONObject option = (JSONObject) rawOption;
                                options.add(new Item.ItemOption(
                                        Integer.parseInt(option.get("id").toString()),
                                        Integer.parseInt(option.get("param").toString())));
                            }
                        }
                        giftcode.option.put(id, options);
                        giftcode.detail.put(id, quantity);
                    }
                }
                fresh.add(giftcode);
            }
        }
        listGiftCode.clear();
        listGiftCode.addAll(fresh);
        return fresh.size();
    }

}
