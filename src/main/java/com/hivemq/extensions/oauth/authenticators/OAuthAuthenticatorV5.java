package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.ExtendedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.http.OauthHttpClient;
import com.hivemq.extensions.oauth.utils.AuthData;
import com.hivemq.extensions.oauth.utils.Constants;
import com.hivemq.extensions.oauth.utils.ServerConfig;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.EXPIRED_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;
import static com.hivemq.extensions.oauth.utils.StringUtils.bytesToHex;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v5 ACE authentication
 */

public class OAuthAuthenticatorV5 implements SimpleAuthenticator, ExtendedAuthenticator {
    private final static Logger LOGGER = Logger.getLogger(OAuthAuthenticatorV5.class.getName());

    private Map<String, AuthData> authDataMap = new HashMap<>();

    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        LOGGER.log(Level.FINE, String.format("Received client CONNECT:\t%s", simpleAuthInput.getConnectPacket()));
        if (!simpleAuthInput.getConnectPacket().getAuthenticationMethod().orElse("")
                .equalsIgnoreCase(Constants.ACE)) {
            simpleAuthOutput.nextExtensionOrDefault();
            return;
        }
        final String asServer;
        final byte[] clientSecret;
        try {
            asServer = ServerConfig.getConfig().getAsServerIP();
            clientSecret = ServerConfig.getConfig().getClientSecrets();
        } catch (IOException e) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.SERVER_UNAVAILABLE, AUTH_SERVER_UNAVAILABLE);
            return;
        }
        // check for v5 client authenticating with username and password
        if (simpleAuthInput.getConnectPacket().getUserName().isPresent() && simpleAuthInput.getConnectPacket().getPassword().isPresent()) {
            new OAuthAuthenticatorV3().onConnect(simpleAuthInput, simpleAuthOutput);
            return;
        }
        if (simpleAuthInput.getConnectPacket().getAuthenticationData().isEmpty()) {
            //TODO: parameter names? cnonce use?
            simpleAuthOutput.getOutboundUserProperties().addUserProperty("AS", asServer);
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, "Authentication token from provided server expected.");
            return;
        }
        // retrieve token and pop from authentication data
        AuthData authData = new AuthData();
        authData.setTokenAndPop(simpleAuthInput.getConnectPacket().getAuthenticationData().get());
        if (authData.getToken().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.PROTOCOL_ERROR, "Missing or invalid token");
        }

        IntrospectionResponse introspectionResponse;
        try {
            OauthHttpClient oauthHttpClient = new OauthHttpClient();
            introspectionResponse = oauthHttpClient.tokenIntrospectionRequest(clientSecret, authData.getToken().get());
        } catch (ASUnreachableException | IOException e) {
            e.printStackTrace();
            simpleAuthOutput.failAuthentication(ConnackReasonCode.SERVER_UNAVAILABLE, AUTH_SERVER_UNAVAILABLE);
            return;
        }
        if (!introspectionResponse.isActive()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, EXPIRED_TOKEN);
            return;
        }
        boolean isValidPOP = false;
        MACCalculator macCalculator = new MACCalculator(
                introspectionResponse.getCnf().getJwk().getK(),
                introspectionResponse.getCnf().getJwk().getAlg());
        if (authData.getPOP().isPresent()) {
            isValidPOP = macCalculator.isMacValid(authData.getPOP().get(), authData.getTokenAsBytes().get());
        } else {
            byte[] nonce = new byte[32];
            new Random().nextBytes(nonce);
            authData.setPop(macCalculator.compute_hmac(nonce));
            authDataMap.put(simpleAuthInput.getConnectPacket().getClientId(), authData);
            simpleAuthOutput.continueToAuth(nonce);
            return;
        }
        if (!isValidPOP) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, POP_FAILED);
        } else {
            simpleAuthOutput.authenticateSuccessfully();
        }
    }

    public void onAUTH(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        LOGGER.log(Level.FINE, String.format("Received client AUTH:\t%s", simpleAuthInput.getAuthPacket()));
        if (!simpleAuthInput.getAuthPacket().getAuthenticationMethod().equalsIgnoreCase(Constants.ACE)) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_AUTHENTICATION_METHOD, "Should be ACE");
            return;
        }
        if (!authDataMap.containsKey(simpleAuthInput.getConnectPacket().getClientId())) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.PROTOCOL_ERROR, "Need to send CONNECT packet first");
            return;
        }
        AuthData authData = authDataMap.remove(simpleAuthInput.getConnectPacket().getClientId());
        LOGGER.log(Level.FINE, String.format("Received client AUTH:\t%s\n" +
                        "reasonCode:\t%s\n" +
                        "reasonString:\t%s\n" +
                        "method:\t%s\n" +
                        "data:\t%s",
                simpleAuthInput.getAuthPacket(),
                simpleAuthInput.getAuthPacket().getReasonCode(),
                simpleAuthInput.getAuthPacket().getReasonString(),
                simpleAuthInput.getAuthPacket().getAuthenticationMethod(),
                bytesToHex(simpleAuthInput.getAuthPacket().getAuthenticationData())));
        simpleAuthInput.getAuthPacket().getAuthenticationData().rewind();

        byte[] expectedPOP = authData.getPOP().get();
        authData.setPop(simpleAuthInput.getAuthPacket().getAuthenticationData());
        if (authData.getPOP().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.PROTOCOL_ERROR, "POP not found or invalid format.");
            return;
        }
        boolean isValidPOP = Arrays.equals(authData.getPOP().get(), expectedPOP);
        if (isValidPOP) {
            simpleAuthOutput.authenticateSuccessfully();
        } else {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, POP_FAILED);
        }
    }
}
