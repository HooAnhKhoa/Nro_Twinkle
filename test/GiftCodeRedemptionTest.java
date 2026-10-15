package nro.models.event.trungthu2026;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.Input;
import nro.models.network.Message;

/** Production-like redemption against QA fixtures only; never redeems a live player's code. */
public class GiftCodeRedemptionTest {
    static final int BASE=2026101700;static int checks;
    static void check(boolean condition,String text){if(!condition)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
    static class User extends TrungThuRuntimeTest.User {
        byte[] form;String lastMessage;
        User(int id){super(id);}
        @Override public void sendMessage(Message m){super.sendMessage(m);if(m.command==-125)form=m.getData();}
    }
    static User user(Connection c,int n,int slots)throws Exception{
        User p=new User(BASE+n);p.inventory.gold=100;p.inventory.gem=100;p.inventory.ruby=100;
        for(int i=0;i<slots;i++)p.inventory.itemsBag.add(new Item());
        try(PreparedStatement q=c.prepareStatement("INSERT INTO player(id,name,account_id,gender,data_intrinsic,items_bag,data_inventory,data_side_task,giftcode) VALUES(?,?,1,0,'[0,0,0,0]','[]','[100,100,100,0,0]','[-1,0,0,0,20,0]','[]') ON DUPLICATE KEY UPDATE giftcode='[]'")){q.setLong(1,p.id);q.setString(2,"Gift QA "+n);q.executeUpdate();}return p;
    }
    static long scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    static void seed(Connection c,int n,String code,int count,String detail,String expiry)throws Exception{
        try(PreparedStatement q=c.prepareStatement("REPLACE INTO giftcode(id,code,count_left,detail,datecreate,expired) VALUES(?,?,?,?,'2000-01-01 00:00:00',?)")){q.setInt(1,BASE+n);q.setString(2,code);q.setInt(3,count);q.setString(4,detail);q.setString(5,expiry);q.executeUpdate();}
    }
    static String reward(int id,int qty){return "[{\"id\":"+id+",\"quantity\":"+qty+",\"options\":[{\"id\":30,\"param\":1}]}]";}
    static int qty(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
    public static void main(String[] args)throws Exception{
        try(Connection c=LocalManager.getConnection()){
            check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
            try(Statement s=c.createStatement()){
                s.execute("CREATE TABLE IF NOT EXISTS giftcode LIKE srcnrofree.giftcode");
                for(String column:List.of("detail","datecreate","expired"))try(ResultSet r=s.executeQuery("SHOW COLUMNS FROM giftcode LIKE '"+column+"'")){if(!r.next())try(Statement a=c.createStatement()){a.execute("ALTER TABLE giftcode ADD "+column+(column.equals("detail")?" LONGTEXT NULL":" DATETIME NULL"));}}
                try(ResultSet r=s.executeQuery("SHOW COLUMNS FROM player LIKE 'giftcode'")){if(!r.next())try(Statement a=c.createStatement()){a.execute("ALTER TABLE player ADD giftcode LONGTEXT NULL");}}
                s.executeUpdate("DELETE FROM giftcode WHERE id BETWEEN "+BASE+" AND "+(BASE+99));s.executeUpdate("DELETE FROM player WHERE id BETWEEN "+BASE+" AND "+(BASE+99));
                try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next()){ItemTemplate t=new ItemTemplate((short)r.getInt("id"),(byte)r.getInt("TYPE"),(byte)r.getInt("gender"),r.getString("NAME"),r.getString("description"),(short)r.getInt("icon_id"),(short)r.getInt("part"),r.getBoolean("is_up_to_up"),r.getInt("power_require"));Manager.ITEM_TEMPLATES.add(t);}}
                try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}
            }
            var service=GiftCodeService.gI();String expiry="2030-01-01 00:00:00";
            String balls="[";for(int id=14;id<=20;id++){if(id>14)balls+=",";balls+=reward(id,10).substring(1,reward(id,10).length()-1);}balls+="]";
            seed(c,1,"p52a2v",2,balls,expiry);User first=user(c,1,100);
            check(service.redeem(first," P52A2V ").success(),"new code redeems without cache reload; accepts case and outside whitespace");
            check(java.util.stream.IntStream.rangeClosed(14,20).allMatch(id->qty(first,id)==10),"p52a2v grants all seven dragon balls with quantity ten");
            check(scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+1))==1,"successful claim decrements exactly one use");
            check(scalar(c,"SELECT JSON_LENGTH(giftcode) FROM player WHERE id="+first.id)==1&&scalar(c,"SELECT JSON_LENGTH(items_bag) FROM player WHERE id="+first.id)==100,"receipt and complete bag persist together");
            User reconnect=new User((int)first.id);check(!service.redeem(reconnect,"p52a2v").success(),"persisted receipt prevents reclaim after reconnect with empty memory");
            check(scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+1))==1,"duplicate claim never consumes another use");
            seed(c,2,"old_name",2,reward(888,3),expiry);try(Statement s=c.createStatement()){s.executeUpdate("UPDATE giftcode SET code='new_name' WHERE id="+(BASE+2));}
            User renamed=user(c,2,8);check(!service.redeem(renamed,"old_name").success()&&service.redeem(renamed,"NEW_NAME").success(),"web rename is effective immediately without Update Code");
            check(!service.redeem(renamed,"missing").success()&&!service.redeem(renamed,"   ").success(),"missing and blank codes fail without granting rewards");
            seed(c,3,"expired_qa",2,reward(888,3),"2001-01-01 00:00:00");check(!service.redeem(renamed,"expired_qa").success()&&scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+3))==2,"expired code cannot consume a use");
            seed(c,4,"future_qa",2,reward(888,3),"2099-01-01 00:00:00");try(Statement s=c.createStatement()){s.executeUpdate("UPDATE giftcode SET datecreate='2090-01-01' WHERE id="+(BASE+4));}
            check(!service.redeem(renamed,"future_qa").success(),"future code is not active yet");
            seed(c,5,"zero_qa",0,reward(888,3),expiry);check(!service.redeem(renamed,"zero_qa").success(),"exhausted code reports unavailable");
            seed(c,6,"unlimited_qa",-1,reward(888,3),expiry);check(service.redeem(renamed,"unlimited_qa").success()&&scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+6))==-1,"unlimited stock stays minus one");
            seed(c,7,"full_qa",2,reward(889,3),expiry);User full=user(c,3,1);full.inventory.itemsBag.set(0,ItemService.gI().createNewItem((short)888,1));
            check(!service.redeem(full,"full_qa").success()&&full.giftCode.rewards.isEmpty()&&scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+7))==2,"full bag leaves receipt and remaining stock untouched");
            full.inventory.itemsBag.set(0,new Item());check(service.redeem(full,"full_qa").success(),"failed full-bag claim can be retried after making space");
            seed(c,8,"stack_qa",2,reward(888,3),expiry);User stack=user(c,4,1);Item existing=ItemService.gI().createNewItem((short)888,1);existing.itemOptions.clear();existing.itemOptions.add(new Item.ItemOption(30,1));stack.inventory.itemsBag.set(0,existing);
            check(service.redeem(stack,"stack_qa").success()&&qty(stack,888)==4,"stack-compatible reward works without empty slots");
            seed(c,9,"invalid_qa",2,"invalid JSON",expiry);check(!service.redeem(renamed,"invalid_qa").success()&&scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+9))==2,"malformed giftcode preserves uses");
            seed(c,10,"money_qa",2,"[{\"id\":-1,\"quantity\":200},{\"id\":-2,\"quantity\":30},{\"id\":-3,\"quantity\":40}]",expiry);User money=user(c,5,0);
            check(service.redeem(money,"money_qa").success()&&money.inventory.gold==300&&money.inventory.gem==130&&money.inventory.ruby==140,"currency rewards work without inventory slots");
            seed(c,11,"limit_qa",2,reward(-2,1),expiry);money.inventory.gem=Integer.MAX_VALUE;check(!service.redeem(money,"limit_qa").success()&&money.inventory.gem==Integer.MAX_VALUE&&scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+11))==2,"currency overflow cannot consume code or lower balance");
            seed(c,12,"rollback_qa",2,reward(888,3),expiry);User rollback=user(c,6,8);
            try(Statement s=c.createStatement()){s.execute("DROP TRIGGER IF EXISTS qa_giftcode_failure");s.execute("CREATE TRIGGER qa_giftcode_failure BEFORE UPDATE ON giftcode FOR EACH ROW BEGIN IF OLD.id="+(BASE+12)+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected QA giftcode failure'; END IF; END");}
            try{check(!service.redeem(rollback,"rollback_qa").success()&&qty(rollback,888)==0&&rollback.giftCode.rewards.isEmpty()&&scalar(c,"SELECT JSON_LENGTH(giftcode) FROM player WHERE id="+rollback.id)==0,"DB failure rolls back stock, persisted receipt and in-memory items");}finally{try(Statement s=c.createStatement()){s.execute("DROP TRIGGER qa_giftcode_failure");}}
            check(service.redeem(rollback,"rollback_qa").success(),"claim works after storage error is resolved");
            seed(c,13,"race_qa",1,reward(888,3),expiry);User a=user(c,7,8),b=user(c,8,8);ExecutorService pool=Executors.newFixedThreadPool(2);
            try{var left=pool.submit(()->service.redeem(a,"race_qa"));var right=pool.submit(()->service.redeem(b,"race_qa"));check(left.get().success()!=right.get().success()&&scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+13))==0,"concurrent claims cannot overdraw the last available use");}finally{pool.shutdownNow();}
            seed(c,14,"input_qa",2,reward(888,3),expiry);User input=user(c,9,8);var kame=TrungThuRuntimeTest.map(5,(byte)0);input.zone=kame.zones.get(0);var santa=new nro.models.npc_list.Santa(5,1,0,0,nro.models.consts.ConstNpc.SANTA,0);santa.openBaseMenu(input);santa.confirmMenu(input,2);check(input.form!=null&&input.idMark.getTypeInput()==Input.GIFT_CODE,"Santa giftcode form routes to the correct input handler");
            var bytes=new java.io.ByteArrayOutputStream();var out=new java.io.DataOutputStream(bytes);out.writeByte(1);out.writeUTF("INPUT_QA");Input.gI().doInput(input,new Message((byte)-125,bytes.toByteArray()));check(qty(input,888)==3,"real game input packet reaches giftcode redemption");
            User coupon=user(c,10,8);coupon.zone=input.zone;coupon.inventory.itemsBag.set(0,ItemService.gI().createNewItem((short)459,1));santa.openBaseMenu(coupon);santa.confirmMenu(coupon,3);
            check(coupon.form!=null&&coupon.idMark.getTypeInput()==Input.GIFT_CODE,"Santa coupon menu offset still opens the giftcode form");
            seed(c,15,"maintenance_qa",2,reward(888,3),expiry);nro.models.server.Maintenance.isRunning=true;
            try{check(!service.redeem(coupon,"maintenance_qa").success()&&scalar(c,"SELECT count_left FROM giftcode WHERE id="+(BASE+15))==2,"maintenance rejects redemption without consuming stock");}finally{nro.models.server.Maintenance.isRunning=false;}
            System.out.println("PASS: "+checks+" giftcode checks; QA only");
        }finally{LocalManager.close();}
    }
}