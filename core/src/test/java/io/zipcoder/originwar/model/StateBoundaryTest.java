package io.zipcoder.originwar.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StateBoundaryTest {
    @Test void presentationCannotMutateAuthoritativeInventoryOrKnowledge() {
        var state=new GameState();var item=new GameState.Item();item.id="tank-1";state.inventory.add(item);state.flags.add("met-alice");
        var view=new WorldSnapshot(state).world();view.inventory.getFirst().id="phantom";view.flags.clear();view.hp=0;
        assertEquals("tank-1",state.inventory.getFirst().id);assertTrue(state.flags.contains("met-alice"));assertEquals(100,state.hp);
    }
    @Test void separatePresentationRandomnessLeavesCombatUnchanged() {
        var s=new GameState();s.combatRng=412;s.perceptionRng=980;
        int hit=DeterministicRandom.bounded(DeterministicRandom.nextState(s.combatRng),100);
        for(int i=0;i<1000;i++)s.perceptionRng=DeterministicRandom.nextState(s.perceptionRng);
        assertEquals(hit,DeterministicRandom.bounded(DeterministicRandom.nextState(s.combatRng),100));
    }
}
