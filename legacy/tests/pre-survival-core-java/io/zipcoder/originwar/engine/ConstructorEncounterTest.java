package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

/** Focused production encounter fixtures; complete seeded-run evidence is in CombatPlaythroughTest. */
class ConstructorEncounterTest {
    @Test void wavesSpawnWithTelegraphAndNeverTakeAnExtraSpawnTick() {
        var run = new RunEngine(encounter());
        for (int i = 0; i < 6; i++) run.submit(PlayerAction.of(WAIT));
        var s = run.saveState();
        var rover = s.actors.stream().filter(a -> a.kind == GameState.ActorKind.ROVER).findFirst().orElseThrow();
        assertEquals("active", s.bossPhase); assertEquals(18, rover.fuse);
        assertEquals(8, rover.x); assertEquals(2, rover.y); assertEquals(354, s.reserve);
    }

    @Test void shieldRejectsDirectDamageButNotItsShotCost() {
        var run = new RunEngine(encounter());
        var shot = run.submit(PlayerAction.at(SHOOT, 8, 3));
        assertTrue(shot.accepted()); assertEquals(23, run.saveState().batteries);
        assertEquals(52, run.saveState().actors.getFirst().hp);
        assertTrue(shot.events().stream().anyMatch(e -> e.detail().contains("shielding")));
        assertFalse(run.saveState().flags.contains("constructor-defeated"));
    }

    @Test void twoActuallyRedirectedWavesDestroyTheMachineAndPreserveTheSavedMidBlastPhase() {
        var s = encounter(); s.intel = 6;
        var run = new RunEngine(s);
        for (int i = 0; i < 6; i++) run.submit(PlayerAction.of(WAIT));
        assertTrue(run.submit(PlayerAction.at(HACK, 8, 2)).accepted());
        run.submit(PlayerAction.of(WAIT));
        var restored = new RunEngine(run.saveState());
        assertEquals(run.submit(PlayerAction.of(WAIT)), restored.submit(PlayerAction.of(WAIT)));
        assertEquals(26, run.saveState().actors.getFirst().hp);
        for (int i = 0; i < 3; i++) assertEquals(run.submit(PlayerAction.of(WAIT)), restored.submit(PlayerAction.of(WAIT)));
        assertTrue(run.submit(PlayerAction.at(HACK, 8, 2)).accepted());
        assertTrue(restored.submit(PlayerAction.at(HACK, 8, 2)).accepted());
        for (int i = 0; i < 2; i++) assertEquals(run.submit(PlayerAction.of(WAIT)), restored.submit(PlayerAction.of(WAIT)));
        var defeated = run.saveState();
        assertTrue(defeated.flags.contains("constructor-defeated")); assertEquals("redirect", defeated.constructorRoute);
        assertEquals("defeated", defeated.bossPhase); assertEquals(100, defeated.hp); assertEquals(345, defeated.reserve);
        assertEquals(defeated.combatRng, restored.saveState().combatRng);
        assertEquals(defeated.flags, restored.saveState().flags);
    }

    @Test void lowIntPhysicalOperatorCombatUnlocksProgressionWithoutHacking() {
        var s = encounter(); s.actors.get(1).x = 4; s.actors.get(1).y = 2;
        s.combatRng = CombatRpgTest.stateForRoll(0);
        var run = new RunEngine(s);
        for (int i = 0; i < 8 && !run.saveState().flags.contains("constructor-defeated"); i++) {
            assertTrue(run.submit(PlayerAction.at(MELEE, 4, 2)).accepted());
        }
        assertEquals(4, run.saveState().intel);
        assertEquals("operator", run.saveState().constructorRoute); assertEquals("defeated", run.saveState().bossPhase);
        assertEquals(24, run.saveState().batteries); assertTrue(run.saveState().reserve > 350);
    }

    @Test void aSwitchAloneCannotSkipTheActualBossAndInvalidHacksRemainFree() {
        var s = encounter(); s.switchOn = true; s.playerX = s.hatchX; s.playerY = s.hatchY;
        var run = new RunEngine(s);
        assertFalse(run.submit(PlayerAction.of(ADVANCE)).accepted()); assertEquals(0, run.saveState().tick);
        assertFalse(run.submit(PlayerAction.at(HACK, 8, 3)).accepted()); assertEquals(0, run.saveState().tick);
        assertFalse(run.saveState().flags.contains("constructor-defeated"));
    }

    private static GameState encounter() {
        var s = RunEngineTest.room(); s.deck = 8; s.playerX = 3; s.playerY = 2; s.bossPhase = "dormant";
        s.combatRng = CombatRpgTest.stateForRoll(0);
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.CONSTRUCTOR, RunContent.CONSTRUCTOR_ID, 8, 3));
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.OPERATOR, RunContent.OPERATOR_ID, 8, 6));
        return s;
    }
}
