package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Observes the separate real director streams without a device, files or world state. */
class AudioDirectorTest {
    private static final String STATIC="audio/voice/radio-static.ogg";
    private static final String FIRST="audio/voice/first.ogg",SECOND="audio/voice/second.ogg";
    private static final class Stream {
        boolean playing,looping,disposed;
        int starts,stops,disposals;
        float volume,position;
        Music.OnCompletionListener completion;
        final Music music=(Music)Proxy.newProxyInstance(Music.class.getClassLoader(),new Class[]{Music.class},(p,m,a)->{
            switch(m.getName()) {
                case "play"->{assertFalse(disposed);starts++;playing=true;}
                case "pause"->playing=false;
                case "stop"->{stops++;playing=false;position=0;}
                case "dispose"->{disposals++;disposed=true;playing=false;}
                case "isPlaying"->{return playing;}
                case "setLooping"->looping=(boolean)a[0];
                case "isLooping"->{return looping;}
                case "setVolume"->volume=(float)a[0];
                case "getVolume"->{return volume;}
                case "getPosition"->{return position;}
                case "setPosition"->position=(float)a[0];
                case "setOnCompletionListener"->completion=(Music.OnCompletionListener)a[0];
                default->{}
            }
            return null;
        });
        void finish(){playing=false;completion.onCompletion(music);}
    }
    private static final class Backend implements AudioDirector.Resources {
        final Map<String,List<Stream>> streams=new HashMap<>();
        boolean staticExists=true;
        int soundStarts,soundDisposals;
        final Sound sound=(Sound)Proxy.newProxyInstance(Sound.class.getClassLoader(),new Class[]{Sound.class},(p,m,a)->{
            if(m.getName().equals("play")){soundStarts++;return (long)soundStarts;}
            if(m.getName().equals("dispose"))soundDisposals++;
            return null;
        });
        public boolean exists(String path){return Set.of("audio/music/exploration.ogg","audio/sfx/ui.wav","audio/voice/index.json",FIRST,SECOND).contains(path)||(staticExists&&path.equals(STATIC));}
        public Music music(String path){var s=new Stream();streams.computeIfAbsent(path,k->new ArrayList<>()).add(s);return s.music;}
        public Sound sound(String path){return sound;}
        public String text(String path){return "{version:1,clips:["+entry("first","First line.",FIRST)+","+entry("second","Second line.",SECOND)+"]}";}
        private String entry(String id,String text,String path){return "{dialogue_id:"+id+",text_sha256:"+AliceVoiceCatalog.hash(text)+",path:"+path+",duration_seconds:20}";}
        Stream latest(String path){return streams.get(path).getLast();}
        List<Stream> all(){return streams.values().stream().flatMap(List::stream).toList();}
    }

    @Test void fiveGainsSeparateSpeechRadioAndEffectsWithBriefStaticFade() {
        var b=new Backend();try(var d=new AudioDirector(b)) {
            d.volumes(1,.5f,0,.7f,.35f);d.dialogue("first","First line.");
            var speech=b.latest(FIRST);var radio=b.latest(STATIC);
            assertTrue(speech.playing);assertTrue(radio.playing);
            assertTrue(d.summary().contains("radioStarts=1, currentRadioActive=true"));
            assertFalse(speech.looping);assertTrue(radio.looping);
            assertEquals(.7f*.7f*.86f,speech.volume,.00001f);
            assertEquals(0,radio.volume);
            d.update(.025f);assertEquals(.7f*.35f*.65f*.5f,radio.volume,.00001f);
            d.update(.025f);assertEquals(.7f*.35f*.65f,radio.volume,.00001f);
            d.play("ui");assertEquals(0,b.soundStarts,"Effects zero cannot disable independent speech");
            d.volumes(1,.5f,.8f,.7f,0);assertTrue(speech.playing);assertFalse(radio.playing);
            d.play("ui");assertEquals(1,b.soundStarts,"Radio zero cannot mute other effects");
            assertEquals(.7f*.7f*.86f,speech.volume,.00001f);
            d.volumes(1,.5f,.8f,.7f,.6f);d.update(.05f);
            assertEquals(.7f*.6f*.65f,radio.volume,.00001f);
            assertEquals(1,speech.starts,"Radio changes cannot restart speech");
        }
    }
    @Test void pauseAndMutesResumePositionsAndRetainIndependentGains() {
        var b=new Backend();try(var d=new AudioDirector(b)) {
            d.volumes(.9f,.3f,.1f,.8f,.4f);d.dialogue("first","First line.");d.update(.1f);
            var speech=b.latest(FIRST);var radio=b.latest(STATIC);speech.position=4.2f;radio.position=3.8f;
            d.pause(true);assertFalse(speech.playing);assertFalse(radio.playing);
            d.update(10);d.pause(false);assertTrue(speech.playing);assertTrue(radio.playing);
            assertEquals(4.2f,speech.position);assertEquals(3.8f,radio.position);
            assertEquals(.9f*.7f*.8f*.86f,speech.volume,.00001f);
            d.update(.05f);assertEquals(.9f*.7f*.4f*.65f,radio.volume,.00001f);
            d.volumes(.9f,.3f,.1f,0,.4f);assertFalse(speech.playing);assertFalse(radio.playing);
            d.volumes(.9f,.3f,.1f,.8f,.4f);assertEquals(4.2f,speech.position);assertTrue(radio.playing);
            d.volumes(0,.3f,.1f,.8f,.4f);assertFalse(speech.playing);assertFalse(radio.playing);
            d.pause(true);d.volumes(.9f,.3f,.1f,.8f,.4f);assertFalse(speech.playing);assertFalse(radio.playing);
            d.pause(false);assertTrue(speech.playing);assertTrue(radio.playing);assertEquals(0,speech.stops);
        }
    }
    @Test void completionStopsStaticImmediatelyAndCannotRestartEitherStream() {
        var b=new Backend();try(var d=new AudioDirector(b)) {
            d.dialogue("first","First line.");d.update(.1f);
            var speech=b.latest(FIRST);var radio=b.latest(STATIC);speech.finish();
            assertFalse(radio.playing);assertEquals(0,radio.volume);assertEquals(1,radio.stops);
            assertTrue(d.summary().contains("currentRadioActive=false"));
            int speechStarts=speech.starts,radioStarts=radio.starts;
            d.dialogue("first","First line.");d.volumes(.8f,.5f,.7f,.7f,.8f);d.pause(true);d.pause(false);d.update(.1f);
            assertEquals(speechStarts,speech.starts);assertEquals(radioStarts,radio.starts);
            assertFalse(speech.playing);assertFalse(radio.playing);
            speech.finish();assertTrue(d.summary().contains("completedSpeech=1"));
        }
    }
    @Test void interruptionRejectsStaleCompletionAndClearCloseDisposeEveryObject() {
        var b=new Backend();var d=new AudioDirector(b);
        d.dialogue("first","First line.");var old=b.latest(FIRST);var oldRadio=b.latest(STATIC);var late=old.completion;
        d.dialogue("second","Second line.");var current=b.latest(SECOND);var currentRadio=b.latest(STATIC);
        assertTrue(old.disposed);assertTrue(oldRadio.disposed);assertFalse(oldRadio.playing);
        late.onCompletion(old.music);assertTrue(current.playing);assertTrue(currentRadio.playing);
        assertTrue(d.summary().contains("completedSpeech=0"));
        for(int i=0;i<20;i++) {
            boolean first=i%2==0;d.dialogue(first?"first":"second",first?"First line.":"Second line.");
            assertEquals(2,b.all().stream().filter(s->s.playing).count(),"Only one speech/static pair can play");
            assertEquals(3,b.all().stream().filter(s->!s.disposed).count(),"One score and one pair are bounded");
        }
        d.dialogue("","");assertEquals(0,b.all().stream().filter(s->s.playing).count());
        d.dialogue("first","Changed text.");assertEquals(0,b.all().stream().filter(s->s.playing).count());
        d.close();d.close();d.dialogue("first","First line.");
        assertTrue(b.all().stream().allMatch(s->s.disposed&&s.disposals==1));assertEquals(1,b.soundDisposals);
    }
    @Test void legacyEffectsMuteStillMutesSpeechAndMissingStaticNeverBlocksSpeech() {
        var b=new Backend();b.staticExists=false;try(var d=new AudioDirector(b)) {
            d.dialogue("first","First line.");var speech=b.latest(FIRST);
            assertTrue(speech.playing);assertEquals(.8f*.7f*.7f*.86f,speech.volume,.00001f);
            assertFalse(b.streams.containsKey(STATIC));
            d.volumes(1,.5f,0);assertFalse(speech.playing);
            d.volumes(1,.5f,.4f);assertTrue(speech.playing);assertEquals(.7f*.4f*.86f,speech.volume,.00001f);
            d.volumes(1,.5f,0,.6f,.2f);assertTrue(speech.playing);assertEquals(.7f*.6f*.86f,speech.volume,.00001f);
        }
    }
}
