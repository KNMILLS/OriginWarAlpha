package io.zipcoder.originwar.desktop;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import java.nio.channels.FileChannel;
import java.util.concurrent.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class SaveStoreTest {
    @TempDir Path dir;
    private GameState valid() {
        var s=new GameState();s.seed=420;s.combatRng=17;s.perceptionRng=22;s.tiles=new Tile[64][40];s.visible=new boolean[64][40];s.explored=new boolean[64][40];s.light=new float[64][40];
        for(var column:s.tiles)java.util.Arrays.fill(column,Tile.FLOOR);s.playerX=3;s.playerY=3;s.activeDialogue="";
        var a=new GameState.Actor();a.id="alice";a.kind=GameState.ActorKind.ALICE;a.x=4;a.y=3;s.actors.add(a);
        var i=new GameState.Item();i.id="dose-1";i.type=GameState.ItemType.MEDICINE;i.name="Velorin";s.inventory.add(i);
        s.flags.add("met-alice");s.medicineEffects.put("Velorin","heal");s.identifiedMedicine.add("Velorin");s.phantomUntil=19;s.phantomX=8;s.phantomY=4;return s;
    }
    @Test void roundTripPreservesTruthKnowledgeAndPerceptionSchedule()throws Exception {
        var s=valid();var store=new SaveStore(dir);store.begin(s);var loaded=new SaveStore(dir).load();
        assertEquals(420,loaded.seed);assertEquals(17,loaded.combatRng);assertEquals(22,loaded.perceptionRng);assertEquals(19,loaded.phantomUntil);
        assertEquals(GameState.ActorKind.ALICE,loaded.actors.getFirst().kind);assertEquals("dose-1",loaded.inventory.getFirst().id);assertTrue(loaded.flags.contains("met-alice"));assertEquals("heal",loaded.medicineEffects.get("Velorin"));assertTrue(loaded.identifiedMedicine.contains("Velorin"));
    }
    @Test void malformedPrimaryFallsBackToLastGoodSave()throws Exception {
        var store=new SaveStore(dir);var s=valid();store.begin(s);s.tick=1;store.save(s);Files.writeString(dir.resolve("active.json"),"{broken");
        var reopened=new SaveStore(dir);assertEquals(0,reopened.load().tick);assertTrue(reopened.usedBackup());
    }
    @Test void recordedDeathCannotResumeAnEarlierBackup()throws Exception {
        var store=new SaveStore(dir);var s=valid();store.begin(s);s.dead=true;s.hp=0;store.save(s);Files.writeString(dir.resolve("active.json"),"broken");
        assertThrows(java.io.IOException.class,()->new SaveStore(dir).load());
    }
    @Test void newExpeditionDoesNotInheritPreviousDeath()throws Exception {
        var store=new SaveStore(dir);var s=valid();store.begin(s);s.dead=true;s.hp=0;store.save(s);store.begin(valid());assertFalse(new SaveStore(dir).load().dead);
    }
    @Test void corruptionBeforeFirstActionCannotResurrectPreviousExpedition()throws Exception {
        var store=new SaveStore(dir);var first=valid();first.seed=1;store.begin(first);first.tick=1;store.save(first);
        var second=valid();second.seed=222;store.begin(second);Files.writeString(dir.resolve("active.json"),"broken");
        assertEquals(222,new SaveStore(dir).load().seed);
    }
    @Test void simulationInvalidPrimaryFallsBackBeforeEngineConstruction()throws Exception {
        var store=new SaveStore(dir);var s=valid();store.begin(s);s.tick=1;store.save(s);
        Path primary=dir.resolve("active.json");String original=Files.readString(primary),broken=original.replace("\"hatchX\":0","\"hatchX\":999");
        assertNotEquals(original,broken);Files.writeString(primary,broken);assertEquals(0,new SaveStore(dir).load().tick);
    }
    @Test void rejectOutOfBoundsAndUnsupportedVersions()throws Exception {
        var s=valid();s.playerX=999;assertThrows(java.io.IOException.class,()->new SaveStore(dir).begin(s));s.playerX=3;s.schemaVersion=99;assertThrows(java.io.IOException.class,()->SaveStore.validate(s));
    }
    @Test @EnabledOnOs(OS.WINDOWS) void briefWindowsDeleteSharingLockDoesNotLoseTheSave()throws Exception {
        var store=new SaveStore(dir);var s=valid();store.begin(s);s.tick=7;
        var lock=FileChannel.open(dir.resolve("active.json"),StandardOpenOption.READ,com.sun.nio.file.ExtendedOpenOption.NOSHARE_DELETE);
        try(var release=Executors.newSingleThreadScheduledExecutor()){
            release.schedule(()->{try{lock.close();}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}},90,TimeUnit.MILLISECONDS);
            try{store.save(s);}finally{lock.close();}
        }
        assertEquals(7,new SaveStore(dir).load().tick);
        Files.writeString(dir.resolve("active.json"),"broken");assertEquals(0,new SaveStore(dir).load().tick);
    }
}
