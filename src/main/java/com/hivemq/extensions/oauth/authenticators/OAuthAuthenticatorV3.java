package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.utils.OauthHttpClient;
import com.hivemq.extensions.oauth.utils.StringUtils;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports both v3.1.1 and v5
 */

public class OAuthAuthenticatorV3 implements SimpleAuthenticator {
    private final String auth = "p*6oso!eI3D2wshK:BByUwb7/FizssDcmI0AGVtIR8vvuZJR0pa7sWF7mDdw=";
    private OauthHttpClient oauthHttpClient = new OauthHttpClient("127.0.0.1", "3001");

    @Override
    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput,
                          @NotNull SimpleAuthOutput simpleAuthOutput) {
        if (!simpleAuthInput.getConnectPacket().getAuthenticationMethod().orElse("")
                .equalsIgnoreCase("ACE")) {
            simpleAuthOutput.nextExtensionOrDefault();
            return;
        }
        String token = simpleAuthInput.getConnectPacket().getUserName().orElse("");
        String mac = StandardCharsets.UTF_8.decode(
                simpleAuthInput.getConnectPacket().getPassword().orElse(ByteBuffer.allocate(0))
        ).toString();
        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(mac)) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Username must be set to the token and password must be the MAC/DiSig");
            return;
        }
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = introspectToken(token);
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
        boolean isValidPOP = validatePOP(introspectionResponse, token, mac, simpleAuthInput.getConnectPacket());
        if (!isValidPOP) {
            simpleAuthOutput.failAuthentication(
                    ConnackReasonCode.NOT_AUTHORIZED, "Unable to proof possession of token");
        } else {
            simpleAuthOutput.authenticateSuccessfully();
        }
    }

    IntrospectionResponse introspectToken(@NotNull String token) throws
            ASUnreachableException {
        Map<String, String> body = new HashMap<>(1);
        body.put("token", token);
        return oauthHttpClient.tokenIntrospectionRequest(auth, body);
    }

    boolean validatePOP(IntrospectionResponse introspectionResponse,
                        String token,
                        String mac,
                        ConnectPacket connectPacket) {
        MACCalculator macCalculator = new MACCalculator(introspectionResponse.cnf.jwk.k, token, introspectionResponse.cnf.jwk.alg);
        String calculatedMAC = macCalculator.compute_hmac(connectPacket);
        return calculatedMAC.equals(mac);
    }
}
