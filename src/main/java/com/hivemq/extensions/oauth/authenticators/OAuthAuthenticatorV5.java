package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.http.OauthHttpClient;
import com.hivemq.extensions.oauth.utils.AuthData;
import com.hivemq.extensions.oauth.utils.Constants;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.nio.charset.MalformedInputException;
import java.util.Optional;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.EXPIRED_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.MISSING_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v5 ACE authentication
 *
 * TODO: separator or length? How to communicate separator
 * ToDo: use extended auth methods.
 */

public class OAuthAuthenticatorV5 implements SimpleAuthenticator {
    private final String auth = "p*6oso!eI3D2wshK:BByUwb7/FizssDcmI0AGVtIR8vvuZJR0pa7sWF7mDdw=";
    private final OauthHttpClient oauthHttpClient = new OauthHttpClient();
    private final String asServer = "127.0.0.1";

    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        if (!simpleAuthInput.getConnectPacket().getAuthenticationMethod().orElse("")
                .equalsIgnoreCase(Constants.ACE)) {
            simpleAuthOutput.nextExtensionOrDefault();
            return;
        }
        if (simpleAuthInput.getConnectPacket().getAuthenticationData().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.USE_ANOTHER_SERVER, asServer);
            return;
        }
        // retrieve token and pop from authentication data
        AuthData authData = new AuthData(simpleAuthInput.getConnectPacket().getAuthenticationData().get());
        String token;
        try {
            token = authData.getToken();
        } catch (MalformedInputException e) {
            e.printStackTrace();
            simpleAuthOutput.failAuthentication(ConnackReasonCode.MALFORMED_PACKET, MISSING_TOKEN);
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
        Optional<byte[]> mac;
        try {
            mac = authData.getData();
        } catch (MalformedInputException e) {
            e.printStackTrace();
            simpleAuthOutput.failAuthentication(ConnackReasonCode.MALFORMED_PACKET, "Authentication data malformed.");
            return;
        }
        boolean isValidPOP = false;
        MACCalculator macCalculator = new MACCalculator(
                introspectionResponse.getCnf().getJwk().getK(),
                token,
                introspectionResponse.getCnf().getJwk().getAlg());
        if (mac.isPresent()) {
            isValidPOP = macCalculator.validatePOP(mac.get(), simpleAuthInput.getConnectPacket());
        } else {
            // ToDo: use extended auth methods.
            throw new UnsupportedOperationException("Not yet implemented");
        }
        if (!isValidPOP) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, POP_FAILED);
        } else {
            simpleAuthOutput.authenticateSuccessfully();
        }
    }


}
