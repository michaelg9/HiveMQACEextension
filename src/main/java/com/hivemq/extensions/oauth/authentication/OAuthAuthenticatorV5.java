package com.hivemq.extensions.oauth.authentication;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.utils.StringUtils;

public class OAuthAuthenticatorV5 implements SimpleAuthenticator {

    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        OAuthAuthenticatorV3 oAuthAuthenticatorV3 = new OAuthAuthenticatorV3();
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
        String pop = null;
        if (authData.contains("NEXT")) {
            String[] parts = authData.split("NEXT");
            token = parts[0];
            pop = parts[1];
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
        if (!StringUtils.isEmpty(pop)) {
//            oAuthAuthenticatorV3.authenticate(simpleAuthOutput, token, pop);
            simpleAuthOutput.authenticateSuccessfully();
        } else {
            // ToDo: use extended auth methods.
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }
}
