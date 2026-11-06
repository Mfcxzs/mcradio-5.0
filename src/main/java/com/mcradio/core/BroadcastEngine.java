package com.mcradio.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Pure-logic on-air engine of the Computer block: queue, per-second player, announcements between tracks,
 * emergency broadcast with ducking. No Minecraft types, so it is unit-testable; the BlockEntity calls
 * {@link #serverTick()} 20x/s and the client mirrors it with {@link #clientTick()} using a {@link Snapshot}.
 *
 * Sync model: the server sends a Snapshot on every state change and every 100 ticks (drift correction);
 * between snapshots every client advances its own copy, so all clients stay within one packet latency.
 */
public final class BroadcastEngine {
    public static final int TPS = 20;
    public static final float DUCK_STEP = 0.1f; // ~0.5 s ramp to full duck

    public enum State { STOPPED, PLAYING, PAUSED }

    /** A playable item: a music track or an announcement. */
    public record Item(String id, int totalSeconds, boolean announcement) {
        public Item {
            totalSeconds = Math.max(1, totalSeconds);
        }
    }

    /** What goes over the wire (and into NBT). */
    public record Snapshot(State state, Item current, int positionTicks, int queueSize, float duck, boolean emergency) {
        public int currentSecond() { return positionTicks / TPS; }
        public int totalSeconds() { return current == null ? 0 : current.totalSeconds(); }
    }

    private final Deque<Item> queue = new ArrayDeque<>();
    private final Deque<Item> announcements = new ArrayDeque<>(); // queued "inserts", played between tracks
    private final Deque<Item> emergencyQueue = new ArrayDeque<>();
    private Item current;
    private Item interrupted;         // music track suspended by an emergency
    private int interruptedPos;
    private int positionTicks;
    private State state = State.STOPPED;
    private float duck;               // 0..1 current ducking amount
    private boolean emergency;
    private boolean loop;
    private boolean dirty;
    private int syncCounter;
    private boolean lastRedstone;
    private int musicEpoch;
    private int annEpoch;

    public boolean consumeDirty() {
        boolean d = dirty;
        dirty = false;
        return d;
    }

    // ---------- queue editing ----------
    public void enqueueTrack(Item track) {
        queue.addLast(new Item(track.id(), track.totalSeconds(), false));
        dirty = true;
    }

    /** An announcement inserted into the common queue: plays after the current track ends. */
    public void enqueueAnnouncement(Item ann) {
        announcements.addLast(new Item(ann.id(), ann.totalSeconds(), true));
        dirty = true;
    }

    public void registerEmergency(Item ann) {
        emergencyQueue.addLast(new Item(ann.id(), ann.totalSeconds(), true));
        dirty = true;
    }

    public void clearQueue() {
        queue.clear();
        dirty = true;
    }

    public boolean removeFromQueue(int index) {
        if (index < 0 || index >= queue.size()) return false;
        List<Item> l = new ArrayList<>(queue);
        l.remove(index);
        queue.clear();
        queue.addAll(l);
        dirty = true;
        return true;
    }

    public void setLoop(boolean loop) { this.loop = loop; }
    public List<Item> queueView() { return List.copyOf(queue); }

    // ---------- transport ----------
    public void play() {
        if (state == State.PAUSED) {
            state = State.PLAYING;
        } else if (state == State.STOPPED) {
            advance();
        }
        dirty = true;
    }

    public void pause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
            dirty = true;
        }
    }

    public void stop() {
        current = null;
        interrupted = null;
        positionTicks = 0;
        state = State.STOPPED;
        emergency = false;
        musicEpoch++;
        annEpoch++;
        dirty = true;
    }

    /** Seek to an absolute second of the current item (clamped). */
    public void seekTo(int second) {
        if (current == null) return;
        positionTicks = Math.max(0, Math.min(current.totalSeconds() - 1, second)) * TPS;
        if (!current.announcement()) musicEpoch++;
        dirty = true;
    }

    public void seekBy(int deltaSeconds) {
        if (current != null) seekTo(positionTicks / TPS + deltaSeconds);
    }

    public void skip() {
        if (current != null) {
            current = null;
            advance();
            dirty = true;
        }
    }

    // ---------- emergency ----------
    /** "Emergency broadcast": interrupts the song now, ducks music by 70%, plays the next emergency item. */
    public boolean triggerEmergency() {
        Item ann = emergencyQueue.pollFirst();
        if (ann == null) ann = announcements.pollFirst();
        if (ann == null) return false;
        if (current != null && !current.announcement() && !emergency) {
            interrupted = current;
            interruptedPos = positionTicks;
        }
        current = ann;
        positionTicks = 0;
        state = State.PLAYING;
        emergency = true;
        annEpoch++;
        dirty = true;
        return true;
    }

    /** Edge-triggered redstone input (alarm systems): rising edge => emergency. */
    public void onRedstone(boolean powered) {
        if (powered && !lastRedstone) triggerEmergency();
        lastRedstone = powered;
    }

    // ---------- ticking ----------
    private void advance() {
        // 1) announcements queued "between tracks" go first
        Item next = announcements.pollFirst();
        // 2) a suspended track after an emergency resumes where it was
        if (next == null && interrupted != null) {
            current = interrupted;
            positionTicks = interruptedPos;
            interrupted = null;
            state = State.PLAYING;
            emergency = false;
            return;
        }
        if (next == null) {
            next = queue.pollFirst();
            if (next != null && loop) queue.addLast(next);
        }
        emergency = false;
        if (next == null) {
            current = null;
            positionTicks = 0;
            state = State.STOPPED;
            musicEpoch++;
        } else {
            current = next;
            positionTicks = 0;
            state = State.PLAYING;
            if (next.announcement()) annEpoch++; else musicEpoch++;
        }
    }

    private void stepDuck() {
        boolean wantDuck = current != null && current.announcement() && state == State.PLAYING;
        duck = wantDuck ? Math.min(1f, duck + DUCK_STEP) : Math.max(0f, duck - DUCK_STEP);
    }

    public void serverTick() {
        stepDuck();
        // The ducked music bed under an announcement keeps running, so it resumes in sync afterwards.
        if (state == State.PLAYING && interrupted != null && current != null && current.announcement()) {
            interruptedPos++;
            if (interruptedPos >= interrupted.totalSeconds() * TPS) interrupted = null;
        }
        if (state == State.PLAYING && current != null) {
            positionTicks++;
            if (positionTicks >= current.totalSeconds() * TPS) {
                // a finished announcement while music was interrupted: advance() restores the music
                current = null;
                advance();
                dirty = true;
            }
        }
        if (++syncCounter >= 100) {
            syncCounter = 0;
            dirty = true;
        }
    }

    /** The music item that should be audible on the music channel (the bed while an announcement plays). */
    public Item musicItem() {
        if (current != null && !current.announcement()) return current;
        return interrupted;
    }

    public int musicPositionTicks() {
        if (current != null && !current.announcement()) return positionTicks;
        return interrupted != null ? interruptedPos : 0;
    }

    public boolean musicPlaying() { return state == State.PLAYING && musicItem() != null; }

    public Item announcementItem() {
        return current != null && current.announcement() ? current : null;
    }

    public int musicEpoch() { return musicEpoch; }
    public int annEpoch() { return annEpoch; }
    public boolean looping() { return loop; }
    public List<Item> announcementsView() { return List.copyOf(announcements); }
    public int emergencyCount() { return emergencyQueue.size(); }

    /** Restores a saved state (always paused so nothing blares after a restart). */
    public void restore(Item cur, int posTicks, List<Item> savedQueue, boolean loopFlag) {
        queue.clear();
        queue.addAll(savedQueue);
        loop = loopFlag;
        if (cur != null && !cur.announcement()) {
            current = cur;
            positionTicks = Math.max(0, Math.min(cur.totalSeconds() * TPS - 1, posTicks));
            state = State.PAUSED;
        }
        musicEpoch++;
        dirty = true;
    }

    public Snapshot snapshot() {
        return new Snapshot(state, current, positionTicks, queue.size(), duck, emergency);
    }

    // ---------- client mirror ----------
    private Snapshot mirror;
    private int mirrorTicks;

    public void applySnapshot(Snapshot s) {
        mirror = s;
        mirrorTicks = s.positionTicks();
    }

    public void clientTick() {
        if (mirror != null && mirror.state() == State.PLAYING && mirror.current() != null) {
            mirrorTicks = Math.min(mirrorTicks + 1, mirror.current().totalSeconds() * TPS);
        }
    }

    /** "current_second / total_seconds" for the progress bar. */
    public String clientProgressLabel() {
        if (mirror == null || mirror.current() == null) return "00:00 / 00:00";
        return fmt(mirrorTicks / TPS) + " / " + fmt(mirror.current().totalSeconds());
    }

    public float clientProgress() {
        if (mirror == null || mirror.current() == null) return 0f;
        return Math.min(1f, mirrorTicks / (float) (mirror.current().totalSeconds() * TPS));
    }

    public static String fmt(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
