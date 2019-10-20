package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.utils.StringUtils;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

public class OAuthAuthenticatorV5 implements SimpleAuthenticator {

    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        if (!simpleAuthInput.getConnectPacket().getAuthenticationMethod().orElse("")
                .equalsIgnoreCase("ACE")) {
            simpleAuthOutput.nextExtensionOrDefault();
            return;
        }
        if (simpleAuthInput.getConnectPacket().getAuthenticationData().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.USE_ANOTHER_SERVER, "127.0.0.1");
            return;
        }
        // retrieve token and pop from authentication data
        String authData = new String(simpleAuthInput.getConnectPacket().getAuthenticationData().get().array());
        String token;
        String mac = null;
        if (authData.contains("NEXT")) {
            String[] parts = authData.split("NEXT");
            token = parts[0];
            mac = parts[1];
        } else {
            token = authData;
        }
        if (StringUtils.isEmpty(token)) {
            // invalid authentication
            // ToDo: use a different reason code
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Authentication token expected when using ACE authentication");
            return;
        }
        OAuthAuthenticatorV3 oAuthAuthenticatorV3 = new OAuthAuthenticatorV3();
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = oAuthAuthenticatorV3.introspectToken(token);
        } catch (ASUnreachableException e) {
            e.printStackTrace();
            simpleAuthOutput.failAuthentication(ConnackReasonCode.SERVER_UNAVAILABLE,
                    "Authorization server is unavailable. Please try later");
            return;
        }
        if (!introspectionResponse.active) {
            simpleAuthOutput.failAuthentication(
                    ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Expired token.");
            return;
        }
        boolean isValidPOP = false;
        if (!StringUtils.isEmpty(mac)) {
            isValidPOP = oAuthAuthenticatorV3.validatePOP(introspectionResponse, token, mac, simpleAuthInput.getConnectPacket());
        } else {
            // ToDo: use extended auth methods.
            throw new UnsupportedOperationException("Not yet implemented");
        }
        if (!isValidPOP) {
            simpleAuthOutput.failAuthentication(
                    ConnackReasonCode.NOT_AUTHORIZED, "Unable to proof possession of token");
        } else {
            simpleAuthOutput.authenticateSuccessfully();
        }
    }
}
