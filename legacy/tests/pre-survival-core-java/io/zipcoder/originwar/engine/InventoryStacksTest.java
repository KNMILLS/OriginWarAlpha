package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.GameState;
import io.zipcoder.originwar.model.PlayerAction;
import org.junit.jupiter.api.Test;
import java.util.*;
import static io.zipcoder.originwar.model.GameState.ItemType.*;
import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class InventoryStacksTest {
    private static GameState.Item item(String id,GameState.ItemType type,String name,int power,int quantity){
        var i=new GameState.Item();i.id=id;i.type=type;i.name=name;i.power=power;i.quantity=quantity;i.x=i.y=-1;return i;
    }
    private static GameState.Item floor(GameState s,GameState.Item i){i.x=s.playerX;i.y=s.playerY;return i;}
    private static void medicine(GameState s){s.medicineEffects.putAll(Map.of("Velorin","heal","Nexax","antidote","Caldex","stimulant","Pharax","toxin"));}
    private static List<String> identity(List<GameState.Item> items){return items.stream().map(i->i.id+":"+i.quantity+":"+i.x+":"+i.y).toList();}
    private static void unchanged(GameState before,GameState after){
        assertEquals(before.tick,after.tick);assertEquals(before.reserve,after.reserve);assertEquals(before.hp,after.hp);
        assertEquals(before.combatRng,after.combatRng);assertEquals(before.lootRng,after.lootRng);
        assertEquals(before.medicineRng,after.medicineRng);assertEquals(before.perceptionRng,after.perceptionRng);
        assertEquals(before.flags,after.flags);assertEquals(before.log,after.log);
        assertEquals(identity(before.inventory),identity(after.inventory));assertEquals(identity(before.floorItems),identity(after.floorItems));
    }
    @Test void combinesQuantitiesInFirstSeenOrderWithoutMergingOrLeakingSavedRecords(){
        var a=item("a",OXYGEN,"Tank",360,2);var b=item("b",CELL,"Cells",12,4);var c=item("c",OXYGEN,"Tank",360,3);
        var inventory=List.of(a,b,c);var rows=InventoryStacks.group(inventory);
        assertEquals(2,rows.size());assertEquals(5,rows.getFirst().quantity());assertEquals(4,rows.get(1).quantity());
        assertEquals(List.of("a","c"),rows.getFirst().items().stream().map(i->i.id).toList());
        rows.getFirst().items().getFirst().quantity=99;assertEquals(2,a.quantity);
        assertEquals(List.of("a","b","c"),inventory.stream().map(i->i.id).toList());
    }
    @Test void separatesPotenciesAndCompoundsButIgnoresUnusedMedicineAndGearPower(){
        var inventory=List.of(item("a",OXYGEN,"Tank",360,1),item("b",OXYGEN,"Tank",180,1),
            item("c",CELL,"Cells",12,1),item("d",CELL,"Cells",24,1),
            item("e",MEDICINE,"Velorin",1,1),item("f",MEDICINE,"Velorin",999,2),item("g",MEDICINE,"Nexax",1,1),
            item("h",RIFLE,"Rifle",15,1),item("i",RIFLE,"Rifle",0,1));
        var rows=InventoryStacks.group(inventory);assertEquals(7,rows.size());
        assertEquals(3,rows.get(4).quantity());assertEquals(2,rows.get(6).quantity());
    }
    @Test void startingTanksShareOneLogicalSlotAndKeepBothOriginalIdentities(){
        var run=new RunEngine(2115);var before=run.saveState();var rows=InventoryStacks.group(before.inventory);
        assertEquals(1,rows.size());assertEquals(2,rows.getFirst().quantity());assertEquals(2,before.inventory.size());
        assertEquals(2,before.inventory.stream().map(i->i.id).distinct().count());unchanged(before,run.saveState());
    }
    @Test void usingStackedBatteriesAndMedicineConsumesOnePackOrDoseWithItsActualEffect(){
        var s=RunEngineTest.room();medicine(s);s.hp=30;s.batteries=5;
        s.inventory.add(item("cells",CELL,"Cells",12,3));s.inventory.add(item("doses",MEDICINE,"Velorin",999,3));
        var run=new RunEngine(s);assertTrue(run.submit(PlayerAction.item(USE,"cells")).accepted());
        assertEquals(17,run.saveState().batteries);assertEquals(2,run.saveState().inventory.getFirst().quantity);
        assertTrue(run.submit(PlayerAction.item(USE,"doses")).accepted());var after=run.saveState();
        assertEquals(62,after.hp);assertEquals(2,after.inventory.getLast().quantity);assertTrue(after.identifiedMedicine.contains("Velorin"));
    }
    @Test void fullStackCapacityStillAcceptsMatchingPickupAndAutoPickupSkipsAnIneligibleNewStack(){
        var s=RunEngineTest.room();
        for(int n=0;n<RunEngine.MAX_INVENTORY;n++)s.inventory.add(item("held-"+n,OXYGEN,"Tank "+n,120,1));
        s.floorItems.add(floor(s,item("new",OXYGEN,"New tank",120,1)));
        s.floorItems.add(floor(s,item("matching",OXYGEN,"Tank 0",120,1)));
        var run=new RunEngine(s);var before=run.saveState();
        assertFalse(run.submit(PlayerAction.item(PICKUP,"new")).accepted());unchanged(before,run.saveState());
        assertTrue(run.submit(PlayerAction.of(PICKUP)).accepted());var after=run.saveState();
        assertEquals(25,after.inventory.size());assertEquals(24,InventoryStacks.count(after.inventory));
        assertEquals(2,InventoryStacks.group(after.inventory).getFirst().quantity());assertEquals("new",after.floorItems.getFirst().id);
        before=after;assertFalse(run.submit(PlayerAction.of(PICKUP)).accepted());unchanged(before,run.saveState());
    }
    @Test void droppingOneFromOldQuantityRecordIsDeterministicBoundedAndCollisionFree(){
        var s=RunEngineTest.room();String longId="x".repeat(200);
        s.inventory.add(item(longId,OXYGEN,"Tank",120,3));
        s.inventory.add(item("drop-d1-t0-1",OXYGEN,"Other tank",180,1));
        s.floorItems.add(floor(s,item("drop-d1-t0-0",OXYGEN,"Ground tank",240,1)));
        var run=new RunEngine(s);var control=new RunEngine(s);
        assertTrue(run.submit(PlayerAction.item(DROP,longId)).accepted());assertTrue(control.submit(PlayerAction.item(DROP,longId)).accepted());
        var after=run.saveState();assertEquals(2,after.inventory.getFirst().quantity);
        var dropped=after.floorItems.getLast();assertEquals("drop-d1-t0-2",dropped.id);assertEquals(1,dropped.quantity);assertTrue(dropped.id.length()<=200);
        assertEquals(identity(after.floorItems),identity(control.saveState().floorItems));
        assertEquals(s.combatRng,after.combatRng);assertEquals(s.lootRng,after.lootRng);assertEquals(s.medicineRng,after.medicineRng);assertEquals(s.perceptionRng,after.perceptionRng);
        assertDoesNotThrow(()->new RunEngine(after));assertTrue(run.submit(PlayerAction.item(PICKUP,dropped.id)).accepted());
        assertEquals(3,InventoryStacks.group(run.saveState().inventory).getFirst().quantity());
    }
    @Test void droppingSpareGearKeepsEquippedIdentityUntilTheLastEquippedUnitIsDropped(){
        var s=RunEngineTest.room();s.weapon="rifle";s.flags.add("equipped-weapon:equipped");
        s.inventory.add(item("equipped",RIFLE,"Rifle",15,1));s.inventory.add(item("spare",RIFLE,"Rifle",15,1));
        var stack=InventoryStacks.group(s.inventory).getFirst();assertEquals("spare",stack.actionId(DROP,s.flags));assertFalse(stack.dropsEquipped(s.flags));
        var run=new RunEngine(s);assertTrue(run.submit(PlayerAction.item(DROP,stack.actionId(DROP,s.flags))).accepted());
        var after=run.saveState();assertEquals("rifle",after.weapon);assertTrue(after.flags.contains("equipped-weapon:equipped"));
        stack=InventoryStacks.group(after.inventory).getFirst();assertTrue(stack.dropsEquipped(after.flags));
        assertTrue(run.submit(PlayerAction.item(DROP,stack.actionId(DROP,after.flags))).accepted());assertEquals("pistol",run.saveState().weapon);
        assertFalse(run.saveState().flags.contains("equipped-weapon:equipped"));
    }
    @Test void splittingAnEquippedMultiQuantityRecordDoesNotUnequipTheRemainingUnit(){
        var s=RunEngineTest.room();s.weapon="rifle";s.flags.add("equipped-weapon:rifle");s.inventory.add(item("rifle",RIFLE,"Rifle",15,2));
        var run=new RunEngine(s);assertTrue(run.submit(PlayerAction.item(DROP,"rifle")).accepted());
        var after=run.saveState();assertEquals(1,after.inventory.getFirst().quantity);assertEquals("rifle",after.weapon);assertTrue(after.flags.contains("equipped-weapon:rifle"));
        assertFalse(InventoryStacks.group(List.of(item("spare",PISTOL,"Pistol",10,1))).getFirst().equipped(Set.of()));
    }
    @Test void selectionFollowsTheStackWhenConsumingItsFirstRecordReordersRows(){
        var s=RunEngineTest.room();medicine(s);s.reserve=1;
        s.inventory.add(item("a",OXYGEN,"Tank",120,1));s.inventory.add(item("dose",MEDICINE,"Velorin",1,1));s.inventory.add(item("b",OXYGEN,"Tank",120,1));
        var old=InventoryStacks.group(s.inventory).getFirst().key();var run=new RunEngine(s);
        assertTrue(run.submit(PlayerAction.item(USE,"a")).accepted());var rows=InventoryStacks.group(run.saveState().inventory);
        assertEquals(1,InventoryStacks.selectionAfter(rows,old,0));assertEquals(old,rows.get(1).key());
        assertTrue(run.submit(PlayerAction.item(USE,"b")).accepted());rows=InventoryStacks.group(run.saveState().inventory);
        assertEquals(0,InventoryStacks.selectionAfter(rows,old,1));assertEquals(0,InventoryStacks.selectionAfter(List.of(),old,1));
    }
    @Test void floorAndCarriedRecordSafetyLimitsRejectBeforeAnyMutationOrTurn(){
        var s=RunEngineTest.room();s.inventory.add(item("bundle",OXYGEN,"Tank",120,3));
        for(int n=0;n<RunEngine.MAX_FLOOR_ITEMS;n++)s.floorItems.add(floor(s,item("floor-"+n,OXYGEN,"Tank",120,1)));
        var run=new RunEngine(s);var before=run.saveState();assertFalse(run.submit(PlayerAction.item(DROP,"bundle")).accepted());unchanged(before,run.saveState());
        s=RunEngineTest.room();for(int n=0;n<RunEngine.MAX_INVENTORY_RECORDS;n++)s.inventory.add(item("held-"+n,OXYGEN,"Tank",120,1));
        s.floorItems.add(floor(s,item("ground",OXYGEN,"Tank",120,1)));run=new RunEngine(s);before=run.saveState();
        assertFalse(run.submit(PlayerAction.of(PICKUP)).accepted());unchanged(before,run.saveState());
        s.inventory.add(item("overflow",OXYGEN,"Tank",120,1));var oversized=s;assertThrows(IllegalArgumentException.class,()->new RunEngine(oversized));
        s.inventory.removeLast();s.inventory.set(0,null);var malformed=s;assertThrows(IllegalArgumentException.class,()->new RunEngine(malformed));
    }
}
