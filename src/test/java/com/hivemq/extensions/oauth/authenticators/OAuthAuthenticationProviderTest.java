package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.auth.parameter.AuthenticatorProviderInput;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class OAuthAuthenticationProviderTest {
    private OAuthAuthenticationProvider oAuthAuthenticationProvider = new OAuthAuthenticationProvider();

    @Test
    void getAuthenticator() {
        AuthenticatorProviderInput input = Mockito.mock(AuthenticatorProviderInput.class, Mockito.RETURNS_DEEP_STUBS);
        when(input.getConnectionInformation().getMqttVersion()).thenReturn(MqttVersion.V_5);
        assertTrue(oAuthAuthenticationProvider.getAuthenticator(input) instanceof OAuthAuthenticatorV5);
        when(input.getConnectionInformation().getMqttVersion()).thenReturn(MqttVersion.V_3_1_1);
        assertTrue(oAuthAuthenticationProvider.getAuthenticator(input) instanceof OAuthAuthenticatorV3);
        when(input.getConnectionInformation().getMqttVersion()).thenReturn(MqttVersion.V_3_1);
        assertTrue(oAuthAuthenticationProvider.getAuthenticator(input) instanceof OAuthAuthenticatorV3);

    }
}