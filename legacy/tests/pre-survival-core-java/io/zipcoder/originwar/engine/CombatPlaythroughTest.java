package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.campaign.Campaign;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

/** Privileged bot planning, ordinary engine actions, finite resources and enabled AI; not human/native play evidence. */
class CombatPlaythroughTest {
    @Test void finiteOperatorRouteReachesOriginalEndingAndReplaysFromNewGame() { traverse(0, false); }
    @Test void earnedIntRedirectRouteReachesOriginalEndingAndReplaysFromNewGame() { traverse(1, true); }

    private static void traverse(long seed, boolean redirect) {
        var pilot = new Pilot(seed, redirect);
        try {
            while (!pilot.run.saveState().victory && pilot.actions < 6000 && !pilot.run.saveState().dead) pilot.decide();
            var end = pilot.run.saveState();
            String evidence = pilot.summary();
            assertFalse(end.dead, evidence); assertTrue(end.hp > 0, evidence); assertEquals(10, end.deck, evidence);
            assertEquals(redirect ? "redirect" : "operator", end.constructorRoute, evidence);
            assertTrue(end.victory, evidence);
            for (String flag : List.of("campaign-started", "alice-joined", "story-read-supply-record", "crew-decided", "crew-safe",
                    "story-read-ethics-record", "agency-decided", "constructor-defeated", "fleet-decided", "fleet-records",
                    "carmack-override", "alice-refused", "broadcast", "sacrifice-chosen", "pod-sealed", "station-destroyed", "alice-sacrificed"))
                assertTrue(end.flags.contains(flag), flag + " missing: " + evidence);
            for (int deck=1;deck<=10;deck++) assertTrue(end.flags.contains("entered-d"+deck), evidence);
            assertTrue(pilot.shots > 0, evidence); assertTrue(pilot.melee > 0, evidence);
            assertTrue(pilot.equipped > 0, evidence); assertTrue(pilot.oxygen > 0, evidence);
            assertEquals(Set.of("midcampaign", "constructor-countdown", "final-dialogue"), pilot.resumed, evidence);
            assertTrue(end.flags.contains(redirect ? "choice-crew-guide" : "choice-crew-share"), evidence);
            assertEquals(redirect, end.flags.contains("fleet-standdown"), evidence);
            assertTrue(end.flags.contains(redirect ? "choice-fleet-persuade" : "choice-fleet-evidence"), evidence);
            assertTrue(Campaign.epilogue(end).contains("refuge technicians' pod"), evidence);
            assertTrue(Campaign.epilogue(end).contains(redirect ? "West holds the assault fleet back" : "West has the records"), evidence);
            if (redirect) {
                assertTrue(pilot.redirects >= 2, evidence);
                assertTrue(pilot.redirectedConstructorHits >= 2, "Actual redirected blasts must damage the Constructor twice: " + evidence);
                assertEquals(0, pilot.operatorMeleeHits, "Redirect route must not use the operator kill");
            } else assertTrue(pilot.operatorMeleeHits >= 2, "Operator must actually be reached and hit in adjacent melee: " + evidence);
            for (String name : List.of("opening", "after-briefing", "sector-1", "sector-2", "sector-3", "sector-4", "sector-5",
                    "first-combat", "alice-encounter", "constructor-before", "constructor-after", "carmack", "refusal", "broadcast", "pod-go", "final-epilogue"))
                assertTrue(pilot.captures.contains(name), "Missing capture " + name);
            Path commands = pilot.export("PASS");
            pilot.replayExport(commands);
            System.out.println("FINITE_CAMPAIGN_RUN " + evidence);
        } catch (RuntimeException | AssertionError failure) {
            pilot.export("FAIL-" + System.currentTimeMillis());
            throw failure;
        }
    }

    private static final class Pilot {
        RunEngine run;
        final long seed;
        final boolean redirect;
        final ArrayList<String> trace = new ArrayList<>();
        final StringBuilder commands = new StringBuilder();
        final StringBuilder detailed = new StringBuilder("step\ttype\tx\ty\tid\tcost\tdeck\ttick\tplayerX\tplayerY\thp\treserve\toxygenation\tbatteries\tdialogue\tbossPhase\tevents\n");
        final StringBuilder resumes = new StringBuilder("checkpoint\taction\tdeck\ttick\thp\treserve\tcombatRng\tlootRng\tmedicineRng\tperceptionRng\tdialogue\tcountdowns\n");
        final ArrayList<TurnResult> results = new ArrayList<>();
        final HashSet<String> captures = new HashSet<>(), resumed = new HashSet<>();
        int actions, preparedDeck, shots, melee, healing, equipped, oxygen, redirects, operatorMeleeHits, redirectedConstructorHits;
        final java.util.HashSet<Integer> readDecks=new java.util.HashSet<>();

        Pilot(long seed, boolean redirect) {
            run = new RunEngine(seed); this.seed=seed; this.redirect = redirect;
            commands.append("# seed=").append(seed).append("; privileged bot planning; ordinary production actions\n");
            captureState(run.saveState());
        }

        void decide() {
            var s = run.saveState();
            if (!s.activeDialogue.isEmpty()) {
                String preferred = switch(s.activeDialogue) {
                    case "crew" -> redirect ? "crew-guide" : "crew-share";
                    case "west-call" -> redirect ? "fleet-persuade" : "fleet-evidence";
                    default -> Campaign.dialogue(s).choices().getFirst().id();
                };
                assertTrue(Campaign.dialogue(s).choices().stream().anyMatch(c -> c.id().equals(preferred) && c.available(s)),
                        "Expected earned/resource-backed choice unavailable: " + preferred + " " + trace);
                act(PlayerAction.item(CHOOSE, preferred)); return;
            }
            if (s.attributePoints > 0) {
                String stat = redirect && s.intel < 6 ? "int" : s.str < 6 ? "str" : "dex";
                act(PlayerAction.item(ALLOCATE, stat)); return;
            }
            var tank = inventory(s, GameState.ItemType.OXYGEN);
            if (s.reserve <= 90 && tank != null) { oxygen++; act(PlayerAction.item(USE, tank.id)); return; }
            var heal = s.inventory.stream().filter(i -> i.type == GameState.ItemType.MEDICINE && s.identifiedMedicine.contains(i.name)
                    && "heal".equals(s.medicineEffects.get(i.name))).findFirst().orElse(null);
            if (s.hp <= 68 && heal != null) { healing++; act(PlayerAction.item(USE, heal.id)); return; }
            var cells = inventory(s, GameState.ItemType.CELL);
            if (s.batteries < 10 && cells != null) { act(PlayerAction.item(USE, cells.id)); return; }
            var imminent = s.actors.stream().filter(a -> CombatRules.alive(a) && a.kind == GameState.ActorKind.ROVER
                    && !a.redirected && a.fuse >= 0 && CombatRules.distance(a.x, a.y, s.playerX, s.playerY) <= 1).findFirst().orElse(null);
            if (imminent != null && escape(s, imminent)) return;
            if (s.deck == 8 && preparedDeck == 8 && !redirect && !s.flags.contains("constructor-defeated")) {
                var operator = s.actors.stream().filter(a -> a.kind == GameState.ActorKind.OPERATOR).findFirst().orElseThrow();
                int distance = CombatRules.distance(s.playerX, s.playerY, operator.x, operator.y);
                if (distance == 1) { act(PlayerAction.at(MELEE, operator.x, operator.y)); return; }
                // This pilot deliberately proves the earned-STR route by physically closing with the operator.
                if (moveToward(s, operator.x, operator.y)) return;
            }

            var threats = s.actors.stream().filter(a -> CombatRules.alive(a) && CombatRules.hostile(a) && !a.redirected
                    && !EnemyTurns.disabled(s, a) && s.visible[a.x][a.y] && a.kind != GameState.ActorKind.CONSTRUCTOR
                    && !(redirect && a.kind == GameState.ActorKind.OPERATOR))
                    .sorted(Comparator.comparingInt(a -> a.kind == GameState.ActorKind.OPERATOR ? -100
                            : CombatRules.distance(a.x, a.y, s.playerX, s.playerY))).toList();
            if (!threats.isEmpty()) {
                var enemy = threats.getFirst();
                int distance = CombatRules.distance(enemy.x, enemy.y, s.playerX, s.playerY);
                if (enemy.kind == GameState.ActorKind.ROVER && enemy.fuse >= 0 && distance <= 1 && escape(s, enemy)) return;
                if (enemy.kind == GameState.ActorKind.ROVER && s.intel >= 6 && distance <= 5) {
                    if (s.deck == 8) redirects++;
                    act(PlayerAction.at(HACK, enemy.x, enemy.y)); return;
                }
                if (distance == 1) { act(PlayerAction.at(MELEE, enemy.x, enemy.y)); return; }
                // Use the STR build's renewable melee against close standard soldiers; save cells for ranged threats/operator.
                if (enemy.kind == GameState.ActorKind.SIRS && distance <= 4 && moveToward(s, enemy.x, enemy.y)) return;
                if (distance <= EquipmentRules.weapon(s.weapon).range() && s.batteries >= EquipmentRules.weapon(s.weapon).batteries()
                        && CombatRules.clearLine(s, s.playerX, s.playerY, enemy.x, enemy.y, true)) {
                    shots++; act(PlayerAction.at(SHOOT, enemy.x, enemy.y)); return;
                }
                if (moveToward(s, enemy.x, enemy.y)) return;
            }

            var pickup = s.floorItems.stream().filter(i -> i.x == s.playerX && i.y == s.playerY && wanted(s, i)).findFirst().orElse(null);
            if (pickup != null && s.inventory.size() < RunEngine.MAX_INVENTORY) { act(PlayerAction.item(PICKUP, pickup.id)); return; }
            var armor = s.inventory.stream().filter(i -> i.type == GameState.ItemType.ARMOR_HEAVY
                    || (s.armor.equals("suit") && i.type == GameState.ItemType.ARMOR_LIGHT)).findFirst().orElse(null);
            if (armor != null && !EquipmentRules.armorId(armor.type).equals(s.armor)) {
                equipped++; act(PlayerAction.item(EQUIP, armor.id)); return;
            }

            if (preparedDeck != s.deck) {
                if (CombatRules.distance(s.playerX, s.playerY, s.terminalX, s.terminalY) > 1) {
                    if (moveToward(s, s.terminalX, s.terminalY)) return;
                } else {
                    boolean unknown = s.inventory.stream().anyMatch(i -> i.type == GameState.ItemType.MEDICINE && !s.identifiedMedicine.contains(i.name));
                    if (unknown) { act(PlayerAction.at(INTERACT, s.terminalX, s.terminalY)); return; }
                    if(readDecks.add(s.deck)){act(PlayerAction.at(INTERACT,s.terminalX,s.terminalY));return;}
                    if (s.intel >= 5 && !s.flags.contains("security-hacked-d" + s.deck)) {
                        act(PlayerAction.at(HACK, s.terminalX, s.terminalY)); return;
                    }
                    preparedDeck = s.deck;
                }
            }

            if (s.deck == 8 && !s.flags.contains("constructor-defeated")) {
                var machine = s.actors.stream().filter(a -> a.kind == GameState.ActorKind.CONSTRUCTOR).findFirst().orElseThrow();
                var operator = s.actors.stream().filter(a -> a.kind == GameState.ActorKind.OPERATOR).findFirst().orElseThrow();
                if (!redirect) { if (moveToward(s, operator.x, operator.y)) return; }
                else {
                    int distance = CombatRules.distance(s.playerX, s.playerY, machine.x, machine.y);
                    if (distance > 5 || !CombatRules.clearLine(s, s.playerX, s.playerY, machine.x, machine.y, false)) {
                        if (moveToward(s, machine.x, machine.y)) return;
                    } else { act(PlayerAction.of(WAIT)); return; }
                }
            }
            int goalX = s.switchOn ? s.hatchX : s.switchX, goalY = s.switchOn ? s.hatchY : s.switchY;
            if (s.playerX == goalX && s.playerY == goalY) { act(PlayerAction.of(s.switchOn ? ADVANCE : INTERACT)); return; }
            assertTrue(moveToward(s, goalX, goalY), "No production path: " + trace);
        }

        boolean escape(GameState s, GameState.Actor enemy) {
            var options = new ArrayList<Coord>();
            for (int[] direction : GridRoutes.DIRECTIONS) {
                int x = s.playerX + direction[0], y = s.playerY + direction[1];
                if (GridRoutes.inside(s, x, y) && !s.tiles[x][y].blocksMovement && CombatRules.at(s, x, y) == null
                        && CombatRules.distance(x, y, enemy.x, enemy.y) > 1) options.add(Coord.get(x, y));
            }
            if (options.isEmpty()) return false;
            if (s.deck == 8 && !redirect && !s.flags.contains("constructor-defeated")) {
                var operator = s.actors.stream().filter(a -> a.kind == GameState.ActorKind.OPERATOR).findFirst().orElseThrow();
                options.sort(Comparator.comparingInt(c -> GridRoutes.route(s, c.x, c.y, operator.x, operator.y, false).size()));
            }
            Coord best = options.getFirst();
            act(PlayerAction.move(best.x - s.playerX, best.y - s.playerY)); return true;
        }

        boolean moveToward(GameState s, int x, int y) {
            var route = GridRoutes.route(s, s.playerX, s.playerY, x, y, false);
            if (route.isEmpty()) return false;
            Coord next = route.getFirst();
            act(PlayerAction.move(next.x - s.playerX, next.y - s.playerY)); return true;
        }

        boolean wanted(GameState s, GameState.Item item) {
            long sameType = s.inventory.stream().filter(i -> i.type == item.type).count();
            return switch (item.type) {
                case OXYGEN -> sameType < 3;
                case CELL -> sameType < 2;
                case MEDICINE -> s.identifiedMedicine.contains(item.name)
                        ? "heal".equals(s.medicineEffects.get(item.name)) && s.inventory.stream().filter(i -> i.type == item.type && i.name.equals(item.name)).count() < 3
                        : s.inventory.stream().filter(i -> i.type == item.type && !s.identifiedMedicine.contains(i.name)).count() < 2;
                default -> sameType == 0;
            };
        }

        void act(PlayerAction action) {
            var before = run.saveState();
            TurnResult result = run.submit(action);
            melee+=(int)result.events().stream().filter(e->e.type().equals("melee")).count();
            GameState after = run.saveState();
            if (before.deck==8) {
                var operator=before.actors.stream().filter(a -> a.kind==GameState.ActorKind.OPERATOR).findFirst().orElseThrow();
                if(result.events().stream().anyMatch(e -> e.type().equals("melee"))
                        && result.events().stream().anyMatch(e -> e.type().equals("hit") && e.x()==operator.x && e.y()==operator.y && e.detail().startsWith("OPERATOR takes"))) {
                    assertEquals(1,CombatRules.distance(before.playerX,before.playerY,operator.x,operator.y));operatorMeleeHits++;
                }
                var machine=before.actors.stream().filter(a -> a.kind==GameState.ActorKind.CONSTRUCTOR).findFirst().orElseThrow();
                if(result.events().stream().anyMatch(e -> e.type().equals("hit") && e.x()==machine.x && e.y()==machine.y && e.detail().equals("CONSTRUCTOR takes 26 damage."))) {
                    assertTrue(result.events().stream().anyMatch(e -> e.type().equals("explosion")),"Constructor damage requires a real explosion");
                    assertTrue(before.actors.stream().anyMatch(a -> a.kind==GameState.ActorKind.ROVER && a.redirected && CombatRules.alive(a)),"Real redirected rover must exist before its blast");
                    redirectedConstructorHits++;
                }
            }
            actions++;
            results.add(result);
            trace.add("d" + before.deck + ":" + before.tick + " " + action.type() + " " + action.x() + "," + action.y()
                    + (action.id().isEmpty() ? "" : " " + action.id()) + " hp=" + after.hp);
            if (trace.size() > 12) trace.removeFirst();
            commands.append(action.type()).append('\t').append(action.x()).append('\t').append(action.y()).append('\t').append(action.id()).append('\n');
            detailed.append(actions).append('\t').append(action.type()).append('\t').append(action.x()).append('\t').append(action.y()).append('\t')
                    .append(action.id()).append('\t').append(result.cost()).append('\t').append(after.deck).append('\t').append(after.tick).append('\t')
                    .append(after.playerX).append('\t').append(after.playerY).append('\t').append(after.hp).append('\t').append(after.reserve).append('\t')
                    .append(after.oxygenation).append('\t').append(after.batteries).append('\t').append(after.activeDialogue).append('\t')
                    .append(after.bossPhase).append('\t').append(result.events().stream().map(e -> e.type()+":"+e.detail()).toList()).append('\n');
            assertTrue(result.accepted(), "Rejected legitimate pilot action " + trace);
            assertTrue(after.hp > 0 || !after.victory, "Dead player received victory: " + trace);
            assertTrue(after.inventory.size() <= RunEngine.MAX_INVENTORY && after.reserve >= 0 && after.batteries >= 0, "Invalid resource boundary " + trace);
            if(action.type()==ALLOCATE) {
                assertTrue(before.attributePoints>0,"No unearned allocation");
                assertEquals(before.attributePoints-1,after.attributePoints);
                assertEquals(before.str+before.intel+before.dex+1,after.str+after.intel+after.dex);
            }
            if (action.type() == CHOOSE && action.id().startsWith("crew-") && !action.id().equals("crew-close")) {
                assertEquals(1, result.cost(), "A real refuge rescue costs a world turn");
                assertEquals(before.reserve-(action.id().equals("crew-share")?41:1), after.reserve, "Finite reserve rescue and its turn");
                assertTrue(after.floorItems.stream().anyMatch(i -> i.id.equals("crew-reserve") && i.power==120), "Real finite rescue reward");
            }
            if (result.events().stream().anyMatch(e -> e.type().equals("gun") || e.type().equals("melee"))) capture("first-combat");
            captureState(after);
            if (after.deck==5 && after.activeDialogue.isEmpty()) resume("midcampaign");
            if (after.deck==8 && !after.flags.contains("constructor-defeated")
                    && after.actors.stream().anyMatch(a -> CombatRules.alive(a) && a.kind==GameState.ActorKind.ROVER && a.fuse>=0)) resume("constructor-countdown");
            if (after.activeDialogue.equals("sacrifice-plan")) resume("final-dialogue");
        }

        void captureState(GameState state) {
            if (actions==0) capture("opening");
            if (state.deck==1 && state.activeDialogue.isEmpty()) capture("after-briefing");
            if (state.deck%2==1) capture("sector-"+((state.deck+1)/2));
            if (state.activeDialogue.equals("alice-meet")) capture("alice-encounter");
            if (state.deck==8 && state.bossPhase.equals("active")) capture("constructor-before");
            if (state.deck==8 && state.flags.contains("constructor-defeated")) capture("constructor-after");
            if (state.activeDialogue.equals("carmack")) capture("carmack");
            if (state.activeDialogue.equals("refusal")) capture("refusal");
            if (state.activeDialogue.equals("sacrifice-plan")) capture("broadcast");
            if (state.flags.contains("sacrifice-chosen")) capture("pod-go");
            if (state.victory) capture("final-epilogue");
        }

        void capture(String name) {
            if(captures.add(name)) commands.append("CAPTURE\t").append(name).append('\n');
        }

        void resume(String checkpoint) {
            if (!resumed.add(checkpoint)) return;
            GameState saved = run.saveState();
            RunEngine restored = new RunEngine(saved);
            assertEquals(canonical(saved), canonical(restored.saveState()), "Save/resume changed complete state at " + checkpoint);
            resumes.append(checkpoint).append('\t').append(actions).append('\t').append(saved.deck).append('\t').append(saved.tick)
                    .append('\t').append(saved.hp).append('\t').append(saved.reserve).append('\t').append(saved.combatRng).append('\t')
                    .append(saved.lootRng).append('\t').append(saved.medicineRng).append('\t').append(saved.perceptionRng).append('\t')
                    .append(saved.activeDialogue).append('\t').append(saved.actors.stream().filter(a -> a.kind==GameState.ActorKind.ROVER && a.fuse>=0)
                            .map(a -> a.id+":"+a.fuse+":"+a.redirected).toList()).append('\n');
            // The ongoing pilot really uses the restored engine; the uninterrupted fresh replay checks subsequent determinism.
            run=restored;
        }

        String summary() {
            var end=run.saveState();
            return "seed="+seed+" route="+(redirect?"redirect":"operator")+" victory="+end.victory+" deck="+end.deck
                    +" hp="+end.hp+" reserve="+end.reserve+" oxygenation="+end.oxygenation+" ammo="+end.batteries
                    +" level="+end.level+" xp="+end.xp+" STR/INT/DEX="+end.str+"/"+end.intel+"/"+end.dex
                    +" actions="+actions+" ticks="+end.tick+" shots="+shots+" melee="+melee+" healing="+healing
                    +" equipped="+equipped+" oxygen="+oxygen+" redirects="+redirects+" operatorMeleeHits="+operatorMeleeHits
                    +" redirectedConstructorHits="+redirectedConstructorHits+" resumed="+resumed+" last="+trace;
        }

        Path export(String outcome) {
            try {
                Path directory = Path.of("build", "evidence"); Files.createDirectories(directory);
                String stem = "campaign-seed" + seed + "-" + (redirect ? "redirect" : "operator") + (outcome.equals("PASS")?"":"-"+outcome);
                Path file=directory.resolve(stem + ".actions.tsv");
                Files.writeString(file, commands.toString());
                Files.writeString(directory.resolve(stem + ".trace.tsv"), detailed.toString());
                Files.writeString(directory.resolve(stem + ".resumes.tsv"), resumes.toString());
                var end = run.saveState();
                Files.writeString(directory.resolve(stem + ".txt"), outcome+" "+summary()+"\nPrivileged bot planning; actual production actions with enabled AI and finite resources. Not human/native play or desktop SaveStore evidence.\nFlags: "+end.flags.stream().sorted().toList()+"\nEpilogue: "+(end.victory?Campaign.epilogue(end):"not reached")+"\nActors: " + end.actors.stream()
                        .map(a -> a.id + " " + a.kind + " " + a.x + "," + a.y + " HP=" + a.hp + " fuse=" + a.fuse + " redirected=" + a.redirected).toList()
                        + "\nInventory: " + end.inventory.stream().map(i -> i.id + " " + i.name + " " + i.type).toList());
                return file;
            } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        }

        void replayExport(Path file) {
            try {
                RunEngine fresh = new RunEngine(seed);
                int step=0;
                for (String line : Files.readAllLines(file)) {
                    if(line.isBlank() || line.startsWith("#"))continue;
                    String[] parts=line.split("\t",-1);
                    if(parts[0].equals("CAPTURE")) { assertEquals(2,parts.length); continue; }
                    assertEquals(4,parts.length,"Runtime action TSV shape");
                    var action=new PlayerAction(PlayerAction.Type.valueOf(parts[0]),Integer.parseInt(parts[1]),Integer.parseInt(parts[2]),parts[3]);
                    TurnResult result=fresh.submit(action);
                    assertEquals(results.get(step++),result,"Fresh exported replay differs at step "+step+": "+line);
                }
                assertEquals(actions,step,"Every actual action exported");
                assertEquals(canonical(run.saveState()),canonical(fresh.saveState()),"Uninterrupted runtime-format replay differs from resumed campaign");
                assertTrue(fresh.saveState().victory && fresh.saveState().hp>0,"Fresh replay must finish alive");
                System.out.println("EXPORTED_REPLAY_PASS seed="+seed+" commands="+step+" captures="+captures.size()+" file="+file.toAbsolutePath());
            } catch(java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        }

        static GameState.Item inventory(GameState s, GameState.ItemType type) {
            return s.inventory.stream().filter(i -> i.type == type).findFirst().orElse(null);
        }
    }

    /** Compare every public save field, including all RNGs, knowledge, actor fuses, grids and story collections. */
    private static String canonical(Object value) {
        if(value==null)return "null";
        if(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>)return value.toString();
        if(value.getClass().isArray()) {
            var result=new ArrayList<String>();for(int n=0;n<Array.getLength(value);n++)result.add(canonical(Array.get(value,n)));return result.toString();
        }
        if(value instanceof Map<?,?> map)return map.entrySet().stream().map(e -> canonical(e.getKey())+"="+canonical(e.getValue())).sorted().toList().toString();
        if(value instanceof Set<?> set)return set.stream().map(CombatPlaythroughTest::canonical).sorted().toList().toString();
        if(value instanceof Iterable<?> list) {var result=new ArrayList<String>();for(Object item:list)result.add(canonical(item));return result.toString();}
        var fields=new ArrayList<String>();
        for(var field:value.getClass().getFields())if(!Modifier.isStatic(field.getModifiers())) {
            try { fields.add(field.getName()+"="+canonical(field.get(value))); }
            catch(IllegalAccessException error) {throw new AssertionError(error);}
        }
        assertFalse(fields.isEmpty(),"Unexpected save value "+value.getClass());
        fields.sort(String::compareTo);return fields.toString();
    }
}
