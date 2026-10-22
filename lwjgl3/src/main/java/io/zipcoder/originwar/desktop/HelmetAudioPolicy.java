package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.model.GameEvent;
import java.util.Set;

/** Pure presentation policy: no world, random generator, timer or fuse mutation. */
final class HelmetAudioPolicy {
    private HelmetAudioPolicy() {}
    record Body(int level, float breath, float heart) {}
    record Cue(String id, float gain, float pan, int priority, float cooldown) {}
    static float unit(float value) { return Float.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0; }
    static Body body(int oxygenation) {
        float strain=unit((70-oxygenation)/70f);
        return new Body(oxygenation>=70?0:oxygenation>=40?1:2, .70f+.20f*strain, .36f+.25f*strain);
    }
    static Cue cue(GameEvent event,int listenerX,int listenerY,boolean legitimatelyHeard) {
        if(!legitimatelyHeard)return null;
        String detail=event.detail()==null?"":event.detail();
        boolean local=event.x()==listenerX&&event.y()==listenerY;
        String id=switch(event.type()) {
            case "oxygen"->"oxygen_refill";
            case "scan","broadcast","journal"->"terminal";
            case "dialogue","level"->"confirm";
            case "hit"->"impact";
            case "step"->detail.contains("GRAV")?"step_lowgrav":"step";
            case "rover"->detail.contains("armed:")?"rover":detail.contains("moves")?"rover_step":"alarm";
            case "robot_step"->detail.startsWith("ADD")?"add_step":detail.startsWith("SIRS")?"sirs_step":"robot_step";
            case "gun"->detail.startsWith("ADD")?"add_gun":detail.startsWith("SIRS")?"sirs_gun":"gun";
            default->event.type();
        };
        int priority=Set.of("rover","explosion","oxygen_refill","warning").contains(id)?3:
                Set.of("gun","sirs_gun","add_gun","impact","melee","death").contains(id)?2:1;
        float gain=switch(id) {
            case "ui","confirm","error","terminal"->.35f;
            case "step","step_lowgrav","pickup","drop"->.45f;
            case "hallucination"->.18f;
            case "oxygen_refill","warning","rover"->.82f;
            default->.64f;
        };
        float pan=0;
        if(!local) {
            int distance=Math.abs(event.x()-listenerX)+Math.abs(event.y()-listenerY);
            gain*=.42f/(1+.15f*distance);
            pan=Math.max(-.35f,Math.min(.35f,(event.x()-listenerX)*.055f));
        }
        float cooldown=id.equals("rover")?.12f:id.equals("warning")?.6f:
                id.endsWith("step")||id.equals("step_lowgrav")?.07f:.035f;
        return new Cue(id,gain,pan,priority,cooldown);
    }
}
