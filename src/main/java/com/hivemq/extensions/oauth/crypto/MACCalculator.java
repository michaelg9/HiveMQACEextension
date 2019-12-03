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
 * lib?
 */
public class MACCalculator {
    private final String algorithm;
    private final byte[] key;

    public MACCalculator(final byte[] key, final String algorithm) {
        if (algorithm.startsWith("HS")) this.algorithm = algorithm.replace("HS", "HmacSHA");
        else this.algorithm = algorithm;
        this.key = key;
    }
    public boolean isMacValid(byte[] mac, byte[] nonce) {
        return Arrays.equals(mac, compute_hmac(nonce));
    }

    public byte[] compute_hmac(final byte[] nonce) {
        try {
            final Mac mac = Mac.getInstance(algorithm);
            final SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);
            mac.init(keySpec);
            return mac.doFinal(nonce);
        } catch (final NoSuchAlgorithmException | InvalidKeyException e) {
            // should never happen for the case of HmacSHA1 / HmacSHA256
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
