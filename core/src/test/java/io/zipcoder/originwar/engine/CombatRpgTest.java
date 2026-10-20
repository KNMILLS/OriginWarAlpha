package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class CombatRpgTest {
    @Test void visibleHostileSharingFriendlyTileRemainsShootableWithoutFriendlyDamage(){
        var s=withEnemy(GameState.ActorKind.SIRS,4,2);s.combatRng=stateForRoll(0);
        var alice=new GameState.Actor();alice.id="story-alice";alice.kind=GameState.ActorKind.ALICE;alice.x=4;alice.y=2;s.actors.add(alice);
        var run=new RunEngine(s);assertTrue(run.submit(PlayerAction.at(SHOOT,4,2)).accepted());
        assertEquals(23,run.saveState().batteries);assertEquals(4,run.saveState().actors.getFirst().hp);assertEquals(alice.hp,run.saveState().actors.getLast().hp);
    }
    @Test void legalEmptyShotCostsAmmoTimeAndCombatRandomnessWithoutCreatingAnActor() {
        var run = new RunEngine(RunEngineTest.room());
        var before = run.saveState();
        var shot = run.submit(PlayerAction.at(SHOOT, 4, 2));
        assertTrue(shot.accepted()); assertEquals(1, shot.cost());
        var after = run.saveState();
        assertEquals(23, after.batteries); assertEquals(1, after.tick); assertEquals(359, after.reserve);
        assertEquals(DeterministicRandom.nextState(before.combatRng), after.combatRng);
        assertEquals(before.lootRng, after.lootRng); assertEquals(before.perceptionRng, after.perceptionRng);
        assertTrue(after.actors.isEmpty()); assertTrue(shot.events().stream().anyMatch(e -> e.type().equals("impact")));
    }

    @Test void insufficientAmmoBlockedLosAndOutOfRangeShotsRejectWithoutTimeOrRandomness() {
        var s = RunEngineTest.room(); s.weapon = "rifle"; s.batteries = 1;
        var run = new RunEngine(s);
        assertFalse(run.submit(PlayerAction.at(SHOOT, 4, 2)).accepted());
        assertEquals(0, run.saveState().tick); assertEquals(s.combatRng, run.saveState().combatRng);
        s.weapon = "pistol"; s.batteries = 24; s.tiles[2][2] = Tile.WALL;
        assertFalse(new RunEngine(s).submit(PlayerAction.at(SHOOT, 4, 2)).accepted());
        s.tiles[2][2] = Tile.FLOOR;
        assertFalse(new RunEngine(s).submit(PlayerAction.at(SHOOT, 10, 2)).accepted());
    }

    @Test void glassPreservesVisionButStopsProjectiles() {
        var s = RunEngineTest.room(); s.tiles[2][2] = Tile.GLASS;
        var run = new RunEngine(s);
        assertTrue(run.saveState().visible[4][2]);
        assertFalse(run.submit(PlayerAction.at(SHOOT, 4, 2)).accepted());
        assertEquals(24, run.saveState().batteries);
    }

    @Test void dexAndCoverAffectRealHitChance() {
        var s = withEnemy(GameState.ActorKind.SIRS, 4, 2); s.combatRng = stateForRoll(80);
        s.dex = 1;
        var low = new RunEngine(s); low.submit(PlayerAction.at(SHOOT, 4, 2));
        s.dex = 10;
        var high = new RunEngine(s); high.submit(PlayerAction.at(SHOOT, 4, 2));
        assertEquals(14, low.saveState().actors.getFirst().hp); assertEquals(4, high.saveState().actors.getFirst().hp);
        s.dex = 4; s.combatRng = stateForRoll(60);
        var open = new RunEngine(s); open.submit(PlayerAction.at(SHOOT, 4, 2));
        s.tiles[2][2] = Tile.COVER;
        var covered = new RunEngine(s); covered.submit(PlayerAction.at(SHOOT, 4, 2));
        assertEquals(4, open.saveState().actors.getFirst().hp); assertEquals(14, covered.saveState().actors.getFirst().hp);
    }

    @Test void gunRolesConsumeTheirActualBatteryCostsAndRanges() {
        for (String id : List.of("pistol", "rifle", "scattergun")) {
            var s = RunEngineTest.room(); s.weapon = id;
            var run = new RunEngine(s);
            assertTrue(run.submit(PlayerAction.at(SHOOT, 4, 2)).accepted());
            assertEquals(24 - EquipmentRules.weapon(id).batteries(), run.saveState().batteries);
        }
        assertEquals(11, EquipmentRules.weapon("rifle").range());
        assertTrue(EquipmentRules.weapon("scattergun").damage() > EquipmentRules.weapon("pistol").damage());
    }

    @Test void meleeBumpUsesStrengthAndDoesNotMoveThroughTheEnemy() {
        var s = withEnemy(GameState.ActorKind.SIRS, 2, 2); s.str = 6; s.combatRng = stateForRoll(0);
        var run = new RunEngine(s);
        var hit = run.submit(PlayerAction.move(1, 0));
        assertTrue(hit.accepted()); assertEquals(1, hit.cost());
        assertTrue(run.saveState().actors.getFirst().dead); assertEquals(1, run.saveState().playerX);
        assertTrue(hit.events().stream().anyMatch(e -> e.type().equals("melee")));
        assertTrue(run.submit(PlayerAction.move(1, 0)).accepted()); assertEquals(2, run.saveState().playerX);
    }

    @Test void actualKillEarnsLevelAndOnlyEarnedPointsAllocateWithoutHealingOrTime() {
        var s = withEnemy(GameState.ActorKind.SIRS, 2, 2); s.actors.getFirst().hp = 1;
        s.hp = 60; s.xp = 29; s.combatRng = stateForRoll(0);
        var run = new RunEngine(s);
        assertFalse(run.submit(PlayerAction.item(ALLOCATE, "dex")).accepted());
        run.submit(PlayerAction.of(MELEE));
        var earned = run.saveState();
        assertEquals(2, earned.level); assertEquals(11, earned.xp); assertEquals(2, earned.attributePoints); assertEquals(60, earned.hp);
        assertTrue(run.submit(PlayerAction.item(ALLOCATE, "int")).accepted());
        assertTrue(run.submit(PlayerAction.item(ALLOCATE, "dex")).accepted());
        assertFalse(run.submit(PlayerAction.item(ALLOCATE, "str")).accepted());
        assertEquals(5, run.saveState().intel); assertEquals(5, run.saveState().dex); assertEquals(0, run.saveState().attributePoints);
        assertEquals(earned.tick, run.saveState().tick); assertEquals(earned.reserve, run.saveState().reserve);
        assertEquals(earned.combatRng, run.saveState().combatRng);
    }

    @Test void armorMitigationAndDexDodgeChangeEnemyResults() {
        var s = withEnemy(GameState.ActorKind.TURRET, 4, 2); s.tick = 2; s.dex = 1; s.combatRng = stateForRoll(60);
        var low = new RunEngine(s); low.submit(PlayerAction.of(WAIT));
        s.dex = 10;
        var agile = new RunEngine(s); agile.submit(PlayerAction.of(WAIT));
        assertEquals(94, low.saveState().hp); assertEquals(100, agile.saveState().hp);
        s.dex = 4; s.combatRng = stateForRoll(0);
        var suit = new RunEngine(s); suit.submit(PlayerAction.of(WAIT));
        s.armor = "heavy";
        var heavy = new RunEngine(s); heavy.submit(PlayerAction.of(WAIT));
        assertEquals(94, suit.saveState().hp); assertEquals(98, heavy.saveState().hp);
    }

    @Test void equipAndDropPreserveRealItemsAndReturnToIssuedFallbacks() {
        var s = RunEngineTest.room();
        s.inventory.add(item(GameState.ItemType.RIFLE, "rifle-1", "Rifle", 15));
        s.inventory.add(item(GameState.ItemType.ARMOR_LIGHT, "armor-1", "Light armor", 3));
        var run = new RunEngine(s);
        assertTrue(run.submit(PlayerAction.item(EQUIP, "rifle-1")).accepted());
        assertTrue(run.submit(PlayerAction.item(EQUIP, "armor-1")).accepted());
        assertEquals("rifle", run.saveState().weapon); assertEquals("light", run.saveState().armor);
        assertEquals(5, EquipmentRules.effectiveDex(run.saveState())); assertEquals(2, run.saveState().inventory.size());
        assertFalse(run.submit(PlayerAction.item(EQUIP, "armor-1")).accepted());
        assertTrue(run.submit(PlayerAction.item(DROP, "rifle-1")).accepted());
        assertEquals("pistol", run.saveState().weapon); assertEquals("rifle-1", run.saveState().floorItems.getFirst().id);
        assertTrue(run.submit(PlayerAction.item(DROP, "armor-1")).accepted());
        assertEquals("suit", run.saveState().armor); assertEquals(4, run.saveState().tick);
    }

    @Test void batteryPacksAndEmptyUseShortcutCannotSelectMedicine() {
        var s = RunEngineTest.room(); s.medicineEffects.put("Velorin", "heal"); s.hp = 50;
        s.inventory.add(item(GameState.ItemType.MEDICINE, "dose", "Velorin", 999));
        s.inventory.add(item(GameState.ItemType.CELL, "cells", "Cells", 12));
        var run = new RunEngine(s);
        assertFalse(run.submit(PlayerAction.of(USE)).accepted()); assertEquals(50, run.saveState().hp);
        assertTrue(run.submit(PlayerAction.item(USE, "cells")).accepted()); assertEquals(36, run.saveState().batteries);
        assertEquals(1, run.saveState().inventory.size());
    }

    @Test void medicineNameMappingKnowledgeAndEffectsSurviveUseDropAndRestore() {
        var s = RunEngineTest.room(); s.hp = 20; s.medicineEffects.put("Velorin", "heal");
        s.inventory.add(item(GameState.ItemType.MEDICINE, "dose-a", "Velorin", 999));
        s.inventory.add(item(GameState.ItemType.MEDICINE, "dose-b", "Velorin", 1));
        var run = new RunEngine(s);
        assertTrue(run.submit(PlayerAction.item(USE, "dose-a")).accepted());
        assertEquals(52, run.saveState().hp); assertTrue(run.saveState().identifiedMedicine.contains("Velorin"));
        assertTrue(run.submit(PlayerAction.item(DROP, "dose-b")).accepted());
        run = new RunEngine(run.saveState());
        assertTrue(run.submit(PlayerAction.item(PICKUP, "dose-b")).accepted());
        assertTrue(run.submit(PlayerAction.item(USE, "dose-b")).accepted());
        assertEquals(84, run.saveState().hp); assertEquals(1, run.saveState().identifiedMedicine.size());
    }

    @Test void medicineRandomizationUsesOnlyItsOwnSavedStream() {
        var a = new RunEngine(116).saveState(); var b = new RunEngine(116).saveState(); var c = new RunEngine(117).saveState();
        assertEquals(4, a.medicineEffects.size()); assertEquals(a.medicineEffects, b.medicineEffects);
        assertEquals(a.medicineRng, b.medicineRng); assertNotEquals(a.medicineEffects, c.medicineEffects);
        assertTrue(a.medicineEffects.values().containsAll(MedicineRules.EFFECTS)); assertTrue(a.identifiedMedicine.isEmpty());
    }

    @Test void medicalTerminalIdentifiesWithoutConsumingTheDoseAndCostsTime() {
        var s = RunEngineTest.room(); s.playerX = 3; s.playerY = 2; s.medicineEffects.put("Velorin", "heal");
        s.inventory.add(item(GameState.ItemType.MEDICINE, "dose", "Velorin", 1));
        var run = new RunEngine(s);
        var result = run.submit(PlayerAction.of(INTERACT));
        assertTrue(result.accepted()); assertEquals(1, result.cost());
        assertTrue(run.saveState().identifiedMedicine.contains("Velorin")); assertEquals(1, run.saveState().inventory.size());
        assertEquals(359, run.saveState().reserve);
    }

    @Test void mildToxinIsWarnedFiniteAndCannotItselfKillOrResurrectSuffocationDeath() {
        var s = RunEngineTest.room(); s.hp = 1; s.medicineEffects.put("Nexax", "toxin"); s.medicineEffects.put("Velorin", "heal");
        s.inventory.add(item(GameState.ItemType.MEDICINE, "dose", "Nexax", 1));
        var run = new RunEngine(s);
        var result = run.submit(PlayerAction.item(USE, "dose"));
        assertTrue(result.events().stream().anyMatch(e -> e.detail().contains("minimum one HP")));
        for (int i = 0; i < 3; i++) run.submit(PlayerAction.of(WAIT));
        assertEquals(1, run.saveState().hp); assertEquals(0, run.saveState().poisonTicks); assertFalse(run.saveState().dead);
        s.reserve = 0; s.oxygenation = 0; s.flags.add(MedicineRules.TOXIN_FLAG); s.poisonTicks = 1;
        run = new RunEngine(s);
        run.submit(PlayerAction.of(WAIT));
        assertEquals(0, run.saveState().hp); assertTrue(run.saveState().dead);
    }

    @Test void forceAndIntHackOfferDifferentActualLockedDoorCosts() {
        var s = RunEngineTest.room(); s.tiles[2][2] = Tile.LOCKED_DOOR;
        var run = new RunEngine(s);
        assertFalse(run.submit(PlayerAction.at(HACK, 2, 2)).accepted());
        var force = run.submit(PlayerAction.at(FORCE, 2, 2));
        assertEquals(3, force.cost()); assertEquals(Tile.DOOR_OPEN, run.saveState().tiles[2][2]); assertEquals(357, run.saveState().reserve);
        s.intel = 5;
        run = new RunEngine(s);
        assertEquals(1, run.submit(PlayerAction.at(HACK, 2, 2)).cost()); assertEquals(Tile.DOOR_OPEN, run.saveState().tiles[2][2]);
    }

    @Test void terminalHackActuallyUnlocksHatchAndDisablesSensorsAndTurrets() {
        var s = RunEngineTest.room(); s.intel = 5; s.playerX = 3; s.playerY = 2;
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.CAMERA, "camera", 7, 2));
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.TURRET, "turret", 8, 2));
        var run = new RunEngine(s);
        assertEquals(2, run.submit(PlayerAction.at(HACK, 3, 3)).cost());
        assertTrue(run.saveState().switchOn); assertTrue(run.saveState().flags.contains("disabled:camera"));
        assertTrue(run.saveState().flags.contains("disabled:turret")); assertEquals(100, run.saveState().hp);
        assertFalse(run.submit(PlayerAction.at(HACK, 3, 3)).accepted()); assertEquals(2, run.saveState().tick);
    }

    @Test void regularRoverHackDisablesItsRealMotorAndFuse() {
        var s = withEnemy(GameState.ActorKind.ROVER, 4, 2); s.intel = 6; s.actors.getFirst().fuse = 3;
        var run = new RunEngine(s);
        assertTrue(run.submit(PlayerAction.at(HACK, 4, 2)).accepted());
        for (int i = 0; i < 8; i++) run.submit(PlayerAction.of(WAIT));
        var rover = run.saveState().actors.getFirst();
        assertEquals(4, rover.x); assertEquals(-1, rover.fuse); assertTrue(run.saveState().flags.contains("disabled:enemy"));
        assertEquals(100, run.saveState().hp);
    }

    @Test void soldiersAdvanceDefenseDroidsHoldRangeAndRoversMoveEveryTick() {
        var s = withEnemy(GameState.ActorKind.SIRS, 6, 2);
        var soldier = new RunEngine(s); soldier.submit(PlayerAction.of(WAIT)); soldier.submit(PlayerAction.of(WAIT));
        assertEquals(5, soldier.saveState().actors.getFirst().x);
        s = withEnemy(GameState.ActorKind.ADD, 6, 2);
        var droid = new RunEngine(s); droid.submit(PlayerAction.of(WAIT));
        var fire = droid.submit(PlayerAction.of(WAIT));
        assertEquals(6, droid.saveState().actors.getFirst().x); assertTrue(fire.events().stream().anyMatch(e -> e.type().equals("gun")));
        s = withEnemy(GameState.ActorKind.ROVER, 6, 2);
        var rover = new RunEngine(s); rover.submit(PlayerAction.of(WAIT));
        assertEquals(5, rover.saveState().actors.getFirst().x);
    }

    @Test void camerasAlertNearbySecurityWithoutDirectDamage() {
        var s = RunEngineTest.room();
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.CAMERA, "camera", 4, 2));
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.SIRS, "remote", 8, 4));
        var run = new RunEngine(s); run.submit(PlayerAction.of(WAIT));
        var remote = run.saveState().actors.get(1);
        assertTrue(remote.alert > 0); assertEquals(1, remote.lastX); assertEquals(2, remote.lastY);
        assertEquals(100, run.saveState().hp);
    }

    @Test void hearingTracksTheSoundAndSearchExpiresWithoutOmniscientSight() {
        var s = withEnemy(GameState.ActorKind.SIRS, 4, 5);
        for (int y = 1; y < s.height - 1; y++) s.tiles[2][y] = Tile.WALL;
        var run = new RunEngine(s);
        run.submit(PlayerAction.at(SHOOT, 1, 3));
        assertTrue(run.saveState().actors.getFirst().alert > 0);
        assertEquals(1, run.saveState().actors.getFirst().lastX);
        for (int i = 0; i < 10; i++) run.submit(PlayerAction.of(WAIT));
        assertEquals(0, run.saveState().actors.getFirst().alert); assertEquals(100, run.saveState().hp);
    }

    @Test void roverTelegraphsThenAllowsTwoRealMovesToEscapeTheBlast() {
        var s = withEnemy(GameState.ActorKind.ROVER, 2, 2);
        var run = new RunEngine(s);
        var armed = run.submit(PlayerAction.of(WAIT));
        assertTrue(armed.events().stream().anyMatch(e -> e.type().equals("rover") && e.detail().contains("two ticks")));
        assertEquals(2, run.saveState().actors.getFirst().fuse);
        run.submit(PlayerAction.move(0, 1));
        var detonation = run.submit(PlayerAction.move(0, 1));
        assertTrue(detonation.events().stream().anyMatch(e -> e.type().equals("explosion")));
        assertEquals(100, run.saveState().hp); assertTrue(run.saveState().actors.getFirst().dead);
    }

    @Test void terrainTicksAdvanceEnemiesExactlyOncePerTickAndSnapshotsNeverDo() {
        var s = withEnemy(GameState.ActorKind.ROVER, 9, 2); s.tiles[2][2] = Tile.NO_GRAV;
        var run = new RunEngine(s);
        for (int i = 0; i < 100; i++) run.snapshot();
        assertEquals(9, run.saveState().actors.getFirst().x); assertEquals(0, run.saveState().tick);
        assertEquals(3, run.submit(PlayerAction.move(1, 0)).cost());
        assertEquals(6, run.saveState().actors.getFirst().x); assertEquals(3, run.saveState().tick);
    }

    @Test void selfInflictedArmedRoverDeathStillChargesTheActionButNoExtraEnemyTick() {
        var s = withEnemy(GameState.ActorKind.ROVER, 2, 2); s.hp = 1; s.combatRng = stateForRoll(0); s.actors.getFirst().fuse = 1;
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.SIRS, "later", 8, 6));
        var run = new RunEngine(s);
        var shot = run.submit(PlayerAction.at(SHOOT, 2, 2));
        assertTrue(run.saveState().dead); assertEquals(1, run.saveState().tick); assertEquals(23, run.saveState().batteries);
        assertEquals(1, shot.events().stream().filter(e -> e.type().equals("death")).count());
        assertEquals(8, run.saveState().actors.get(1).x);
    }

    @Test void passiveStoryActorsAreNeverAttackedOrUsedAsBlockingCombatPets() {
        for (var kind : List.of(GameState.ActorKind.ALICE, GameState.ActorKind.WEST, GameState.ActorKind.SURVIVOR, GameState.ActorKind.CARMACK)) {
            var s = RunEngineTest.room(); s.actors.add(EnemyTurns.actor(kind, "story-actor", 2, 2));
            var run = new RunEngine(s);
            assertFalse(run.submit(PlayerAction.at(SHOOT, 2, 2)).accepted()); assertFalse(run.submit(PlayerAction.at(MELEE, 2, 2)).accepted());
            assertTrue(run.submit(PlayerAction.move(1, 0)).accepted()); assertEquals(2, run.saveState().playerX);
            assertEquals(14, run.saveState().actors.getFirst().hp); assertEquals(100, run.saveState().hp);
        }
    }

    @Test void malformedRpgKnowledgeStatsAndActorsAreRejectedByPublicSaveValidator() {
        var negativeAmmo = RunEngineTest.room(); negativeAmmo.batteries = -1;
        assertThrows(IllegalArgumentException.class, () -> RunEngine.validateState(negativeAmmo));
        var knowledge = RunEngineTest.room(); knowledge.identifiedMedicine.add("no mapping");
        assertThrows(IllegalArgumentException.class, () -> RunEngine.validateState(knowledge));
        var actor = withEnemy(GameState.ActorKind.SIRS, 2, 2); actor.actors.getFirst().hp = -3;
        assertThrows(IllegalArgumentException.class, () -> RunEngine.validateState(actor));
        var weapon = RunEngineTest.room(); weapon.weapon = "invented";
        assertThrows(IllegalArgumentException.class, () -> RunEngine.validateState(weapon));
        var missingSupply = RunEngineTest.room(); missingSupply.medicineEffects.put("Nexax", "toxin");
        assertThrows(IllegalArgumentException.class, () -> RunEngine.validateState(missingSupply));
    }

    @Test void acceptedAttacksPersistPlayerAndRobotFacingWithoutChangingTargetRules() {
        var run = new RunEngine(RunEngineTest.room());
        assertTrue(run.submit(PlayerAction.at(SHOOT, 4, 4)).accepted()); assertEquals(1, run.saveState().facing);
        var s = withEnemy(GameState.ActorKind.SIRS, 1, 1); s.facing = 2;
        run = new RunEngine(s); assertTrue(run.submit(PlayerAction.at(MELEE, 1, 1)).accepted()); assertEquals(0, run.saveState().facing);
        s = withEnemy(GameState.ActorKind.SIRS, 4, 2); s.tick = 1;
        run = new RunEngine(s); run.submit(PlayerAction.of(WAIT)); assertEquals(3, run.saveState().actors.getFirst().facing);
    }

    static GameState withEnemy(GameState.ActorKind kind, int x, int y) {
        var s = RunEngineTest.room(); s.actors.add(EnemyTurns.actor(kind, "enemy", x, y)); return s;
    }
    static GameState.Item item(GameState.ItemType type, String id, String name, int power) {
        var item = new GameState.Item(); item.type = type; item.id = id; item.name = name; item.power = power; item.x = -1; item.y = -1; return item;
    }
    static long stateForRoll(int result) {
        for (long state = 0; state < 100000; state++)
            if (DeterministicRandom.bounded(DeterministicRandom.nextState(state), 100) == result) return state;
        throw new AssertionError("Could not select reproducible combat roll");
    }
}
