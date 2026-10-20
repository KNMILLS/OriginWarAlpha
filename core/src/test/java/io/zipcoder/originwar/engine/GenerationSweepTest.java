package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static io.zipcoder.originwar.model.PlayerAction.Type.*;
import static org.junit.jupiter.api.Assertions.*;

class GenerationSweepTest {
    @Test void twoHundredSeedsTraverseAllTenDecksWithFiniteProductionSupplies() {
        int decks = 0;
        for (long seed = 0; seed < 200; seed++) {
            RunEngine run = new RunEngine(seed);
            for (int deck = 1; deck <= RunEngine.LAST_DECK; deck++) {
                GameState floor = run.saveState();
                String context = "seed=" + seed + ", deck=" + deck;
                assertEquals(deck, floor.deck, context);
                assertEquals(Tile.FLOOR, floor.tiles[floor.playerX][floor.playerY], context);
                assertFalse(floor.dead, context);
                var connected = GridRoutes.distances(floor, floor.playerX, floor.playerY, false);
                var itemIds = new HashSet<String>();
                for (int x = 0; x < floor.width; x++) for (int y = 0; y < floor.height; y++) {
                    assertNotNull(floor.tiles[x][y], context);
                    if (GridRoutes.passable(floor.tiles[x][y]))
                        assertTrue(connected.gradientMap[x][y] < com.github.yellowstonegames.path.DijkstraMap.FLOOR, context + " disconnected " + x + "," + y);
                }
                for (var item : floor.floorItems) {
                    assertTrue(itemIds.add(item.id), context);
                    assertTrue(GridRoutes.passable(floor.tiles[item.x][item.y]), context);
                    assertTrue(item.power > 0, context);
                }
                var firstLeg = GridRoutes.route(floor, floor.playerX, floor.playerY, floor.switchX, floor.switchY, false);
                var secondLeg = GridRoutes.route(floor, floor.switchX, floor.switchY, floor.hatchX, floor.hatchY, false);
                assertFalse(firstLeg.isEmpty(), context); assertFalse(secondLeg.isEmpty(), context);
                Driver driver = new Driver(run, floor, context);
                driver.collect(floor.playerX, floor.playerY);
                driver.walk(firstLeg);
                driver.act(PlayerAction.of(INTERACT));
                assertTrue(run.saveState().switchOn, context);
                driver.walk(secondLeg);
                GameState arrival = run.saveState();
                assertFalse(arrival.dead, context); assertEquals(100, arrival.hp, context);
                assertEquals(driver.reserve, arrival.reserve, context);
                assertEquals(floor.combatRng, arrival.combatRng, context);
                assertEquals(floor.medicineRng, arrival.medicineRng, context);
                assertEquals(floor.perceptionRng, arrival.perceptionRng, context);
                if (deck < RunEngine.LAST_DECK) driver.act(PlayerAction.of(ADVANCE));
                else {
                    assertEquals(0, run.submit(PlayerAction.of(ADVANCE)).cost(), context);
                    assertFalse(run.saveState().victory, context);
                }
                decks++;
            }
        }
        assertEquals(2000, decks);
    }

    /** Drives actual production actions along the production route; no alternate generator/simulation. */
    private static final class Driver {
        final RunEngine run;
        final GameState floor;
        final String context;
        final HashSet<String> collected = new HashSet<>();
        final HashSet<Coord> opened = new HashSet<>();
        int x, y, reserve, inventoryCount;

        Driver(RunEngine run, GameState floor, String context) {
            this.run = run; this.floor = floor; this.context = context;
            x = floor.playerX; y = floor.playerY; reserve = floor.reserve; inventoryCount = floor.inventory.size();
        }

        void walk(List<Coord> route) {
            for (Coord destination : route) {
                int dx = destination.x - x, dy = destination.y - y;
                assertEquals(1, Math.abs(dx) + Math.abs(dy), context);
                if (floor.tiles[destination.x][destination.y] == Tile.DOOR_CLOSED && opened.add(destination))
                    act(PlayerAction.move(dx, dy));
                act(PlayerAction.move(dx, dy));
                x = destination.x; y = destination.y;
                collect(x, y);
            }
        }

        void collect(int atX, int atY) {
            for (var item : floor.floorItems) if (item.x == atX && item.y == atY
                    && inventoryCount < RunEngine.MAX_INVENTORY && collected.add(item.id)) {
                act(PlayerAction.item(PICKUP, item.id));
                inventoryCount++;
            }
        }

        void act(PlayerAction action) {
            if (reserve <= RunEngine.LOW_RESERVE) {
                GameState current = run.saveState();
                var tank = current.inventory.stream().filter(i -> i.type == GameState.ItemType.OXYGEN && i.power > 0).findFirst().orElse(null);
                assertNotNull(tank, context + " no finite supply before oxygen warning");
                assertTrue(run.submit(PlayerAction.item(USE, tank.id)).accepted(), context);
                GameState refilled = run.saveState();
                reserve = refilled.reserve;
                inventoryCount = refilled.inventory.size();
            }
            TurnResult result = run.submit(action);
            assertTrue(result.accepted(), context + " rejected " + action + " at " + x + "," + y);
            reserve -= result.cost();
            assertTrue(reserve >= 0, context + " required route exhausted reserve");
        }
    }
}
