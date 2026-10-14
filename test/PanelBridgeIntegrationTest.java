import java.sql.*;
import java.lang.reflect.Method;
import nro.models.boss.*;
import nro.models.consts.*;
import nro.models.data.LocalManager;
import nro.models.server.*;

/** Run only against the disposable nro_panel_qa_20261013 database. */
public class PanelBridgeIntegrationTest {
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);System.out.println("PASS: "+name);}
    public static void main(String[] args)throws Exception {
        try(Connection c=LocalManager.getConnection()) {
            check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
            Boss b=new Boss(-999999,new BossData("QA runtime boss",(byte)0,new short[]{297,298,299},10,new int[]{100},new int[]{1},new int[][]{},new String[]{},new String[]{},new String[]{},60));
            String key=b.getClass().getName();
            Method publish=PanelBridge.class.getDeclaredMethod("publishBosses",Connection.class);publish.setAccessible(true);publish.invoke(null,c);
            b.data[0].setSecondsRest(0);publish.invoke(null,c);
            try(PreparedStatement s=c.prepareStatement("SELECT rest_seconds FROM boss_web_config WHERE boss_key=?")){s.setString(1,key);try(ResultSet r=s.executeQuery()){r.next();check(r.getInt(1)==0,"zero rest is preserved instead of forced to one second");}}
            nro.models.map.Map map=new nro.models.map.Map(0,"QA Làng Aru",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[][]{{0}},new int[]{0},0,10,new java.util.ArrayList<>());
            b.zone=new nro.models.map.Zone(map,2,10);publish.invoke(null,c);
            try(PreparedStatement s=c.prepareStatement("SELECT current_locations FROM boss_web_config WHERE boss_key=?")){s.setString(1,key);try(ResultSet r=s.executeQuery()){r.next();com.google.gson.JsonObject location=new com.google.gson.JsonParser().parse(r.getString(1)).getAsJsonArray().get(0).getAsJsonObject();check(location.get("map_name").getAsString().equals("QA Làng Aru")&&location.get("zone_id").getAsInt()==2,"actual map name and zone are published");}}
            b.zone=null;publish.invoke(null,c);
            try(PreparedStatement s=c.prepareStatement("SELECT current_locations FROM boss_web_config WHERE boss_key=?")){s.setString(1,key);try(ResultSet r=s.executeQuery()){r.next();check(r.getString(1).equals("[]"),"location clears when boss leaves map");}}
            try(PreparedStatement s=c.prepareStatement("UPDATE boss_web_config SET hp=5000,damage=333,rest_seconds=600,enabled=1,customized=1 WHERE boss_key=?")){s.setString(1,key);s.executeUpdate();}
            try(Statement s=c.createStatement()){s.executeUpdate("UPDATE server_rates SET exp_rate=5,upgrade_1=100,star_1=0,star_9=100,drop_multiplier=0,revision=revision+1 WHERE id=1");}
            Method poll=PanelBridge.class.getDeclaredMethod("poll");poll.setAccessible(true);poll.invoke(null);
            PanelBridge.configure(b);check(b.data[0].getHp()[0]==5000&&b.data[0].getDame()==333&&b.data[0].getSecondsRest()==600,"boss configuration applied");
            b.currentLevel=0;PanelBridge.applySpawnStats(b);check(b.nPoint.hpMax==5000&&b.nPoint.dame==333,"spawn overrides specialized HP/damage");
            b.nPoint.hp=4000;PanelBridge.applySpawnStats(b);check(b.nPoint.hp==4000,"map transition does not heal boss twice");
            check(Manager.RATE_EXP_SERVER==5,"live EXP setting applied");check(PanelBridge.upgrade(0,80)==100,"upgrade chance applied");check(!PanelBridge.rollDrop(100,100),"zero multiplier prevents random drops");
            check(nro.models.combine.PhaLeHoaTrangBi.getRatio(0)==0&&nro.models.combine.PhaLeHoaTrangBi.getRatio(8)==100,"star chances applied independently at both boundaries");
            check(PanelBridge.star(9,17)==17,"unsupported star level keeps original fallback");
            try(Statement s=c.createStatement()){
                s.executeUpdate("UPDATE server_rates SET drop_multiplier=2 WHERE id=1");
                s.executeUpdate("UPDATE panel_drop_rate SET mode='multiplier',value=1");
                s.executeUpdate("UPDATE panel_drop_rate SET value=3 WHERE category='gold'");
                s.executeUpdate("UPDATE panel_drop_rate SET value=0 WHERE category='skh'");
            }
            poll.invoke(null);
            check(PanelBridge.dropChance("gold",1,20)==30,"gold group multiplies base chance and global factor");
            check(PanelBridge.dropChance("dragon_ball",1,100)==2,"gold group does not change dragon ball group");
            check(!PanelBridge.rollDrop("skh",100,100),"zero group multiplier disables guaranteed random roll");
            check(PanelBridge.dropChance("gold",100,100)==100,"effective drop chance caps at 100 percent");
            check(PanelBridge.dropChance("gold",1,0)==0,"invalid denominator cannot produce a drop");
            try(Statement s=c.createStatement()){s.executeUpdate("UPDATE server_rates SET drop_multiplier=1 WHERE id=1");}poll.invoke(null);
            try(Statement s=c.createStatement()){s.executeUpdate("INSERT INTO panel_skill_rate(skill_id,multiplier) VALUES(0,2) ON DUPLICATE KEY UPDATE multiplier=2");}poll.invoke(null);
            nro.models.player.Player p=new nro.models.player.Player();p.isPlayer=true;
            p.playerSkill.skillSelect=new nro.models.skill.Skill();p.playerSkill.skillSelect.template=new nro.models.player_system.Template.SkillTemplate();p.playerSkill.skillSelect.template.id=0;
            check(PanelBridge.skillDamage(p,100)==200,"player skill multiplier applied");
            check(PanelBridge.skillDamage(b,100)==100,"boss skill damage remains independent");
            try(Statement s=c.createStatement()){s.executeUpdate("UPDATE server_rates SET drop_multiplier=1 WHERE id=1");}poll.invoke(null);check(PanelBridge.rollDrop(100,100),"guaranteed drop preserved at x1");
            try(PreparedStatement s=c.prepareStatement("UPDATE boss_web_config SET enabled=0 WHERE boss_key=?")){s.setString(1,key);s.executeUpdate();}poll.invoke(null);check(!PanelBridge.enabled(b),"disabled boss recognized");
            try(PreparedStatement s=c.prepareStatement("UPDATE boss_web_config SET enabled=1 WHERE boss_key=?")){s.setString(1,key);s.executeUpdate();}
            long id;try(PreparedStatement s=c.prepareStatement("INSERT INTO boss_web_command(boss_key) VALUES(?)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,key);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();id=r.getLong(1);}}
            poll.invoke(null);PanelBridge.tickBoss(b);check(b.bossStatus==BossStatus.RESPAWN,"spawn queued on boss update thread");poll.invoke(null);
            try(PreparedStatement s=c.prepareStatement("SELECT completed_at FROM boss_web_command WHERE id=?")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){r.next();check(r.getTimestamp(1)!=null,"spawn acknowledgement stored");}}
            try(PreparedStatement s=c.prepareStatement("INSERT INTO boss_web_command(boss_key,requested_at) VALUES(?,NOW()-INTERVAL 60 SECOND)",Statement.RETURN_GENERATED_KEYS)){s.setString(1,key);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();id=r.getLong(1);}}
            poll.invoke(null);b.bossStatus=BossStatus.REST;PanelBridge.tickBoss(b);check(b.bossStatus==BossStatus.REST,"expired command cannot spawn later");
            System.out.println("Java bridge checks complete.");
        }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);
    }
}
