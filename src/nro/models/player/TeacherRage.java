package nro.models.player;
import nro.models.item.Item;
/** Every 120 equipped seconds, option 222 grants a 30 second damage window. */
public final class TeacherRage {
 private Item equipped;private long since;private int published;
 public int percent(Item skin,long now){
  if(skin==null||!skin.isNotNullItem()||skin.template.type!=5){equipped=null;since=now;return 0;}
  int percent=0;for(Item.ItemOption o:skin.itemOptions){if(o.optionTemplate.id==222)percent=Math.max(percent,o.param);if(o.optionTemplate.id==93&&now>=skin.createTime+(long)o.param*86400000L){equipped=null;since=now;return 0;}}
  if(percent<=0){equipped=null;since=now;return 0;}
  if(equipped!=skin||now<since){equipped=skin;since=now;}
  long elapsed=now-since;return elapsed>=120000&&elapsed%120000<30000?percent:0;
 }
 public int percent(Player p,long now){return percent(!p.isDie()&&p.inventory.itemsBody.size()>5?p.inventory.itemsBody.get(5):null,now);}
 public boolean changed(Player p,long now){int value=percent(p,now);if(value==published)return false;published=value;return true;}
 public static long bossDamage(Player attacker,Player target,long damage){
  if(attacker==null||target==null||!target.isBoss||damage<=0||attacker.inventory.itemsBody.size()<=5)return damage;
  Item skin=attacker.inventory.itemsBody.get(5);if(skin==null||!skin.isNotNullItem())return damage;
  int bonus=0;long now=System.currentTimeMillis();for(Item.ItemOption o:skin.itemOptions){if(o.optionTemplate.id==93&&now>=skin.createTime+(long)o.param*86400000L)return damage;if(o.optionTemplate.id==204)bonus+=o.param;}
  return damage+damage*Math.max(0,bonus)/100L;
 }
}