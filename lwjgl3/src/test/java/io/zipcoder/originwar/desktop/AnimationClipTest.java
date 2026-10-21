package io.zipcoder.originwar.desktop;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AnimationClipTest {
 private AnimationClip clip(boolean loop){return new AnimationClip(List.of(new AnimationClip.Frame("contact",.1f),new AnimationClip.Frame("passing",.2f),new AnimationClip.Frame("plant",.1f)),List.of(new AnimationClip.Marker("contact",0),new AnimationClip.Marker("release",.12f),new AnimationClip.Marker("contact",.3f),new AnimationClip.Marker("recovery",.4f)),loop,2);}
 @Test void authoredHoldsAndLoopAreRespected(){var c=clip(false);assertEquals("contact",c.at(.05f));assertEquals("passing",c.at(.21f));assertEquals("plant",c.at(2));assertEquals("contact",clip(true).at(.42f));}
 @Test void comparisonRetimingKeepsAuthoredReleaseAndRecoveryTogether(){var c=clip(false);assertEquals(c.at(.12f),c.retimedFire(.14f,.5f,.14f));assertEquals("plant",c.retimedFire(.5f,.5f,.14f));assertEquals("contact",c.retimedFire(0,.5f,.14f));}
 @Test void repeatedMarkersRetainOrderAndDoNotReplayAtBoundary(){var c=clip(false);assertEquals(List.of("contact","release","contact","recovery"),c.markers().stream().map(AnimationClip.Marker::kind).toList());assertEquals(2,c.between(.1f,.3f).size());assertTrue(c.between(.3f,.31f).isEmpty());assertEquals(.12f,c.marker("release",0));}
 @Test void invalidFramesAndMarkersFailBeforePlayback(){assertThrows(IllegalArgumentException.class,()->new AnimationClip(List.of(),List.of(),true,2));assertThrows(IllegalArgumentException.class,()->new AnimationClip(List.of(new AnimationClip.Frame("bad",0)),List.of(),false,0));assertThrows(IllegalArgumentException.class,()->new AnimationClip(List.of(new AnimationClip.Frame("ok",.1f)),List.of(new AnimationClip.Marker("late",1)),false,0));}
}
