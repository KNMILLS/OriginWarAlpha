package io.zipcoder.originwar.campaign;

import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

/** Authored graph/gate tests; full ordinary-action campaign runs are a separate acceptance scope. */
class CampaignTest {
    private final ArrayList<GameEvent> events=new ArrayList<>();
    @Test void briefingRevealsReclamationBeforeJoiningUniqueNonlethalAlice() {
        var s=new RunEngine(2016).saveState();Campaign.begin(s);
        assertEquals(0,Campaign.choose(s,"briefing-question",events));
        assertEquals("west-truth",s.activeDialogue);
        assertTrue(Campaign.dialogue(s).text().contains("reclaim Mars"));
        Campaign.choose(s,"west-question",events);assertTrue(s.activeDialogue.isEmpty());
        s.deck=2;Campaign.enterDeck(s,events);assertEquals("alice-meet",s.activeDialogue);
        Campaign.choose(s,"alice-welcome",events);Campaign.enterDeck(s,events);
        assertEquals(1,s.actors.stream().filter(a->a.kind==GameState.ActorKind.ALICE).count());
        assertTrue(s.flags.contains("alice-joined"));assertEquals(2,s.aliceTrust);
    }
    @Test void medicChoiceChecksRealIntelligenceReserveAndCannotDuplicateReward() {
        var s=new RunEngine(2115).saveState();s.deck=4;s.activeDialogue="crew";s.intel=4;s.reserve=30;
        assertEquals(-1,Campaign.choose(s,"crew-guide",events));assertEquals(-1,Campaign.choose(s,"crew-share",events));
        assertEquals("crew",s.activeDialogue);assertFalse(s.flags.contains("crew-safe"));
        s.reserve=80;assertEquals(1,Campaign.choose(s,"crew-share",events));assertEquals(40,s.reserve);
        assertTrue(s.flags.contains("crew-safe"));assertEquals(1,s.floorItems.stream().filter(i->i.id.equals("crew-reserve")).count());
        s.activeDialogue="crew";Campaign.choose(s,"crew-share",events);assertEquals(40,s.reserve);
        assertEquals(1,s.floorItems.stream().filter(i->i.id.equals("crew-reserve")).count());
    }
    @Test void commandHatchCannotReplaceTheCarmackRefusalBroadcastSacrificeSequence() {
        var s=new RunEngine(2016).saveState();s.deck=10;s.playerX=s.hatchX;s.playerY=s.hatchY;s.switchOn=true;
        assertFalse(Campaign.escape(s,events));assertFalse(s.victory);
        s.activeDialogue="carmack";Campaign.choose(s,"carmack-cover",events);
        assertEquals("refusal",s.activeDialogue);assertTrue(s.flags.contains("alice-refused"));
        assertFalse(Campaign.escape(s,events));
        Campaign.choose(s,"broadcast-accept",events);assertTrue(s.flags.contains("broadcast"));
        assertEquals("sacrifice-plan",s.activeDialogue);assertFalse(Campaign.escape(s,events));
        Campaign.choose(s,"pod-go",events);assertTrue(Campaign.escape(s,events));
        assertTrue(s.victory);assertTrue(s.flags.contains("station-destroyed"));assertTrue(s.flags.contains("alice-sacrificed"));
        assertTrue(Campaign.epilogue(s).contains("Destination: Mars"));
    }
    @Test void evidenceAndCrewDecisionsHaveDistinctRecordedConsequences() {
        var s=new RunEngine(50).saveState();s.deck=9;s.activeDialogue="west-call";s.intel=4;
        assertEquals(-1,Campaign.choose(s,"fleet-persuade",events));assertFalse(s.flags.contains("fleet-standdown"));
        Campaign.choose(s,"fleet-evidence",events);assertTrue(Campaign.epilogue(s).contains("West has the records"));
        s.activeDialogue="west-call";s.intel=6;Campaign.choose(s,"fleet-persuade",events);
        assertTrue(Campaign.epilogue(s).contains("holds the assault fleet back"));
    }
    @Test void anchorChambersNeverCloseAnExistingPassageOrMoveObjectives() {
        for(long seed=0;seed<20;seed++) {
            var s=new RunEngine(seed).saveState();var before=s.copy();Campaign.enterDeck(s,events);
            assertEquals(before.hatchX,s.hatchX);assertEquals(before.switchX,s.switchX);
            for(int x=0;x<s.width;x++)for(int y=0;y<s.height;y++)
                if(!before.tiles[x][y].blocksMovement)assertFalse(s.tiles[x][y].blocksMovement);
        }
    }
}
