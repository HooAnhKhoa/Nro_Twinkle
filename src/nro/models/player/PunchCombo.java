package nro.models.player;

import nro.models.item.Item;
import nro.models.skill.Skill;

/** Option 156: consecutive successful basic punches, session-only, strongest cap wins. */
public final class PunchCombo {
    private Object source;
    private int cap, stacks;
    public static boolean isPunch(int skill) {
        return skill==Skill.DRAGON || skill==Skill.DEMON || skill==Skill.GALICK;
    }
    private void sync(Player player) {
        Object equipped=null;int maximum=0;
        if(player!=null && player.inventory!=null)for(Item item:player.inventory.itemsBody){
            if(item==null || !item.isNotNullItem())continue;
            for(Item.ItemOption option:item.itemOptions)if(option.optionTemplate.id==156 && option.param>maximum){maximum=option.param;equipped=item;}
        }
        if(player!=null)for(var card:player.Cards)if(card!=null&&card.Used==1){
            for(var option:card.Options)if((option.active==card.Level||(card.Level==-1&&option.active==0))&&option.id==156&&option.param>maximum){maximum=option.param;equipped=card;}
            break;
        }
        if(equipped!=source || cap!=maximum){source=equipped;cap=maximum;stacks=0;}
        if(player==null || player.isDie())stacks=0;
    }
    public synchronized int percent(Player player){sync(player);return stacks;}
    public synchronized boolean useSkill(Player player,int skill){sync(player);if(!isPunch(skill)&&stacks>0){stacks=0;return true;}return false;}
    public synchronized boolean hit(Player player,int skill,long damage){
        sync(player);
        if(!isPunch(skill))return useSkill(player,skill);
        if(damage<=0 || cap<=0 || stacks>=cap || player.isDie())return false;
        stacks++;return true;
    }
    public synchronized void reset(){stacks=0;}
}