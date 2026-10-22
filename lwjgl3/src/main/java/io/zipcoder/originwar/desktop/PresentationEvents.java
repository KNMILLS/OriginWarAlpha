package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.GameEvent;
import io.zipcoder.originwar.model.GameState;
import java.util.Set;

/** The event DTO has no deck ID, so old-floor spatial cues are discarded at a transition. */
final class PresentationEvents {
    private static final Set<String> TRANSITION_NOTICES=Set.of("hatch","journal","warning","death","level");
    static boolean audible(GameEvent event,GameState before,GameState after){
        if(before.deck!=after.deck&&!TRANSITION_NOTICES.contains(event.type()))return false;
        int x=event.x(),y=event.y();
        return x==after.playerX&&y==after.playerY||x>=0&&y>=0&&x<after.width&&y<after.height&&after.visible[x][y];
    }
    private PresentationEvents(){}
}
