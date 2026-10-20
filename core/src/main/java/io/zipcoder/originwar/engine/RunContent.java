package io.zipcoder.originwar.engine;

import com.github.tommyettinger.random.WhiskerRandom;
import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Small production encounter and equipment definitions, separate from campaign prose. */
final class RunContent {
    static final String CONSTRUCTOR_ID = "constructor-machine", OPERATOR_ID = "constructor-operator";
    private RunContent() {}

    static void geometry(GameState s, WhiskerRandom random) {
        if (s.deck == 8) {
            Coord center = floorCells(s).stream().max(Comparator.comparingInt(c -> CombatRules.distance(c.x, c.y, s.playerX, s.playerY))).orElseThrow();
            int width = Math.min(13, s.width - 2), height = Math.min(9, s.height - 2);
            int left = Math.max(1, Math.min(s.width - width - 1, center.x - width / 2));
            int bottom = Math.max(1, Math.min(s.height - height - 1, center.y - height / 2));
            for (int x = left; x < left + width; x++) for (int y = bottom; y < bottom + height; y++) {
                if (!special(s.tiles[x][y])) s.tiles[x][y] = Tile.FLOOR;
            }
            s.flags.removeIf(f -> f.startsWith("constructor-room:"));
            s.flags.add("constructor-room:" + left + ":" + bottom + ":" + width + ":" + height);
        }
        // Openable shortcuts replace walls, so low-stat mandatory routes are never removed.
        if (s.deck >= 2) {
            int shortcuts = 0;
            for (int attempt = 0; attempt < 250 && shortcuts < 2; attempt++) {
                int x = 2 + random.nextInt(s.width - 4), y = 2 + random.nextInt(s.height - 4);
                if (s.tiles[x][y] != Tile.WALL) continue;
                boolean horizontal = !s.tiles[x - 1][y].blocksMovement && !s.tiles[x + 1][y].blocksMovement;
                boolean vertical = !s.tiles[x][y - 1].blocksMovement && !s.tiles[x][y + 1].blocksMovement;
                if (horizontal == vertical) continue;
                int ax = horizontal ? x - 1 : x, ay = horizontal ? y : y - 1;
                int bx = horizontal ? x + 1 : x, by = horizontal ? y : y + 1;
                if (GridRoutes.route(s, ax, ay, bx, by, false).size() < 8) continue;
                s.tiles[x][y] = Tile.LOCKED_DOOR; shortcuts++;
            }
        }
        // Cover sits inside open rooms, leaving all four routes around it connected.
        for (int attempt = 0, cover = 0; attempt < 150 && cover < 4; attempt++) {
            int x = 2 + random.nextInt(s.width - 4), y = 2 + random.nextInt(s.height - 4);
            if (s.tiles[x][y] != Tile.FLOOR || CombatRules.distance(x, y, s.playerX, s.playerY) < 4) continue;
            boolean open = true;
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++)
                if (s.tiles[x + dx][y + dy].blocksMovement) open = false;
            if (open) { s.tiles[x][y] = Tile.COVER; cover++; }
        }
    }

    static void populate(GameState s) {
        MedicineRules.initialize(s);
        List<Coord> floors = floorCells(s);
        add(s, GameState.ItemType.CELL, "entry-cells", "Battery pack (+12 cells)", s.playerX, s.playerY, 12);
        add(s, GameState.ItemType.MEDICINE, "entry-medical", medicineFor(s, "heal"), s.playerX, s.playerY, 1);
        if (s.deck == 1) add(s, GameState.ItemType.ARMOR_LIGHT, "light-armor", "Light armor (+3 protection, +1 effective DEX)", s.playerX, s.playerY, 3);
        if (s.deck == 2) add(s, GameState.ItemType.RIFLE, "rifle", "Rifle (range 11, 2 cells)", s.playerX, s.playerY, 15);
        if (s.deck == 4) add(s, GameState.ItemType.SCATTERGUN, "scattergun", "Scattergun (range 4, 3 cells)", s.playerX, s.playerY, 20);
        if (s.deck == 5) add(s, GameState.ItemType.ARMOR_HEAVY, "heavy-armor", "Heavy armor (+5 protection, -2 effective DEX)", s.playerX, s.playerY, 5);
        List<String> medicines = s.medicineEffects.keySet().stream().sorted().toList();
        for (int i = 0; i < 3; i++) {
            Coord cell = lootCell(s, floors);
            s.lootRng = DeterministicRandom.nextState(s.lootRng);
            add(s, GameState.ItemType.MEDICINE, "medicine-" + i,
                    medicines.get(DeterministicRandom.bounded(s.lootRng, medicines.size())), cell.x, cell.y, 1);
        }
        Coord ammunition = lootCell(s, floors);
        add(s, GameState.ItemType.CELL, "spare-cells", "Battery pack (+18 cells)", ammunition.x, ammunition.y, 18);

        if (s.deck == 8) constructor(s, floors);
        else if (!s.flags.contains("constructor-defeated")) s.bossPhase = "inactive";
        int mobile = 2 + s.deck / 3;
        for (int i = 0; i < mobile; i++) {
            GameState.ActorKind kind = s.deck >= 4 && i == mobile - 1 ? GameState.ActorKind.ROVER
                    : s.deck >= 3 && i == mobile - 2 ? GameState.ActorKind.ADD : GameState.ActorKind.SIRS;
            spawn(s, floors, kind, "security-" + i);
        }
        if (s.deck >= 2) spawn(s, floors, GameState.ActorKind.CAMERA, "camera");
        if (s.deck >= 5) spawn(s, floors, GameState.ActorKind.TURRET, "turret");
    }

    private static void constructor(GameState s, List<Coord> floors) {
        String room = s.flags.stream().filter(f -> f.startsWith("constructor-room:")).findFirst().orElseThrow();
        String[] values = room.split(":");
        int left = Integer.parseInt(values[1]), bottom = Integer.parseInt(values[2]);
        int width = Integer.parseInt(values[3]), height = Integer.parseInt(values[4]);
        Coord machine = nearestFree(s, left + width - 3, bottom + height / 2, floors);
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.CONSTRUCTOR, CONSTRUCTOR_ID, machine.x, machine.y));
        Coord operator = nearestFree(s, left + 2, bottom + height / 2, floors);
        s.actors.add(EnemyTurns.actor(GameState.ActorKind.OPERATOR, OPERATOR_ID, operator.x, operator.y));
        s.bossPhase = "dormant";
        s.journal.add("Automated-defense deck: the Constructor deploys explosive R.O.V.E.R.s. INT 6 can redirect visible units within five tiles. The operator is physically accessible without a stat gate.");
    }

    private static void spawn(GameState s, List<Coord> floors, GameState.ActorKind kind, String suffix) {
        List<Coord> candidates = floors.stream().filter(c -> CombatRules.distance(c.x, c.y, s.playerX, s.playerY) >= 12
                && !special(s.tiles[c.x][c.y]) && s.actors.stream().noneMatch(a -> a.x == c.x && a.y == c.y)).toList();
        if (candidates.isEmpty()) return; // Tiny test maps have no safe encounter room; production maps are 64 by 40.
        s.lootRng = DeterministicRandom.nextState(s.lootRng);
        Coord cell = candidates.get(DeterministicRandom.bounded(s.lootRng, candidates.size()));
        s.actors.add(EnemyTurns.actor(kind, "d" + s.deck + "-" + suffix, cell.x, cell.y));
    }

    private static Coord nearestFree(GameState s, int x, int y, List<Coord> floors) {
        return floors.stream().filter(c -> !special(s.tiles[c.x][c.y])
                        && s.actors.stream().noneMatch(a -> a.x == c.x && a.y == c.y))
                .min(Comparator.comparingInt(c -> CombatRules.distance(c.x, c.y, x, y))).orElseThrow();
    }

    private static Coord lootCell(GameState s, List<Coord> floors) {
        s.lootRng = DeterministicRandom.nextState(s.lootRng);
        return floors.get(DeterministicRandom.bounded(s.lootRng, floors.size()));
    }

    static String medicineFor(GameState s, String effect) {
        return s.medicineEffects.entrySet().stream().filter(e -> e.getValue().equals(effect))
                .map(java.util.Map.Entry::getKey).findFirst().orElseThrow();
    }

    static GameState.Item add(GameState s, GameState.ItemType type, String suffix, String name, int x, int y, int power) {
        var item = new GameState.Item();
        item.id = "d" + s.deck + "-" + suffix; item.name = name; item.type = type;
        item.x = x; item.y = y; item.power = power;
        s.floorItems.add(item);
        return item;
    }

    private static boolean special(Tile tile) { return tile == Tile.SWITCH || tile == Tile.HATCH || tile == Tile.TERMINAL; }
    private static List<Coord> floorCells(GameState s) {
        var result = new ArrayList<Coord>();
        for (int x = 1; x < s.width - 1; x++) for (int y = 1; y < s.height - 1; y++)
            if (!s.tiles[x][y].blocksMovement) result.add(Coord.get(x, y));
        return result;
    }
}
