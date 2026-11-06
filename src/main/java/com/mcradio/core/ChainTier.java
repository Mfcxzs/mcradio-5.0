package com.mcradio.core;

/** Antenna tiers (stage 2 of the chain) and relay range. Radii are in blocks. */
public enum ChainTier {
    TIER_1(1, 50), TIER_2(2, 100), TIER_3(3, 200);

    public static final double THUNDER_FACTOR = 0.70; // -30% during thunderstorms
    public static final int RELAY_MIN = 50;
    public static final int RELAY_MAX = 200;

    public final int level;
    public final int radius;

    ChainTier(int level, int radius) {
        this.level = level;
        this.radius = radius;
    }

    public static ChainTier byLevel(int level) {
        for (ChainTier t : values()) {
            if (t.level == level) return t;
        }
        return TIER_1;
    }

    /** Effective radius, reduced by 30% while level.isThundering(). */
    public double effectiveRadius(boolean thundering) {
        return thundering ? radius * THUNDER_FACTOR : radius;
    }

    /** A relay extends the signal by the antenna tier radius, clamped to 50..200. */
    public static int relayRange(ChainTier source) {
        return Math.max(RELAY_MIN, Math.min(RELAY_MAX, source.radius));
    }

    /** Extra crackle level 0..1 for the client; only during thunder, grows toward the edge of coverage. */
    public static double crackle(boolean thundering, double distance, double radius) {
        if (!thundering || radius <= 0) return 0;
        double edge = Math.max(0, Math.min(1, distance / radius));
        return 0.15 + 0.35 * edge;
    }
}
