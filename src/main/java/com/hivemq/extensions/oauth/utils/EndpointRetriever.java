package com.hivemq.extensions.oauth.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;

public class EndpointRetriever {
    private final String protocol;
    private final String target;
    private final String port;

    public EndpointRetriever(@NotNull String protocol, @NotNull String target, @NotNull String port) {
        this.protocol = protocol;
        this.target = target;
        this.port = port;
    }

    public @NotNull String getTokenIntrospectionEndpoint() {
        return String.format("%s://%s:%s%s", protocol, target, port, Endpoints.TOKEN_INTRO);
    }

    private static final class Endpoints {
        final static String TOKEN_INTRO = "/api/rs/introspect";
    }
}
