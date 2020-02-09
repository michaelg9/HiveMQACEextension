package com.hivemq.extensions.oauth.http;

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

    @NotNull String getEndpoint(@NotNull final EndpointRetriever.ASEndpoint endpoint) {
        return String.format("%s://%s:%s%s", protocol, target, port, endpoint.name);
    }

    public enum ASEndpoint {
        TOKEN_INTROSPECTION("/api/rs/introspect"),
        CLIENT_REG("/api/client/dyn_client_reg");
        private final String name;
        ASEndpoint(@NotNull final String name) {
            this.name = name;
        }
    }
}
