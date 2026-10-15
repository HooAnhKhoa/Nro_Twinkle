import java.sql.*;
import nro.models.data.LocalManager;
import nro.models.event.ThanhCo;
public class ThanhCoMigrationTest {
 static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);System.out.println("PASS: "+text);}
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"isolated QA database");
   ThanhCo.ensureData(c);ThanhCo.ensureData(c);
   try(ResultSet r=s.executeQuery("SELECT i.TYPE,i.gender,i.NAME,i.icon_id,source.icon_id FROM item_template i JOIN item_template source ON source.id=1787 WHERE i.id=2000")){check(r.next()&&r.getInt(1)==75&&r.getInt(2)==3&&r.getString(3).equals("Vé Thành Cổ")&&r.getInt(4)==r.getInt(5),"new usable ticket reuses a real existing ticket icon");}
   try(ResultSet r=s.executeQuery("SELECT COUNT(*),MIN(i.cost),MIN(i.type_sell),MIN(i.is_sell) FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id WHERE s.tag_name='SHOP_CHI_CHI' AND i.temp_id=2000")){r.next();check(r.getInt(1)==1&&r.getInt(2)==5&&r.getInt(3)==1&&r.getInt(4)==1,"idempotent Chi Chi shop seed costs five green gems");}
   s.executeUpdate("UPDATE item_shop SET cost=7 WHERE temp_id=2000");ThanhCo.ensureData(c);
   try(ResultSet r=s.executeQuery("SELECT cost FROM item_shop WHERE temp_id=2000")){check(r.next()&&r.getInt(1)==7,"restart does not overwrite later shop price edits");}
   s.executeUpdate("UPDATE item_shop SET cost=5 WHERE temp_id=2000");
   s.executeUpdate("UPDATE item_template SET NAME='Reserved other item' WHERE id=2000");
   boolean refused=false;try{ThanhCo.ensureData(c);}catch(IllegalStateException expected){refused=true;}
   finally{s.executeUpdate("UPDATE item_template SET NAME='Vé Thành Cổ' WHERE id=2000");}
   check(refused,"conflicting item ID is rejected before touching another item");
  }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);
 }
}
