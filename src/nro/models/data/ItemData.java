package nro.models.data;

import nro.models.player_system.Template.ArrHead2Frames;
import nro.models.player_system.Template.ItemOptionTemplate;
import nro.models.server.Manager;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player_system.Template;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemData {

    public static void updateItem(MySession session) {
        updateItemOptionItemplate(session);
        updateItemArrHead2FTemplate(session);
        sendItemTemplates(session);
    }

    private static void updateItemOptionItemplate(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);
            msg.writer().writeByte(8);
            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(0); //update option
            msg.writer().writeByte(Manager.ITEM_OPTION_TEMPLATES.size());
            for (ItemOptionTemplate io : Manager.ITEM_OPTION_TEMPLATES) {
                msg.writer().writeUTF(io.name);
                msg.writer().writeByte(io.type);
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // -28 has a two-byte payload length. Bound the actual encoded bytes, not the item count.
    static List<Message> itemTemplateMessages() throws IOException {
        final int limit = 60000;
        List<Message> packets = new ArrayList<>();
        int start = 0;
        while (start < Manager.ITEM_TEMPLATES.size()) {
            boolean first = start == 0;
            ByteArrayOutputStream records = new ByteArrayOutputStream();
            int end = start;
            while (end < Manager.ITEM_TEMPLATES.size() && (!first || end < 750)) {
                Template.ItemTemplate item = Manager.ITEM_TEMPLATES.get(end);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(bytes);
                out.writeByte(item.type);
                out.writeByte(item.gender);
                out.writeUTF(item.name);
                out.writeUTF(item.description);
                out.writeByte(item.level);
                out.writeInt(item.strRequire);
                out.writeShort(item.iconID);
                out.writeShort(item.part);
                out.writeBoolean(item.isUpToUp);
                byte[] record = bytes.toByteArray();
                int header = first ? 5 : 7;
                if (header + record.length > limit) {
                    throw new IOException("Item template too large: " + item.id);
                }
                if (header + records.size() + record.length > limit) break;
                records.write(record);
                end++;
            }
            Message msg = new Message(-28);
            msg.writer().writeByte(8);
            msg.writer().writeByte(DataGame.vsItem);
            msg.writer().writeByte(first ? 1 : 2);
            if (first) {
                msg.writer().writeShort(end);
            } else {
                msg.writer().writeShort(start);
                msg.writer().writeShort(end);
            }
            msg.writer().write(records.toByteArray());
            packets.add(msg);
            start = end;
        }
        return packets;
    }

    private static void sendItemTemplates(MySession session) {
        try {
            for (Message msg : itemTemplateMessages()) {
                session.doSendMessage(msg);
                msg.cleanup();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateItemArrHead2FTemplate(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);
            msg.writer().writeByte(8);
            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(100);
            msg.writer().writeShort(Manager.ARR_HEAD_2_FRAMES.size());
            for (ArrHead2Frames io : Manager.ARR_HEAD_2_FRAMES) {
                msg.writer().writeByte(io.frames.size());
                for (int i : io.frames) {
                    msg.writer().writeShort(i);
                }
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
