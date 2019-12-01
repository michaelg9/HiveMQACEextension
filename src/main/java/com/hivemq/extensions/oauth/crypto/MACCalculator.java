package com.hivemq.extensions.oauth.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * TODO:
 * compute MAC over whole Connect
 * condition on algorithm
 */
public class MACCalculator {
    private final String algorithm = "HmacSHA256";
    private final byte[] key;

    public MACCalculator(final byte[] key, String algorithm) {
        this.key = key;
    }

    public boolean isMacValid(byte[] mac, byte[] nonce) {
        return Arrays.equals(mac, compute_hmac(nonce));
    }

    public byte[] compute_hmac(final byte[] nonce) {
        try {
            final Mac sha512_HMAC = Mac.getInstance(algorithm);
            final SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);
            sha512_HMAC.init(keySpec);
            return sha512_HMAC.doFinal(nonce);
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            // should never happen for the case of HmacSHA1 / HmacSHA256
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
