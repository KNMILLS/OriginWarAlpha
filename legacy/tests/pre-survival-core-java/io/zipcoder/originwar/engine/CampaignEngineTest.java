package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit story fixtures test action boundaries; seeded full runs are separate. */
class CampaignEngineTest {
    @Test void realOpeningRequiresValidChoicesAndCostsNoBreathingOrRandomness() {
        var run=new RunEngine(2016);var before=run.saveState();
        assertFalse(run.submit(PlayerAction.of(WAIT)).accepted());
        assertFalse(run.submit(PlayerAction.item(CHOOSE,"invented")).accepted());
        assertEquals(0,run.submit(PlayerAction.item(CHOOSE,"briefing-question")).cost());
        assertEquals("west-truth",run.saveState().activeDialogue);
        assertEquals(0,run.submit(PlayerAction.item(CHOOSE,"west-question")).cost());
        assertTrue(run.saveState().activeDialogue.isEmpty());assertEquals(before.combatRng,run.saveState().combatRng);
        assertEquals(0,run.saveState().tick);assertEquals(RunEngine.STARTING_RESERVE,run.saveState().reserve);
        assertTrue(run.submit(PlayerAction.of(WAIT)).accepted());assertEquals(1,run.saveState().tick);
    }
    @Test void crewOxygenTransferUsesOneRealWorldTickAndPersistsItsDecision() {
        var s=storyRoom();s.deck=4;s.activeDialogue="crew";s.reserve=80;
        var run=new RunEngine(s);assertEquals(1,run.submit(PlayerAction.item(CHOOSE,"crew-share")).cost());
        assertEquals(39,run.saveState().reserve);assertEquals(1,run.saveState().tick);
        var restored=new RunEngine(run.saveState());assertTrue(restored.saveState().flags.contains("crew-safe"));
        assertTrue(restored.saveState().floorItems.stream().anyMatch(i->i.id.equals("crew-reserve")));
    }
    @Test void companionOnHatchCannotBypassConstructorOrStoryGates() {
        for(int deck:new int[]{4,8}){
            var s=storyRoom();s.deck=deck;s.switchOn=true;s.playerX=s.hatchX;s.playerY=s.hatchY;
            var alice=new GameState.Actor();alice.id="story-alice";alice.kind=GameState.ActorKind.ALICE;alice.x=s.playerX;alice.y=s.playerY;s.actors.add(alice);
            var run=new RunEngine(s);assertFalse(run.submit(PlayerAction.of(INTERACT)).accepted());
            assertEquals(deck,run.saveState().deck);assertEquals(0,run.saveState().tick);
        }
    }
    @Test void canonicalDialogueAndPhysicalPodActionAreBothRequiredForVictory() {
        var run=finale();assertFalse(run.saveState().victory);
        run.submit(PlayerAction.item(CHOOSE,"carmack-cover"));assertEquals("refusal",run.saveState().activeDialogue);
        run.submit(PlayerAction.item(CHOOSE,"broadcast-accept"));assertEquals("sacrifice-plan",run.saveState().activeDialogue);
        var mid=new RunEngine(run.saveState());assertTrue(mid.saveState().flags.contains("broadcast"));
        mid.submit(PlayerAction.item(CHOOSE,"pod-go"));assertFalse(mid.saveState().victory);assertEquals(0,mid.saveState().tick);
        assertEquals(1,mid.submit(PlayerAction.of(ADVANCE)).cost());assertTrue(mid.saveState().victory);
        assertTrue(mid.saveState().flags.contains("station-destroyed"));assertTrue(mid.saveState().flags.contains("alice-sacrificed"));
        assertFalse(mid.submit(PlayerAction.of(WAIT)).accepted());
    }
    @Test void deathOnPodTransitionCannotCreateAnEscapeVictory() {
        var run=finale();run.submit(PlayerAction.item(CHOOSE,"carmack-cover"));run.submit(PlayerAction.item(CHOOSE,"broadcast-accept"));run.submit(PlayerAction.item(CHOOSE,"pod-go"));
        var s=run.saveState();s.hp=4;s.reserve=0;s.oxygenation=0;run=new RunEngine(s);
        assertTrue(run.submit(PlayerAction.of(ADVANCE)).accepted());assertTrue(run.saveState().dead);assertFalse(run.saveState().victory);
        assertFalse(run.saveState().flags.contains("station-destroyed"));
    }
    private GameState storyRoom(){var s=RunEngineTest.room();s.flags.add("campaign-started");return s;}
    private RunEngine finale(){var s=storyRoom();s.deck=10;s.switchOn=true;s.playerX=s.hatchX;s.playerY=s.hatchY;s.activeDialogue="carmack";return new RunEngine(s);}
}
