package nro.models.services;

import nro.models.consts.ConstNpc;
import nro.models.intrinsic.Intrinsic;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.network.Message;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import nro.models.utils.Util;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.Objects;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import nro.models.item.Item;
import nro.models.server.Manager;

/**
 *
 * @author By Mr Blue
 *
 */
public class IntrinsicService {

    private static IntrinsicService I;
    private volatile Catalog catalog;
    private long catalogRevision = -1;
    private static final class Catalog {
        final Map<Integer,Intrinsic> byId;
        final List<Intrinsic> td,nm,xd;
        Catalog(Map<Integer,Intrinsic> source) throws SQLException {
            byId=Map.copyOf(source);
            List<Intrinsic> a=new ArrayList<>(),b=new ArrayList<>(),c=new ArrayList<>();
            for(Intrinsic value:source.values()) {
                if(value.gender==0||value.gender==3)a.add(value);
                if(value.gender==1||value.gender==3)b.add(value);
                if(value.gender==2||value.gender==3)c.add(value);
            }
            for(List<Intrinsic> list:List.of(a,b,c))if(list.size()<2||list.size()>256||list.get(0).id!=0)throw new SQLException("Invalid intrinsic catalog ordering or race coverage");
            td=List.copyOf(a);nm=List.copyOf(b);xd=List.copyOf(c);
        }
    }

    /** Publish complete immutable lists, keeping existing players' rolled values intact. */
    public synchronized void reloadCatalog(Connection connection) throws SQLException {
        long revision;
        try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery("SELECT revision FROM panel_intrinsic_template_config WHERE id=1")) {
            if(!rows.next())return;revision=rows.getLong(1);
        }
        if(catalog==null||revision!=catalogRevision) {
            Map<Integer,Intrinsic> next=new TreeMap<>();
            try(Statement statement=connection.createStatement();ResultSet rows=statement.executeQuery("SELECT * FROM intrinsic ORDER BY id")) {
                while(rows.next()) {
                    Intrinsic value=new Intrinsic();value.id=rows.getInt("id");value.name=rows.getString("NAME");
                    value.paramFrom1=rows.getShort("param_from_1");value.paramTo1=rows.getShort("param_to_1");
                    value.paramFrom2=rows.getShort("param_from_2");value.paramTo2=rows.getShort("param_to_2");
                    value.icon=rows.getShort("icon");value.gender=rows.getByte("gender");
                    if(value.id<0||value.id>127||value.name==null||value.gender<0||value.gender>3||value.paramFrom1<0||value.paramFrom2<0||value.paramFrom1>value.paramTo1||value.paramFrom2>value.paramTo2)throw new SQLException("Invalid intrinsic template "+value.id);
                    Intrinsic previous=next.putIfAbsent(value.id,value);
                    if(previous!=null&&(!Objects.equals(previous.name,value.name)||previous.gender!=value.gender||previous.icon!=value.icon||previous.paramFrom1!=value.paramFrom1||previous.paramTo1!=value.paramTo1||previous.paramFrom2!=value.paramFrom2||previous.paramTo2!=value.paramTo2))throw new SQLException("Conflicting intrinsic copies "+value.id);
                }
            }
            Catalog ready=new Catalog(next);catalog=ready;catalogRevision=revision;
        }
        try(PreparedStatement statement=connection.prepareStatement("UPDATE panel_intrinsic_template_config SET applied_revision=?,heartbeat=NOW() WHERE id=1")){statement.setLong(1,catalogRevision);statement.executeUpdate();}
    }
    private static final int[] COST_OPEN = {10, 20, 40, 80, 160, 320, 640, 1280};

    public static IntrinsicService gI() {
        if (IntrinsicService.I == null) {
            IntrinsicService.I = new IntrinsicService();
        }
        return IntrinsicService.I;
    }

    public List<Intrinsic> getIntrinsics(byte playerGender) {
        Catalog current=catalog;
        if(current!=null)return playerGender==0?current.td:playerGender==1?current.nm:current.xd;
        switch (playerGender) {
            case 0:
                return Manager.INTRINSIC_TD;
            case 1:
                return Manager.INTRINSIC_NM;
            default:
                return Manager.INTRINSIC_XD;
        }
    }

    public Intrinsic getIntrinsicById(int id) {
        Catalog current=catalog;
        if(current!=null){Intrinsic value=current.byId.get(id);return value==null?null:new Intrinsic(value);}
        for (Intrinsic intrinsic : Manager.INTRINSICS) {
            if (intrinsic.id == id) {
                return new Intrinsic(intrinsic);
            }
        }
        return null;
    }

    public void sendInfoIntrinsic(Player player) {
        Message msg;
        try {
            msg = new Message(112);
            msg.writer().writeByte(0);
            msg.writer().writeShort(player.playerIntrinsic.intrinsic.icon);
            msg.writer().writeUTF(player.playerIntrinsic.intrinsic.getName());
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void showAllIntrinsic(Player player) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        Message msg;
        try {
            msg = new Message(112);
            msg.writer().writeByte(1);
            msg.writer().writeByte(1); //count tab
            msg.writer().writeUTF("Nội tại");
            msg.writer().writeByte(listIntrinsic.size() - 1);
            for (int i = 1; i < listIntrinsic.size(); i++) {
                msg.writer().writeShort(listIntrinsic.get(i).icon);
                msg.writer().writeUTF(listIntrinsic.get(i).getDescription());
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void showMenu(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.INTRINSIC, -1,
                "Nội tại là một kỹ năng bị động hỗ trợ đặc biệt\nBạn có muốn mở hoặc thay đổi nội tại không?",
                "Xem\ntất cả\nNội Tại", "Mở\nNội Tại", "Mở VIP", "Từ chối");
    }

    public void showConfirmOpen(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.CONFIRM_OPEN_INTRINSIC, -1, "Bạn muốn đổi Nội Tại khác\nvới giá là "
                + COST_OPEN[player.playerIntrinsic.countOpen] + " Tr vàng ?", "Mở\nNội Tại", "Từ chối");
    }

    public void showConfirmOpenVip(Player player) {
        NpcService.gI().createMenuConMeo(player, ConstNpc.CONFIRM_OPEN_INTRINSIC_VIP, -1,
                "Bạn có muốn mở Nội Tại\nvới giá là 100 ngọc và\ntái lập giá vàng quay lại ban đầu không?", "Mở\nNội VIP", "Từ chối");
    }

    private void changeIntrinsic(Player player) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        player.playerIntrinsic.intrinsic = new Intrinsic(listIntrinsic.get(Util.nextInt(1, listIntrinsic.size() - 1)));
        player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom1, player.playerIntrinsic.intrinsic.paramTo1);
        player.playerIntrinsic.intrinsic.param2 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom2, player.playerIntrinsic.intrinsic.paramTo2);
        String name = player.playerIntrinsic.intrinsic.getName();
        int index = name.indexOf(" [");

        if (index != -1) {
            Service.gI().sendThongBao(player, "Bạn nhận được Nội tại:\n" + name.substring(0, index));
        } else {
            Service.gI().sendThongBao(player, "Bạn nhận được Nội tại:\n" + name);
        }

        sendInfoIntrinsic(player);
    }

    public void doinoitai(Player player) {
        List<Intrinsic> listIntrinsic = getIntrinsics(player.gender);
        player.playerIntrinsic.intrinsic = new Intrinsic(listIntrinsic.get(Util.nextInt(1, listIntrinsic.size() - 1)));
        player.playerIntrinsic.intrinsic.param1 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom1, player.playerIntrinsic.intrinsic.paramTo1);
        player.playerIntrinsic.intrinsic.param2 = (short) Util.nextInt(player.playerIntrinsic.intrinsic.paramFrom2, player.playerIntrinsic.intrinsic.paramTo2);
        Service.gI().sendThongBao(player, "Bạn nhận được Nội tại:\n" + player.playerIntrinsic.intrinsic.getName().substring(0, player.playerIntrinsic.intrinsic.getName().indexOf(" [")));
        sendInfoIntrinsic(player);
    }

    public void open(Player player) {
        if (player.nPoint.power >= 10000000000L) {
            int goldRequire = COST_OPEN[player.playerIntrinsic.countOpen] * 1000000;
            if (player.inventory.gold >= goldRequire) {
                player.inventory.gold -= goldRequire;
                PlayerService.gI().sendInfoHpMpMoney(player);
                changeIntrinsic(player);
                if (player.playerIntrinsic.countOpen < COST_OPEN.length - 1) {
                    player.playerIntrinsic.countOpen++;
                }
            } else {
                Service.gI().sendThongBao(player, "Bạn không đủ vàng, còn thiếu "
                        + Util.numberToMoney(goldRequire - player.inventory.gold) + " vàng nữa");
            }
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu sức mạnh tối thiểu 10 tỷ");
        }
    }

    public void openVip(Player player) {
        if (player.nPoint.power >= 10000000000L) {
            int gemRequire = 100;
            if (player.inventory.gem >= 100) {
                player.inventory.gem -= gemRequire;
                PlayerService.gI().sendInfoHpMpMoney(player);
                changeIntrinsic(player);
                player.playerIntrinsic.countOpen = 0;
            } else {
                Service.gI().sendThongBao(player, "Bạn không có đủ ngọc, còn thiếu "
                        + (gemRequire - player.inventory.gem) + " ngọc nữa");
            }
        } else {
            Service.gI().sendThongBao(player, "Yêu cầu sức mạnh tối thiểu 10 tỷ");
        }
    }

}
