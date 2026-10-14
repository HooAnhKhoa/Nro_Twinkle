import java.sql.*;
import java.lang.reflect.Method;
import nro.models.data.LocalManager;
import nro.models.server.PanelBridge;
public class RegionalDropBridgeTest {
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println("PASS: "+name);}
    static boolean eq(double a,double b){return Math.abs(a-b)<0.000001;}
    public static void main(String[] args)throws Exception {
        try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
            check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
            Method poll=PanelBridge.class.getDeclaredMethod("poll");poll.setAccessible(true);
            s.executeUpdate("UPDATE panel_drop_rule SET percent_override=NULL");
            s.executeUpdate("UPDATE server_rates SET drop_multiplier=3,revision=revision+1 WHERE id=1");
            s.executeUpdate("UPDATE panel_drop_rate SET mode='multiplier',value=2");poll.invoke(null);
            check(eq(PanelBridge.dropChance("upgrade_stone","upgrade_stone.normal",2,100),12),"unmodified normal rule retains old multipliers");
            check(eq(PanelBridge.dropChance("upgrade_stone","upgrade_stone.cold",20,100),100),"unmodified cold chance remains capped");
            s.executeUpdate("UPDATE panel_drop_rule SET percent_override=7 WHERE rule_key='upgrade_stone.normal'");poll.invoke(null);
            check(eq(PanelBridge.dropChance("upgrade_stone","upgrade_stone.normal",2,100),7),"regional percent replaces shared multipliers");
            check(eq(PanelBridge.dropChance("upgrade_stone","upgrade_stone.cold",20,100),100),"sibling region unchanged");
            s.executeUpdate("UPDATE panel_drop_rule SET percent_override=0 WHERE rule_key='upgrade_stone.cold'");poll.invoke(null);
            check(!PanelBridge.rollDrop("upgrade_stone","upgrade_stone.cold",20,100),"zero regional chance never drops");
            s.executeUpdate("UPDATE panel_drop_rule SET percent_override=8 WHERE rule_key='porata.soul'");
            s.executeUpdate("UPDATE panel_drop_rule SET percent_override=6 WHERE rule_key='dragon_ball.regions'");poll.invoke(null);
            check(eq(PanelBridge.dropChance("porata","porata.soul",15,100),12),"existing additive clover bonus ratio retained");
            check(eq(PanelBridge.dropChance("dragon_ball","dragon_ball.regions",11,70),6.6),"existing truncated bonus retained");
            check(eq(PanelBridge.dropChance("gold","upgrade_stone.normal",1,20),30),"wrong-category key cannot use another override");
            check(eq(PanelBridge.dropChance("gold","missing",1,20),30),"unknown key preserves original chance");
            check(eq(PanelBridge.dropChance("gold","missing",1,0),0),"invalid denominator disabled");
            s.executeUpdate("UPDATE server_rates SET drop_multiplier=0 WHERE id=1");poll.invoke(null);
            check(!PanelBridge.rollDrop("upgrade_stone","upgrade_stone.normal",2,100),"global x0 disables custom rules");
            s.executeUpdate("UPDATE server_rates SET drop_multiplier=1 WHERE id=1");
            s.executeUpdate("UPDATE panel_drop_rate SET value=0 WHERE category='upgrade_stone'");poll.invoke(null);
            check(!PanelBridge.rollDrop("upgrade_stone","upgrade_stone.normal",2,100),"group x0 disables its custom rules");
            s.executeUpdate("UPDATE panel_drop_rate SET value=1");
            s.executeUpdate("UPDATE panel_drop_rule SET percent_override=100 WHERE rule_key='upgrade_stone.normal'");poll.invoke(null);
            check(PanelBridge.rollDrop("upgrade_stone","upgrade_stone.normal",2,100),"100 percent always succeeds");
            s.executeUpdate("UPDATE panel_drop_rule SET percent_override=NULL");poll.invoke(null);
            check(eq(PanelBridge.dropChance("upgrade_stone","upgrade_stone.normal",2,100),2),"reset restores original base chance");
            try(ResultSet r=s.executeQuery("SELECT heartbeat,applied_revision FROM panel_drop_runtime WHERE id=1")){r.next();check(r.getTimestamp(1)!=null&&r.getLong(2)>0,"regional runtime acknowledges revision");}
        }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);
    }
}
