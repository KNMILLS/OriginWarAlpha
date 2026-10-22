package io.zipcoder.originwar.engine;

import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class OfficerEventSubjectTest {
 @Test void injuryAndDeathIdentifyTheirSubjectWithoutChangingDamageOrRandomState(){
  var s=new RunEngine(2115).saveState();long rng=s.combatRng;var events=new ArrayList<GameEvent>();
  CombatRules.damagePlayer(s,8,events,"Test impact");assertEquals(93,s.hp);assertEquals(rng,s.combatRng);assertEquals("officer",events.getFirst().sourceId());assertEquals("hit",events.getFirst().type());
  CombatRules.damagePlayer(s,100,events,"Test impact");assertTrue(s.dead);assertEquals(0,s.hp);assertEquals("death",events.getLast().type());assertEquals("officer",events.getLast().sourceId());
 }
}
