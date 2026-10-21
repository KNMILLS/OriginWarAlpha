package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class RunEngineTest {
    @Test void seedsReplayGeometryAndHaveIndependentStreams() {
        var a = new RunEngine(431).saveState();
        var b = new RunEngine(431).saveState();
        assertTrue(Arrays.deepEquals(a.tiles, b.tiles));
        assertEquals(a.playerX, b.playerX); assertEquals(a.playerY, b.playerY);
        assertEquals(a.lootRng, b.lootRng);
        assertNotEquals(a.combatRng, a.lootRng);
        assertNotEquals(a.combatRng, a.medicineRng);
        assertNotEquals(a.combatRng, a.perceptionRng);
        assertFalse(Arrays.deepEquals(a.tiles, new RunEngine(432).saveState().tiles));
        assertEquals("briefing", a.activeDialogue);
    }

    @Test void movementIsCardinalAndRejectedActionsChangeNothing() {
        var run = new RunEngine(room());
        var initial = run.saveState();
        assertFalse(run.submit(PlayerAction.move(1, 1)).accepted());
        assertFalse(run.submit(PlayerAction.move(Integer.MIN_VALUE, 0)).accepted());
        assertFalse(run.submit(PlayerAction.move(0, 0)).accepted());
        assertFalse(run.submit(PlayerAction.move(-1, 0)).accepted());
        assertFalse(run.submit(PlayerAction.at(SHOOT, -1, -1)).accepted());
        assertFalse(run.submit(PlayerAction.item(USE, "missing")).accepted());
        assertFalse(run.submit(PlayerAction.at(INTERACT, 8, 5)).accepted());
        assertFalse(run.submit(null).accepted());
        assertUnchanged(initial, run.saveState());
    }

    @Test void terrainAndWaitCostExactTicks() {
        var s = room();
        s.tiles[2][2] = Tile.LOW_GRAV;
        s.tiles[3][2] = Tile.NO_GRAV;
        s.tiles[4][2] = Tile.HAZARD;
        var run = new RunEngine(s);
        assertEquals(2, run.submit(PlayerAction.move(1, 0)).cost());
        assertEquals(3, run.submit(PlayerAction.move(1, 0)).cost());
        assertEquals(2, run.submit(PlayerAction.move(1, 0)).cost());
        assertEquals(1, run.submit(PlayerAction.of(WAIT)).cost());
        assertEquals(8, run.saveState().tick);
        assertEquals(352, run.saveState().reserve);
    }

    @Test void closedDoorsOpenBeforeMovingAndBlockVisionUntilOpened() {
        var s = room();
        for (int y = 1; y < s.height - 1; y++) s.tiles[2][y] = Tile.WALL;
        s.tiles[2][2] = Tile.DOOR_CLOSED;
        var run = new RunEngine(s);
        assertFalse(run.saveState().visible[3][2]);
        var opened = run.submit(PlayerAction.move(1, 0));
        assertTrue(opened.accepted()); assertEquals(1, opened.cost());
        assertEquals("door", opened.events().getFirst().type());
        assertEquals(1, run.saveState().playerX);
        assertEquals(Tile.DOOR_OPEN, run.saveState().tiles[2][2]);
        assertTrue(run.saveState().visible[3][2]);
        assertTrue(run.submit(PlayerAction.move(1, 0)).accepted());
        assertEquals(2, run.saveState().playerX);
    }

    @Test void rememberedTerrainSurvivesLeavingSightAndOpaqueWallsOcclude() {
        var s = room();
        s.width = 28; s.height = 12;
        initializeArrays(s);
        s.switchX = 24; s.switchY = 2; s.hatchX = 25; s.hatchY = 2; s.terminalX = 26; s.terminalY = 2;
        s.tiles[24][2] = Tile.SWITCH; s.tiles[25][2] = Tile.HATCH; s.tiles[26][2] = Tile.TERMINAL;
        s.tiles[2][3] = Tile.WALL;
        var run = new RunEngine(s);
        assertFalse(run.saveState().visible[3][4]);
        assertTrue(run.saveState().explored[1][2]);
        for (int x = 0; x < 15; x++) assertTrue(run.submit(PlayerAction.move(1, 0)).accepted());
        assertFalse(run.saveState().visible[1][2]);
        assertEquals(0f, run.saveState().light[1][2]);
        assertTrue(run.saveState().explored[1][2]);
    }

    @Test void pathDoesNotRevealUnknownTerrainAndDoesNotAdvanceRng() {
        var s = room();
        s.width = 28; s.height = 12; initializeArrays(s);
        s.switchX = 24; s.switchY = 2; s.hatchX = 25; s.hatchY = 2; s.terminalX = 26; s.terminalY = 2;
        s.tiles[24][2] = Tile.SWITCH; s.tiles[25][2] = Tile.HATCH; s.tiles[26][2] = Tile.TERMINAL;
        var run = new RunEngine(s);
        var before = run.saveState();
        assertTrue(run.pathTo(24, 2).isEmpty());
        assertTrue(run.pathTo(-1, -1).isEmpty());
        var path = run.pathTo(6, 2);
        assertEquals(5, path.size());
        assertEquals(path, run.pathTo(6, 2));
        assertThrows(UnsupportedOperationException.class, () -> path.clear());
        assertUnchanged(before, run.saveState());
        for (var step : path) assertTrue(run.submit(step).accepted());
        assertEquals(6, run.saveState().playerX);
    }

    @Test void knownDoorRouteIncludesOpeningAndAvoidsLockedDoor() {
        var s = room();
        for (int x = 0; x < s.width; x++) Arrays.fill(s.explored[x], true);
        for (int y = 1; y < s.height - 1; y++) s.tiles[2][y] = Tile.WALL;
        s.tiles[2][2] = Tile.DOOR_CLOSED;
        var run = new RunEngine(s);
        var path = run.pathTo(3, 2);
        assertEquals(3, path.size());
        for (var step : path) assertTrue(run.submit(step).accepted());
        assertEquals(3, run.saveState().playerX);
        s.tiles[2][2] = Tile.LOCKED_DOOR;
        assertTrue(new RunEngine(s).pathTo(3, 2).isEmpty());
    }

    @Test void finiteTanksPickupDropAndUseResolveBeforeConsumption() {
        var s = room();
        s.reserve = 0; s.oxygenation = 0; s.hp = 4;
        s.inventory.add(StationGenerator.tank(1, -1, -1, 120, "test"));
        var run = new RunEngine(s);
        var refilled = run.submit(PlayerAction.item(USE, "oxygen-d1-test"));
        assertTrue(refilled.accepted()); assertEquals(1, refilled.cost());
        var after = run.saveState();
        assertEquals(119, after.reserve); assertEquals(4, after.oxygenation); assertEquals(4, after.hp);
        assertFalse(after.dead); assertTrue(after.inventory.isEmpty());
        assertFalse(run.submit(PlayerAction.item(USE, "oxygen-d1-test")).accepted());

        s = room(); s.reserve = 100;
        s.floorItems.add(StationGenerator.tank(1, 1, 2, 120, "floor"));
        run = new RunEngine(s);
        assertTrue(run.submit(PlayerAction.of(PICKUP)).accepted());
        assertTrue(run.saveState().floorItems.isEmpty());
        assertEquals(-1, run.saveState().inventory.getFirst().x);
        assertTrue(run.submit(PlayerAction.item(DROP, "oxygen-d1-floor")).accepted());
        assertTrue(run.saveState().inventory.isEmpty());
        assertEquals(1, run.saveState().floorItems.getFirst().x);
        assertTrue(run.submit(PlayerAction.item(PICKUP, "oxygen-d1-floor")).accepted());
        assertTrue(run.submit(PlayerAction.of(USE)).accepted());
        assertEquals(216, run.saveState().reserve);
        assertEquals(4, run.saveState().tick);
    }

    @Test void aFullReserveRejectsTankUseWithoutWasteOrFreeRecovery() {
        var s = room(); s.oxygenation = 20;
        s.inventory.add(StationGenerator.tank(1, -1, -1, 120, "test"));
        var run = new RunEngine(s);
        var before = run.saveState();
        assertFalse(run.submit(PlayerAction.of(USE)).accepted());
        for (int i = 0; i < 20; i++) assertTrue(run.submit(PlayerAction.of(SCAN)).accepted());
        assertUnchanged(before, run.saveState());
        assertTrue(run.submit(PlayerAction.of(WAIT)).accepted());
        assertEquals(24, run.saveState().oxygenation);
    }

    @Test void reserveEmptiesBeforeOxygenationAndThenHealth() {
        var s = room(); s.reserve = 1;
        var run = new RunEngine(s);
        assertTrue(run.submit(PlayerAction.of(WAIT)).events().stream().anyMatch(e -> e.type().equals("warning")));
        assertEquals(100, run.saveState().oxygenation);
        for (int i = 0; i < 19; i++) run.submit(PlayerAction.of(WAIT));
        assertEquals(5, run.saveState().oxygenation); assertEquals(100, run.saveState().hp);
        run.submit(PlayerAction.of(WAIT));
        assertEquals(0, run.saveState().oxygenation); assertEquals(96, run.saveState().hp);
        for (int i = 0; i < 24; i++) run.submit(PlayerAction.of(WAIT));
        assertTrue(run.saveState().dead);
        assertEquals(0, run.saveState().hp);
        int deathTick = run.saveState().tick;
        assertFalse(run.submit(PlayerAction.of(WAIT)).accepted());
        assertEquals(deathTick, run.saveState().tick);
        assertFalse(new RunEngine(44).saveState().dead);
    }

    @Test void lowReserveWarnsOnceAndStatusUsesActionTicks() {
        var s = room(); s.reserve = 91; s.poisonTicks = 3; s.tiles[2][2] = Tile.NO_GRAV;
        var run = new RunEngine(s);
        var result = run.submit(PlayerAction.move(1, 0));
        assertEquals(1, result.events().stream().filter(e -> e.type().equals("warning")).count());
        assertEquals(97, run.saveState().hp); assertEquals(0, run.saveState().poisonTicks);
        assertEquals(88, run.saveState().reserve);
        assertTrue(run.submit(PlayerAction.of(WAIT)).events().isEmpty());
    }

    @Test void switchesAndTerminalsCannotBeRepeatedForFreeProgress() {
        var s = room(); s.playerX = 2; s.playerY = 3;
        var run = new RunEngine(s);
        assertTrue(run.submit(PlayerAction.of(INTERACT)).accepted());
        assertTrue(run.saveState().switchOn);
        int tick = run.saveState().tick;
        assertFalse(run.submit(PlayerAction.at(INTERACT, 2, 3)).accepted());
        assertEquals(tick, run.saveState().tick);
        s = room(); s.playerX = 3; s.playerY = 3;
        run = new RunEngine(s);
        assertTrue(run.submit(PlayerAction.of(INTERACT)).accepted());
        assertEquals(1, run.saveState().journal.size());
        assertFalse(run.submit(PlayerAction.of(INTERACT)).accepted());
        assertEquals(1, run.saveState().tick);
    }

    @Test void hatchRequiresItsSwitchAndStandingOnItThenChargesTransitionTick() {
        var s = room(); s.playerX = s.hatchX; s.playerY = s.hatchY;
        var run = new RunEngine(s);
        assertFalse(run.submit(PlayerAction.of(ADVANCE)).accepted());
        s.switchOn = true;
        run = new RunEngine(s);
        var result = run.submit(PlayerAction.of(ADVANCE));
        assertTrue(result.accepted()); assertEquals(1, result.cost());
        var after = run.saveState();
        assertEquals(2, after.deck); assertEquals(359, after.reserve); assertEquals(1, after.tick);
        assertFalse(after.switchOn); assertFalse(after.victory);
        assertEquals("hatch", result.events().getFirst().type());
        assertFalse(run.submit(PlayerAction.of(ADVANCE)).accepted());
    }

    @Test void finalDeckExplainsItsObjectiveWithoutSubstitutingAnEndingOrChargingRepeatedTime() {
        var s = room(); s.deck = 10; s.switchOn = true; s.playerX = s.hatchX; s.playerY = s.hatchY;
        var run = new RunEngine(s);
        var first = run.submit(PlayerAction.of(INTERACT));
        assertTrue(first.accepted()); assertEquals(0, first.cost());
        assertTrue(first.events().getFirst().detail().contains("Carmack"));
        assertEquals(1, run.saveState().journal.size());
        for (int i = 0; i < 10; i++) assertEquals(0, run.submit(PlayerAction.of(ADVANCE)).cost());
        assertEquals(0, run.saveState().tick); assertEquals(360, run.saveState().reserve);
        assertEquals(1, run.saveState().journal.size()); assertFalse(run.saveState().victory);
    }

    @Test void lethalTransitionTicksLeaveTheDepartingDeckAndGenerationRngUntouched() {
        for (boolean suffocation : new boolean[]{true, false}) {
            var s = room(); s.playerX = s.hatchX; s.playerY = s.hatchY; s.switchOn = true;
            s.reserve = suffocation ? 0 : 360; s.oxygenation = suffocation ? 0 : 100;
            s.hp = suffocation ? 4 : 1; s.poisonTicks = suffocation ? 0 : 1;
            var run = new RunEngine(s);
            var before = run.saveState();
            var result = run.submit(PlayerAction.of(ADVANCE));
            var after = run.saveState();
            assertTrue(result.accepted()); assertEquals(1, result.cost());
            assertTrue(after.dead); assertEquals(1, after.deck); assertEquals(1, after.tick);
            assertEquals(before.playerX, after.playerX); assertEquals(before.playerY, after.playerY);
            assertEquals(before.lootRng, after.lootRng); assertEquals(before.flags, after.flags);
            assertTrue(Arrays.deepEquals(before.tiles, after.tiles));
            assertTrue(result.events().stream().noneMatch(e -> e.type().equals("hatch")));
            assertTrue(result.events().stream().anyMatch(e -> e.type().equals("death")));
        }
    }

    @Test void fullInventoryRejectsPickupBeforeRemovingAnItemOrSpendingTimeOrRng() {
        var s = room();
        // Capacity counts distinct logical stacks; matching tanks deliberately share a slot.
        for (int i = 0; i < RunEngine.MAX_INVENTORY; i++) {
            var item=StationGenerator.tank(1, -1, -1, 120, "pack-" + i);item.name="Distinct tank "+i;s.inventory.add(item);
        }
        s.floorItems.add(StationGenerator.tank(1, s.playerX, s.playerY, 120, "on-floor"));
        var run = new RunEngine(s);
        var before = run.saveState();
        assertFalse(run.submit(PlayerAction.of(PICKUP)).accepted());
        assertUnchanged(before, run.saveState());
        assertEquals(RunEngine.MAX_INVENTORY, run.saveState().inventory.size());
        assertEquals("oxygen-d1-on-floor", run.saveState().floorItems.getFirst().id);
        assertTrue(run.submit(PlayerAction.item(DROP, "oxygen-d1-pack-0")).accepted());
        assertTrue(run.submit(PlayerAction.item(PICKUP, "oxygen-d1-on-floor")).accepted());
        assertEquals(RunEngine.MAX_INVENTORY, run.saveState().inventory.size());
        assertEquals("oxygen-d1-pack-0", run.saveState().floorItems.getFirst().id);
    }

    @Test void restoringAndPresentationCopiesCannotWriteLiveState() {
        var s = room();
        s.inventory.add(StationGenerator.tank(1, -1, -1, 120, "test"));
        var run = new RunEngine(s);
        s.hp = 1; s.tiles[1][2] = Tile.WALL; s.inventory.getFirst().power = 999;
        var view = run.snapshot().world();
        view.hp = 2; view.tiles[1][2] = Tile.WALL; view.inventory.clear(); view.flags.add("fake");
        var save = run.saveState(); save.explored[1][2] = false; save.journal.add("fake");
        assertEquals(100, run.saveState().hp); assertEquals(Tile.FLOOR, run.saveState().tiles[1][2]);
        assertEquals(120, run.saveState().inventory.getFirst().power);
        assertFalse(run.saveState().flags.contains("fake")); assertTrue(run.saveState().journal.isEmpty());
        assertTrue(run.saveState().explored[1][2]);
    }

    @Test void restoredReplayProducesIdenticalRulesAndEvents() {
        var first = new RunEngine(room());
        first.submit(PlayerAction.move(1, 0));
        var second = new RunEngine(first.saveState());
        for (var action : new PlayerAction[]{PlayerAction.move(1, 0), PlayerAction.of(WAIT), PlayerAction.of(SCAN), PlayerAction.move(0, 1)}) {
            assertEquals(first.submit(action), second.submit(action));
            assertUnchanged(first.saveState(), second.saveState());
        }
    }

    @Test void dialogueChoiceIsFreeAndWorldActionsAreBlockedWhileItIsOpen() {
        var s = room(); s.activeDialogue = "later-story";
        var run = new RunEngine(s);
        assertFalse(run.submit(PlayerAction.of(WAIT)).accepted());
        assertTrue(run.submit(PlayerAction.of(CHOOSE)).accepted());
        assertEquals(0, run.saveState().tick);
        assertTrue(run.submit(PlayerAction.of(WAIT)).accepted());
    }

    @Test void hostileBumpsCommitMeleeButAliceDoesNotBlock() {
        var s = room(); var actor = new GameState.Actor(); actor.id = "robot"; actor.x = 2; actor.y = 2;
        s.actors.add(actor);
        var hostile = new RunEngine(s);
        assertTrue(hostile.submit(PlayerAction.move(1, 0)).accepted());
        assertEquals(1, hostile.saveState().playerX);
        actor.kind = GameState.ActorKind.ALICE;
        assertTrue(new RunEngine(s).submit(PlayerAction.move(1, 0)).accepted());
    }

    @Test void malformedRestoresFailBeforeAllocatingOrCopyingUnsafeState() {
        var s = room(); s.width = Integer.MAX_VALUE;
        assertThrows(IllegalArgumentException.class, () -> new RunEngine(s));
        var badTile = room(); badTile.tiles[2][2] = null;
        assertThrows(IllegalArgumentException.class, () -> new RunEngine(badTile));
        var badArray = room(); badArray.visible[2] = null;
        assertThrows(IllegalArgumentException.class, () -> new RunEngine(badArray));
        var badItem = room(); badItem.inventory.add(new GameState.Item());
        assertThrows(IllegalArgumentException.class, () -> new RunEngine(badItem));
        var badDeath = room(); badDeath.hp = 0;
        assertThrows(IllegalArgumentException.class, () -> new RunEngine(badDeath));
        var oversizedPack = room();
        for (int i = 0; i <= RunEngine.MAX_INVENTORY; i++) {
            var item=StationGenerator.tank(1, -1, -1, 120, "oversized-" + i);item.name="Distinct tank "+i;oversizedPack.inventory.add(item);
        }
        assertThrows(IllegalArgumentException.class, () -> new RunEngine(oversizedPack));
    }

    static GameState room() {
        var s = new GameState();
        s.width = 12; s.height = 9; s.playerX = 1; s.playerY = 2;
        s.switchX = 2; s.switchY = 3; s.hatchX = 8; s.hatchY = 5; s.terminalX = 3; s.terminalY = 3;
        s.activeDialogue = "";
        initializeArrays(s);
        s.tiles[s.switchX][s.switchY] = Tile.SWITCH;
        s.tiles[s.hatchX][s.hatchY] = Tile.HATCH;
        s.tiles[s.terminalX][s.terminalY] = Tile.TERMINAL;
        return s;
    }

    private static void initializeArrays(GameState s) {
        s.tiles = new Tile[s.width][s.height]; s.visible = new boolean[s.width][s.height];
        s.explored = new boolean[s.width][s.height]; s.light = new float[s.width][s.height];
        for (int x = 0; x < s.width; x++) for (int y = 0; y < s.height; y++)
            s.tiles[x][y] = x == 0 || y == 0 || x == s.width - 1 || y == s.height - 1 ? Tile.WALL : Tile.FLOOR;
    }

    private static void assertUnchanged(GameState a, GameState b) {
        assertEquals(a.tick, b.tick); assertEquals(a.playerX, b.playerX); assertEquals(a.playerY, b.playerY);
        assertEquals(a.hp, b.hp); assertEquals(a.reserve, b.reserve); assertEquals(a.oxygenation, b.oxygenation);
        assertEquals(a.combatRng, b.combatRng); assertEquals(a.lootRng, b.lootRng);
        assertEquals(a.medicineRng, b.medicineRng); assertEquals(a.perceptionRng, b.perceptionRng);
        assertEquals(a.flags, b.flags); assertEquals(a.journal, b.journal); assertEquals(a.log, b.log);
        assertTrue(Arrays.deepEquals(a.tiles, b.tiles)); assertTrue(Arrays.deepEquals(a.explored, b.explored));
    }
}
