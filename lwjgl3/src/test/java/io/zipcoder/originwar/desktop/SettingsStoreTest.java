package io.zipcoder.originwar.desktop;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class SettingsStoreTest {
    @TempDir Path dir;
    @Test void olderMutedEffectsSettingsKeepVoiceMutedUntilItIsAdjusted()throws Exception{
        Files.writeString(dir.resolve("settings.properties"),"sfx=0\nmusic=.3\n");
        var s=new SettingsStore(dir);assertEquals(0,s.voice);assertEquals(0,s.sfx);assertEquals(.35f,s.radio);assertEquals(.3f,s.music);
    }
    @Test void voiceAndStaticPersistIndependentlyFromEffects()throws Exception{
        var s=new SettingsStore(dir);s.voice=.8f;s.radio=0;s.sfx=.2f;s.save();s=new SettingsStore(dir);
        assertEquals(.8f,s.voice);assertEquals(0,s.radio);assertEquals(.2f,s.sfx);
    }
    @Test void newChannelsRejectNonFiniteAndClampOutOfRangeSettings()throws Exception{
        Files.writeString(dir.resolve("settings.properties"),"sfx=.4\nvoice=NaN\nradio=2\n");
        var s=new SettingsStore(dir);assertEquals(.4f,s.voice);assertEquals(1,s.radio);
    }
}
