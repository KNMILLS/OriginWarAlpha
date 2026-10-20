package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;

import java.util.List;

/** One scheduler invocation per actual world tick, never per animation/render frame. */
final class EnemyTurns {
    private EnemyTurns() {}

    static boolean disabled(GameState s, GameState.Actor actor) { return s.flags.contains("disabled:" + actor.id); }

    static void hear(GameState s, int x, int y, int radius) {
        for (var actor : s.actors) {
            if (!CombatRules.alive(actor) || !CombatRules.hostile(actor) || disabled(s, actor)) continue;
            int distance = CombatRules.distance(actor.x, actor.y, x, y);
            if (distance <= radius && (distance <= radius / 2 || CombatRules.clearLine(s, actor.x, actor.y, x, y, false))) {
                actor.alert = Math.max(actor.alert, 7); actor.lastX = x; actor.lastY = y;
            }
        }
    }

    static void advance(GameState s, List<GameEvent> events) {
        var actorsAtTickStart = List.copyOf(s.actors);
        for (var actor : actorsAtTickStart) {
            if (s.dead) break;
            if (!CombatRules.alive(actor) || !CombatRules.hostile(actor) || disabled(s, actor)) continue;
            if (actor.kind == GameState.ActorKind.CONSTRUCTOR) { constructor(s, actor, events); continue; }
            if (actor.kind == GameState.ActorKind.CAMERA) { camera(s, actor, events); continue; }
            if (actor.kind == GameState.ActorKind.ROVER && actor.redirected) { rover(s, actor, events); continue; }
            int range = actor.kind == GameState.ActorKind.ADD ? 9 : actor.kind == GameState.ActorKind.ROVER ? 8 : 7;
            boolean sees = CombatRules.distance(actor.x, actor.y, s.playerX, s.playerY) <= range
                    && CombatRules.clearLine(s, actor.x, actor.y, s.playerX, s.playerY, false);
            if (sees) { actor.alert = 8; actor.lastX = s.playerX; actor.lastY = s.playerY; }
            else if (actor.alert > 0) actor.alert--;
            if (actor.kind == GameState.ActorKind.ROVER) { rover(s, actor, events); continue; }
            if (actor.alert <= 0) {
                if (actor.kind == GameState.ActorKind.SIRS && s.tick % 4 == 0) patrol(s, actor, events);
                continue;
            }
            int distance = CombatRules.distance(actor.x, actor.y, s.playerX, s.playerY);
            if (actor.kind == GameState.ActorKind.TURRET) {
                if (sees && distance <= 7 && s.tick % 3 == 0) attack(s, actor, 7, 74, events);
            } else if (actor.kind == GameState.ActorKind.ADD) {
                if (s.tick % 2 != 0) continue;
                if (sees && distance < 3) retreat(s, actor, events);
                else if (sees && distance <= 7) attack(s, actor, 8, 78, events);
                else pursue(s, actor, actor.lastX, actor.lastY, events);
            } else if (actor.kind == GameState.ActorKind.OPERATOR) {
                if (sees && distance <= 5 && s.tick % 3 == 0) attack(s, actor, 6, 65, events);
            } else if (s.tick % 2 == 0) {
                if (sees && distance <= 3) attack(s, actor, 5, 64, events);
                else pursue(s, actor, actor.lastX, actor.lastY, events);
            }
        }
    }

    private static void attack(GameState s, GameState.Actor actor, int damage, int accuracy, List<GameEvent> events) {
        if (!CombatRules.clearLine(s, actor.x, actor.y, s.playerX, s.playerY, true)) return;
        actor.facing = CombatRules.facing(actor.x, actor.y, s.playerX, s.playerY);
        int chance = CombatRules.clampChance(accuracy - EquipmentRules.effectiveDex(s) * 3
                - (CombatRules.cover(s, actor.x, actor.y, s.playerX, s.playerY) ? 25 : 0));
        events.add(new GameEvent("gun", actor.x, actor.y, actor.kind + " fires."));
        if (CombatRules.roll(s, 100) < chance) CombatRules.damagePlayer(s, damage, events, actor.kind.toString());
        else events.add(new GameEvent("impact", s.playerX, s.playerY, "Dodged " + actor.kind + " fire."));
    }

    private static void camera(GameState s, GameState.Actor camera, List<GameEvent> events) {
        boolean sees = CombatRules.distance(camera.x, camera.y, s.playerX, s.playerY) <= 9
                && CombatRules.clearLine(s, camera.x, camera.y, s.playerX, s.playerY, false);
        if (!sees) { camera.alert = Math.max(0, camera.alert - 1); return; }
        if (camera.alert == 0) events.add(new GameEvent("warning", camera.x, camera.y, "Security camera detected movement. Nearby robots alerted."));
        camera.alert = 8;
        for (var actor : s.actors) if (actor != camera && CombatRules.alive(actor) && CombatRules.hostile(actor)
                && CombatRules.distance(camera.x, camera.y, actor.x, actor.y) <= 10) {
            actor.alert = 8; actor.lastX = s.playerX; actor.lastY = s.playerY;
        }
    }

    private static void constructor(GameState s, GameState.Actor machine, List<GameEvent> events) {
        if (s.flags.contains("constructor-defeated")) return;
        boolean sees = CombatRules.distance(machine.x, machine.y, s.playerX, s.playerY) <= 12
                && CombatRules.clearLine(s, machine.x, machine.y, s.playerX, s.playerY, false);
        if (sees && s.bossPhase.equals("dormant")) {
            s.bossPhase = "active";
            events.add(new GameEvent("rover", machine.x, machine.y, "Constructor online. Explosive R.O.V.E.R. deployment initiated. Redirect units or locate the operator."));
        }
        if (!s.bossPhase.equals("active") || s.tick % 6 != 0
                || s.actors.stream().filter(a -> CombatRules.alive(a) && a.id.startsWith("constructor-wave-")).count() >= 4) return;
        for (int[] direction : GridRoutes.DIRECTIONS) {
            int x = machine.x + direction[0], y = machine.y + direction[1];
            if (!canEnter(s, null, x, y)) continue;
            if (s.actors.size() >= 400) s.actors.removeIf(a -> a.dead && a.id.startsWith("constructor-wave-"));
            var rover = actor(GameState.ActorKind.ROVER, "constructor-wave-" + s.tick, x, y);
            rover.fuse = 18; rover.alert = 8; rover.lastX = s.playerX; rover.lastY = s.playerY;
            s.actors.add(rover);
            events.add(new GameEvent("rover", x, y, "Explosive R.O.V.E.R. deployed. Countdown: 18 ticks."));
            return;
        }
    }

    private static void rover(GameState s, GameState.Actor rover, List<GameEvent> events) {
        GameState.Actor controller = rover.redirected ? s.actors.stream().filter(a -> CombatRules.alive(a)
                && a.kind == GameState.ActorKind.CONSTRUCTOR).findFirst().orElse(null) : null;
        int targetX = controller == null ? s.playerX : controller.x;
        int targetY = controller == null ? s.playerY : controller.y;
        int distance = CombatRules.distance(rover.x, rover.y, targetX, targetY);
        if (rover.redirected && controller == null) { rover.dead = true; rover.hp = 0; return; }
        if (rover.fuse >= 0) {
            rover.fuse--;
            if (rover.fuse == 0) { CombatRules.explode(s, rover, events); return; }
            if (distance <= 1) {
                if (rover.fuse > 2) {
                    rover.fuse = 2;
                    events.add(new GameEvent("rover", rover.x, rover.y, "R.O.V.E.R. armed: two ticks until detonation."));
                }
                return;
            }
        } else if (rover.alert > 0 && distance <= 1) {
            rover.fuse = 2;
            events.add(new GameEvent("rover", rover.x, rover.y, "R.O.V.E.R. armed: two ticks until detonation."));
            return;
        }
        if (rover.redirected || rover.alert > 0) pursue(s, rover, rover.redirected ? targetX : rover.lastX,
                rover.redirected ? targetY : rover.lastY, events);
    }

    static GameState.Actor actor(GameState.ActorKind kind, String id, int x, int y) {
        var actor = new GameState.Actor();
        actor.kind = kind; actor.id = id; actor.x = x; actor.y = y; actor.lastX = x; actor.lastY = y;
        actor.maxHp = switch (kind) { case ADD -> 28; case ROVER, CAMERA -> 8; case TURRET -> 18; case OPERATOR -> 24; case CONSTRUCTOR -> 52; default -> 14; };
        actor.hp = actor.maxHp;
        return actor;
    }

    private static void pursue(GameState s, GameState.Actor actor, int x, int y, List<GameEvent> events) {
        if (actor.x == x && actor.y == y) return;
        // Small bounded per-actor routes, using the same actual station grid as player movement.
        for (Coord cell : GridRoutes.route(s, actor.x, actor.y, x, y, false)) {
            if (s.tiles[cell.x][cell.y] == Tile.DOOR_CLOSED) {
                if (actor.kind == GameState.ActorKind.ROVER) return;
                s.tiles[cell.x][cell.y] = Tile.DOOR_OPEN;
                events.add(new GameEvent("door", cell.x, cell.y, actor.kind + " opens a bulkhead."));
                return;
            }
            if (canEnter(s, actor, cell.x, cell.y)) move(s, actor, cell.x, cell.y, events);
            return;
        }
    }

    private static void retreat(GameState s, GameState.Actor actor, List<GameEvent> events) {
        int best = CombatRules.distance(actor.x, actor.y, s.playerX, s.playerY);
        int nx = actor.x, ny = actor.y;
        for (int[] direction : GridRoutes.DIRECTIONS) {
            int x = actor.x + direction[0], y = actor.y + direction[1];
            int distance = CombatRules.distance(x, y, s.playerX, s.playerY);
            if (distance > best && canEnter(s, actor, x, y)) { best = distance; nx = x; ny = y; }
        }
        if (nx != actor.x || ny != actor.y) move(s, actor, nx, ny, events);
    }

    private static void patrol(GameState s, GameState.Actor actor, List<GameEvent> events) {
        int[] direction = GridRoutes.DIRECTIONS[Math.floorMod(actor.facing, 4)];
        int x = actor.x + direction[0], y = actor.y + direction[1];
        if (canEnter(s, actor, x, y)) move(s, actor, x, y, events);
        else actor.facing = (actor.facing + 1) % 4;
    }

    private static boolean canEnter(GameState s, GameState.Actor self, int x, int y) {
        return GridRoutes.inside(s, x, y) && !s.tiles[x][y].blocksMovement
                && !(s.playerX == x && s.playerY == y)
                && s.actors.stream().noneMatch(a -> a != self && CombatRules.alive(a) && CombatRules.hostile(a) && a.x == x && a.y == y);
    }

    private static void move(GameState s, GameState.Actor actor, int x, int y, List<GameEvent> events) {
        actor.facing = x > actor.x ? 1 : y > actor.y ? 2 : x < actor.x ? 3 : 0;
        actor.x = x; actor.y = y;
        events.add(new GameEvent(actor.kind == GameState.ActorKind.ROVER ? "rover" : "robot_step", x, y, actor.kind + " moves."));
    }
}
