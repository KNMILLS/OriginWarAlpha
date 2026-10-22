package io.zipcoder.originwar.model;

/** Small explicit-state RNG for saved gameplay streams. Generation uses SquidSquad's RNG. */
public final class DeterministicRandom {
    private DeterministicRandom() {}
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }
    public static long nextState(long state) { return state + 0x9e3779b97f4a7c15L; }
    public static int bounded(long state, int bound) {
        if (bound <= 0) throw new IllegalArgumentException("Positive bound required");
        return (int) Long.remainderUnsigned(mix(state), bound);
    }
}
