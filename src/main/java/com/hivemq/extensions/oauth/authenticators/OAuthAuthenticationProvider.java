package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.Authenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthenticatorProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Michaelides
 * Todo: thread safe?
 */

public class OAuthAuthenticationProvider implements AuthenticatorProvider {
    private Map<String, OAuthAuthenticatorV5> authAuthenticatorV5Map = new HashMap<>();

    @Override
    public @NotNull Authenticator getAuthenticator(@NotNull AuthenticatorProviderInput authenticatorProviderInput) {
        if (authenticatorProviderInput.getConnectionInformation().getMqttVersion().equals(MqttVersion.V_5)) {
            String clientID = authenticatorProviderInput.getClientInformation().getClientId();
            OAuthAuthenticatorV5 auth;
            if (authAuthenticatorV5Map.containsKey(clientID)) {
                auth = authAuthenticatorV5Map.remove(clientID);
            } else {
                auth = new OAuthAuthenticatorV5();
                authAuthenticatorV5Map.put(clientID, auth);
            }
            return auth;
        } else {
            return new OAuthAuthenticatorV3();
        }
    }

}
