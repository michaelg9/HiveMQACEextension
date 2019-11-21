package com.hivemq.extensions.oauth.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * TODO:
 * compute MAC over whole Connect
 * condition on algorithm
 * define separator
 */
public class MACCalculator {
    private final String algorithm = "HmacSHA256";
    private final String key;
    private final String token;

    public MACCalculator(final String key, final String token, String algorithm) {
        this.key = key;
        this.token = token;
    }

    public boolean validatePOP(
                        byte[] mac,
                        byte[] plain) {
        return Arrays.equals(mac, compute_hmac(plain));
    }

    byte[] compute_hmac(final byte[] content) {
        final byte[] byteKey = key.getBytes(StandardCharsets.UTF_8);
        try {
            final Mac sha512_HMAC = Mac.getInstance(algorithm);
            final SecretKeySpec keySpec = new SecretKeySpec(byteKey, algorithm);
            sha512_HMAC.init(keySpec);
            return bytesToHex(sha512_HMAC.doFinal(content)).getBytes();
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            // should never happen for the case of HmacSHA1 / HmacSHA256
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private String bytesToHex(final byte[] bytes) {
        final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        final char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            final int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

}
