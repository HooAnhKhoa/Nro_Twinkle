package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.consts.ConstTask;
import nro.models.services.AchievementService;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.TaskService;
import nro.models.services_func.Input;

/**
 *
 * @author By Mr Blue
 *
 */
public class BoMong extends Npc {
    private static final int HOME_EXCHANGE = 202610210;

    public BoMong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    private boolean isHome() { return mapId == 21 || mapId == 22 || mapId == 23; }

    private boolean supportsServices() { return mapId == 47 || mapId == 84; }

    @Override
    public void openBaseMenu(Player player) {
        if (isHome()) {
            if (canOpenNpc(player)) createOtherMenu(player, HOME_EXCHANGE,
                    "Đổi số dư VNĐ của tài khoản thành thỏi vàng hoặc ngọc xanh.\n10.000 VNĐ = 40 thỏi vàng hoặc 10.000 ngọc xanh.\nMỗi 10.000 VNĐ đổi thêm 5.000 điểm Quy Lão.",
                    "Đổi\nthỏi vàng", "Đổi\nngọc xanh", "Đóng");
            return;
        }
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            if (canOpenNpc(player)) {
                if (supportsServices()) {
                    this.createOtherMenu(player, ConstNpc.BASE_MENU,
                            "Ngươi muốn có thêm ngọc thì chịu khó làm vài nhiệm vụ sẽ được ngọc thưởng", "Nhiệm vụ\nhàng ngày", "Nhiệm vụ\nthành tích", "Nạp Ngọc", "Từ chối");
                }
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (isHome()) {
                if (player.idMark.getIndexMenu() == HOME_EXCHANGE) {
                    if (select == 0) Input.gI().createFormTradeGold(player);
                    else if (select == 1) Input.gI().createFormTradeGem(player);
                }
                return;
            }
            if (supportsServices()) {
                if (player.idMark.isBaseMenu()) {
                    switch (select) {
                        case 0 -> {
                            if (player.playerTask.sideTask.template != null) {
                                String npcSay = "Nhiệm vụ hiện tại: " + player.playerTask.sideTask.getName() + " ("
                                        + player.playerTask.sideTask.getLevel() + ")"
                                        + "\nHiện tại đã hoàn thành: " + player.playerTask.sideTask.count + "/"
                                        + player.playerTask.sideTask.maxCount + " ("
                                        + player.playerTask.sideTask.getPercentProcess() + "%)\nSố nhiệm vụ còn lại trong ngày: "
                                        + player.playerTask.sideTask.leftTask + "/" + ConstTask.MAX_SIDE_TASK;
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_PAY_SIDE_TASK,
                                        npcSay, "Trả nhiệm\nvụ", "Hủy nhiệm\nvụ");
                            } else {
                                this.createOtherMenu(player, ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK,
                                        "Tôi có vài nhiệm vụ theo cấp bậc, "
                                        + "sức cậu có thể làm được cái nào?",
                                        "Dễ", "Bình thường", "Khó", "Siêu khó", "Từ chối");
                            }
                        }
                        case 1 -> {
                            AchievementService.gI().openAchievementUI(player);
                        }
                        case 2 -> {
                            Input.gI().createFormTradeGem(player);
                        }
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_LEVEL_SIDE_TASK) {
                    switch (select) {
                        case 0, 1, 2, 3 ->
                            TaskService.gI().changeSideTask(player, (byte) select);
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_PAY_SIDE_TASK) {
                    switch (select) {
                        case 0 ->
                            TaskService.gI().paySideTask(player);
                        case 1 ->
                            TaskService.gI().removeSideTask(player);
                    }
                }
            }
        }
    }
}
