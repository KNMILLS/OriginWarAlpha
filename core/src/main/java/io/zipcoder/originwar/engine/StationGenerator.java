package io.zipcoder.originwar.engine;

import com.github.tommyettinger.random.WhiskerRandom;
import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.path.DijkstraMap;
import com.github.yellowstonegames.place.ConnectingMapGenerator;
import io.zipcoder.originwar.model.DeterministicRandom;
import io.zipcoder.originwar.model.GameState;
import io.zipcoder.originwar.model.Tile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Seeded rectangular compartments and connecting corridors, never natural caves. */
final class StationGenerator {
    private StationGenerator() {}

    static void generate(GameState s) {
        var rng = new WhiskerRandom(DeterministicRandom.mix(s.seed ^ (s.deck * 0xD1B54A32D192ED03L)));
        int zone = (s.deck - 1) / 2;
        int roomWidth = 5 + rng.nextInt(4), roomHeight = 4 + rng.nextInt(3);
        char[][] map = new ConnectingMapGenerator(s.width, s.height, roomWidth, roomHeight, rng, 1, 0.18).generate();
        s.tiles = new Tile[s.width][s.height];
        s.visible = new boolean[s.width][s.height];
        s.explored = new boolean[s.width][s.height];
        s.light = new float[s.width][s.height];
        s.floorItems.clear();
        s.actors.clear();
        s.switchOn = false;
        var floors = new ArrayList<Coord>();
        for (int x = 0; x < s.width; x++) for (int y = 0; y < s.height; y++) {
            s.tiles[x][y] = map[x][y] == '#' ? Tile.WALL : Tile.FLOOR;
            if (s.tiles[x][y] == Tile.FLOOR) floors.add(Coord.get(x, y));
        }
        if (floors.isEmpty()) throw new IllegalStateException("Station generator produced no compartments");
        Coord start = floors.get(rng.nextInt(floors.size()));
        s.playerX = start.x; s.playerY = start.y;
        Coord activation = farthest(s, start.x, start.y, null);
        s.switchX = activation.x; s.switchY = activation.y;
        Coord hatch = farthest(s, activation.x, activation.y, start);
        s.hatchX = hatch.x; s.hatchY = hatch.y;
        Coord terminal = floors.stream().filter(c -> !c.equals(start) && !c.equals(activation) && !c.equals(hatch))
                .min((a, b) -> Integer.compare(Math.abs(a.x - start.x) + Math.abs(a.y - start.y),
                        Math.abs(b.x - start.x) + Math.abs(b.y - start.y))).orElseThrow();
        s.terminalX = terminal.x; s.terminalY = terminal.y;
        s.tiles[activation.x][activation.y] = Tile.SWITCH;
        s.tiles[hatch.x][hatch.y] = Tile.HATCH;
        s.tiles[terminal.x][terminal.y] = Tile.TERMINAL;

        for (Coord cell : floors) {
            int x = cell.x, y = cell.y;
            if (s.tiles[x][y] != Tile.FLOOR || Math.abs(x - start.x) + Math.abs(y - start.y) < 3) continue;
            boolean narrow = (s.tiles[x - 1][y] == Tile.WALL && s.tiles[x + 1][y] == Tile.WALL)
                    || (s.tiles[x][y - 1] == Tile.WALL && s.tiles[x][y + 1] == Tile.WALL);
            if (narrow && rng.nextInt(12) == 0) s.tiles[x][y] = Tile.DOOR_CLOSED;
            else {
                int roll = rng.nextInt(100);
                if (roll < 2 + zone) s.tiles[x][y] = Tile.LOW_GRAV;
                else if (roll < 3 + zone) s.tiles[x][y] = Tile.NO_GRAV;
                else if (roll < 5 + zone) s.tiles[x][y] = Tile.HAZARD;
            }
        }
        RunContent.geometry(s, rng);
        s.tiles[start.x][start.y] = Tile.FLOOR;
        // Geometry may install cover or a locked shortcut; optional supplies need actual open cells.
        floors.removeIf(cell -> s.tiles[cell.x][cell.y].blocksMovement);

        // Supply the genuine required route, including door-opening and terrain time, not a second test map.
        var required = new ArrayList<Coord>();
        required.add(start);
        required.addAll(GridRoutes.route(s, start.x, start.y, activation.x, activation.y, false));
        required.addAll(GridRoutes.route(s, activation.x, activation.y, hatch.x, hatch.y, false));
        var supplied = new HashSet<Coord>();
        addTank(s, start, s.deck == 1 ? 360 : 180, supplied);
        int costSinceSupply = 0;
        for (Coord cell : required) {
            costSinceSupply += GridRoutes.entryCost(s.tiles[cell.x][cell.y]);
            if (costSinceSupply >= 80 && supplied.add(cell)) {
                s.floorItems.add(tank(s.deck, cell.x, cell.y, 180, "route-" + s.floorItems.size()));
                costSinceSupply = 0;
            }
        }
        // Optional supply rooms are positioned with the loot stream, independent of geometry.
        for (int i = 0; i < 2; i++) {
            s.lootRng = DeterministicRandom.nextState(s.lootRng);
            addTank(s, floors.get(DeterministicRandom.bounded(s.lootRng, floors.size())), 120, supplied);
        }
        RunContent.populate(s);
        if (!s.flags.contains("entered-deck-" + s.deck)) {
            s.flags.add("entered-deck-" + s.deck);
            s.journal.add("Deck " + s.deck + ": locate the security switch, then reach the exit hatch.");
        }
        Visibility.refresh(s);
    }

    private static Coord farthest(GameState s, int x, int y, Coord excluded) {
        DijkstraMap distances = GridRoutes.distances(s, x, y, false);
        Coord best = null;
        float longest = -1f;
        for (int nx = 1; nx < s.width - 1; nx++) for (int ny = 1; ny < s.height - 1; ny++) {
            float value = distances.gradientMap[nx][ny];
            if (GridRoutes.passable(s.tiles[nx][ny]) && value < DijkstraMap.FLOOR && value > longest
                    && (excluded == null || excluded.x != nx || excluded.y != ny)) {
                longest = value; best = Coord.get(nx, ny);
            }
        }
        if (best == null) throw new IllegalStateException("Station has no connected objective room");
        return best;
    }

    private static void addTank(GameState s, Coord location, int power, HashSet<Coord> supplied) {
        if (supplied.add(location)) s.floorItems.add(tank(s.deck, location.x, location.y, power, "supply-" + s.floorItems.size()));
    }

    static GameState.Item tank(int deck, int x, int y, int power, String suffix) {
        var tank = new GameState.Item();
        tank.id = "oxygen-d" + deck + "-" + suffix;
        tank.name = "Oxygen tank (" + power + ")";
        tank.type = GameState.ItemType.OXYGEN;
        tank.x = x; tank.y = y; tank.power = power;
        return tank;
    }
}
