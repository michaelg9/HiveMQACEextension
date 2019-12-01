package com.hivemq.extensions.oauth.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.nio.ByteBuffer;

public class StringUtils {

    @NotNull
    public static boolean isEmpty(@Nullable String text) {
        return text == null || "".equals(text);
    }

    @NotNull
    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @NotNull
    public static String bytesToHex(byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    @NotNull
    public static String bytesToHex(@NotNull final ByteBuffer buf) {
        final byte[] b = new byte[buf.remaining()];
        buf.get(b);
        return bytesToHex(b);
    }
}
