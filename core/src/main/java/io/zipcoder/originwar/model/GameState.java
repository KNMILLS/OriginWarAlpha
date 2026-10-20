package io.zipcoder.originwar.model;

import java.util.*;

/** Versioned save DTO. Only RunEngine mutates live instances; presentation receives copies. */
public final class GameState {
    public int schemaVersion = 1, generationVersion = 1;
    public long seed, combatRng, lootRng, medicineRng, perceptionRng;
    public int width = 64, height = 40, deck = 1, tick;
    public Tile[][] tiles;
    public boolean[][] visible, explored;
    public float[][] light;
    public int playerX, playerY, facing;
    public int hp = 100, maxHp = 100, str = 4, intel = 4, dex = 4;
    public int level = 1, xp, attributePoints;
    public int reserve = 360, maxReserve = 360, oxygenation = 100;
    public int switchX, switchY, hatchX, hatchY, terminalX, terminalY;
    public boolean switchOn, dead, victory;
    public String weapon = "pistol", armor = "suit", activeDialogue = "briefing";
    public int batteries = 24, aliceTrust, poisonTicks, scannedUntil;
    public String constructorRoute = "", bossPhase = "inactive";
    public long phantomUntil;
    public int phantomX = -1, phantomY = -1;
    public ArrayList<Actor> actors = new ArrayList<>();
    public ArrayList<Item> floorItems = new ArrayList<>(), inventory = new ArrayList<>();
    public ArrayList<String> journal = new ArrayList<>(), log = new ArrayList<>();
    public HashSet<String> flags = new HashSet<>(), identifiedMedicine = new HashSet<>();
    public HashMap<String,String> medicineEffects = new HashMap<>();

    public enum ActorKind { SIRS, ADD, ROVER, CAMERA, TURRET, OPERATOR, CONSTRUCTOR, ALICE, WEST, CARMACK, SURVIVOR }
    public static final class Actor {
        public String id = "";
        public ActorKind kind = ActorKind.SIRS;
        public int x, y, hp = 12, maxHp = 12, alert, lastX, lastY, fuse = -1, facing;
        public boolean redirected, dead;
        public Actor() {}
        public Actor copy() { var a = new Actor(); a.id=id;a.kind=kind;a.x=x;a.y=y;a.hp=hp;a.maxHp=maxHp;a.alert=alert;a.lastX=lastX;a.lastY=lastY;a.fuse=fuse;a.facing=facing;a.redirected=redirected;a.dead=dead;return a; }
    }
    public enum ItemType { OXYGEN, CELL, MEDICINE, PISTOL, RIFLE, SCATTERGUN, ARMOR_LIGHT, ARMOR_HEAVY }
    public static final class Item {
        public String id = "", name = "";
        public ItemType type = ItemType.OXYGEN;
        public int x, y, quantity = 1, power;
        public Item() {}
        public Item copy() { var a=new Item();a.id=id;a.name=name;a.type=type;a.x=x;a.y=y;a.quantity=quantity;a.power=power;return a; }
    }
    public GameState copy() {
        var s = new GameState();
        s.schemaVersion=schemaVersion;s.generationVersion=generationVersion;s.seed=seed;s.combatRng=combatRng;s.lootRng=lootRng;s.medicineRng=medicineRng;s.perceptionRng=perceptionRng;
        s.width=width;s.height=height;s.deck=deck;s.tick=tick;s.playerX=playerX;s.playerY=playerY;s.facing=facing;
        s.hp=hp;s.maxHp=maxHp;s.str=str;s.intel=intel;s.dex=dex;s.level=level;s.xp=xp;s.attributePoints=attributePoints;
        s.reserve=reserve;s.maxReserve=maxReserve;s.oxygenation=oxygenation;s.switchX=switchX;s.switchY=switchY;s.hatchX=hatchX;s.hatchY=hatchY;s.terminalX=terminalX;s.terminalY=terminalY;
        s.switchOn=switchOn;s.dead=dead;s.victory=victory;s.weapon=weapon;s.armor=armor;s.activeDialogue=activeDialogue;s.batteries=batteries;s.aliceTrust=aliceTrust;s.poisonTicks=poisonTicks;s.scannedUntil=scannedUntil;
        s.constructorRoute=constructorRoute;s.bossPhase=bossPhase;s.phantomUntil=phantomUntil;s.phantomX=phantomX;s.phantomY=phantomY;
        if(tiles!=null){s.tiles=new Tile[width][];s.visible=new boolean[width][];s.explored=new boolean[width][];s.light=new float[width][];for(int x=0;x<width;x++){s.tiles[x]=tiles[x].clone();s.visible[x]=visible[x].clone();s.explored[x]=explored[x].clone();s.light[x]=light[x].clone();}}
        for(var a:actors)s.actors.add(a.copy());for(var i:floorItems)s.floorItems.add(i.copy());for(var i:inventory)s.inventory.add(i.copy());
        s.journal.addAll(journal);s.log.addAll(log);s.flags.addAll(flags);s.identifiedMedicine.addAll(identifiedMedicine);s.medicineEffects.putAll(medicineEffects);return s;
    }
}
