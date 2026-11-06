package com.mcradio.core;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Protected frequencies 310/320/330. Broadcasting or receiving needs the master password. */
public final class SecureChannels {
    private static final int[] SECURE = {310, 320, 330};
    /** Only the SHA-256 hash is compiled in, the plain password never is. Change via config in stage 2. */
    private static final byte[] PASSWORD_HASH = sha("28751522");

    private SecureChannels() {
    }

    public static boolean isSecure(int frequency) {
        for (int f : SECURE) {
            if (f == frequency) return true;
        }
        return false;
    }

    public static boolean passwordOk(String password) {
        return password != null && MessageDigest.isEqual(PASSWORD_HASH, sha(password.trim()));
    }

    /** True if a device (transmitter or receiver) set to {@code frequency} with {@code password} may work. */
    public static boolean canUse(int frequency, String password) {
        return !isSecure(frequency) || passwordOk(password);
    }

    /** What the receiver should present to the player. */
    public enum RxState { CLEAR, ENCRYPTED }

    public static RxState receiverState(int frequency, String password) {
        return canUse(frequency, password) ? RxState.CLEAR : RxState.ENCRYPTED;
    }

    private static byte[] sha(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
