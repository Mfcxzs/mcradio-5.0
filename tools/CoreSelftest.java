package com.mcradio.core;

public final class CoreSelftest {
    static void check(boolean c, String m) {
        if (!c) throw new AssertionError(m);
        System.out.println("ok  " + m);
    }

    public static void main(String[] a) {
        check(!SecureChannels.canUse(310, ""), "310 locked without password");
        check(!SecureChannels.canUse(320, "wrong"), "320 locked with wrong password");
        check(SecureChannels.canUse(330, "28751522"), "330 open with master password");
        check(SecureChannels.canUse(101, null), "normal frequency free");
        check(SecureChannels.receiverState(310, null) == SecureChannels.RxState.ENCRYPTED, "encrypted state");
        check(ChainTier.TIER_3.effectiveRadius(true) == 140.0, "tier3 thunder radius 140");
        check(ChainTier.relayRange(ChainTier.TIER_1) == 50 && ChainTier.relayRange(ChainTier.TIER_3) == 200, "relay 50..200");

        BroadcastEngine e = new BroadcastEngine();
        e.enqueueTrack(new BroadcastEngine.Item("a", 3, false));
        e.enqueueTrack(new BroadcastEngine.Item("b", 3, false));
        e.enqueueAnnouncement(new BroadcastEngine.Item("ann", 1, true));
        e.play();
        check(e.snapshot().current().id().equals("ann") || e.snapshot().current().id().equals("a"), "started");
        // announcement is queued 'between tracks': first pick is announcement queue per advance(); run to end
        StringBuilder order = new StringBuilder();
        String last = "";
        for (int i = 0; i < 20 * 20 && e.snapshot().state() != BroadcastEngine.State.STOPPED; i++) {
            var s = e.snapshot();
            if (s.current() != null && !s.current().id().equals(last)) {
                last = s.current().id();
                order.append(last).append(' ');
            }
            e.serverTick();
        }
        check(order.toString().trim().equals("ann a b"), "order: " + order);

        BroadcastEngine m = new BroadcastEngine();
        m.enqueueTrack(new BroadcastEngine.Item("song", 100, false));
        m.play();
        for (int i = 0; i < 20 * 10; i++) m.serverTick();
        check(m.snapshot().currentSecond() == 10, "second counter = 10");
        m.pause();
        for (int i = 0; i < 40; i++) m.serverTick();
        check(m.snapshot().currentSecond() == 10, "pause holds position");
        m.seekBy(30);
        check(m.snapshot().currentSecond() == 40, "seek +30");
        m.play();
        m.registerEmergency(new BroadcastEngine.Item("alarm", 5, true));
        m.onRedstone(true);
        check(m.snapshot().emergency() && m.snapshot().current().id().equals("alarm"), "redstone triggers emergency");
        for (int i = 0; i < 10; i++) m.serverTick();
        Mixer mx = new Mixer();
        check(Math.abs(mx.musicGain(m.snapshot().duck()) - 0.30f) < 1e-4, "music ducked by 70%");
        for (int i = 0; i < 5 * 20; i++) m.serverTick();
        check(m.snapshot().current().id().equals("song") && m.snapshot().currentSecond() == 45, "music resumes in sync (45s, bed advanced)");
        // bed: music keeps advancing under the announcement
        BroadcastEngine b = new BroadcastEngine();
        b.enqueueTrack(new BroadcastEngine.Item("song", 100, false));
        b.play();
        for (int i = 0; i < 20 * 10; i++) b.serverTick();
        b.registerEmergency(new BroadcastEngine.Item("alarm", 5, true));
        int ep = b.musicEpoch();
        b.triggerEmergency();
        check(b.musicItem() != null && b.musicItem().id().equals("song") && b.announcementItem().id().equals("alarm"), "bed + announcement");
        for (int i = 0; i < 5 * 20; i++) b.serverTick();
        check(b.snapshot().current().id().equals("song") && b.snapshot().currentSecond() == 15 && b.musicEpoch() == ep, "resumed in sync at 15s, no seek");
        // client mirror
        BroadcastEngine c = new BroadcastEngine();
        c.applySnapshot(m.snapshot());
        for (int i = 0; i < 40; i++) c.clientTick();
        check(c.clientProgressLabel().equals("00:47 / 01:40"), "client label " + c.clientProgressLabel());
        System.out.println("ALL OK");
    }
}
