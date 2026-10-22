package io.zipcoder.originwar.desktop;

import com.badlogic.gdx.utils.Json;
import io.zipcoder.originwar.engine.RunEngine;
import io.zipcoder.originwar.model.GameState;
import io.zipcoder.originwar.model.PlayerAction;
import io.zipcoder.originwar.model.TurnResult;
import io.zipcoder.originwar.perception.PerceptionModel;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

/** Integrated campaign/AI evidence via the existing immutable action fixture, without another planner or injected state. */
class HypoxiaCampaignTest {
    @Test void realCampaignCanReachCriticalHypoxiaRefillAndRecoverWithPurePerception() throws Exception {
        var trace=new Trace();
        try {
            String source;
            try(var input=getClass().getResourceAsStream("/campaign/redirect-seed1.tsv")) {
                assertNotNull(input);source=new String(input.readAllBytes(),StandardCharsets.UTF_8).replace("\r\n","\n");
            }
            assertEquals("0094e429f2fa5a1a2830251dd96d8b8186038d613d7142c2270329af15cc43c7",
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8))));
            for(String line:source.lines().toList()) {
                String[] p=line.split("\t",-1);assertEquals(4,p.length);
                trace.act(new PlayerAction(PlayerAction.Type.valueOf(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]),p[3]));
                if(trace.state().activeDialogue.equals("sacrifice-plan"))break;
            }
            assertEquals(1067,trace.actions,"Exact known ordinary campaign prefix");
            GameState before=trace.state();
            assertEquals(10,before.deck);assertEquals("redirect",before.constructorRoute);
            assertTrue(before.flags.containsAll(List.of("constructor-defeated","crew-safe","fleet-standdown","carmack-override","alice-refused","broadcast")));
            assertTrue(before.inventory.stream().anyMatch(i -> i.type==GameState.ItemType.OXYGEN && i.power>0),"Finite tank must genuinely have been acquired");
            trace.act(PlayerAction.item(CHOOSE,"pod-go"));
            assertTrue(trace.state().activeDialogue.isEmpty());assertFalse(trace.state().victory);
            PerceptionModel.Frame clear=pureFrame(trace.run);assertEquals(PerceptionModel.Stage.CLEAR,clear.stage());
            trace.capture("hypoxia-clear");

            PerceptionModel.Frame warning=null;int waits=0;
            // Capacity increased after owner exploration feedback; drain the actual finite suit.
            while(trace.state().oxygenation>20 && waits<before.maxReserve+100) {
                trace.act(PlayerAction.of(WAIT));waits++;
                GameState s=trace.state();
                if(warning==null && s.oxygenation<=60 && s.oxygenation>30) {
                    warning=pureFrame(trace.run);assertEquals(PerceptionModel.Stage.WARNING,warning.stage());
                    trace.capture("hypoxia-warning");
                }
            }
            assertNotNull(warning,"Actual physiology must traverse warning stage");
            GameState criticalState=trace.state();assertEquals(20,criticalState.oxygenation);assertEquals(0,criticalState.reserve);
            PerceptionModel.Frame critical=pureFrame(trace.run);
            assertEquals(PerceptionModel.Stage.CRITICAL,critical.stage());assertTrue(critical.intensity()>warning.intensity());
            assertFalse(critical.impressions().isEmpty(),"Critical presentation must actually contain impressions here");
            trace.capture("hypoxia-critical");
            var scan=trace.act(PlayerAction.of(SCAN));assertEquals(0,scan.cost());
            assertTrue(scan.events().stream().anyMatch(e -> e.type().equals("scan") && e.detail().contains("oxygenation 20")),"Reliable true status remains accessible");

            var tank=trace.state().inventory.stream().filter(i -> i.type==GameState.ItemType.OXYGEN && i.power>0).findFirst().orElseThrow();
            int count=trace.state().inventory.size();
            trace.act(PlayerAction.item(USE,tank.id));GameState refilled=trace.state();
            assertEquals(count-1,refilled.inventory.size());assertFalse(refilled.inventory.stream().anyMatch(i -> i.id.equals(tank.id)));
            assertEquals(Math.min(criticalState.maxReserve,tank.power)-1,refilled.reserve,"Refill effect precedes its charged turn");
            assertEquals(20+RunEngine.OXYGENATION_RECOVERY,refilled.oxygenation,"Tank is not instant physiological recovery");
            assertTrue(refilled.hp<=criticalState.hp,"An oxygen tank does not grant health");
            for(int n=0;n<13;n++)trace.act(PlayerAction.of(WAIT));
            GameState recovered=trace.state();
            assertTrue(recovered.oxygenation>=70);assertEquals(Math.min(100,20+14*RunEngine.OXYGENATION_RECOVERY),recovered.oxygenation);
            assertEquals(refilled.reserve-13,recovered.reserve);assertEquals(before.perceptionRng,recovered.perceptionRng);
            PerceptionModel.Frame normal=pureFrame(trace.run);assertEquals(PerceptionModel.Stage.CLEAR,normal.stage());
            assertTrue(normal.impressions().isEmpty());assertEquals(0,normal.intensity());
            trace.capture("hypoxia-recovered");
            trace.export("PASS", "waits-to-critical="+waits+" tank="+tank.id+" power="+tank.power+" prefix=1067; actual AI, no state injection");
            System.out.println("INTEGRATED_HYPOXIA_PASS "+trace.summary()+" waits-to-critical="+waits+" tank="+tank.id);
        } catch(Exception | AssertionError failure) {
            trace.export("FAIL-"+System.currentTimeMillis(),failure.toString());
            throw failure;
        }
    }

    private static PerceptionModel.Frame pureFrame(RunEngine run) {
        GameState real=run.saveState();var json=new Json();json.setUsePrototypes(false);json.setTypeName(null);
        String stateBefore=json.toJson(real),engineBefore=json.toJson(run.saveState());
        PerceptionModel.Frame frame=PerceptionModel.sample(real,3,false);
        for(int n=0;n<100;n++)PerceptionModel.sample(real,n*.13,n%2==0);
        assertEquals(stateBefore,json.toJson(real),"Presentation must preserve every real DTO field, actor/resource and RNG");
        assertEquals(engineBefore,json.toJson(run.saveState()),"Presentation sampling must not advance the authoritative engine");
        for(var impression:frame.impressions()) {
            assertTrue(real.visible[impression.x()][impression.y()]);assertFalse(real.tiles[impression.x()][impression.y()].blocksMovement);
            assertFalse(real.actors.stream().anyMatch(a -> !a.dead && a.x==impression.x() && a.y==impression.y()),"A visual impression is not a real actor");
        }
        return frame;
    }

    private static final class Trace {
        final RunEngine run=new RunEngine(1);
        final StringBuilder commands=new StringBuilder("# seed=1; integrated campaign hypoxia; privileged fixture provenance, ordinary production actions\n");
        final StringBuilder details=new StringBuilder("action\ttype\tid\tcost\ttick\tdeck\thp\treserve\toxygenation\tperceptionRng\tevents\n");
        int actions;
        GameState state(){return run.saveState();}
        TurnResult act(PlayerAction action) {
            int tick=state().tick;TurnResult result=run.submit(action);actions++;GameState after=state();
            commands.append(action.type()).append('\t').append(action.x()).append('\t').append(action.y()).append('\t').append(action.id()).append('\n');
            details.append(actions).append('\t').append(action.type()).append('\t').append(action.id()).append('\t').append(result.cost()).append('\t')
                    .append(after.tick).append('\t').append(after.deck).append('\t').append(after.hp).append('\t').append(after.reserve).append('\t')
                    .append(after.oxygenation).append('\t').append(after.perceptionRng).append('\t').append(result.events()).append('\n');
            assertTrue(result.accepted(),"Rejected ordinary command at "+actions+": "+action);
            assertFalse(after.dead,"Real AI/oxygen death; preserve this first failure before any changed approach at "+actions);
            assertTrue(after.hp>0);
            assertEquals(tick+result.cost(),after.tick,"Presentation/commands may not add world ticks");
            return result;
        }
        void capture(String name) {
            commands.append("CAPTURE\t").append(name).append('\n');
            GameState s=state();details.append("CAPTURE\t").append(name).append("\ttick=").append(s.tick).append("\thp=").append(s.hp)
                    .append("\treserve=").append(s.reserve).append("\toxygenation=").append(s.oxygenation).append('\n');
        }
        String summary(){GameState s=state();return "actions="+actions+" tick="+s.tick+" deck="+s.deck+" hp="+s.hp+" reserve="+s.reserve+" oxygenation="+s.oxygenation+" actualActors="+s.actors.size();}
        void export(String outcome,String note) throws Exception {
            Path directory=Path.of("build","evidence");Files.createDirectories(directory);
            String stem="integrated-hypoxia-seed1"+(outcome.equals("PASS")?"":"-"+outcome);
            Files.writeString(directory.resolve(stem+".actions.tsv"),commands,StandardCharsets.UTF_8);
            Files.writeString(directory.resolve(stem+".trace.tsv"),details,StandardCharsets.UTF_8);
            Files.writeString(directory.resolve(stem+".txt"),outcome+" "+summary()+"\n"+note+"\n",StandardCharsets.UTF_8);
        }
    }
}
