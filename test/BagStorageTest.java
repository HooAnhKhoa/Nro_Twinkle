import java.sql.*;
import nro.models.data.LocalManager;
import nro.models.services.BagProtocol;

public class BagStorageTest {
    static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);System.out.println("PASS: "+text);}
    public static void main(String[] args)throws Exception {
        try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
            check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
            // Connection-local shadow table: the actual QA player table is never altered.
            s.execute("CREATE TEMPORARY TABLE player(id INT PRIMARY KEY,items_bag TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT 'Túi đồ')");
            String before="Đá nâng cấp ".repeat(2500);
            try(PreparedStatement q=c.prepareStatement("INSERT INTO player VALUES(1,?)")){q.setString(1,before);q.executeUpdate();}
            BagProtocol.ensureStorage(c);BagProtocol.ensureStorage(c);
            try(ResultSet r=s.executeQuery("SHOW FULL COLUMNS FROM player LIKE 'items_bag'")){r.next();check("longtext".equals(r.getString("Type"))&&"NO".equals(r.getString("Null"))&&"utf8mb4_unicode_ci".equals(r.getString("Collation"))&&"Túi đồ".equals(r.getString("Comment")),"migration is idempotent and retains nullability, Vietnamese collation and comment");}
            try(ResultSet r=s.executeQuery("SELECT items_bag FROM player WHERE id=1")){r.next();check(before.equals(r.getString(1)),"existing Unicode inventory preserved exactly");}
            String large="Vật phẩm Trung Thu ".repeat(10000);
            try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=? WHERE id=1")){q.setString(1,large);q.executeUpdate();}
            try(ResultSet r=s.executeQuery("SELECT items_bag FROM player WHERE id=1")){r.next();check(large.equals(r.getString(1)),"inventory beyond 65KB saves without truncation");}
            s.execute("DROP TEMPORARY TABLE player");
        }
        System.out.println("PASS: 4 storage migration checks; only connection-local temporary table changed");System.exit(0);
    }
}
