package io.zipcoder.originwar.model;
/** Isolated presentation copy; never authoritative. */
public record WorldSnapshot(GameState world) {
    public WorldSnapshot { world = world.copy(); }
}
