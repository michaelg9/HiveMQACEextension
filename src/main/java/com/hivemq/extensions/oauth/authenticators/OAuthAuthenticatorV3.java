package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.utils.AuthData;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.USERNAME_PASSWORD_MISSING;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v3.1.1 ACE authentication
 * TODO: is input valid?
 */

public class OAuthAuthenticatorV3 extends OAuthAuthenticator implements SimpleAuthenticator {

    @Override
    @Nullable AuthData parseAuthData(@NotNull SimpleAuthInput simpleAuthInput,
                                     @NotNull SimpleAuthOutput simpleAuthOutput) {
        AuthData authData = new AuthData();
        if (simpleAuthInput.getConnectPacket().getUserName().isPresent() &&
                simpleAuthInput.getConnectPacket().getPassword().isPresent()) {
            authData.setToken(simpleAuthInput.getConnectPacket().getUserName().get());
            authData.setPop(simpleAuthInput.getConnectPacket().getPassword().get());
        }
        if (authData.getToken().isEmpty() || authData.getPOP().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD, USERNAME_PASSWORD_MISSING);
            authData = null;
        }
        return authData;
    }

    @Override
    void proceedToChallenge(@NotNull IntrospectionResponse introspectionResponse,
                            @NotNull MACCalculator macCalculator,
                            @NotNull SimpleAuthOutput simpleAuthOutput) {
        throw new IllegalStateException("Version 3 client for challenge auth");
    }
}
