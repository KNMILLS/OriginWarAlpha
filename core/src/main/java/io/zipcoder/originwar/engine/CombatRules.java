package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.BresenhamLine;
import io.zipcoder.originwar.model.*;

import java.util.List;

/** Real attacks and damage share these rules with the enemy scheduler. */
final class CombatRules {
    private CombatRules() {}
    static boolean hostile(GameState.Actor actor) {
        return switch (actor.kind) { case SIRS, ADD, ROVER, TURRET, CAMERA, OPERATOR, CONSTRUCTOR -> true; default -> false; };
    }
    static boolean alive(GameState.Actor actor) { return !actor.dead && actor.hp > 0; }
    static GameState.Actor at(GameState s, int x, int y) {
        return s.actors.stream().filter(a -> alive(a) && hostile(a) && a.x == x && a.y == y).findFirst().orElse(null);
    }
    static int distance(int ax, int ay, int bx, int by) { return Math.abs(ax - bx) + Math.abs(ay - by); }
    static int roll(GameState s, int bound) {
        s.combatRng = DeterministicRandom.nextState(s.combatRng);
        return DeterministicRandom.bounded(s.combatRng, bound);
    }
    static int clampChance(int chance) { return Math.max(15, Math.min(95, chance)); }
    static int facing(int ax, int ay, int bx, int by) {
        int dx = bx - ax, dy = by - ay;
        return Math.abs(dx) >= Math.abs(dy) ? (dx >= 0 ? 1 : 3) : (dy >= 0 ? 2 : 0);
    }

    static boolean clearLine(GameState s, int ax, int ay, int bx, int by, boolean projectile) {
        if (!GridRoutes.inside(s, ax, ay) || !GridRoutes.inside(s, bx, by)) return false;
        var line = BresenhamLine.lineArray(ax, ay, bx, by);
        for (int i = 1; i < line.length; i++) {
            var cell = line[i];
            Tile tile = s.tiles[cell.x][cell.y];
            if (i < line.length - 1 && (tile.blocksVision || (projectile && tile == Tile.GLASS))) return false;
        }
        // A target behind glass is visible but cannot be shot through it.
        return !projectile || s.tiles[bx][by] != Tile.GLASS;
    }

    static boolean cover(GameState s, int ax, int ay, int bx, int by) {
        var line = BresenhamLine.lineArray(ax, ay, bx, by);
        for (int i = 1; i < line.length - 1; i++) if (s.tiles[line[i].x][line[i].y] == Tile.COVER) return true;
        return false;
    }

    static int shoot(GameState s, int x, int y, List<GameEvent> events) {
        if (!GridRoutes.inside(s, x, y) || (x == s.playerX && y == s.playerY) || !s.visible[x][y]) return 0;
        var weapon = EquipmentRules.weapon(s.weapon);
        int distance = distance(s.playerX, s.playerY, x, y);
        if (distance > weapon.range() || s.batteries < weapon.batteries()
                || !clearLine(s, s.playerX, s.playerY, x, y, true)) return 0;
        var target = at(s, x, y);
        // Never allow attacks on an actual companion or civilian.
        if (target==null&&s.actors.stream().anyMatch(a -> alive(a) && !hostile(a) && a.x == x && a.y == y)) return 0;
        s.batteries -= weapon.batteries();
        s.facing = facing(s.playerX, s.playerY, x, y);
        int random = roll(s, 100); // Empty legal shots still cost a turn, ammo and the same RNG draw.
        events.add(new GameEvent("gun", s.playerX, s.playerY, "Fired " + s.weapon + "."));
        if (target != null) {
            int dodge = target.kind == GameState.ActorKind.ROVER ? 18 : target.kind == GameState.ActorKind.ADD ? 8 : 0;
            int chance = weapon.accuracy() + 3 * EquipmentRules.effectiveDex(s) - distance * 3 - dodge
                    - (cover(s, s.playerX, s.playerY, x, y) ? 25 : 0);
            if (random < clampChance(chance)) {
                int protection = target.kind == GameState.ActorKind.ADD ? 3 : 0;
                damageActor(s, target, Math.max(1, weapon.damage() - protection), false, events);
            } else events.add(new GameEvent("impact", x, y, "Shot missed the " + target.kind + "."));
        } else events.add(new GameEvent("impact", x, y, "Shot hit empty space or station plating."));
        EnemyTurns.hear(s, s.playerX, s.playerY, 12);
        return 1;
    }

    static int melee(GameState s, int x, int y, List<GameEvent> events) {
        GameState.Actor target = null;
        if (x == 0 && y == 0) {
            for (int[] direction : GridRoutes.DIRECTIONS) {
                target = at(s, s.playerX + direction[0], s.playerY + direction[1]);
                if (target != null) break;
            }
        } else target = at(s, x, y);
        if (target == null || distance(s.playerX, s.playerY, target.x, target.y) != 1) return 0;
        s.facing = facing(s.playerX, s.playerY, target.x, target.y);
        int dodge = target.kind == GameState.ActorKind.ROVER ? 15 : 0;
        int chance = clampChance(78 + EquipmentRules.effectiveDex(s) * 2 - dodge);
        events.add(new GameEvent("melee", target.x, target.y, "Melee strike."));
        if (roll(s, 100) < chance) damageActor(s, target, 4 + s.str * 2, false, events);
        else events.add(new GameEvent("impact", target.x, target.y, "Melee strike dodged."));
        EnemyTurns.hear(s, s.playerX, s.playerY, 5);
        return 1;
    }

    static void damageActor(GameState s, GameState.Actor target, int damage, boolean redirectedExplosion, List<GameEvent> events) {
        if (!alive(target)) return;
        if (target.kind == GameState.ActorKind.CONSTRUCTOR && !redirectedExplosion) {
            events.add(new GameEvent("impact", target.x, target.y, "Constructor shielding absorbs the attack. Redirect its R.O.V.E.R.s or find its operator."));
            return;
        }
        target.hp = Math.max(0, target.hp - damage);
        target.alert = 8; target.lastX = s.playerX; target.lastY = s.playerY;
        events.add(new GameEvent("hit", target.x, target.y, target.kind + " takes " + damage + " damage."));
        if (target.hp > 0) return;
        target.dead = true;
        events.add(new GameEvent("impact", target.x, target.y, target.kind + " disabled."));
        awardXp(s, switch (target.kind) { case ADD -> 20; case ROVER -> 8; case TURRET -> 15; case CAMERA -> 5; case OPERATOR, CONSTRUCTOR -> 35; default -> 12; }, events);
        if (target.kind == GameState.ActorKind.OPERATOR || target.kind == GameState.ActorKind.CONSTRUCTOR)
            finishConstructor(s, redirectedExplosion ? "redirect" : "operator", events);
        else if (target.kind == GameState.ActorKind.ROVER && target.fuse >= 0) explode(s, target, events);
    }

    static void damagePlayer(GameState s, int damage, List<GameEvent> events, String source) {
        if (s.dead) return;
        int mitigated = Math.max(1, damage - EquipmentRules.protection(s.armor));
        s.hp = Math.max(0, s.hp - mitigated);
        events.add(new GameEvent("hit", s.playerX, s.playerY, source + ": " + mitigated + " damage."));
        if (s.hp == 0) {
            s.dead = true;
            events.add(new GameEvent("death", s.playerX, s.playerY, "Life signs lost. The expedition has ended."));
        }
    }

    static void explode(GameState s, GameState.Actor rover, List<GameEvent> events) {
        // Mark first so overlapping explosions cannot recurse back into this unit.
        rover.dead = true; rover.hp = 0;
        boolean redirected = rover.redirected;
        events.add(new GameEvent("explosion", rover.x, rover.y, "R.O.V.E.R. detonated. Blast radius: one tile."));
        if (distance(rover.x, rover.y, s.playerX, s.playerY) <= 1)
            damagePlayer(s, 16, events, "R.O.V.E.R. blast");
        for (var actor : List.copyOf(s.actors)) if (actor != rover && alive(actor) && hostile(actor)
                && distance(rover.x, rover.y, actor.x, actor.y) <= 1)
            damageActor(s, actor, redirected ? 26 : 16, redirected, events);
    }

    static void awardXp(GameState s, int amount, List<GameEvent> events) {
        s.xp += amount;
        while (s.level < 20 && s.xp >= s.level * 30) {
            s.xp -= s.level * 30;
            s.level++; s.attributePoints += 2;
            events.add(new GameEvent("level", s.playerX, s.playerY,
                    "Level " + s.level + ": two attribute points earned. Allocate STR, INT or DEX."));
        }
    }

    static void finishConstructor(GameState s, String route, List<GameEvent> events) {
        if (!s.flags.add("constructor-defeated")) return;
        s.constructorRoute = route; s.bossPhase = "defeated";
        for (var actor : s.actors) if (actor.kind == GameState.ActorKind.CONSTRUCTOR || actor.kind == GameState.ActorKind.OPERATOR
                || actor.id.startsWith("constructor-wave-")) { actor.dead = true; actor.hp = 0; }
        s.journal.add("The Constructor is disabled via " + route + ". The deck hatch can now be released.");
        events.add(new GameEvent("hack", s.playerX, s.playerY, "Constructor disabled via " + route + "."));
    }
}
