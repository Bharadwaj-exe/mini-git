package minigit.hashing;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class HashUtils {

    public static final int HASH_LENGTH = 40;

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private HashUtils() {
    }

    public static String sha1(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return toHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }

    public static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            hex[i * 2] = HEX_DIGITS[value >>> 4];
            hex[i * 2 + 1] = HEX_DIGITS[value & 0x0f];
        }
        return new String(hex);
    }

    public static byte[] fromHex(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return bytes;
    }

    public static boolean isHexPrefix(String value) {
        return !value.isEmpty() && value.length() <= HASH_LENGTH && value.matches("[0-9a-f]+");
    }
}
