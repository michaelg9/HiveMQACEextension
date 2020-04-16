package com.hivemq.extensions.oauth.authorizer;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;

public class AceAuthorizerProvider implements com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider {
    private final Authorizer authorizer = new Authorizer();

    @Override
    public @Nullable com.hivemq.extension.sdk.api.auth.Authorizer getAuthorizer(@NotNull AuthorizerProviderInput authorizerProviderInput) {
        return authorizer;
    }
}
