package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.JsonReader;
import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.lang.reflect.*;
import java.nio.file.Path;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Exercises the real director and real content bindings, replacing only the device boundary. */
final class SurvivalAudioTest {
    private static final String OFFICER="audio/survival/voice/contact-officer.ogg";
    private static final String ALICE="audio/survival/voice/contact-alice.ogg";
    private static final String SCORE="audio/music/exploration.ogg";
    private static final String WARNING="audio/sfx/warning.wav";
    private static final String ARM="audio/survival/sfx/rover_arm.wav";
    @TempDir Path temporary;
    private Audio originalAudio;private Files originalFiles;private Application originalApp;
    private Backend backend;private SettingsStore settings;private SurvivalAudio director;
    private GameState state;

    @BeforeEach void installDeviceBoundary(){
        originalAudio=Gdx.audio;originalFiles=Gdx.files;originalApp=Gdx.app;
        backend=new Backend(findAssets());
        Gdx.files=proxy(Files.class,(p,m,a)->switch(m.getName()){
            case "internal","classpath"->new FileHandle(backend.assets.resolve((String)a[0]).toFile());
            case "getFileHandle"->new FileHandle(backend.assets.resolve((String)a[0]).toFile());
            default->defaultValue(m.getReturnType());
        });
        Gdx.audio=proxy(Audio.class,(p,m,a)->switch(m.getName()){
            case "newMusic"->backend.music((FileHandle)a[0]);
            case "newSound"->backend.sound((FileHandle)a[0]);
            default->defaultValue(m.getReturnType());
        });
        Gdx.app=proxy(Application.class,(p,m,a)->{if(m.getName().equals("error"))backend.errors.add(Arrays.toString(a));return defaultValue(m.getReturnType());});
        settings=new SettingsStore(temporary);settings.master=1;settings.voice=1;settings.sfx=1;settings.radio=1;settings.music=1;settings.soundCaptions=true;
        state=new RunEngine(2016).saveState();state.radioConnected=true;state.actors.clear();state.tick=30;
        director=new SurvivalAudio();director.settings(settings);
    }
    @AfterEach void restoreGlobals(){
        try{if(director!=null)director.close();assertTrue(backend.errors.isEmpty(),"The real director reported backend failures: "+backend.errors);assertTrue(backend.streams.stream().allMatch(s->s.disposed),"Every allocated stream must be released");assertTrue(backend.sounds.values().stream().allMatch(s->s.disposed),"Every allocated effect must be released");}
        finally{Gdx.audio=originalAudio;Gdx.files=originalFiles;Gdx.app=originalApp;}
    }

    @Test void lostRadioHoldsAliceWithoutLosingWordsOrCompletingTheSegment(){
        resumeAtAlice();tick(1,false);var speech=backend.latest(ALICE);assertTrue(speech.playing);assertTrue(director.subtitle().startsWith("A.L.I.C.E."));
        state.radioConnected=false;director.update(0,state,false);float stoppedAt=speech.position;
        tick(backend.duration(ALICE)+2,false);
        assertFalse(speech.playing);assertFalse(speech.disposed);assertEquals(stoppedAt,speech.position,.0001);assertNull(director.takeCompleted());assertEquals("",director.subtitle());
        state.radioConnected=true;director.update(0,state,false);
        assertTrue(speech.playing);assertEquals(stoppedAt,speech.position,.0001,"Reconnection resumes the same decoder position");assertEquals(1,backend.count(ALICE));
        tick(backend.duration(ALICE)-stoppedAt+.15f,false);
        assertEquals("complete:contact:1",director.takeCompleted());assertNull(director.takeCompleted());assertFalse(director.speaking());
    }

    @Test void loadingResumesOnlyUnfinishedEssentialSegments(){
        state.playedExchanges.add("quiet");state.completedSpeech.add("quiet:0");resumeAtAlice();director.update(0,state,false);
        assertEquals(0,backend.count(OFFICER),"The completed officer line must not restart");assertEquals(1,backend.count(ALICE));
        assertTrue(backend.streams.stream().noneMatch(s->s.path.contains("/quiet-")),"Optional conversations must not become a saved backlog");
        tick(backend.duration(ALICE)+.2f,false);assertEquals("complete:contact:1",director.takeCompleted());state.completedSpeech.add("contact:1");
        director.close();director=new SurvivalAudio();director.settings(settings);director.resumeEssential(state);tick(1,false);
        assertEquals(1,backend.count(ALICE),"A fully completed essential exchange remains complete after another load");assertEquals(0,backend.count(OFFICER));assertFalse(director.speaking());assertNull(director.takeCompleted());
        assertTrue(director.history(state).stream().anyMatch(s->s.contains("I can. A.L.I.C.E.")),"Completed speech remains available in the saved information archive");
    }

    @Test void aDeadLinkCannotStartAnAliceSegmentButVictoryCanCarryTheFarewell(){
        state.radioConnected=false;resumeAtAlice();director.update(0,state,false);var waiting=backend.latest(ALICE);
        assertEquals(0,waiting.starts);tick(12,false);assertEquals(0,waiting.starts);assertNull(director.takeCompleted());
        director.reset();state.playedExchanges.clear();state.completedSpeech.clear();state.playedExchanges.add("farewell");state.victory=true;
        director.resumeEssential(state);director.update(0,state,false);
        assertTrue(backend.streams.stream().anyMatch(s->s.path.contains("/farewell-")&&s.playing),"The explicit endgame link exception must preserve farewell playback");
    }

    @Test void criticalArmingAndWarningsSurviveCrowdedRoutineSoundAndCaptions(){
        director.event(event("warning","Oxygen reserve low."),state);director.event(event("rover","R.O.V.E.R. armed: two ticks after your next commitment."),state);
        var warning=backend.latestSound(WARNING).latest();var arming=backend.latestSound(ARM).latest();
        for(int n=0;n<40;n++)director.event(event("robot_step",n%2==0?"SIRS moves.":"ADD moves."),state);
        assertTrue(warning.live&&warning.playing,"Routine footsteps must not evict the warning");assertTrue(arming.live&&arming.playing,"Routine footsteps must not evict the real arming signature");assertTrue(backend.liveEffects()<=6,"Device voices remain bounded under a burst");
        assertTrue(director.caption().contains("armed"),"The critical caption must remain readable after lower-priority events");
    }

    @Test void criticalCuesPauseEssentialSpeechAndDiscardOptionalSpeech(){
        resumeAtAlice();tick(1,false);var essential=backend.latest(ALICE);director.event(event("warning","Oxygen reserve low."),state);director.update(0,state,false);float position=essential.position;
        tick(1,false);assertFalse(essential.playing);assertEquals(position,essential.position,.0001);assertNull(director.takeCompleted());
        tick(.35f,false);assertTrue(essential.playing);assertEquals(1,backend.count(ALICE));assertFalse(essential.disposed);
        director.reset();director.event(event("speech","quiet"),state);director.update(0,state,false);var optional=backend.latest("audio/survival/voice/quiet-officer.ogg");assertTrue(optional.playing);
        director.event(event("rover","R.O.V.E.R. armed: two ticks after your next commitment."),state);director.update(0,state,false);
        assertTrue(optional.disposed);assertFalse(director.speaking());tick(20,false);assertEquals(1,backend.count("audio/survival/voice/quiet-officer.ogg"));assertNull(director.takeCompleted(),"Interrupted optional lines do not falsely complete");
    }

    @Test void pauseFreezesSpeechEffectsAndCompletionThenResumesTheirPositions(){
        resumeAtAlice();tick(1,false);var speech=backend.latest(ALICE);director.event(event("lamp","Helmet light off."),state);var effect=backend.latestSound("audio/survival/sfx/lamp.wav").latest();
        float speechAt=speech.position;director.update(0,state,true);float effectAt=effect.position;tick(30,true);
        assertFalse(speech.playing);assertFalse(effect.playing);assertTrue(backend.streams.stream().noneMatch(s->s.playing));assertEquals(speechAt,speech.position,.0001);assertEquals(effectAt,effect.position,.0001);assertNull(director.takeCompleted());assertFalse(speech.disposed);
        director.update(0,state,false);assertTrue(speech.playing);assertTrue(effect.playing);assertEquals(speechAt,speech.position,.0001);assertEquals(1,backend.count(ALICE));
        tick(backend.duration(ALICE)-speechAt+.2f,false);assertEquals("complete:contact:1",director.takeCompleted());assertNull(director.takeCompleted());
    }

    @Test void unpausingWithALostLinkNeverBrieflyReopensAlicesOutput(){
        resumeAtAlice();tick(1,false);var speech=backend.latest(ALICE);int starts=speech.starts;
        // A circuit action commits, then the player pauses before the next audio presentation update.
        state.radioConnected=false;director.update(0,state,true);director.update(0,state,false);
        assertFalse(speech.playing);assertEquals(starts,speech.starts,"Unpause must not play even one buffer through a link already known to be dead");assertNull(director.takeCompleted());
        state.radioConnected=true;director.update(0,state,false);assertTrue(speech.playing);assertEquals(starts+1,speech.starts);
    }

    @Test void concealedContactsGiveOnlyCoarseDirectionAndGenericMachineSound(){
        var heard=new GameEvent("heard",-1,-1,"Mechanical contact to the east.","",-1,-1,0,state.tick,1,false,true,"contact");director.event(heard,state);
        assertEquals(Set.of("audio/sfx/robot_step.wav"),backend.sounds.keySet(),"A heard contact cannot reveal the concealed machine's exact kind");
        var sound=backend.latestSound("audio/sfx/robot_step.wav").latest();assertTrue(sound.pan>0&&sound.pan<=.6f);assertTrue(director.caption().contains("east"));assertFalse(director.caption().contains("SIRS"));
    }

    @Test void unpauseDoesNotBrieflyPlayEssentialSpeechOverAPendingCriticalCue(){
        resumeAtAlice();tick(1,false);var speech=backend.latest(ALICE);int starts=speech.starts;
        director.event(event("rover","R.O.V.E.R. armed: two ticks after your next commitment."),state);director.update(0,state,true);director.update(0,state,false);
        assertFalse(speech.playing);assertEquals(starts,speech.starts,"A critical cue must keep the speech gate closed throughout unpause");
        tick(1,false);assertFalse(speech.playing);tick(.3f,false);assertTrue(speech.playing);assertEquals(starts+1,speech.starts);
    }

    @Test void intermittentScoreRemainsIndependentWhenEffectsAreMuted(){
        settings.sfx=0;settings.music=1;settings.voice=0;settings.radio=0;tick(425,false);
        var score=backend.latest(SCORE);assertTrue(score.volume>0,"Effects zero must not silence the independently enabled score");
        assertEquals(0,backend.latest("audio/ambience/breath_healthy.ogg").volume);assertEquals(0,backend.latest("audio/survival/ambience/station_powered.ogg").volume);
        settings.music=0;director.update(.01f,state,false);assertEquals(0,score.volume);
        settings.music=1;director.update(.01f,state,false);assertTrue(score.volume>0);assertEquals(1,backend.count(SCORE),"Volume changes must not recreate score streams");
    }

    private void resumeAtAlice(){state.playedExchanges.add("contact");state.completedSpeech.add("contact:0");director.resumeEssential(state);}
    private GameEvent event(String type,String text){return new GameEvent(type,state.playerX,state.playerY,text);}
    private void tick(float seconds,boolean freeze){for(float remaining=seconds;remaining>.00001f;){float dt=Math.min(.05f,remaining);director.update(dt,state,freeze);backend.advance(dt);remaining-=dt;}}
    private static Path findAssets(){Path candidate=Path.of("").toAbsolutePath();while(candidate!=null){Path assets=candidate.resolve("assets");if(java.nio.file.Files.isRegularFile(assets.resolve("audio/survival/exchanges.json")))return assets;candidate=candidate.getParent();}throw new AssertionError("Real survival content not found");}
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T> type,InvocationHandler handler){return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class[]{type},handler);}
    private static Object defaultValue(Class<?> type){if(!type.isPrimitive()||type==void.class)return null;if(type==boolean.class)return false;if(type==long.class)return 0L;if(type==float.class)return 0f;if(type==double.class)return 0d;if(type==char.class)return '\0';if(type==byte.class)return (byte)0;if(type==short.class)return (short)0;return 0;}

    private static final class Backend {
        final Path assets;final ArrayList<Stream> streams=new ArrayList<>();final Map<String,EffectResource> sounds=new HashMap<>();final Map<String,Float> durations=new HashMap<>();final ArrayList<String> errors=new ArrayList<>();private long nextHandle;
        Backend(Path assets){this.assets=assets;var manifest=new JsonReader().parse(new FileHandle(assets.resolve("audio/survival/exchanges.json").toFile()));for(var ex:manifest.get("exchanges"))for(var line:ex.get("lines")){durations.put(line.getString("path"),line.getFloat("duration"));durations.put(line.getString("clear_path"),line.getFloat("clear_duration"));}}
        String path(FileHandle file){assertTrue(file.exists(),"The production binding must resolve a real asset: "+file);return assets.relativize(file.file().toPath().toAbsolutePath().normalize()).toString().replace('\\','/');}
        Music music(FileHandle file){var stream=new Stream(path(file));streams.add(stream);return stream.music;}
        Sound sound(FileHandle file){var resource=new EffectResource(path(file),this);sounds.put(resource.path,resource);return resource.sound;}
        Stream latest(String path){return streams.stream().filter(s->s.path.equals(path)).reduce((a,b)->b).orElseThrow(()->new AssertionError("No stream allocated for "+path));}
        long count(String path){return streams.stream().filter(s->s.path.equals(path)).count();}
        EffectResource latestSound(String path){var resource=sounds.get(path);assertNotNull(resource,"No effect allocated for "+path);return resource;}
        float duration(String path){var seconds=durations.get(path);assertNotNull(seconds,"No authored duration for "+path);return seconds;}
        long liveEffects(){return sounds.values().stream().flatMap(s->s.voices.values().stream()).filter(v->v.live).count();}
        void advance(float dt){for(var s:streams)if(s.playing){s.position+=dt;if(!s.looping&&s.position>=durations.getOrDefault(s.path,Float.POSITIVE_INFINITY))s.playing=false;}for(var r:sounds.values())for(var v:r.voices.values())if(v.playing)v.position+=dt;}
    }
    private static final class Stream {
        final String path;boolean playing,looping,disposed,configured;int starts,stops;float position,volume;final Music music;
        Stream(String path){this.path=path;music=proxy(Music.class,(p,m,a)->{
            switch(m.getName()){
                case "play"->{assertFalse(disposed,"Cannot play a disposed decoder: "+path);assertTrue(configured,"Configure volume before opening the output gate");playing=true;starts++;}
                case "pause"->playing=false;
                case "stop"->{playing=false;position=0;stops++;}
                case "dispose"->{playing=false;disposed=true;}
                case "setLooping"->looping=(boolean)a[0];case "isLooping"->{return looping;}
                case "setVolume"->{volume=(float)a[0];configured=true;assertTrue(Float.isFinite(volume)&&volume>=0&&volume<=1,"Finite normalized stream gain");}
                case "getVolume"->{return volume;}case "isPlaying"->{return playing;}case "getPosition"->{return position;}case "setPosition"->position=(float)a[0];
                default->{return defaultValue(m.getReturnType());}
            }return null;
        });}
    }
    private static final class Voice {boolean live=true,playing=true;float position,volume,pan;}
    private static final class EffectResource {
        final String path;final Map<Long,Voice> voices=new LinkedHashMap<>();final Sound sound;boolean disposed;
        EffectResource(String path,Backend backend){this.path=path;sound=proxy(Sound.class,(p,m,a)->{
            switch(m.getName()){
                case "play","loop"->{assertFalse(disposed);var voice=new Voice();voice.volume=a==null||a.length==0?1:(float)a[0];long handle=++backend.nextHandle;voices.put(handle,voice);return handle;}
                case "pause","resume","stop"->{var selection=a==null||a.length==0?voices.values():List.of(voices.get((long)a[0]));for(var voice:selection)if(voice!=null){if(m.getName().equals("stop")){voice.live=false;voice.playing=false;}else voice.playing=m.getName().equals("resume")&&voice.live;}}
                case "setVolume"->{var v=voices.get((long)a[0]);if(v!=null)v.volume=(float)a[1];}
                case "setPan"->{var v=voices.get((long)a[0]);if(v!=null){v.pan=(float)a[1];v.volume=(float)a[2];}}
                case "dispose"->{disposed=true;for(var v:voices.values()){v.live=false;v.playing=false;}}
                default->{return defaultValue(m.getReturnType());}
            }return defaultValue(m.getReturnType());
        });}
        Voice latest(){return voices.values().stream().reduce((a,b)->b).orElseThrow();}
    }
}
