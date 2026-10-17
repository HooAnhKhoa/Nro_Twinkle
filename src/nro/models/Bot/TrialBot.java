package nro.models.Bot;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import nro.models.boss.Boss;
import nro.models.boss.BossBounty;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.server.PanelBridge;
import nro.models.services.*;
import nro.models.skill.Skill;

/** Sessionless simulation. Never registered as an account or eligible for VNĐ bounty/trading. */
public final class TrialBot extends Player {
    public enum Mode { FARM, HUNT }
    private static final AtomicLong IDS = new AtomicLong(1_800_000_000L);
    public final Mode mode;
    private final int number;
    private Mob mobTarget;
    private Boss bossTarget;
    private long lastAttack, lastRoute, nextScan, reviveAt, lastHeal;
    private int route;
    public long kills, pickups, attacks, errors;
    public String activity = "Đang tìm map";
    private final short headPart, bodyPart, legPart;

    TrialBot(Mode mode, int number) {
        this.mode = mode; this.number = number;
        id = IDS.incrementAndGet(); isBot = true;
        gender = (byte) (mode == Mode.HUNT ? 2 : (number - 1) % 2);
        name = mode == Mode.HUNT ? "BOT SanBoss" : "BOT Luyen" + number;
        originalName = name;
        headPart = (short) (gender * 3); bodyPart = (short) (gender * 3 + 1); legPart = (short) (gender * 3 + 2);
        for (int i=0;i<11;i++) inventory.itemsBody.add(new Item());
        for (int i=0;i<100;i++) inventory.itemsBag.add(new Item());
        playerTask.taskMain = new nro.models.task.TaskMain();
        playerIntrinsic.intrinsic = new nro.models.intrinsic.Intrinsic();
        nPoint.limitPower = 8; nPoint.power = mode == Mode.HUNT ? 100_000_000L : 10000;
        nPoint.hpg = mode == Mode.HUNT ? 5_000_000 : 20000;
        nPoint.mpg = 100000; nPoint.dameg = mode == Mode.HUNT ? 100000 : 300;
        nPoint.maxStamina = nPoint.stamina = 20000;
        nPoint.calPoint(); nPoint.setFullHpMp();
        int punch = gender == 0 ? Skill.DRAGON : gender == 1 ? Skill.DEMON : Skill.GALICK;
        for (var nclass : Manager.NCLASS) if (nclass.classId == gender)
            for (var template : nclass.skillTemplatess) if (template.id == punch && !template.skillss.isEmpty()) {
                Skill source = template.skillss.stream().filter(s -> s.point == 7).findFirst().orElse(template.skillss.get(0));
                playerSkill.skillSelect = new Skill(source); playerSkill.skills.add(playerSkill.skillSelect);
            }
        if (playerSkill.skillSelect == null) throw new IllegalStateException("Missing punch skill for bot " + gender);
    }
    @Override public short getHead() { return headPart; }
    @Override public short getBody() { return bodyPart; }
    @Override public short getLeg() { return legPart; }
    @Override public short getFlagBag() { return -1; }
    @Override public void update() { tick(System.currentTimeMillis()); }

    static boolean allowedFarmMap(int map) { return map>=1&&map<=6 || map>=8&&map<=13 || map>=15&&map<=20; }
    static boolean eligibleBoss(Boss boss) { return eligibleBoss(boss,null); }
    private static boolean eligibleBoss(Boss boss, Zone current) {
        if (boss == null || boss.zone == null || boss.isDie() || boss.bossStatus != BossStatus.ACTIVE
                || !BossBounty.combat(boss.getClass().getName()) || !PanelBridge.enabled(boss)) return false;
        int map = boss.zone.map.mapId;
        return (map>=1&&map<=20 || map>=27&&map<=30 || map>=63&&map<=83 || map>=92&&map<=110)
                && boss.nPoint.hpMax<=200_000_000 && (boss.zone==current || !boss.zone.isFullPlayer());
    }
    private boolean ownBoss(Boss boss) { return eligibleBoss(boss,zone); }
    void tick(long now) {
        if (beforeDispose) return;
        if (isDie()) {
            if (reviveAt == 0) { reviveAt = now + 30000; activity = "Chờ hồi sinh 30 giây"; }
            if (now < reviveAt) return;
            Service.gI().hsChar(this,nPoint.hpMax,nPoint.mpMax);
            reviveAt = 0; mobTarget = null; bossTarget = null; lastRoute = 0;
            farmRoute(now); return;
        }
        effectSkill.update();
        if (now-lastHeal>=5000) { nPoint.addHp(Math.max(1,nPoint.hpMax/20)); nPoint.addMp(Math.max(1,nPoint.mpMax/10)); nPoint.stamina=nPoint.maxStamina; lastHeal=now; }
        if (mode == Mode.HUNT && now>=nextScan) {
            nextScan = now+5000;
            if (!ownBoss(bossTarget)) bossTarget = new ArrayList<>(BossManager.gI().getBosses()).stream()
                    .filter(TrialBot::eligibleBoss).min(Comparator.comparingInt(b -> b.nPoint.hpMax)).orElse(null);
            if (bossTarget!=null && bossTarget.zone!=zone) enter(bossTarget.zone,bossTarget.location.x-120,bossTarget.location.y);
        }
        if (mode == Mode.HUNT && ownBoss(bossTarget)) {
            if (bossTarget.zone!=zone) { bossTarget=null; return; }
            activity = "Săn " + bossTarget.name;
            if (approach(bossTarget.location.x,bossTarget.location.y) && ready(now)) {
                boolean deadBefore=bossTarget.isDie(); SkillService.gI().useSkill(this,bossTarget,null,0,null); attacks++;
                if (!deadBefore&&bossTarget.isDie()) kills++;
            }
        } else {
            if (zone==null || now-lastRoute>=120000 || zone.mobs.isEmpty()) farmRoute(now);
            if (zone==null) { activity="Chờ map luyện quái"; return; }
            if (mobTarget==null || mobTarget.zone!=zone || mobTarget.isDie()) mobTarget=zone.mobs.stream()
                    .filter(m -> m.tempId>0 && !m.isDie() && !m.isBigBoss() && m.point.maxHp<=Math.max(10000,nPoint.dame*30))
                    .min(Comparator.comparingInt(m -> Math.abs(m.location.x-location.x))).orElse(null);
            if (mobTarget!=null) {
                activity="Luyện quái " + mobTarget.tempId;
                if (approach(mobTarget.location.x,mobTarget.location.y) && ready(now)) {
                    int hp=mobTarget.point.hp; SkillService.gI().useSkill(this,null,mobTarget,0,null); attacks++;
                    if(hp>0&&mobTarget.isDie()) {
                        kills++; long gain=Math.max(100,mobTarget.point.maxHp/10); nPoint.power+=gain; nPoint.tiemNang+=gain;
                        if(kills%10==0) {nPoint.hpg+=200;nPoint.dameg+=20;nPoint.calPoint();}
                    }
                }
            } else activity="Chờ quái hồi sinh";
        }
        pickup();
    }
    private boolean ready(long now) {
        if (now-lastAttack<Math.max(800,playerSkill.skillSelect.coolDown)
                || !SkillService.gI().canUseSkillWithCooldown(this) || !SkillService.gI().canUseSkillWithMana(this)) return false;
        lastAttack=now; return true;
    }
    private boolean approach(int x,int y) {
        int dx=x-location.x;
        if(Math.abs(dx)>70 || Math.abs(y-location.y)>70) {
            int nx=location.x+Math.max(-45,Math.min(45,dx));
            int ny=location.y+Math.max(-45,Math.min(45,y-location.y));
            PlayerService.gI().playerMove(this,nx,ny); return false;
        }
        return true;
    }
    void farmRoute(long now) {
        int[] maps=gender==1?new int[]{8,9,10,11,12,13}:gender==2?new int[]{15,16,17,18,19,20}:new int[]{1,2,3,4,5,6};
        for(int i=0;i<maps.length;i++) {
            int map=maps[(route+i)%maps.length]; var world=MapService.gI().getMapById(map);
            if(world==null)continue;
            Zone target=world.zones.stream().filter(z -> !z.isFullPlayer()&&!z.mobs.isEmpty()).findFirst().orElse(null);
            if(target!=null) { Mob mob=target.mobs.get(0);enter(target,Math.max(24,mob.location.x-100),mob.location.y);route=(route+i+1)%maps.length;lastRoute=now;mobTarget=null;return; }
        }
        lastRoute=now;
    }
    private void enter(Zone target,int x,int y) {
        if(target==null || target==zone)return;
        ChangeMapService.gI().goToMap(this,target);
        location.x=Math.max(24,x);location.y=Math.max(0,y);
        target.load_Me_To_Another(this);
    }
    void pickup() {
        if(zone==null)return;
        int count=0;
        for(ItemMap item:new ArrayList<>(zone.items)) {
            if(count>=3)break;
            if(item==null || item.isPickedUp || item.playerId!=id || item.itemTemplate.type==22
                    || Math.abs(item.x-location.x)>120 || Math.abs(item.y-location.y)>120)continue;
            int type=item.itemTemplate.type;
            if(type!=9&&type!=10&&type!=34&&InventoryService.gI().getCountEmptyBag(this)<1)continue;
            zone.pickItem(this,item.itemMapId);
            if(item.isPickedUp) {pickups++;count++;}
        }
    }
    void stop() {beforeDispose=true;if(zone!=null){ChangeMapService.gI().exitMap(this);zone=null;}}
    public java.util.Map<String,Object> status() {
        return java.util.Map.of("name",name,"mode",mode.name(),"map",zone==null?-1:zone.map.mapId,"zone",zone==null?-1:zone.zoneId,
                "hp",nPoint.hp,"kills",kills,"pickups",pickups,"attacks",attacks,"activity",activity,"errors",errors);
    }
}
