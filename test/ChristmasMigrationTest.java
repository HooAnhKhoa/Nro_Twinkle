import java.sql.*;
import nro.models.event.ChristmasGifts;
import nro.models.data.LocalManager;
public class ChristmasMigrationTest {
 static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);System.out.println("PASS: "+text);}
 public static void main(String[] args)throws Exception{try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
  check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA only");ChristmasGifts.ensureData(c);ChristmasGifts.ensureData(c);
  try(ResultSet r=s.executeQuery("SELECT COUNT(*),MIN(s.npc_id),MIN(s.type_shop),MIN(i.cost),MIN(i.type_sell),MIN(i.temp_id),MIN(t.id) FROM shop s JOIN tab_shop t ON t.shop_id=s.id JOIN item_shop i ON i.tab_id=t.id WHERE s.tag_name='SANTA_NOEL'")){r.next();check(r.getInt(1)==1&&r.getInt(2)==39&&r.getInt(3)==0&&r.getInt(4)==20&&r.getInt(5)==1&&r.getInt(6)==649,"one stocking offer at normal Santa for existing 20-gem price");check(!java.util.Set.of(30,44,45,49,59,60,61,62).contains(r.getInt(7)),"new tab cannot use a special-currency or reward branch");}
  s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id SET i.cost=17 WHERE s.tag_name='SANTA_NOEL'");ChristmasGifts.ensureData(c);
  try(ResultSet r=s.executeQuery("SELECT i.cost FROM item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id WHERE s.tag_name='SANTA_NOEL'")){check(r.next()&&r.getInt(1)==17,"subsequent shop edits survive startup migration");}
  s.executeUpdate("UPDATE item_shop i JOIN tab_shop t ON t.id=i.tab_id JOIN shop s ON s.id=t.shop_id SET i.cost=20 WHERE s.tag_name='SANTA_NOEL'");
 }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);}
}
