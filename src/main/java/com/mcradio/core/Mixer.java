package com.mcradio.core;

/** music_volume / announcement_volume / master_volume, each 0..100. Values are what goes to NBT. */
public final class Mixer {
    public static final double DUCK_GAIN = 0.30; // music is attenuated by 70% during an announcement

    private int music = 100;
    private int announcement = 100;
    private int master = 100;

    public int music() { return music; }
    public int announcement() { return announcement; }
    public int master() { return master; }

    public void setMusic(int v) { music = clamp(v); }
    public void setAnnouncement(int v) { announcement = clamp(v); }
    public void setMaster(int v) { master = clamp(v); }

    /** Final music gain with ducking applied (duck 0..1 = how much of the 70% attenuation is applied). */
    public float musicGain(float duck) {
        float g = (float) (1.0 - duck * (1.0 - DUCK_GAIN));
        return g * music / 100f * master / 100f;
    }

    public float announcementGain() {
        return announcement / 100f * master / 100f;
    }

    /** Flat form for NBT: {"music":..,"announcement":..,"master":..}. */
    public int[] toArray() { return new int[] {music, announcement, master}; }

    public void fromArray(int[] a) {
        if (a != null && a.length >= 3) {
            setMusic(a[0]);
            setAnnouncement(a[1]);
            setMaster(a[2]);
        }
    }

    private static int clamp(int v) { return Math.max(0, Math.min(100, v)); }
}
