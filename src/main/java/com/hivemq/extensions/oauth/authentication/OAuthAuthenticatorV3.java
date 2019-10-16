package com.hivemq.extensions.oauth.authentication;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
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
    private final String auth = "g0uZrCzzonfyIU3R:td5l3yHaHHsOYcuhdO0MnEqIlpElf9q0301jRk6B7oM=";
    private OauthHttpClient oauthHttpClient = new OauthHttpClient("127.0.0.1", "3001");

    @Override
    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput,
                          @NotNull SimpleAuthOutput simpleAuthOutput) {
        MqttVersion mqttVersion = simpleAuthInput.getConnectPacket().getMqttVersion();
        String token = simpleAuthInput.getConnectPacket().getUserName().orElse("");
        String pop = StandardCharsets.UTF_8.
                decode(simpleAuthInput.getConnectPacket().getPassword().orElse(ByteBuffer.allocate(0))).toString();
        if (mqttVersion.equals(MqttVersion.V_3_1_1)) {
            authenticate(simpleAuthOutput, token, pop);
        } else {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.UNSUPPORTED_PROTOCOL_VERSION,
                    String.format("Unsupported version %s", mqttVersion.toString()));
        }
    }

    void authenticate(@NotNull SimpleAuthOutput simpleAuthOutput,
                      @NotNull String token,
                      @NotNull String pop) {
        if (StringUtils.isEmpty(token) || StringUtils.isEmpty(pop)) {
            simpleAuthOutput.nextExtensionOrDefault();
            return;
        }
        Map<String, String> body = new HashMap<>(1);
        body.put("token", token);
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = oauthHttpClient.tokenIntrospectionRequest(auth, body);
        } catch (ASUnreachableException e) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.SERVER_UNAVAILABLE, e.getLocalizedMessage());
            return;
        }
        if (introspectionResponse == null) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Invalid token specified");
            return;
        }
        if (!introspectionResponse.active) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Expired token specified");
            return;
        }
        simpleAuthOutput.authenticateSuccessfully();
    }
}
