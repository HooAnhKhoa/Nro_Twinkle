import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.intrinsic.Intrinsic;
import nro.models.services.IntrinsicService;
public class IntrinsicCatalogBridgeTest {
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS: "+name);}
    public static void main(String[] args)throws Exception {
        try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
            check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
            IntrinsicService service=IntrinsicService.gI();service.reloadCatalog(c);
            for(byte race=0;race<3;race++){
                List<Intrinsic> list=service.getIntrinsics(race);Set<Integer> ids=new HashSet<>();
                for(Intrinsic value:list){check(ids.add(value.id),"unique template ID for race "+race+": "+value.id);check(value.gender==race||value.gender==3,"correct race eligibility");}
                check(list.get(0).id==0&&list.stream().filter(v->v.id==0).count()==1,"single sentinel at first slot");
                check(list.stream().anyMatch(v->v.id==23),"shared templates available to race "+race);
            }
            Intrinsic previous=service.getIntrinsicById(16);previous.param1=19;short oldFrom=previous.paramFrom1;
            s.executeUpdate("UPDATE intrinsic SET param_from_1=33,param_to_1=33 WHERE id=16");s.executeUpdate("UPDATE panel_intrinsic_template_config SET revision=revision+1 WHERE id=1");service.reloadCatalog(c);
            Intrinsic next=service.getIntrinsicById(16);
            check(next.paramFrom1==33&&next.paramTo1==33,"reload changes fresh intrinsic roll bounds");
            check(service.getIntrinsics((byte)2).stream().anyMatch(v->v.id==16&&v.getDescription().contains("33%")),"in-game list uses new description bounds");
            check(previous.param1==19&&previous.paramFrom1==oldFrom,"existing rolled intrinsic unchanged");
            next.paramFrom1=1;check(service.getIntrinsicById(16).paramFrom1==33,"ID lookup returns independent copies");
            boolean immutable=false;try{service.getIntrinsics((byte)2).clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"published race lists immutable");
            long oldAck;try(ResultSet r=s.executeQuery("SELECT applied_revision FROM panel_intrinsic_template_config WHERE id=1")){r.next();oldAck=r.getLong(1);}
            s.executeUpdate("INSERT INTO intrinsic SELECT * FROM intrinsic WHERE id=16 LIMIT 1");s.executeUpdate("UPDATE intrinsic SET param_to_1=34 WHERE id=16 LIMIT 1");s.executeUpdate("UPDATE panel_intrinsic_template_config SET revision=revision+1 WHERE id=1");
            boolean conflict=false;try{service.reloadCatalog(c);}catch(SQLException expected){conflict=true;}check(conflict,"conflicting duplicate templates rejected");
            check(service.getIntrinsicById(16).paramTo1==33,"failed reload keeps complete previous snapshot");
            try(ResultSet r=s.executeQuery("SELECT applied_revision FROM panel_intrinsic_template_config WHERE id=1")){r.next();check(r.getLong(1)==oldAck,"failed revision not acknowledged");}
            s.executeUpdate("UPDATE intrinsic SET param_from_1=5,param_to_1=25 WHERE id=16");s.executeUpdate("UPDATE panel_intrinsic_template_config SET revision=revision+1 WHERE id=1");service.reloadCatalog(c);
            check(service.getIntrinsicById(16).paramFrom1==5,"valid revision recovers after conflict");
            try(ResultSet r=s.executeQuery("SELECT revision,applied_revision,heartbeat FROM panel_intrinsic_template_config WHERE id=1")){r.next();check(r.getLong(1)==r.getLong(2)&&r.getTimestamp(3)!=null,"runtime publishes applied catalog revision");}
        }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);
    }
}
