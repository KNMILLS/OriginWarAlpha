package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.PlayerAction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MovementIntentTest {
 @Test void repeatedKeyEventsCannotAccumulateActionsAndReleaseRetainsOnlyABoundedTap(){
  var b=new MovementIntent();var east=PlayerAction.move(1,0);
  assertTrue(b.press(1,east,0));assertFalse(b.press(1,east,1));
  b.release(1);assertEquals(east,b.peek(.17f));assertNull(b.peek(.19f));
 }
 @Test void heldMovementStopsOnReleaseAfterItsOneIntentWasConsumed(){
  var b=new MovementIntent();var east=PlayerAction.move(1,0);
  b.press(1,east,0);b.consumed();assertEquals(east,b.peek(20));
  b.release(1);assertNull(b.peek(20));
 }
 @Test void latestDirectionWinsAndFocusOrMenuClearCannotLeaveLatentMovement(){
  var b=new MovementIntent();b.press(1,PlayerAction.move(1,0),0);b.press(2,PlayerAction.move(0,-1),.1f);
  assertEquals(PlayerAction.move(0,-1),b.peek(1));b.clear();assertNull(b.peek(1));
  assertFalse(b.press(1,PlayerAction.move(1,0),2));b.release(1);assertTrue(b.press(1,PlayerAction.move(1,0),2));
 }
}
