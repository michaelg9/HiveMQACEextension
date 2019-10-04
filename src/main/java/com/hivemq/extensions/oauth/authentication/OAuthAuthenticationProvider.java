package com.hivemq.extensions.oauth.authentication;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;

/**
 * @author Michael Michaelides
 */

public class OAuthAuthenticationProvider implements AuthenticatorProvider{

    @Override
    public @NotNull Authenticator getAuthenticator(@NotNull AuthenticatorProviderInput authenticatorProviderInput) {
        return new OAuthAuthenticator();
    }

}
