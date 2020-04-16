package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.services.auth.provider.EnhancedAuthenticatorProvider;

import java.util.logging.Logger;

public class ACEAuthenticatorProvider implements EnhancedAuthenticatorProvider {
    final static Logger LOGGER = Logger.getLogger(ACEAuthenticatorProvider.class.getName());
    private final AuthenticatorV5 authenticatorv5 = new AuthenticatorV5();
    private final AuthenticatorV3 authenticatorv3 = new AuthenticatorV3();

    @Override
    public @Nullable EnhancedAuthenticator getEnhancedAuthenticator(@NotNull AuthenticatorProviderInput authenticatorProviderInput) {
        MqttVersion version = authenticatorProviderInput.getConnectionInformation().getMqttVersion();
        LOGGER.info(String.format("Version %s client connected on %s",version, authenticatorProviderInput.getConnectionInformation().getTlsInformation().isPresent() ? "TLS" : "TCP"));
        if (version.equals(MqttVersion.V_5)) {
            return authenticatorv5;
        } else {
            return authenticatorv3;
        }
    }
}
