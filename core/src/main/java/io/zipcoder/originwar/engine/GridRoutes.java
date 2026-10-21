package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import com.github.yellowstonegames.grid.Measurement;
import com.github.yellowstonegames.path.DijkstraMap;
import io.zipcoder.originwar.model.GameState;
import io.zipcoder.originwar.model.Tile;

import java.util.ArrayList;
import java.util.List;

/** Cardinal routes use SquidSquad gradients, with deterministic tie breaking and no game RNG. */
final class GridRoutes {
    static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    private GridRoutes() {}

    static boolean inside(GameState s, int x, int y) {
        return x >= 0 && y >= 0 && x < s.width && y < s.height;
    }

    static boolean passable(Tile tile) {
        return !tile.blocksMovement || tile == Tile.DOOR_CLOSED;
    }

    static int entryCost(Tile tile) {
        return tile == Tile.DOOR_CLOSED ? 2 : tile.cost;
    }

    static DijkstraMap distances(GameState s, int goalX, int goalY, boolean knownOnly) {
        char[][] map = new char[s.width][s.height];
        float[][] costs = new float[s.width][s.height];
        for (int x = 0; x < s.width; x++) for (int y = 0; y < s.height; y++) {
            Tile known=knownOnly?StationSystems.knownTile(s,x,y):s.tiles[x][y];
            map[x][y] = known!=null&&passable(known) && (!knownOnly || s.explored[x][y]) ? '.' : '#';
            costs[x][y] = known==null?1:entryCost(known);
        }
        DijkstraMap distances = new DijkstraMap(map, Measurement.MANHATTAN);
        distances.initializeCost(costs);
        distances.setGoal(goalX, goalY);
        distances.scan();
        return distances;
    }

    /** Returns destination cells, excluding the start. An empty list also denotes no route. */
    static List<Coord> route(GameState s, int fromX, int fromY, int toX, int toY, boolean knownOnly) {
        if (!inside(s, fromX, fromY) || !inside(s, toX, toY)
                || (knownOnly ? !s.explored[toX][toY]||StationSystems.knownTile(s,toX,toY)==null||!passable(StationSystems.knownTile(s,toX,toY)) : !passable(s.tiles[toX][toY]))) return List.of();
        DijkstraMap distances = distances(s, toX, toY, knownOnly);
        if (distances.gradientMap[fromX][fromY] >= DijkstraMap.FLOOR) return List.of();
        ArrayList<Coord> result = new ArrayList<>();
        int x = fromX, y = fromY;
        while (x != toX || y != toY) {
            int nextX = -1, nextY = -1;
            float best = Float.POSITIVE_INFINITY;
            for (int[] direction : DIRECTIONS) {
                int nx = x + direction[0], ny = y + direction[1];
                if (!inside(s, nx, ny) || (knownOnly?StationSystems.knownTile(s,nx,ny)==null||!passable(StationSystems.knownTile(s,nx,ny)):!passable(s.tiles[nx][ny]))
                        || (knownOnly && !s.explored[nx][ny])) continue;
                float gradient = distances.gradientMap[nx][ny];
                if (gradient >= distances.gradientMap[x][y]) continue;
                float score = gradient + entryCost(knownOnly?StationSystems.knownTile(s,nx,ny):s.tiles[nx][ny]);
                if (score < best) { best = score; nextX = nx; nextY = ny; }
            }
            if (nextX < 0 || result.size() >= s.width * s.height) return List.of();
            result.add(Coord.get(nextX, nextY));
            x = nextX; y = nextY;
        }
        return List.copyOf(result);
    }
}
