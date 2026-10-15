package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.Input;

/** QA database only; does not spend a real account's VND balance. */
public class VndExchangeTest {
 static final int BASE=2026101800;static int checks;
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class User extends TrungThuRuntimeTest.User {User(int id){super(id);}}
 static long scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static User user(Connection c,int n,int balance,int slots,boolean active)throws Exception{
  User p=new User(BASE+n);p.getSession().userId=BASE+n;p.getSession().vnd=balance;p.getSession().actived=active;p.inventory.gem=100;
  for(int i=0;i<slots;i++)p.inventory.itemsBag.add(new Item());
  try(PreparedStatement q=c.prepareStatement("INSERT INTO account(id,username,is_admin,vnd,active,last_time_login,last_time_logout) VALUES(?,?,0,?,?,'2000-01-01','2000-01-02') ON DUPLICATE KEY UPDATE vnd=VALUES(vnd),active=VALUES(active)")){q.setInt(1,BASE+n);q.setString(2,"vnd_qa_"+n);q.setInt(3,balance);q.setBoolean(4,active);q.executeUpdate();}
  try(PreparedStatement q=c.prepareStatement("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task,event_point) VALUES(?,?,?,0,'[0,0,0,0]','[]','[0,100,0,0,0]','[-1,0,0,0,20,0]',0) ON DUPLICATE KEY UPDATE account_id=VALUES(account_id),event_point=0")){q.setLong(1,p.id);q.setString(2,"VND QA "+n);q.setInt(3,BASE+n);q.executeUpdate();}return p;
 }
 static int qty(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 static long balance(Connection c,User p)throws Exception{return scalar(c,"SELECT vnd FROM account WHERE id="+p.getSession().userId);}
 public static void main(String[] args)throws Exception{
  try(Connection c=LocalManager.getConnection()){
   check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
   try(Statement s=c.createStatement()){
    for(String spec:List.of("account:active","player:event_point")){String[] part=spec.split(":");try(ResultSet r=s.executeQuery("SHOW COLUMNS FROM "+part[0]+" LIKE '"+part[1]+"'")){if(!r.next())try(Statement a=c.createStatement()){a.execute("ALTER TABLE "+part[0]+" ADD "+part[1]+" INT NOT NULL DEFAULT 0");}}}
    s.executeUpdate("DELETE FROM player WHERE id BETWEEN "+BASE+" AND "+(BASE+99));s.executeUpdate("DELETE FROM account WHERE id BETWEEN "+BASE+" AND "+(BASE+99));
    try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),r.getBoolean("is_up_to_up"),r.getInt("power_require")));}
    try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
   }
   User gold=user(c,1,50000,8,true);check(VndExchangeService.redeem(gold,10000,true).success(),"gold-bar exchange succeeds");
   check(qty(gold,457)==40&&qty(gold,718)==10&&gold.event.getEventPoint()==50,"10000 VND grants 40 bars, ten tickets and fifty points");
   check(balance(c,gold)==40000&&gold.getSession().vnd==40000&&scalar(c,"SELECT event_point FROM player WHERE id="+gold.id)==50,"database balance, session mirror and points update together");
   User gem=user(c,2,50000,8,false);gem.getSession().vnd=0;check(VndExchangeService.redeem(gem,10000,false).success()&&gem.inventory.gem==10100&&qty(gem,718)==10,"gem exchange uses actual DB balance even with stale zero session");
   check(balance(c,gem)==40000&&scalar(c,"SELECT JSON_EXTRACT(data_inventory,'$[1]') FROM player WHERE id="+gem.id)==10100,"gem reward persists with debit");
   User poor=user(c,3,5000,8,true);poor.getSession().vnd=100000;check(!VndExchangeService.redeem(poor,10000,true).success()&&balance(c,poor)==5000&&poor.getSession().vnd==5000&&qty(poor,457)==0,"stale high session cannot overspend the database balance");
   for(int invalid:new int[]{-1,0,9999,5000001})check(!VndExchangeService.redeem(poor,invalid,false).success()&&balance(c,poor)==5000,"reject amount outside original bounds: "+invalid);
   User inactive=user(c,4,50000,8,false);check(!VndExchangeService.redeem(inactive,10000,true).success()&&balance(c,inactive)==50000,"gold keeps original activated-account requirement");
   User packed=user(c,5,50000,1,true);check(!VndExchangeService.redeem(packed,10000,true).success()&&qty(packed,457)==0&&qty(packed,718)==0&&balance(c,packed)==50000,"partial gold/ticket bag addition rolls back without spending VND");
   packed.inventory.itemsBag.set(0,ItemService.gI().createNewItem((short)888,1));check(!VndExchangeService.redeem(packed,10000,false).success()&&packed.inventory.gem==100&&balance(c,packed)==50000,"full bag also rolls back gem reward and balance");
   User limit=user(c,6,50000,8,true);limit.inventory.gem=Integer.MAX_VALUE;check(!VndExchangeService.redeem(limit,10000,false).success()&&balance(c,limit)==50000&&qty(limit,718)==0,"gem overflow never debits VND");
   limit.event.setEventPoint(Integer.MAX_VALUE);check(!VndExchangeService.redeem(limit,10000,true).success()&&balance(c,limit)==50000,"point overflow never debits VND");
   User fractional=user(c,7,50000,8,true);check(VndExchangeService.redeem(fractional,15000,true).success()&&qty(fractional,457)==60&&qty(fractional,718)==10&&fractional.event.getEventPoint()==50,"non-round amounts keep existing integer reward ratios");
   User rollback=user(c,8,50000,8,true);
   try(Statement s=c.createStatement()){s.execute("DROP TRIGGER IF EXISTS qa_vnd_failure");s.execute("CREATE TRIGGER qa_vnd_failure BEFORE UPDATE ON player FOR EACH ROW BEGIN IF OLD.id="+rollback.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected QA VND failure'; END IF; END");}
   try{check(!VndExchangeService.redeem(rollback,10000,true).success()&&balance(c,rollback)==50000&&qty(rollback,457)==0&&qty(rollback,718)==0&&rollback.getSession().vnd==50000&&rollback.event.getEventPoint()==0,"storage failure restores DB debit, bag, balance mirror and points");}finally{try(Statement s=c.createStatement()){s.execute("DROP TRIGGER qa_vnd_failure");}}
   check(VndExchangeService.redeem(rollback,10000,true).success(),"storage failure can be retried after recovery");
   User mismatch=user(c,9,50000,8,true);try(Statement s=c.createStatement()){s.executeUpdate("UPDATE player SET account_id=1 WHERE id="+mismatch.id);}check(!VndExchangeService.redeem(mismatch,10000,true).success()&&balance(c,mismatch)==50000&&qty(mismatch,457)==0,"wrong character account cannot debit funds or gain rewards");
   User a=user(c,10,10000,8,true),b=user(c,11,10000,8,true);b.getSession().userId=a.getSession().userId;try(Statement s=c.createStatement()){s.executeUpdate("UPDATE player SET account_id="+a.getSession().userId+" WHERE id="+b.id);}
   ExecutorService pool=Executors.newFixedThreadPool(2);try{var left=pool.submit(()->VndExchangeService.redeem(a,10000,true));var right=pool.submit(()->VndExchangeService.redeem(b,10000,true));check(left.get().success()!=right.get().success()&&balance(c,a)==0,"concurrent requests cannot overspend one shared balance");}finally{pool.shutdownNow();}
   User input=user(c,12,50000,8,true);input.idMark.setTypeInput(Input.TRADE_GEM);var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);out.writeByte(1);out.writeUTF("10000");Input.gI().doInput(input,new Message((byte)-125,bytes.toByteArray()));check(input.inventory.gem==10100&&balance(c,input)==40000,"real input packet uses atomic VND exchange");
   nro.models.server.Maintenance.isRunning=true;try{check(!VndExchangeService.redeem(input,10000,true).success()&&balance(c,input)==40000,"maintenance blocks exchange without debit");}finally{nro.models.server.Maintenance.isRunning=false;}
   System.out.println("PASS: "+checks+" VND exchange checks; QA only");
  }finally{LocalManager.close();}
 }
}