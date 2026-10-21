package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;
import io.zipcoder.originwar.campaign.Campaign;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class GenerationSweepTest {
    /** Structural/resource evidence only. Combat playthroughs are separate tests, with AI fully enabled. */
    @Test void oneThousandSeedsKeepAllTenDecksConnectedSuppliedAndSafelyPopulated() {
        int decks = 0;
        for (long seed = 0; seed < 1000; seed++) {
            GameState floor = new RunEngine(seed).saveState();
            for (int deck = 1; deck <= RunEngine.LAST_DECK; deck++) {
                if (deck > 1) { floor.deck = deck; StationGenerator.generate(floor); }
                Campaign.enterDeck(floor,new ArrayList<>());
                String context = "seed=" + seed + ", deck=" + deck;
                assertEquals(Tile.FLOOR, floor.tiles[floor.playerX][floor.playerY], context);
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
                    if (item.type == GameState.ItemType.OXYGEN || item.type == GameState.ItemType.CELL) assertTrue(item.power > 0, context);
                }
                var firstLeg = GridRoutes.route(floor, floor.playerX, floor.playerY, floor.switchX, floor.switchY, false);
                var secondLeg = GridRoutes.route(floor, floor.switchX, floor.switchY, floor.hatchX, floor.hatchY, false);
                assertFalse(firstLeg.isEmpty(), context); assertFalse(secondLeg.isEmpty(), context);
                var required = new ArrayList<Coord>(); required.add(Coord.get(floor.playerX, floor.playerY));
                required.addAll(firstLeg); required.addAll(secondLeg);
                var positions = new HashSet<>(required);
                int routeCost = required.stream().mapToInt(c -> GridRoutes.entryCost(floor.tiles[c.x][c.y])).sum();
                int supply = floor.floorItems.stream().filter(i -> i.type == GameState.ItemType.OXYGEN && positions.contains(Coord.get(i.x, i.y)))
                        .mapToInt(i -> i.power).sum();
                // This budgets pickups, refills and interactions; it is not a combat win claim.
                assertTrue(floor.maxReserve + supply >= routeCost + floor.floorItems.size() * 2 + 4, context + " oxygen budget");
                assertFalse(floor.actors.isEmpty(), context);
                for (var actor : floor.actors) {
                    assertFalse(floor.tiles[actor.x][actor.y].blocksMovement, context);
                    if(CombatRules.hostile(actor))assertTrue(CombatRules.distance(actor.x, actor.y, floor.playerX, floor.playerY) >= 10, context + " unsafe spawn " + actor.id);
                }
                if (deck == 8) {
                    var operator = floor.actors.stream().filter(a -> a.kind == GameState.ActorKind.OPERATOR).findFirst().orElseThrow();
                    assertFalse(GridRoutes.route(floor, floor.playerX, floor.playerY, operator.x, operator.y, false).isEmpty(), context + " low-INT operator route");
                    assertEquals("dormant", floor.bossPhase);
                }
                RunEngine.validateState(floor);
                decks++;
            }
        }
        assertEquals(10000, decks);
    }
}
