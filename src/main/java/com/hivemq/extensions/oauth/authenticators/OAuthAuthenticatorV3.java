package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.utils.Constants;
import com.hivemq.extensions.oauth.http.OauthHttpClient;
import com.hivemq.extensions.oauth.utils.StringUtils;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.nio.ByteBuffer;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.EXPIRED_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.USERNAME_PASSWORD_MISSING;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v3.1.1 ACE authentication
 */

public class OAuthAuthenticatorV3 implements SimpleAuthenticator {
    private final String auth = "p*6oso!eI3D2wshK:BByUwb7/FizssDcmI0AGVtIR8vvuZJR0pa7sWF7mDdw=";
    private final OauthHttpClient oauthHttpClient = new OauthHttpClient();

    @Override
    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput,
                          @NotNull SimpleAuthOutput simpleAuthOutput) {
        if (!simpleAuthInput.getConnectPacket().getAuthenticationMethod().orElse("")
                .equalsIgnoreCase(Constants.ACE)) {
            simpleAuthOutput.nextExtensionOrDefault();
            return;
        }
        String token = simpleAuthInput.getConnectPacket().getUserName().orElse("");
        byte[] mac = simpleAuthInput.getConnectPacket().getPassword().orElse(ByteBuffer.allocate(0)).array();

        if (StringUtils.isEmpty(token) || mac.length == 0) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD, USERNAME_PASSWORD_MISSING);
            return;
        }
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = oauthHttpClient.tokenIntrospectionRequest(auth, token);
        } catch (ASUnreachableException e) {
            e.printStackTrace();
            simpleAuthOutput.failAuthentication(ConnackReasonCode.SERVER_UNAVAILABLE, AUTH_SERVER_UNAVAILABLE);
            return;
        }
        if (!introspectionResponse.isActive()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD, EXPIRED_TOKEN);
            return;
        }
        MACCalculator macCalculator = new MACCalculator(
                introspectionResponse.getCnf().getJwk().getK(),
                token,
                introspectionResponse.getCnf().getJwk().getAlg());
        boolean isValidPOP = macCalculator.validatePOP(mac, simpleAuthInput.getConnectPacket());
        if (!isValidPOP) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, POP_FAILED);
        } else {
            simpleAuthOutput.authenticateSuccessfully();
        }
    }

}
