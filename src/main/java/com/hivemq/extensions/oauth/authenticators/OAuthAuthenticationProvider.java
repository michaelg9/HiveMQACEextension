package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;


/**
 * @author Michael Michaelides
 */

public class OAuthAuthenticationProvider implements AuthenticatorProvider {
    private final OAuthAuthenticatorV5 authAuthenticatorV5 = new OAuthAuthenticatorV5();
    private final OAuthAuthenticatorV3 authAuthenticatorV3 = new OAuthAuthenticatorV3();

    @Override
    public @NotNull Authenticator getAuthenticator(@NotNull AuthenticatorProviderInput authenticatorProviderInput) {
        if (authenticatorProviderInput.getConnectionInformation().getMqttVersion().equals(MqttVersion.V_5)) {
            return authAuthenticatorV5;
        } else {
            return authAuthenticatorV3;
        }
    }
}
