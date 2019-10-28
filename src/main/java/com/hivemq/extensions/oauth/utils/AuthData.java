package com.hivemq.extensions.oauth.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AuthData {
    private final ByteBuffer authData;
    public AuthData(@NotNull ByteBuffer authData) {
        this.authData = authData;
    }

    @NotNull
    public String getToken() throws MalformedInputException {
        short tokenLength = authData.getShort();
        if (authData.remaining() < tokenLength) {
            throw new MalformedInputException(tokenLength);
        }
        byte[] token = new byte[tokenLength];
        authData.get(token);
        return new String(token, StandardCharsets.UTF_8);
    }

    @NotNull
    public Optional<byte[]> getData() throws MalformedInputException {
        if (!authData.hasRemaining()) {
            return Optional.empty();
        }
        short macLength = authData.getShort();
        if (authData.remaining() < macLength) {
            throw new MalformedInputException(macLength);
        }
        byte[] mac = new byte[macLength];
        authData.get(mac);
        return Optional.of(mac);
    }
}
