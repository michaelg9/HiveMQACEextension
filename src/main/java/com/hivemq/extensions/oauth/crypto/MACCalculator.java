package com.hivemq.extensions.oauth.crypto;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.util.Base64URL;

import java.util.Arrays;

public class MACCalculator {
    private final String algorithm;
    private final byte[] key;

    public MACCalculator(final byte[] key, final String algorithm) {
        this.algorithm = algorithm;
        this.key = key;
    }
    public boolean isMacValid(byte[] mac, byte[] nonce) {
        byte[] calculated;
        try {
            calculated = signNonce(nonce);
        } catch (final JOSEException e) {
            e.printStackTrace();
            return false;
        }
        return Arrays.equals(mac, calculated);
    }

    public byte[] signNonce(final byte[] nonce) throws JOSEException {
        final JWSSigner signer = new MACSigner(key);
        final Base64URL url = signer.sign(new JWSHeader(new JWSAlgorithm(algorithm)), nonce);
        return url.decode();
    }

}
