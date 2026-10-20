package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.campaign.Campaign;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.nio.file.Files;
import java.nio.file.Path;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

/** Actual seeded engines with finite resources and enabled AI. Planner knows geometry; not native/human play evidence. */
class CombatPlaythroughTest {
    @Test void originalStatsAndFiniteLootCanReachCommandDeckThroughOperatorRoute() { traverse(0, false); }
    @Test void earnedIntAndRealRedirectedExplosionsCanReachCommandDeck() { traverse(1, true); }

    private static void traverse(long seed, boolean redirect) {
        var pilot = new Pilot(seed, redirect);
        while (pilot.run.saveState().deck < 10 && pilot.actions < 6000 && !pilot.run.saveState().dead) pilot.decide();
        var end = pilot.run.saveState();
        String evidence = "seed=" + seed + " route=" + (redirect ? "redirect" : "operator") + " deck=" + end.deck
                + " hp=" + end.hp + " reserve=" + end.reserve + " ammo=" + end.batteries
                + " level=" + end.level + " actions=" + pilot.actions + " shots=" + pilot.shots + " melee=" + pilot.melee
                + " healing=" + pilot.healing + " equipped=" + pilot.equipped + " oxygen=" + pilot.oxygen + " redirects=" + pilot.redirects + " last=" + pilot.trace;
        pilot.export(seed, evidence);
        assertFalse(end.dead, evidence); assertEquals(10, end.deck, evidence);
        assertTrue(end.flags.contains("constructor-defeated"), evidence);
        assertEquals(redirect ? "redirect" : "operator", end.constructorRoute, evidence);
        assertFalse(end.victory, evidence);
        assertTrue(pilot.shots > 0, evidence); assertTrue(pilot.melee > 0, evidence);
        assertTrue(pilot.equipped > 0, evidence); assertTrue(pilot.oxygen > 0, evidence);
        if (redirect) assertTrue(pilot.redirects >= 2, evidence);
        System.out.println("FINITE_COMBAT_RUN " + evidence);
    }

    private static final class Pilot {
        final RunEngine run;
        final boolean redirect;
        final ArrayList<String> trace = new ArrayList<>();
        final StringBuilder replay = new StringBuilder("step\ttype\tx\ty\tid\tcost\tdeck\ttick\tplayerX\tplayerY\thp\treserve\toxygenation\tbatteries\n");
        int actions, preparedDeck, shots, melee, healing, equipped, oxygen, redirects;
        final java.util.HashSet<Integer> readDecks=new java.util.HashSet<>();

        Pilot(long seed, boolean redirect) { run = new RunEngine(seed); this.redirect = redirect; }

        void decide() {
            var s = run.saveState();
            if (!s.activeDialogue.isEmpty()) { act(PlayerAction.item(CHOOSE,Campaign.dialogue(s).choices().stream().filter(c->c.available(s)).findFirst().orElseThrow().id())); return; }
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
            actions++;
            trace.add("d" + before.deck + ":" + before.tick + " " + action.type() + " " + action.x() + "," + action.y()
                    + (action.id().isEmpty() ? "" : " " + action.id()) + " hp=" + run.saveState().hp);
            if (trace.size() > 12) trace.removeFirst();
            replay.append(actions).append('\t').append(action.type()).append('\t').append(action.x()).append('\t').append(action.y()).append('\t')
                    .append(action.id()).append('\t').append(result.cost()).append('\t').append(after.deck).append('\t').append(after.tick).append('\t')
                    .append(after.playerX).append('\t').append(after.playerY).append('\t').append(after.hp).append('\t').append(after.reserve).append('\t')
                    .append(after.oxygenation).append('\t').append(after.batteries).append('\n');
            assertTrue(result.accepted(), "Rejected legitimate pilot action " + trace);
        }

        void export(long seed, String summary) {
            try {
                Path directory = Path.of("build", "evidence"); Files.createDirectories(directory);
                String stem = "combat-seed" + seed + "-" + (redirect ? "redirect" : "operator");
                Files.writeString(directory.resolve(stem + ".tsv"), replay.toString());
                var end = run.saveState();
                Files.writeString(directory.resolve(stem + ".txt"), summary + "\nActors: " + end.actors.stream()
                        .map(a -> a.id + " " + a.kind + " " + a.x + "," + a.y + " HP=" + a.hp + " fuse=" + a.fuse + " redirected=" + a.redirected).toList()
                        + "\nInventory: " + end.inventory.stream().map(i -> i.id + " " + i.name + " " + i.type).toList());
            } catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        }

        static GameState.Item inventory(GameState s, GameState.ItemType type) {
            return s.inventory.stream().filter(i -> i.type == type).findFirst().orElse(null);
        }
    }
}
