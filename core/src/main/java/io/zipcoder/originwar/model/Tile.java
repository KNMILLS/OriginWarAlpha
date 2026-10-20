package io.zipcoder.originwar.model;

public enum Tile {
    WALL(true, true, 1), FLOOR(false, false, 1), DOOR_CLOSED(true, true, 1),
    DOOR_OPEN(false, false, 1), LOCKED_DOOR(true, true, 1), LOW_GRAV(false, false, 2),
    NO_GRAV(false, false, 3), HAZARD(false, false, 2), COVER(true, false, 1),
    GLASS(true, false, 1), SWITCH(false, false, 1), HATCH(false, false, 1),
    TERMINAL(false, false, 1);
    public final boolean blocksMovement, blocksVision;
    public final int cost;
    Tile(boolean movement, boolean vision, int cost) {
        this.blocksMovement = movement; this.blocksVision = vision; this.cost = cost;
    }
}
