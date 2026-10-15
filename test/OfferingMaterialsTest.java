import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import nro.models.event.*;
import nro.models.server.*;
import nro.models.player.Player;
import nro.models.player_system.Template.*;
import nro.models.mob.Mob;
import nro.models.map.Zone;
import nro.models.map.ItemMap;
import nro.models.network.Message;
import nro.models.services.*;
import nro.models.item.Item;

/** Isolated game objects: never connects to a database or starts a server. */
public class OfferingMaterialsTest {
 static int checks;
 static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);checks++;System.out.println("PASS: "+text);}
 static void flags(boolean hung,boolean rewards,boolean summer)throws Exception{
  Field f=EventManager.class.getDeclaredField("switches");f.setAccessible(true);f.set(null,Map.of("hung_vuong",hung,"mob_rewards",rewards,"summer",summer));
 }
 static class Client extends Player {
  byte[] packet;int maps,respawns;
  Client(){isPlayer=true;id=123;}
  @Override public void sendMessage(Message m){if(m.command==-24){packet=m.getData().clone();maps++;}if(m.command==-13)respawns++;}
 }
 static int transmittedMobs(Client player)throws Exception{
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(player.packet));for(int i=0;i<5;i++)in.readByte();in.readUTF();in.readByte();in.readShort();in.readShort();check(in.readUnsignedByte()==0,"empty waypoint fixture");return in.readUnsignedByte();
 }
 static nro.models.map.Map map(int id){int[][] tiles=new int[24][52];nro.models.map.Map m=new nro.models.map.Map(id,"QA "+id,(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,tiles,new int[]{0},2,10,new ArrayList<>());Manager.MAPS.add(m);return m;}
 static Item item(int id,int quantity){return ItemService.gI().createNewItem((short)id,quantity);}
 static Client cookPlayer(Zone zone,int[][] recipe,boolean space){Client p=new Client();p.zone=zone;for(int[] r:recipe)p.inventory.itemsBag.add(item(r[0],r[1]));if(space)p.inventory.itemsBag.add(ItemService.gI().createItemNull());p.idMark.setIndexMenu(1);return p;}
 static int quantity(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 public static void main(String[] args)throws Exception{
  try{
   flags(false,true,true);
   for(int id=82;id<=84;id++){MobTemplate t=new MobTemplate();t.id=id;t.name="QA offering "+id;t.hp=id==82?20000000:id==83?12000000:15000000;t.percentDame=5;t.percentTiemNang=10;Manager.MOB_TEMPLATES.add(t);}
   for(int id=0;id<2000;id++)Manager.ITEM_TEMPLATES.add(new ItemTemplate((short)id,(byte)27,(byte)3,"Item "+id,"",(short)0,(short)0,false,0));
   for(int id=0;id<200;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
   for(int id:new int[]{183,184,185,1,2,3,4,5,6,8,9})map(id);
   OfferingMobs.seed();long total=Manager.MAPS.stream().flatMap(m->m.zones.stream()).mapToLong(z->z.mobs.size()).sum();
   check(total==36,"three offerings in six castle zones and six random world zones");
   for(nro.models.map.Map m:Manager.MAPS)for(Zone z:m.zones)for(int i=0;i<z.mobs.size();i++){Mob mob=z.mobs.get(i);check(mob.id==i&&mob.zone==z&&mob.isBigBoss()&&mob.isDie(),"stable indices, real giant subclass and OFF state");}
   OfferingMobs.seed();check(total==Manager.MAPS.stream().flatMap(m->m.zones.stream()).mapToLong(z->z.mobs.size()).sum(),"seed idempotent");
   Zone z=Manager.MAPS.get(0).zones.get(0);Client p=new Client();p.zone=z;z.addPlayer(p);z.mapInfo(p);check(transmittedMobs(p)==0,"OFF omits offering slots from client map");
   flags(true,true,true);for(Mob mob:z.mobs)OfferingMobs.tick(mob);EventManager.syncNpcVisibility(p);
   check(transmittedMobs(p)==3&&EventManager.clientKnowsMob(p,z.mobs.get(0)),"ON refresh introduces all three mob slots");
   check(z.mobs.get(0).point.hp==20000000&&z.mobs.get(1).point.hp==12000000&&z.mobs.get(2).point.hp==15000000,"HP comes from real templates");
   Mob first=z.mobs.get(0);first.point.hp=0;first.lastTimeDie=System.currentTimeMillis();z.mapInfo(p);check(transmittedMobs(p)==3,"dead first offering does not shift remaining client indices");
   OfferingMobs.tick(first);check(first.isDie(),"no early respawn");first.lastTimeDie-=61000;OfferingMobs.tick(first);check(!first.isDie()&&p.respawns==1,"60-second respawn informs known client");
   for(int id=82;id<=84;id++){check(EventDrops.rollMaterials(id,true).equals(List.of(1220+id-82)),"offering mob drops only its matching gift");}
   List<ItemMap> drops=new ArrayList<>();EventDrops.appendMobDrops(first,p,drops,10,10);check(drops.size()==1&&drops.get(0).itemTemplate.id==1220&&drops.get(0).playerId==p.id,"actual ItemMap drop retains owner and template");
   check(EventDrops.rollMaterials(0,false).isEmpty()&&EventDrops.rollMaterials(4,true).isEmpty(),"training dummy and other giant do not drop cooking materials");
   Class<?> rule=Class.forName("nro.models.server.PanelBridge$DropRule");Constructor<?> ctor=rule.getDeclaredConstructors()[0];ctor.setAccessible(true);Field rules=PanelBridge.class.getDeclaredField("dropRules");rules.setAccessible(true);Map<String,Object> forced=new HashMap<>();for(int id:EventDrops.COOKING_MATERIALS)forced.put("hung.cooking."+id,ctor.newInstance("hung_vuong",5d,100d));rules.set(null,forced);
   check(new HashSet<>(EventDrops.rollMaterials(4,false)).equals(Set.of(1544,1545,1546,1547,1548,1549)),"ordinary mob supports every cooking material and per-item percent override");
   forced.put("hung.cooking.1544",ctor.newInstance("hung_vuong",5d,0d));check(!EventDrops.rollMaterials(4,false).contains(1544),"zero override disables only selected ingredient");
   flags(false,true,true);int hp=first.point.hp;first.injured(p,999,true);check(first.point.hp==hp,"OFF blocks stale attacks before damage and reward");OfferingMobs.tick(first);EventManager.syncNpcVisibility(p);check(transmittedMobs(p)==0&&first.isDie(),"OFF removes client offerings and suspends mob");check(EventDrops.rollMaterials(82,true).isEmpty(),"Hung OFF blocks material drops");
   flags(true,false,false);check(EventDrops.rollMaterials(82,true).isEmpty()&&PanelBridge.dropChance("summer",5,100)==0,"master OFF blocks seasonal drops");check(PanelBridge.dropChance("bill_food",5,100)==5&&PanelBridge.dropChance("quest_items",20,100)==20,"food and quests independent of seasonal master");
   flags(true,true,false);check(PanelBridge.dropChance("summer",5,100)==0,"summer OFF blocks sugarcane");
   int[][] day={{1546,99},{1547,10},{1545,10},{1544,1}},chung={{1546,99},{1548,99},{1549,99}};
   NoiBanh npc=new NoiBanh(183,1,0,0,66,0);
   for(int kind=0;kind<2;kind++){int[][] recipe=kind==0?day:chung;Client cook=cookPlayer(z,recipe,true);npc.confirmMenu(cook,kind);check(quantity(cook,kind==0?1542:1556)==1,"recipe produces correct cake "+kind);for(int[] r:recipe)check(quantity(cook,r[0])==0,"recipe consumes exact ingredient amount "+r[0]);}
   Client shortBag=cookPlayer(z,day,true);shortBag.inventory.itemsBag.get(0).quantity=98;npc.confirmMenu(shortBag,0);check(quantity(shortBag,1542)==0&&quantity(shortBag,1546)==98&&quantity(shortBag,1544)==1,"insufficient quantities preserve all ingredients");
   Client full=cookPlayer(z,day,false);npc.confirmMenu(full,0);check(quantity(full,1542)==0&&quantity(full,1546)==99&&quantity(full,1544)==1,"full bag cannot consume ingredients");
   flags(false,true,true);Client off=cookPlayer(z,day,true);npc.confirmMenu(off,0);check(quantity(off,1542)==0&&quantity(off,1546)==99,"event OFF rejects stale cooking menu");
   System.out.println("PASS: "+checks+" offering/drop/recipe assertions");
  }catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);
 }
}
