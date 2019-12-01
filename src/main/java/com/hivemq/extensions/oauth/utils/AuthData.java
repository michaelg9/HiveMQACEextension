package com.hivemq.extensions.oauth.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Optional;


public class AuthData {
    @Nullable private String token;
    @Nullable private byte[] pop;

    public void setToken(@NotNull String token) {
        this.token = token;
    }

    public void setPop(byte[] pop) { this.pop = pop; }

    public void setToken(@NotNull ByteBuffer buf) {
        byte[] tokenBytes = readValue(buf);
        if (tokenBytes != null) {
            //TODO: encoding?
            token = new String(tokenBytes);
        } else {
            token = null;
        }
    }

    public void setPop(@NotNull ByteBuffer buf) {
        pop = readValue(buf);
    }

    public void setTokenAndPop(@NotNull ByteBuffer buf) {
        setToken(buf);
        setPop(buf);
    }

    @Nullable
    private byte[] readValue(ByteBuffer buf) {
        byte[] value = null;
        short valueLength = 0;
        if (buf.remaining() > 2) {
            valueLength = buf.getShort();
        }
        if (valueLength > 0 && buf.remaining() >= valueLength) {
            value = new byte[valueLength];
            buf.get(value);
        }
        return value;
    }

    @NotNull
    public Optional<byte[]> getPOP() {
        return Optional.ofNullable(pop);
    }

    @NotNull
    public Optional<String> getToken() {
        return Optional.ofNullable(token);
    }

    @NotNull
    public Optional<byte[]> getTokenAsBytes() {
        if (token == null) {
            return Optional.empty();
        }
        return Optional.of(token.getBytes());
    }
}
