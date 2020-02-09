package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthConnectInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.EnhancedAuthOutput;
import com.hivemq.extension.sdk.api.auth.parameter.OverloadProtectionThrottlingLevel;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.utils.AuthData;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.util.Optional;
import java.util.logging.Level;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.EXPIRED_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v3.1.1 ACE authentication
 * TODO: is input valid?
 */

public class OAuthAuthenticatorV3 extends OAuthAuthenticator implements EnhancedAuthenticator {

    @Nullable AuthData parseAuthData(@NotNull ConnectPacket connectPacket) {
        AuthData authData = new AuthData();
        if (connectPacket.getUserName().isPresent() &&
                connectPacket.getPassword().isPresent()) {
            authData.setToken(connectPacket.getUserName().get());
            authData.setPop(connectPacket.getPassword().get());
        }
        if (authData.getToken().isEmpty() || authData.getPOP().isEmpty()) {
            authData = null;
        }
        return authData;
    }

    @Override
    public void onConnect(@NotNull EnhancedAuthConnectInput input, @NotNull EnhancedAuthOutput output) {
        LOGGER.log(Level.FINE, String.format("Received client CONNECT:\t%s", input.getConnectPacket()));
        Optional<String> errors = isInputValid(input.getConnectPacket());
        if (errors.isPresent()) {
            output.failAuthentication(DisconnectedReasonCode.PAYLOAD_FORMAT_INVALID, errors.get());
            return;
        }
        AuthData authData = parseAuthData(input.getConnectPacket());
        if (authData == null || authData.getToken().isEmpty() || authData.getPOP().isEmpty()) {
            output.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, AUTH_SERVER_UNAVAILABLE);
            return;
        }
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = super.introspectToken(authData.getToken().get());
        } catch (ASUnreachableException e) {
            e.printStackTrace();
            output.failAuthentication(DisconnectedReasonCode.SERVER_UNAVAILABLE, AUTH_SERVER_UNAVAILABLE);
            return;
        }
        if (!introspectionResponse.isActive()) {
            output.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, EXPIRED_TOKEN);
            return;
        }
        MACCalculator macCalculator = new MACCalculator(
                introspectionResponse.getCnf().getJwk().getK(),
                introspectionResponse.getCnf().getJwk().getAlg());
        boolean isValidPOP = macCalculator.isMacValid(authData.getPOP().get(), authData.getTokenAsBytes().get());
        if (isValidPOP) {
            authenticateClient(output, introspectionResponse);
        } else {
            output.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, POP_FAILED);
        }
    }

    void authenticateClient(@NotNull EnhancedAuthOutput output, @NotNull final IntrospectionResponse introspectionResponse) {
        TopicPermission permission = super.registerClient(introspectionResponse);
        output.getDefaultPermissions().add(permission);
        output.authenticateSuccessfully();
    }

    @Override
    public void onAuth(@NotNull EnhancedAuthInput enhancedAuthInput, @NotNull EnhancedAuthOutput enhancedAuthOutput) {
        enhancedAuthOutput.failAuthentication("Version 3 client can't use AUTH packets");
    }
}
