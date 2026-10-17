package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import nro.models.clan.Clan;
import nro.models.data.LocalManager;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.Service;
import static nro.models.event.trungthu2026.TrungThuConfig.*;

public final class TrungThuClan {
    private record Session(String day,Zone zone,Set<Long> roster,Set<Long> checked,long expires){}
    private static final Map<String,Session> SESSIONS=new HashMap<>();
    static boolean equipped(Player p){return p.inventory.itemsBody.stream().anyMatch(i->i.isNotNullItem()&&i.template.type==11&&(Arrays.stream(numbers("clan.lantern-ids")).anyMatch(id->id==i.template.id)||Arrays.stream(numbers("clan.lantern-parts")).anyMatch(part->part==i.template.part)));}
    public static synchronized void click(Player p,String kind){
        if(!Set.of("parade","attendance").contains(kind)||!nro.models.event.EventManager.allowNpc(p,"trung_thu"))return;
        if(p.clan==null||p.zone==null||p.zone.map.mapId!=number("map.clan-area")||!equipped(p)){Service.gI().sendThongBao(p,"Cần ở Lãnh địa Bang Hội và đeo lồng đèn/cờ lồng đèn.");return;}
        Clan clan=p.clan;String key=clan.id+":"+kind;String day=TrungThuStore.today();long now=System.currentTimeMillis();Set<Long> awarded=Set.of();
        try(Connection c=LocalManager.getConnection()){
            if(TrungThuStore.claimed(c,clan.id,kind)){Service.gI().sendThongBao(p,"Bang đã nhận thưởng hoạt động này hôm nay.");return;}
            Session session=SESSIONS.get(key);
            if(session==null||!session.day.equals(day)||session.expires<now){
                boolean leader=clan.members.stream().anyMatch(m->m.id==p.id&&m.role==Clan.LEADER);
                if(!leader){Service.gI().sendThongBao(p,"Chờ bang chủ bắt đầu hoạt động Trung Thu.");return;}
                Set<Long> ids=new HashSet<>();for(Player member:new ArrayList<>(clan.membersInGame))if(member.zone==p.zone&&!member.isDie()&&equipped(member))ids.add(member.id);
                if(ids.isEmpty())return;session=new Session(day,p.zone,Set.copyOf(ids),new HashSet<>(),now+number("clan.session-seconds")*1000L);SESSIONS.put(key,session);
            }
            if(session.zone!=p.zone||!session.roster.contains(p.id)){Service.gI().sendThongBao(p,"Bạn không thuộc danh sách lúc bắt đầu lượt này. Tham gia lượt sau.");return;}
            Set<Long> stillEligible=new HashSet<>();for(Player member:new ArrayList<>(clan.membersInGame))if(member.zone==session.zone&&!member.isDie()&&equipped(member))stillEligible.add(member.id);
            if(!stillEligible.containsAll(session.roster)){SESSIONS.remove(key);Service.gI().sendThongBao(p,"Lượt bị hủy vì thành viên rời khu hoặc tháo lồng đèn. Bang chủ có thể bắt đầu lại.");return;}
            session.checked.add(p.id);
            if(!session.checked.containsAll(session.roster)){Service.gI().sendThongBao(p,"Đã xác nhận "+session.checked.size()+"/"+session.roster.size()+" thành viên.");return;}
            c.setAutoCommit(false);
            try{
                TrungThuStore.receipt(c,clan.id,kind);
                long until=now+number(kind.equals("parade")?"buff.parade-minutes":"buff.attendance-minutes")*60000L;
                try(PreparedStatement update=c.prepareStatement("INSERT INTO trung_thu_2026_state(player_id,double_until) VALUES(?,?) ON DUPLICATE KEY UPDATE double_until=GREATEST(double_until,VALUES(double_until))")){
                    for(long id:session.roster){update.setLong(1,id);update.setLong(2,until);update.addBatch();}update.executeBatch();
                }c.commit();
            }catch(SQLException error){c.rollback();throw error;}
            awarded=session.roster;SESSIONS.remove(key);
        }catch(SQLException error){nro.models.utils.Logger.logException(TrungThuClan.class,error);Service.gI().sendThongBao(p,"Chưa lưu được thưởng bang hội, hãy thử lại.");return;}
        for(long id:awarded)TrungThuStore.invalidate(id);
        Service.gI().sendThongBao(p,"Các thành viên tham gia đã nhận X2 EXP 1 giờ. Mỗi loại hoạt động nhận một lần/ngày.");
    }
    private TrungThuClan(){}
}
