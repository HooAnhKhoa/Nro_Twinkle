package nro.models.event.trungthu2026;
import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player_badges.*;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import org.json.simple.*;
public class TrungThuTopStatsTest {
 static int checks;
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 public static void main(String[] args)throws Exception{
  try{
   TrungThuRuntimeTest.User p;
   try(Connection c=LocalManager.getConnection()){
    ok("nro_trungthu_rules_20261017".equals(c.getCatalog()),"QA DB guard");
    try(Statement q=c.createStatement()){
     q.executeUpdate("DELETE FROM player WHERE id=2026101698");
     try(ResultSet r=q.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){ItemTemplate t=new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),false,r.getInt("power_require"));Manager.ITEM_TEMPLATES.add(t);}}
     try(ResultSet r=q.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
    }
    p=TrungThuRuntimeTest.user(c,98,10);
   }
   p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;p.nPoint.calPoint();
   int hp=p.nPoint.hpMax,ki=p.nPoint.mpMax;long sd=p.nPoint.dame;
   Item pet=TrungThuRuntimeTest.item(2059,1);for(int id:new int[]{50,77,103})pet.itemOptions.add(new Item.ItemOption(id,30));
   for(int i=0;i<8;i++)p.inventory.itemsBody.add(new Item());p.inventory.itemsBody.set(7,pet);p.nPoint.calPoint();
   ok(p.nPoint.hpMax==hp*130/100&&p.nPoint.mpMax==ki*130/100&&p.nPoint.dame==sd*130/100,"pet adds 30 percent real HP KI damage");
   p.inventory.itemsBody.set(7,new Item());p.nPoint.calPoint();
   Item title=TrungThuRewards.item(1885,1,365);for(int id:new int[]{50,77,103})title.itemOptions.add(new Item.ItemOption(id,15));p.inventory.itemsBag.set(0,title);
   ok(TrungThuTopTitles.use(p,title),"title usable while event off");
   ok(TrungThuRuntimeTest.qty(p,1885)==0&&p.dataBadges.size()==1,"exact title consumed once");
   ok(p.nPoint.hpMax==hp*115/100&&p.nPoint.mpMax==ki*115/100&&p.nPoint.dame==sd*115/100,"title adds 15 percent real HP KI damage");
   BadgesData b=p.dataBadges.get(0);ok(b.timeofUseBadges>System.currentTimeMillis()+364*86400000L,"365 day title not truncated");
   try(Connection c=LocalManager.getConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT dataBadges,items_bag FROM player WHERE id="+p.id)){
    r.next();JSONArray list=(JSONArray)JSONValue.parse(r.getString(1));JSONObject row=(JSONObject)list.get(0);ok(((JSONArray)row.get("rewardOptions")).size()==3,"badge options persisted as structured JSON");ok(!r.getString(2).contains("1885"),"consumed bag saved with badge");
   }
   Item duplicate=TrungThuRewards.item(1885,1,365);p.inventory.itemsBag.set(1,duplicate);TrungThuTopTitles.use(p,duplicate);ok(TrungThuRuntimeTest.qty(p,1885)==1&&p.dataBadges.size()==1,"duplicate title not consumed or stacked");
   b.timeofUseBadges=System.currentTimeMillis()-1;p.nPoint.calPoint();ok(p.nPoint.hpMax==hp&&p.nPoint.mpMax==ki&&p.nPoint.dame==sd,"expired title stops adding all stats");
   Item vip=TrungThuRewards.item(2076,1,0);for(int id:new int[]{50,77,103})vip.itemOptions.add(new Item.ItemOption(id,15));p.inventory.itemsBag.set(2,vip);TrungThuTopTitles.use(p,vip);ok(p.dataBadges.size()==1&&p.dataBadges.get(0).timeofUseBadges==Long.MAX_VALUE,"permanent VIP title has no expiry and removes expired entry");
   ok(p.nPoint.hpMax==hp*115/100,"permanent title applies stats");
   System.out.println("PASS: "+checks+" top reward stat checks");
  }finally{LocalManager.close();}
 }
}
