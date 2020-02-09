package com.hivemq.extensions.oauth.utils.dataclasses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public final class ClientRegistrationRequest {
    @JsonProperty("client_name")
    private final String clientName;
    @JsonProperty("client_uri")
    private final String clientUri;

    public ClientRegistrationRequest(@NotNull final String clientName, @NotNull final String clientUri) {
        this.clientName = clientName;
        this.clientUri = clientUri;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientUri() {
        return clientUri;
    }
}
