package io.zipcoder.originwar.desktop;

import io.zipcoder.originwar.campaign.Campaign;
import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.GameState;
import io.zipcoder.originwar.model.PlayerAction;
import io.zipcoder.originwar.model.TurnResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

/** Immutable successful action streams, not a second planner. Filesystem persistence uses production SaveStore. */
class CampaignSaveTest {
    @TempDir Path temporary;

    private record Fixture(String name,long seed,String route,int actions,String hash) {}
    private static final Fixture OPERATOR=new Fixture("operator-seed0.tsv",0,"operator",1936,
            "9362102c1330c001f77fbf80b0710217d37e05bd7c46c9353a4dda26e63d4422");
    private static final Fixture REDIRECT=new Fixture("redirect-seed1.tsv",1,"redirect",1131,
            "0094e429f2fa5a1a2830251dd96d8b8186038d613d7142c2270329af15cc43c7");

    @Test void operatorCampaignSurvivesRealFilesAndCorruptPrimaryFallback() throws Exception { replayCampaign(OPERATOR,true); }
    @Test void redirectCampaignSurvivesRealFilesAtAllThreeCriticalCheckpoints() throws Exception { replayCampaign(REDIRECT,false); }

    private void replayCampaign(Fixture fixture,boolean corruptMidcampaign) throws Exception {
        List<PlayerAction> actions=fixture(fixture);
        RunEngine reference=new RunEngine(fixture.seed),resumed=new RunEngine(fixture.seed);
        assertEquals(4,resumed.saveState().str);assertEquals(4,resumed.saveState().intel);assertEquals(4,resumed.saveState().dex);
        Path directory=temporary.resolve(fixture.route);
        SaveStore writer=new SaveStore(directory);writer.begin(resumed.saveState());
        Set<String> checkpoints=new HashSet<>();
        StringBuilder evidence=new StringBuilder("checkpoint\taction\tdeck\ttick\thp\treserve\tbatteries\tcombatRng\tlootRng\tmedicineRng\tperceptionRng\tdialogue\tliveRovers\tusedBackup\n");
        int step=0;
        try {
            for (PlayerAction action:actions) {
                TurnResult expected=reference.submit(action),actual=resumed.submit(action);step++;
                assertTrue(actual.accepted(),"Rejected fixture action "+fixture.name+":"+step+" "+action);
                assertEquals(expected,actual,"Post-resume event/cost determinism at "+fixture.name+":"+step);
                GameState truth=reference.saveState(),saved=resumed.saveState();
                equalState(truth,saved,fixture.name+":"+step);
                assertTrue(!saved.victory || saved.hp>0,"Dead victory");
                String checkpoint=checkpoint(saved,checkpoints);
                if(checkpoint==null)continue;
                checkpoints.add(checkpoint);
                writer.save(saved);
                boolean fallback=corruptMidcampaign && checkpoint.equals("midcampaign");
                if(fallback) {
                    // Write a genuinely later state with the next immutable normal action. Its backup is this real checkpoint.
                    // The uninterrupted reference remains here; after fallback both engines consume that action normally.
                    RunEngine later=new RunEngine(saved);
                    assertTrue(later.submit(actions.get(step)).accepted());
                    assertTrue(later.saveState().tick>saved.tick,"Distinct later primary for meaningful fallback");
                    writer.save(later.saveState());
                    Files.writeString(directory.resolve("active.json"),"{corrupt-primary",StandardCharsets.UTF_8);
                }
                SaveStore reopened=new SaveStore(directory);
                GameState loaded=reopened.load();
                assertEquals(fallback,reopened.usedBackup(),"Expected primary versus backup selection");
                equalState(saved,loaded,"JSON file roundtrip "+checkpoint);
                resumed=new RunEngine(loaded);writer=reopened;
                equalState(truth,resumed.saveState(),"Engine resume "+checkpoint);
                evidence.append(checkpoint).append('\t').append(step).append('\t').append(loaded.deck).append('\t').append(loaded.tick)
                        .append('\t').append(loaded.hp).append('\t').append(loaded.reserve).append('\t').append(loaded.batteries)
                        .append('\t').append(loaded.combatRng).append('\t').append(loaded.lootRng).append('\t').append(loaded.medicineRng)
                        .append('\t').append(loaded.perceptionRng).append('\t').append(loaded.activeDialogue).append('\t')
                        .append(loaded.actors.stream().filter(a -> a.kind==GameState.ActorKind.ROVER && !a.dead && a.fuse>=0)
                                .map(a -> a.id+":"+a.fuse+":"+a.redirected).toList()).append('\t').append(reopened.usedBackup()).append('\n');
            }
            assertEquals(Set.of("midcampaign","constructor-countdown","final-dialogue"),checkpoints);
            GameState end=resumed.saveState();
            assertTrue(end.victory && !end.dead && end.hp>0,"Canonical ending must survive actual files");
            assertEquals(fixture.route,end.constructorRoute);
            for(String flag:List.of("constructor-defeated","crew-safe","fleet-records","carmack-override","alice-refused",
                    "broadcast","sacrifice-chosen","pod-sealed","station-destroyed","alice-sacrificed"))assertTrue(end.flags.contains(flag),flag);
            assertTrue(end.flags.contains(fixture.seed==0?"choice-crew-share":"choice-crew-guide"));
            assertEquals(fixture.seed==1,end.flags.contains("fleet-standdown"));
            assertEquals(Campaign.epilogue(reference.saveState()),Campaign.epilogue(end));
            // A completed expedition is also a truthful file save; no extra action may run after victory.
            writer.save(end);SaveStore completed=new SaveStore(directory);GameState completedState=completed.load();
            assertFalse(completed.usedBackup());equalState(end,completedState,"Final ending file");
            assertFalse(new RunEngine(completedState).submit(PlayerAction.of(WAIT)).accepted());
            evidence.append("PASS actions=").append(step).append(" ticks=").append(end.tick).append(" hp=").append(end.hp)
                    .append(" reserve=").append(end.reserve).append(" batteries=").append(end.batteries).append(" route=").append(end.constructorRoute).append('\n');
            writeEvidence(fixture.route,evidence.toString());
            System.out.println("FILE_CAMPAIGN_PASS seed="+fixture.seed+" route="+fixture.route+" actions="+step+" checkpoints="+checkpoints+" fallback="+corruptMidcampaign);
        } catch(Exception | AssertionError failure) {
            writeEvidence(fixture.route+"-FAIL-"+System.currentTimeMillis(),evidence+"FAIL at action "+step+": "+failure+"\n");
            throw failure;
        }
    }

    @Test void ordinaryWaitDeathBlocksLivingBackupAndNewRunStillStartsClean() throws Exception {
        RunEngine run=new RunEngine(0);
        // Only real opening choices and WAIT commands: no HP/resource/actor/state injection.
        assertTrue(run.submit(PlayerAction.item(CHOOSE,"briefing-question")).accepted());
        assertTrue(run.submit(PlayerAction.item(CHOOSE,"west-question")).accepted());
        SaveStore store=new SaveStore(temporary);store.begin(run.saveState());
        GameState living=run.saveState();int waits=0;
        while(!run.saveState().dead && waits<1000) {
            living=run.saveState();assertTrue(run.submit(PlayerAction.of(WAIT)).accepted());waits++;
        }
        GameState ended=run.saveState();assertTrue(ended.dead);assertEquals(0,ended.hp);assertTrue(living.hp>0);
        store.save(living);store.save(ended);
        assertThrows(SaveStore.EndedRunException.class,()->new SaveStore(temporary).load());
        Files.writeString(temporary.resolve("active.json"),"{corrupt-dead-primary");
        assertThrows(IOException.class,()->new SaveStore(temporary).load(),"Living backup must not resurrect recorded death");
        GameState fresh=new RunEngine(1).saveState();new SaveStore(temporary).begin(fresh);
        Files.writeString(temporary.resolve("active.json"),"{corrupt-new-primary");
        SaveStore reopened=new SaveStore(temporary);GameState loaded=reopened.load();
        assertTrue(reopened.usedBackup());assertFalse(loaded.dead);equalState(fresh,loaded,"New expedition backup after real prior death");
        writeEvidence("death","PASS seed0 ordinary WAIT death after "+waits+" waits; HP=0; living same-run backup rejected; seed1 fresh backup retained after corrupt first primary.\n");
    }

    private static String checkpoint(GameState s,Set<String> seen) {
        if(s.deck==5 && s.activeDialogue.isEmpty() && !seen.contains("midcampaign"))return "midcampaign";
        if(s.deck==8 && !s.flags.contains("constructor-defeated") && !seen.contains("constructor-countdown")
                && s.actors.stream().anyMatch(a -> !a.dead && a.kind==GameState.ActorKind.ROVER && a.fuse>=0))return "constructor-countdown";
        if(s.activeDialogue.equals("sacrifice-plan") && !seen.contains("final-dialogue"))return "final-dialogue";
        return null;
    }

    private static List<PlayerAction> fixture(Fixture fixture) throws Exception {
        String text;
        try(var input=CampaignSaveTest.class.getResourceAsStream("/campaign/"+fixture.name)) {
            assertNotNull(input,"Checked-in ordinary-action fixture");text=new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n");
        }
        assertEquals(fixture.hash,HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))),"Immutable normalized action fixture");
        var actions=new ArrayList<PlayerAction>();
        for(String line:text.lines().toList()) {
            String[] parts=line.split("\t",-1);assertEquals(4,parts.length,"Only normal action lines in fixture");
            actions.add(new PlayerAction(PlayerAction.Type.valueOf(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]),parts[3]));
        }
        assertEquals(fixture.actions,actions.size());return List.copyOf(actions);
    }

    /** Every DTO field, with deep primitive grids and ordered actor/item lists; excludes no RNG or story field. */
    private static void equalState(Object expected,Object actual,String context) {
        if(expected==null || expected instanceof String || expected instanceof Number || expected instanceof Boolean
                || expected instanceof Enum<?> || expected instanceof Map<?,?> || expected instanceof Set<?> || expected.getClass().isArray()) {
            assertTrue(Objects.deepEquals(expected,actual),()->"Saved value mismatch: "+context);return;
        }
        assertNotNull(actual,context);assertEquals(expected.getClass(),actual.getClass(),context);
        if(expected instanceof List<?> first && actual instanceof List<?> second) {
            assertEquals(first.size(),second.size(),context);
            for(int n=0;n<first.size();n++)equalState(first.get(n),second.get(n),context+"["+n+"]");return;
        }
        assertTrue(expected instanceof GameState || expected instanceof GameState.Actor || expected instanceof GameState.Item,"Unexpected DTO type");
        for(var field:expected.getClass().getFields())if(!Modifier.isStatic(field.getModifiers())) {
            try {equalState(field.get(expected),field.get(actual),context+"."+field.getName());}
            catch(IllegalAccessException error) {throw new AssertionError(error);}
        }
    }

    private static void writeEvidence(String name,String text) throws IOException {
        Path directory=Path.of("build","evidence");Files.createDirectories(directory);
        Files.writeString(directory.resolve("campaign-files-"+name+".tsv"),text,StandardCharsets.UTF_8);
    }
}
