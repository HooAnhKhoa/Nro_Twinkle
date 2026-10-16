package nro.models.event.trungthu2026;
import java.sql.*;import java.nio.file.*;import java.lang.reflect.*;import java.util.*;
import nro.models.data.LocalManager;import nro.models.item.Item;import nro.models.player.*;import nro.models.server.Manager;import nro.models.services.*;import nro.models.services_func.UseItem;import nro.models.player_system.Template.*;import nro.models.skill.*;import nro.models.utils.SkillUtil;import nro.models.consts.ConstNpc;
/** QA-only checks for Chi Chi's skill reroll and the actual power-limit menu/debit. */
public class PetShopTest {
 static int checks;static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 static class User extends TrungThuRuntimeTest.User {String notice="";User(){super(1999180001);nPoint.power=100000000000L;inventory.gem=100;inventory.gold=60000000;}@Override public void sendMessage(nro.models.network.Message m){super.sendMessage(m);if(m.command==-25)try{notice=new java.io.DataInputStream(new java.io.ByteArrayInputStream(m.getData())).readUTF();}catch(Exception ignored){}}}
 static long scalar(Connection c,String sql)throws Exception{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 public static void main(String[] args)throws Exception{try(Connection c=LocalManager.getConnection()){
 check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
 try(var s=c.createStatement()){for(String table:List.of("shop","tab_shop","item_shop"))s.execute("CREATE TABLE IF NOT EXISTS "+table+" LIKE srcnrofree."+table);
 try(var r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)r.getInt("id"),r.getByte("TYPE"),r.getByte("gender"),r.getString("NAME"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),false,0));}
 try(var r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("NAME"),0));}}
 c.setAutoCommit(false);try{
 try(var s=c.createStatement()){s.executeUpdate("INSERT IGNORE INTO shop SELECT * FROM srcnrofree.shop WHERE id=33");s.executeUpdate("INSERT IGNORE INTO tab_shop SELECT * FROM srcnrofree.tab_shop WHERE id=58");s.executeUpdate("DELETE FROM item_shop WHERE tab_id=58 AND temp_id=1760");}
 String migration=Files.readString(Path.of(args[0],"web/pet_shop_schema.sql"));for(int pass=0;pass<2;pass++)try(var s=c.createStatement()){for(String sql:migration.split(";"))if(!sql.isBlank())s.execute(sql);}
 check(scalar(c,"SELECT COUNT(*) FROM item_shop WHERE tab_id=58 AND temp_id=1760")==1,"repeated migration adds exactly one Chi Chi reroll item");check(scalar(c,"SELECT cost FROM item_shop WHERE tab_id=58 AND temp_id=1760")==30&&scalar(c,"SELECT type_sell FROM item_shop WHERE tab_id=58 AND temp_id=1760")==1,"shop price is 30 gems");
 try(var s=c.createStatement()){s.executeUpdate("UPDATE item_shop SET cost=41 WHERE tab_id=58 AND temp_id=1760");for(String sql:migration.split(";"))if(!sql.isBlank())s.execute(sql);}check(scalar(c,"SELECT cost FROM item_shop WHERE tab_id=58 AND temp_id=1760")==41,"restart preserves admin price changes");
 }finally{c.rollback();c.setAutoCommit(true);}
 for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());for(int id:new int[]{Skill.BIEN_KHI,Skill.DE_TRUNG,Skill.KHIEN_NANG_LUONG}){SkillTemplate t=new SkillTemplate();t.id=(byte)id;t.name="QA";Skill sk=new Skill();sk.template=t;sk.point=1;sk.skillId=(short)(id*10+1);t.skillss.add(sk);Manager.NCLASS.get(0).skillTemplatess.add(t);}
 Method use=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);use.setAccessible(true);User p=new User();Item reroll=ItemService.gI().createNewItem((short)1760,3);p.inventory.itemsBag.add(reroll);
 use.invoke(UseItem.gI(),p,reroll,0);check(reroll.quantity==3,"no pet never consumes item");p.pet=new Pet(p);for(int i=0;i<4;i++)p.pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
 use.invoke(UseItem.gI(),p,reroll,0);check(reroll.quantity==3,"locked fourth skill never consumes item");p.pet.playerSkill.skills.set(3,SkillUtil.createSkill(Skill.BIEN_KHI,1));
 use.invoke(UseItem.gI(),p,reroll,0);check(reroll.quantity==2&&p.pet.playerSkill.skills.get(3)!=null,"valid reroll consumes exactly one item and sets skill four");check(Set.of((int)Skill.BIEN_KHI,(int)Skill.DE_TRUNG,(int)Skill.KHIEN_NANG_LUONG).contains((int)p.pet.playerSkill.skills.get(3).template.id),"reroll keeps existing skill pool");
 var npc=new nro.models.npc_list.QuocVuong(0,1,0,0,0,0){@Override public boolean canOpenNpc(Player player){return true;}};
 p.pet.nPoint.limitPower=0;npc.openBaseMenu(p);npc.confirmMenu(p,1);var packet=new java.io.DataInputStream(new java.io.ByteArrayInputStream(p.menuPacket));packet.readShort();packet.readUTF();packet.readByte();String option=packet.readUTF();check(option.contains("vàng")&&!option.contains("ngọc"),"pet limit menu labels price as gold");
 npc.confirmMenu(p,0);check(p.pet.nPoint.limitPower==1&&p.inventory.gold==10000000&&p.inventory.gem==100,"opening pet limit deducts fifty million gold only");
 npc.openBaseMenu(p);npc.confirmMenu(p,1);npc.confirmMenu(p,0);check(p.pet.nPoint.limitPower==1&&p.inventory.gold==10000000&&p.notice.contains("vàng")&&!p.notice.contains("ngọc"),"insufficient gold preserves limit and uses correct currency message");
 System.out.println("PASS: "+checks+" pet shop/power checks; QA only");
 }catch(Throwable e){e.printStackTrace();System.exit(1);}finally{LocalManager.close();}System.exit(0);}
}