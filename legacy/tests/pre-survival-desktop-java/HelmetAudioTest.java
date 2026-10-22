package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import io.zipcoder.originwar.campaign.Campaign;
import io.zipcoder.originwar.model.GameEvent;
import io.zipcoder.originwar.model.GameState;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Fake backend observes actual director calls; no device/window or simulation writes. */
class HelmetAudioTest {
    private static Path assets(){return Files.isDirectory(Path.of("../assets"))?Path.of("../assets"):Path.of("assets");}
    static final class FakeMusic {
        int starts;boolean playing,disposed;float volume,position;
        Music.OnCompletionListener listener;
        final Music proxy=(Music)Proxy.newProxyInstance(Music.class.getClassLoader(),new Class[]{Music.class},(p,m,a)->{
            switch(m.getName()) {
                case "play"->{starts++;playing=true;}
                case "pause","stop"->playing=false;
                case "dispose"->{disposed=true;playing=false;}
                case "isPlaying"->{return playing;}
                case "setVolume"->volume=(float)a[0];
                case "getVolume"->{return volume;}
                case "setPosition"->position=(float)a[0];
                case "getPosition"->{return position;}
                case "setOnCompletionListener"->listener=(Music.OnCompletionListener)a[0];
                case "isLooping"->{return true;}
                default->{}
            }
            return null;
        });
        void finish(){playing=false;if(listener!=null)listener.onCompletion(proxy);}
    }
    static final class FakeSound {
        long next;int starts;final Set<Long> live=new HashSet<>();
        final Sound proxy=(Sound)Proxy.newProxyInstance(Sound.class.getClassLoader(),new Class[]{Sound.class},(p,m,a)->{
            if(m.getName().equals("play")){starts++;live.add(++next);return next;}
            if(m.getName().equals("stop")){if(a==null||a.length==0)live.clear();else live.remove((long)a[0]);}
            return null;
        });
    }
    static final class Backend implements AudioDirector.Resources {
        final Map<String,List<FakeMusic>> music=new HashMap<>();
        final Map<String,FakeSound> sounds=new HashMap<>();
        public boolean exists(String p){return true;}
        public Music music(String p){var m=new FakeMusic();music.computeIfAbsent(p,k->new ArrayList<>()).add(m);return m.proxy;}
        public Sound sound(String p){var s=new FakeSound();sounds.put(p,s);return s.proxy;}
        public String text(String p){try{return Files.readString(assets().resolve(p));}catch(Exception e){throw new RuntimeException(e);}}
        FakeMusic latest(String p){var ms=music.get(p);return ms.getLast();}
        int sfxStarts(){return sounds.values().stream().mapToInt(s->s.starts).sum();}
        long liveSfx(){return sounds.values().stream().mapToInt(s->s.live.size()).sum();}
    }
    static GameState alice(String id,int oxygen,int trust){var s=new GameState();s.activeDialogue=id;s.oxygenation=oxygen;s.aliceTrust=trust;return s;}
    static void advance(AudioDirector d,int frames){for(int i=0;i<frames;i++)d.update(.1f);}

    @Test void trueOxygenationSelectsBoundedBodyLevelsAndNoWorldIsMutated() {
        assertEquals(0,HelmetAudioPolicy.body(100).level());assertEquals(1,HelmetAudioPolicy.body(60).level());
        assertEquals(2,HelmetAudioPolicy.body(20).level());
        assertTrue(HelmetAudioPolicy.body(-200).breath()<=.9f);
        var backend=new Backend();try(var d=new AudioDirector(backend)){
            d.context(100,true,2,3);advance(d,8);
            assertTrue(backend.latest("audio/ambience/breath_healthy.ogg").volume>0);
            d.context(20,true,2,3);advance(d,8);
            assertTrue(backend.latest("audio/ambience/breath_healthy.ogg").disposed);
            assertTrue(backend.latest("audio/ambience/breath_critical.ogg").volume>0);
            d.context(20,false,2,3);advance(d,8);
            assertFalse(backend.latest("audio/ambience/breath_critical.ogg").playing);
            d.context(20,true,2,3);advance(d,8);
            assertTrue(backend.latest("audio/ambience/breath_critical.ogg").playing);
        }
    }
    @Test void hiddenEventsAreSilentAndTurnBasedArmingNeverBecomesTimedCountdown() {
        var event=new GameEvent("rover",4,2,"R.O.V.E.R. armed: two ticks until detonation.");
        assertNull(HelmetAudioPolicy.cue(event,2,2,false));
        assertEquals("rover",HelmetAudioPolicy.cue(event,2,2,true).id());
        var b=new Backend();try(var d=new AudioDirector(b)){
            d.context(100,true,2,2);d.event(event,false);assertEquals(0,b.sfxStarts());
            d.event(event,true);assertEquals(1,b.sfxStarts());advance(d,1000);
            assertEquals(1,b.sfxStarts(),"Waiting cannot invent fuse steps or threat events");
        }
        assertEquals("add_gun",HelmetAudioPolicy.cue(new GameEvent("gun",3,2,"ADD fires."),2,2,true).id());
        assertEquals("sirs_step",HelmetAudioPolicy.cue(new GameEvent("robot_step",3,2,"SIRS moves."),2,2,true).id());
    }
    @Test void actualAllNineVoiceMappingsMatchCampaignAndRejectChangedText() throws Exception {
        var catalog=new AliceVoiceCatalog(Files.readString(assets().resolve("audio/voice/index.json")));
        assertEquals(9,catalog.size());
        for(String id:List.of("alice-meet","alice-record","alice-agency","refusal","sacrifice-plan","pod-ready")){
            var s=alice(id,100,0);String text=Campaign.dialogue(s).text();
            assertNotNull(catalog.find(id,text),id);assertNull(catalog.find(id,text+" Changed."));
        }
        for(var s:List.of(alice("alice-talk",50,0),alice("alice-talk",100,3),alice("alice-talk",100,0)))
            assertNotNull(catalog.find("alice-talk",Campaign.dialogue(s).text()));
        assertThrows(IllegalArgumentException.class,()->new AliceVoiceCatalog("{version:1,clips:[{dialogue_id:x,text_sha256:bad,path:../bad, duration_seconds:1}]}"));
    }
    @Test void speechDoesNotRepeatEachFrameAndPauseMuteInterruptCloseAreOwned() {
        var b=new Backend();var d=new AudioDirector(b);var s=alice("refusal",100,0);
        String text=Campaign.dialogue(s).text();d.dialogue("refusal",text);
        var voice=b.latest("audio/voice/refusal.ogg");assertEquals(1,voice.starts);
        for(int i=0;i<10;i++){d.dialogue("refusal",text);d.update(.1f);}assertEquals(1,voice.starts);
        d.pause(true);assertFalse(voice.playing);d.pause(false);assertTrue(voice.playing);
        d.volumes(0,.5f,.7f);assertFalse(voice.playing);d.volumes(.8f,.5f,.7f);assertTrue(voice.playing);
        voice.finish();int finishedStarts=voice.starts;d.pause(true);d.pause(false);d.volumes(.6f,.5f,.7f);
        assertEquals(finishedStarts,voice.starts,"Completed speech must never restart after focus or volume changes");
        d.dialogue("","");assertTrue(voice.disposed);d.close();d.close();
        assertTrue(b.music.values().stream().flatMap(List::stream).allMatch(m->m.disposed));
    }
    @Test void sfxPoolAndPriorityRemainBoundedAndMutedEventsAllocateNoVoice() {
        var b=new Backend();try(var d=new AudioDirector(b)){
            d.context(100,true,0,0);
            for(String id:List.of("gun","impact","melee","door","step","pickup","hatch","warning","oxygen_refill","explosion"))d.play(id);
            assertTrue(b.liveSfx()<=6);int started=b.sfxStarts();
            d.volumes(0,.5f,.7f);d.play("confirm");assertEquals(started,b.sfxStarts());
            d.pause(true);assertEquals(0,b.liveSfx());d.pause(false);advance(d,50);assertEquals(0,b.liveSfx());
        }
    }
}
