package nro.models.mob;
import java.sql.*;import java.util.*;import java.lang.reflect.*;
import nro.models.data.LocalManager;import nro.models.event.EventManager;import nro.models.item.Item;import nro.models.map.ItemMap;import nro.models.network.*;import nro.models.player.*;import nro.models.player_system.Template.*;import nro.models.server.*;import nro.models.services.*;import nro.models.task.TaskMain;

/** QA templates, actual Mob.getItemMobReward and actual master pickup; no production writes. */
public class PetGodFoodTest {
 static int checks;static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static class Socket extends java.net.Socket {
  @Override public java.net.SocketAddress getRemoteSocketAddress(){return new java.net.InetSocketAddress("127.0.0.1",1);}
  @Override public java.net.InetAddress getInetAddress(){return java.net.InetAddress.getLoopbackAddress();}
  @Override public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(new byte[0]);}
  @Override public java.io.OutputStream getOutputStream(){return new java.io.ByteArrayOutputStream();}
 }
 static class User extends Player {User(){id=12345;name="Food QA";isPlayer=true;session=new MySession(new Socket());session.userId=12345;session.version=220;playerTask.taskMain=new TaskMain();nPoint.hp=nPoint.hpMax=100;nPoint.power=10000;for(int i=0;i<20;i++)inventory.itemsBag.add(new Item());}@Override public void sendMessage(Message message){}}
 static void body(Player p,boolean god){p.inventory.itemsBody.clear();for(int i=0;i<11;i++)p.inventory.itemsBody.add(new Item());if(god){int[] ids={555,556,562,563,561};for(int i=0;i<ids.length;i++)p.inventory.itemsBody.set(i,ItemService.gI().createNewItem((short)ids[i]));}}
 static class Scene {
  final User master=new User();final Pet pet=new Pet(master);final Mob mob=new Mob();
  Scene(int map,boolean masterGod,boolean petGod){var m=new nro.models.map.Map(map,"Food QA "+map,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,new int[24][60],new int[]{0},1,20,new ArrayList<>());Manager.MAPS.add(m);master.zone=pet.zone=mob.zone=m.zones.get(0);master.pet=pet;pet.id=1000000000-master.id;pet.name="QA pet";pet.playerTask.taskMain=new TaskMain();pet.nPoint.hp=100;pet.nPoint.power=10000;body(master,masterGod);body(pet,petGod);mob.tempId=1;}
  List<ItemMap> drop(Player attacker){mob.zone.items.clear();return mob.getItemMobReward(attacker,20,30).stream().filter(i->i.itemTemplate.id>=663&&i.itemTemplate.id<=667).toList();}
 }
 static void chances(double value,double group,double global)throws Exception {
  Class<?> cfg=Class.forName("nro.models.server.PanelBridge$DropConfig");Constructor<?> constructor=cfg.getDeclaredConstructor(String.class,double.class);constructor.setAccessible(true);Map<String,Object> groups=new HashMap<>();
  for(String key:List.of("food","god","dragon_ball","upgrade_stone","shop","crystal","porata","angel","skh","gold","capsule","hung_vuong","trung_thu","october20","summer","bill_food","quest_items","gem","skill","event"))groups.put(key,constructor.newInstance("multiplier",key.equals("food")?group:0d));
  Field f=PanelBridge.class.getDeclaredField("dropRates");f.setAccessible(true);f.set(null,groups);
  Class<?> rule=Class.forName("nro.models.server.PanelBridge$DropRule");Constructor<?> rc=rule.getDeclaredConstructor(String.class,double.class,double.class);rc.setAccessible(true);Field rf=PanelBridge.class.getDeclaredField("dropRules");rf.setAccessible(true);rf.set(null,Map.of("food.god",rc.newInstance("food",300d/333d,value)));PanelBridge.dropMultiplier=global;
 }
 public static void main(String[] args)throws Exception {
  try{
   try(Connection c=LocalManager.getConnection();Statement s=c.createStatement()){
    check("nro_panel_qa_20261013".equals(c.getCatalog()),"QA database guard");
    try(ResultSet r=s.executeQuery("SELECT * FROM item_template ORDER BY id")){while(r.next())Manager.ITEM_TEMPLATES.add(new ItemTemplate(r.getShort("id"),r.getByte("type"),r.getByte("gender"),r.getString("name"),r.getString("description"),r.getShort("icon_id"),r.getShort("part"),r.getBoolean("is_up_to_up"),0));}
    try(ResultSet r=s.executeQuery("SELECT * FROM item_option_template ORDER BY id")){while(r.next())Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(r.getInt("id"),r.getString("name"),0));}
   }
   Field events=EventManager.class.getDeclaredField("switches");events.setAccessible(true);events.set(null,Map.of("mob_rewards",false));chances(100,1,1);
   for(int map:new int[]{1,79,105,110}){
    Scene petOnly=new Scene(map,false,true);check(petOnly.pet.setClothes.checkSetGod()&&!petOnly.master.setClothes.checkSetGod(),"fixture pet alone has complete God set on map "+map);
    var drops=petOnly.drop(petOnly.pet);check(drops.size()==1&&drops.get(0).quantity==1&&drops.get(0).playerId==petOnly.master.id,"pet God set drops one food owned by master on map "+map);
    check(petOnly.drop(petOnly.master).size()==1,"master kills also recognize accompanying pet God set on map "+map);
    Scene masterOnly=new Scene(map,true,false);check(masterOnly.drop(masterOnly.master).size()==1&&masterOnly.drop(masterOnly.pet).size()==1,"master God set remains effective for either killer on map "+map);
   }
   Scene both=new Scene(105,true,true);for(int i=0;i<100;i++)if(both.drop(both.pet).size()!=1)throw new AssertionError("duplicate/missing food");check(true,"100 guaranteed-rate kills with two full sets produce one food per kill");
   Scene bare=new Scene(1,false,false);check(bare.drop(bare.master).isEmpty()&&bare.drop(bare.pet).isEmpty(),"no full set produces no food even at 100 percent");
   Scene missing=new Scene(105,false,true);missing.pet.inventory.itemsBody.set(4,new Item());check(missing.drop(missing.pet).isEmpty(),"pet missing God ring does not qualify");
   Scene remote=new Scene(1,false,true);remote.pet.zone=null;check(remote.drop(remote.master).isEmpty(),"pet outside master's zone cannot enable food for master");
   Scene shortBody=new Scene(1,false,false);shortBody.pet.inventory.itemsBody.clear();check(shortBody.drop(shortBody.master).isEmpty(),"unloaded pet body is handled without index exception");
   Scene pickup=new Scene(105,false,true);ItemMap food=pickup.drop(pickup.pet).get(0);pickup.mob.zone.pickItem(pickup.master,food.itemMapId);check(pickup.master.inventory.itemsBag.stream().anyMatch(i->i.isNotNullItem()&&i.template.id==food.itemTemplate.id&&i.quantity==1),"actual master pickup succeeds after pet cold-map kill");
   chances(0,1,1);check(both.drop(both.pet).isEmpty(),"regional zero percent still disables food");chances(100,0,1);check(both.drop(both.pet).isEmpty(),"food group x0 remains respected");chances(100,1,0);check(both.drop(both.pet).isEmpty(),"global x0 remains respected");
   chances(100,1,1);Scene dummy=new Scene(1,false,true);dummy.mob.tempId=0;check(dummy.drop(dummy.pet).isEmpty(),"training dummy does not yield food");
   Scene bot=new Scene(1,true,false);bot.master.isBot=true;check(bot.drop(bot.master).isEmpty(),"bots do not receive God-food eligibility");
   System.out.println("PASS: "+checks+" God-food checks; actual pet kills on normal/Nappa/cold maps");
  }finally{LocalManager.close();}System.exit(0);
 }
}
