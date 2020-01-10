package com.hivemq.extensions.oauth.utils.dataclasses;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public final class PendingAuthenticationDetails {
    private final IntrospectionResponse introspectionResponse;
    private final byte[] expectedPOP;

    public PendingAuthenticationDetails(@NotNull final IntrospectionResponse introspectionResponse, @NotNull final byte[] expectedPOP) {
        this.introspectionResponse = introspectionResponse;
        this.expectedPOP = expectedPOP;
    }

    public IntrospectionResponse getIntrospectionResponse() {
        return introspectionResponse;
    }

    public byte[] getExpectedPOP() {
        return expectedPOP;
    }
}
