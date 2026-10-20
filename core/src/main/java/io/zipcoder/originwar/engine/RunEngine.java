package io.zipcoder.originwar.engine;

import com.github.yellowstonegames.grid.Coord;
import io.zipcoder.originwar.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** The sole gameplay writer. Each accepted action applies its effect, then its integer world ticks. */
public final class RunEngine {
    public static final int LAST_DECK = 10, LOW_RESERVE = 90, MAX_INVENTORY = 24;
    public static final int OXYGENATION_LOSS = 5, OXYGENATION_RECOVERY = 4, SUFFOCATION_DAMAGE = 4;
    private final GameState state;

    public RunEngine(long seed) {
        state = new GameState();
        state.seed = seed;
        state.combatRng = DeterministicRandom.mix(seed ^ 0x14A76CB1L);
        state.lootRng = DeterministicRandom.mix(seed ^ 0x3B719A62L);
        state.medicineRng = DeterministicRandom.mix(seed ^ 0x5CB41F83L);
        state.perceptionRng = DeterministicRandom.mix(seed ^ 0x7DA65294L);
        state.activeDialogue = "";
        state.inventory.add(StationGenerator.tank(0, -1, -1, 240, "starter"));
        state.log.add("Boarded the station. Find the security switch and exit hatch. Oxygen is finite.");
        StationGenerator.generate(state);
    }

    public RunEngine(GameState restored) {
        validateState(restored);
        state = restored.copy();
        Visibility.refresh(state);
    }

    public WorldSnapshot snapshot() { return new WorldSnapshot(state); }
    public GameState saveState() { return state.copy(); }

    /** Known terrain only. Closed doors emit two moves: open in place, then enter. */
    public List<PlayerAction> pathTo(int x, int y) {
        if (state.dead || state.victory) return List.of();
        var result = new ArrayList<PlayerAction>();
        int px = state.playerX, py = state.playerY;
        for (Coord destination : GridRoutes.route(state, px, py, x, y, true)) {
            PlayerAction move = PlayerAction.move(destination.x - px, destination.y - py);
            if (state.tiles[destination.x][destination.y] == Tile.DOOR_CLOSED) result.add(move);
            result.add(move);
            px = destination.x; py = destination.y;
        }
        return List.copyOf(result);
    }

    public TurnResult submit(PlayerAction action) {
        if (action == null || action.type() == null || state.dead || state.victory) return TurnResult.rejected();
        if (action.type() == PlayerAction.Type.CHOOSE) {
            if (state.activeDialogue.isEmpty()) return TurnResult.rejected();
            state.activeDialogue = "";
            return new TurnResult(true, 0, List.of(new GameEvent("dialogue", state.playerX, state.playerY, "closed")));
        }
        if (action.type() == PlayerAction.Type.SCAN)
            return new TurnResult(true, 0, List.of(new GameEvent("scan", state.playerX, state.playerY,
                    "Reserve " + state.reserve + "/" + state.maxReserve + "; oxygenation " + state.oxygenation + "; HP " + state.hp)));
        if (!state.activeDialogue.isEmpty()) return TurnResult.rejected();
        if (atFinalHatch(action)) {
            String objective = "Command deck: locate Carmack and A.L.I.C.E.; the escape systems remain locked.";
            if (state.flags.add("command-deck-objective")) {
                state.journal.add(objective);
                state.log.add(objective);
            }
            return new TurnResult(true, 0, List.of(new GameEvent("hatch", state.playerX, state.playerY, objective)));
        }
        var events = new ArrayList<GameEvent>();
        boolean transitioning = requestsHatch(action);
        int cost = switch (action.type()) {
            case MOVE -> move(action.x(), action.y(), events);
            case WAIT -> 1;
            case INTERACT -> interact(action.x(), action.y(), events);
            case ADVANCE -> prepareAdvance();
            case PICKUP -> pickup(action.id(), events);
            case USE -> use(action.id(), events);
            case DROP -> drop(action.id(), events);
            default -> 0;
        };
        if (cost == 0) return TurnResult.rejected();
        for (int i = 0; i < cost && !state.dead; i++) tick(events);
        // Depart only after surviving the transition tick. Death cannot generate/loot an unseen next deck.
        if (transitioning && !state.dead) {
            state.deck++;
            StationGenerator.generate(state);
            events.add(new GameEvent("hatch", state.playerX, state.playerY, "Entered deck " + state.deck + "."));
        }
        Visibility.refresh(state);
        for (GameEvent event : events) {
            if (!event.type().equals("step")) state.log.add(event.detail());
        }
        while (state.log.size() > 60) state.log.removeFirst();
        return new TurnResult(true, cost, events);
    }

    private boolean atFinalHatch(PlayerAction action) {
        if (state.deck != LAST_DECK || !state.switchOn || state.playerX != state.hatchX || state.playerY != state.hatchY) return false;
        return requestsHatch(action);
    }

    private boolean requestsHatch(PlayerAction action) {
        if (state.playerX != state.hatchX || state.playerY != state.hatchY) return false;
        return action.type() == PlayerAction.Type.ADVANCE || (action.type() == PlayerAction.Type.INTERACT
                && ((action.x() == 0 && action.y() == 0) || (action.x() == state.playerX && action.y() == state.playerY)));
    }

    private int move(int dx, int dy, List<GameEvent> events) {
        if (!((dx == 0 && (dy == -1 || dy == 1)) || (dy == 0 && (dx == -1 || dx == 1)))) return 0;
        int x = state.playerX + dx, y = state.playerY + dy;
        if (!GridRoutes.inside(state, x, y)) return 0;
        Tile tile = state.tiles[x][y];
        if (tile == Tile.DOOR_CLOSED) {
            state.tiles[x][y] = Tile.DOOR_OPEN;
            events.add(new GameEvent("door", x, y, "Opened the bulkhead."));
            return 1;
        }
        if (tile.blocksMovement || occupied(x, y)) return 0;
        state.playerX = x; state.playerY = y;
        state.facing = dx > 0 ? 1 : dy > 0 ? 2 : dx < 0 ? 3 : 0;
        events.add(new GameEvent("step", x, y, tile.name()));
        return tile.cost;
    }

    private boolean occupied(int x, int y) {
        return state.actors.stream().anyMatch(a -> !a.dead && a.hp > 0 && a.kind != GameState.ActorKind.ALICE && a.x == x && a.y == y);
    }

    private int interact(int x, int y, List<GameEvent> events) {
        if (x == 0 && y == 0) {
            if (interactable(state.playerX, state.playerY)) return interactAt(state.playerX, state.playerY, events);
            for (int[] direction : GridRoutes.DIRECTIONS) {
                int nx = state.playerX + direction[0], ny = state.playerY + direction[1];
                if (GridRoutes.inside(state, nx, ny) && interactable(nx, ny)) {
                    int cost = interactAt(nx, ny, events);
                    if (cost > 0) return cost;
                }
            }
            return 0;
        }
        if (!GridRoutes.inside(state, x, y) || Math.abs((long)x - state.playerX) + Math.abs((long)y - state.playerY) > 1) return 0;
        return interactAt(x, y, events);
    }

    private boolean interactable(int x, int y) {
        return switch (state.tiles[x][y]) {
            case SWITCH -> !state.switchOn;
            case HATCH, TERMINAL, DOOR_CLOSED -> true;
            default -> false;
        };
    }

    private int interactAt(int x, int y, List<GameEvent> events) {
        switch (state.tiles[x][y]) {
            case SWITCH -> {
                if (state.switchOn) return 0;
                state.switchOn = true;
                events.add(new GameEvent("switch", x, y, "Security override accepted. Exit hatch unlocked."));
                return 1;
            }
            case HATCH -> { return x == state.playerX && y == state.playerY ? prepareAdvance() : 0; }
            case DOOR_CLOSED -> {
                state.tiles[x][y] = Tile.DOOR_OPEN;
                events.add(new GameEvent("door", x, y, "Opened the bulkhead."));
                return 1;
            }
            case TERMINAL -> {
                String flag = "terminal-d" + state.deck;
                if (state.flags.contains(flag)) return 0;
                state.flags.add(flag);
                String text = "Deck " + state.deck + " station log: emergency supplies remain in the compartments. The security switch controls the exit hatch.";
                state.journal.add(text);
                events.add(new GameEvent("terminal", x, y, text));
                return 1;
            }
            default -> { return 0; }
        }
    }

    private int prepareAdvance() {
        if (!state.switchOn || state.playerX != state.hatchX || state.playerY != state.hatchY || state.deck >= LAST_DECK) return 0;
        return 1;
    }

    private int pickup(String id, List<GameEvent> events) {
        if (state.inventory.size() >= MAX_INVENTORY) return 0;
        var item = state.floorItems.stream().filter(i -> i.x == state.playerX && i.y == state.playerY
                && (id == null || id.isEmpty() || id.equals(i.id))).findFirst().orElse(null);
        if (item == null) return 0;
        state.floorItems.remove(item);
        item.x = -1; item.y = -1;
        state.inventory.add(item);
        events.add(new GameEvent("pickup", state.playerX, state.playerY, "Picked up " + item.name + "."));
        return 1;
    }

    private int use(String id, List<GameEvent> events) {
        var item = state.inventory.stream().filter(i -> i.type == GameState.ItemType.OXYGEN && i.power > 0 && i.quantity > 0
                && (id == null || id.isEmpty() || id.equals(i.id))).findFirst().orElse(null);
        if (item == null || state.reserve >= state.maxReserve) return 0;
        int added = (int)Math.min((long)state.maxReserve - state.reserve, item.power);
        state.reserve += added;
        if (--item.quantity == 0) state.inventory.remove(item);
        events.add(new GameEvent("oxygen", state.playerX, state.playerY, "Refilled " + added + " oxygen. Tank consumed."));
        return 1;
    }

    private int drop(String id, List<GameEvent> events) {
        var item = state.inventory.stream().filter(i -> id != null && id.equals(i.id)).findFirst().orElse(null);
        if (item == null) return 0;
        state.inventory.remove(item);
        item.x = state.playerX; item.y = state.playerY;
        state.floorItems.add(item);
        events.add(new GameEvent("drop", state.playerX, state.playerY, "Dropped " + item.name + "."));
        return 1;
    }

    private void tick(List<GameEvent> events) {
        state.tick++;
        int previousReserve = state.reserve, previousOxygenation = state.oxygenation;
        if (state.reserve > 0) {
            state.reserve--;
            state.oxygenation = Math.min(100, state.oxygenation + OXYGENATION_RECOVERY);
        } else {
            state.oxygenation = Math.max(0, state.oxygenation - OXYGENATION_LOSS);
            if (state.oxygenation == 0) state.hp = Math.max(0, state.hp - SUFFOCATION_DAMAGE);
        }
        if (state.poisonTicks > 0) { state.poisonTicks--; state.hp = Math.max(0, state.hp - 1); }
        if (previousReserve > LOW_RESERVE && state.reserve <= LOW_RESERVE)
            events.add(new GameEvent("warning", state.playerX, state.playerY, "Oxygen reserve low. Use a finite oxygen tank."));
        if (previousReserve > 0 && state.reserve == 0)
            events.add(new GameEvent("warning", state.playerX, state.playerY, "Reserve empty. Oxygenation is falling."));
        if (previousOxygenation > 70 && state.oxygenation <= 70)
            events.add(new GameEvent("warning", state.playerX, state.playerY, "Hypoxia warning. Refill oxygen now."));
        if (previousOxygenation > 30 && state.oxygenation <= 30)
            events.add(new GameEvent("warning", state.playerX, state.playerY, "Critical oxygenation."));
        if (state.hp == 0) {
            state.dead = true;
            events.add(new GameEvent("death", state.playerX, state.playerY, "Life signs lost. The expedition has ended."));
        }
    }

    public static void validateState(GameState s) {
        Objects.requireNonNull(s, "Saved state");
        if (s.schemaVersion != 1 || s.generationVersion != 1 || s.width < 5 || s.height < 5 || s.width > 256 || s.height > 256
                || s.deck < 1 || s.deck > LAST_DECK || s.tick < 0 || s.hp < 0 || s.maxHp <= 0 || s.hp > s.maxHp
                || s.maxReserve <= 0 || s.reserve < 0 || s.reserve > s.maxReserve || s.oxygenation < 0 || s.oxygenation > 100)
            throw new IllegalArgumentException("Invalid saved state bounds or version");
        if (s.tiles == null || s.visible == null || s.explored == null || s.light == null || s.tiles.length != s.width
                || s.visible.length != s.width || s.explored.length != s.width || s.light.length != s.width)
            throw new IllegalArgumentException("Missing saved station arrays");
        for (int x = 0; x < s.width; x++) {
            if (s.tiles[x] == null || s.visible[x] == null || s.explored[x] == null || s.light[x] == null
                    || s.tiles[x].length != s.height || s.visible[x].length != s.height
                    || s.explored[x].length != s.height || s.light[x].length != s.height)
                throw new IllegalArgumentException("Invalid saved station dimensions");
            for (Tile tile : s.tiles[x]) if (tile == null) throw new IllegalArgumentException("Missing saved tile");
        }
        if (!GridRoutes.inside(s, s.playerX, s.playerY) || s.tiles[s.playerX][s.playerY].blocksMovement
                || !GridRoutes.inside(s, s.switchX, s.switchY) || !GridRoutes.inside(s, s.hatchX, s.hatchY)
                || !GridRoutes.inside(s, s.terminalX, s.terminalY) || s.activeDialogue == null
                || s.actors == null || s.floorItems == null || s.inventory == null || s.journal == null || s.log == null
                || s.flags == null || s.identifiedMedicine == null || s.medicineEffects == null)
            throw new IllegalArgumentException("Invalid saved player, objectives or collections");
        if ((s.hp == 0) != s.dead) throw new IllegalArgumentException("Saved death state disagrees with HP");
        if (s.inventory.size() > MAX_INVENTORY) throw new IllegalArgumentException("Saved inventory exceeds capacity");
        for (var item : s.inventory) validateItem(s, item, false);
        for (var item : s.floorItems) validateItem(s, item, true);
        var ids = new java.util.HashSet<String>();
        for (var item : s.inventory) if (!ids.add(item.id)) throw new IllegalArgumentException("Duplicate saved item identity");
        for (var item : s.floorItems) if (!ids.add(item.id)) throw new IllegalArgumentException("Duplicate saved item identity");
        for (var actor : s.actors) if (actor == null || actor.kind == null || actor.id == null || !GridRoutes.inside(s, actor.x, actor.y))
            throw new IllegalArgumentException("Invalid saved actor");
    }

    private static void validateItem(GameState s, GameState.Item item, boolean floor) {
        if (item == null || item.type == null || item.id == null || item.id.isEmpty() || item.name == null || item.quantity <= 0
                || (floor && (!GridRoutes.inside(s, item.x, item.y) || !GridRoutes.passable(s.tiles[item.x][item.y]))))
            throw new IllegalArgumentException("Invalid saved item");
    }
}
