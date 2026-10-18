package nro.models.event.trungthu2026;
import java.sql.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.data.LocalManager;
import nro.models.server.Manager;
import nro.models.npc.*;
import nro.models.network.Message;
import nro.models.services_func.*;
public class GreenDragonBoxTest {
 static int checks;
 static void ok(boolean b,String label){if(!b)throw new AssertionError(label);checks++;System.out.println("PASS: "+label);}
 static void use(TrungThuRuntimeTest.User p)throws Exception{Message msg=new Message((byte)-43,new byte[]{0,1,0});try{UseItem.gI().doItem(p,msg);}finally{msg.cleanup();}}
 public static void main(String[] args)throws Exception{
  try{
   List<TrungThuRuntimeTest.User> users=new ArrayList<>();
   try(Connection c=LocalManager.getConnection()){
    ok("nro_trungthu_rules_20261017".equals(c.getCatalog()),"QA DB guard");
    try(Statement q=c.createStatement()){q.executeUpdate("DELETE FROM player WHERE id BETWEEN 2026101680 AND 2026101689");q.execute("DROP TRIGGER IF EXISTS qa_green_box_fail");}
    ActivationCapsuleTest.load(c);for(int id=2064;id<=2071;id++)Manager.ITEM_TEMPLATES.get(id).isUpToUp=true;
    for(int n=80;n<=89;n++)users.add(TrungThuRuntimeTest.user(c,n,20));
   }
   NpcFactory.createNpcConMeo();Npc cat=Manager.NPCS.stream().filter(n->n.tempId==5).findFirst().orElseThrow();
   for(int star=0;star<7;star++){
    var p=users.get(star);p.inventory.itemsBag.set(0,TrungThuRuntimeTest.item(2071,2));String before=TrungThuStore.bag(p);use(p);
    ok(p.idMark.getIndexMenu()==GreenDragonBox.MENU&&before.equals(TrungThuStore.bag(p)),"native use opens selector without consuming box "+star);
    try(var r=new java.io.DataInputStream(new java.io.ByteArrayInputStream(p.menuPacket))){ok(r.readShort()==5,"selector belongs to Con Meo");r.readUTF();ok(r.readUnsignedByte()==8,"seven stars and close button");}
    cat.confirmMenu(p,star);ok(TrungThuRuntimeTest.qty(p,2064+star)==1&&TrungThuRuntimeTest.qty(p,2071)==1,"selected star granted and exactly one box consumed "+(star+1));
    cat.confirmMenu(p,star);ok(TrungThuRuntimeTest.qty(p,2064+star)==1,"replayed menu cannot grant again");
    try(Connection c=LocalManager.getConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT items_bag FROM player WHERE id="+p.id)){r.next();ok(r.getString(1).equals(TrungThuStore.bag(p)),"selected reward and box consumption persisted together");}
   }
   var full=users.get(7);full.inventory.itemsBag.clear();full.inventory.itemsBag.add(TrungThuRuntimeTest.item(2071,2));full.inventory.itemsBag.add(TrungThuRuntimeTest.item(457,1));String before=TrungThuStore.bag(full);use(full);cat.confirmMenu(full,0);ok(before.equals(TrungThuStore.bag(full)),"full bag preserves boxes");
   full.inventory.itemsBag.set(1,TrungThuRuntimeTest.item(2064,1));use(full);cat.confirmMenu(full,0);ok(TrungThuRuntimeTest.qty(full,2064)==2&&TrungThuRuntimeTest.qty(full,2071)==1,"full bag permits compatible green ball stack");
   full.inventory.itemsBag.set(1,TrungThuRuntimeTest.item(457,1));use(full);cat.confirmMenu(full,6);ok(TrungThuRuntimeTest.qty(full,2070)==1&&TrungThuRuntimeTest.qty(full,2071)==0,"last box can use its own freed slot");
   var fail=users.get(8);fail.inventory.itemsBag.set(0,TrungThuRuntimeTest.item(2071,1));TrungThuStore.persistBag(fail);before=TrungThuStore.bag(fail);
   try(Connection c=LocalManager.getConnection();Statement q=c.createStatement()){q.execute("CREATE TRIGGER qa_green_box_fail BEFORE UPDATE ON player FOR EACH ROW BEGIN IF NEW.id="+fail.id+" THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='expected box save failure'; END IF; END");}
   try{use(fail);cat.confirmMenu(fail,0);ok(before.equals(TrungThuStore.bag(fail)),"SQL failure restores box without reward");}finally{try(Connection c=LocalManager.getConnection();Statement q=c.createStatement()){q.execute("DROP TRIGGER qa_green_box_fail");}}
   use(fail);cat.confirmMenu(fail,0);ok(TrungThuRuntimeTest.qty(fail,2064)==1&&TrungThuRuntimeTest.qty(fail,2071)==0,"retry succeeds after save recovers");
   var cancel=users.get(9);cancel.inventory.itemsBag.set(0,TrungThuRuntimeTest.item(2071,1));before=TrungThuStore.bag(cancel);use(cancel);cat.confirmMenu(cancel,7);ok(before.equals(TrungThuStore.bag(cancel)),"closing selector never consumes box");
   use(cancel);cancel.inventory.itemsBag.set(0,new Item());cat.confirmMenu(cancel,0);ok(TrungThuRuntimeTest.qty(cancel,2064)==0,"moved or removed box cannot be redeemed through stale menu");
   cancel.idMark.setIndexMenu(GreenDragonBox.MENU);cat.confirmMenu(cancel,0);ok(TrungThuRuntimeTest.qty(cancel,2064)==0,"no pending use cannot forge reward");
   System.out.println("PASS: "+checks+" green dragon box checks");
  }finally{LocalManager.close();}
  System.exit(0);
 }
}
