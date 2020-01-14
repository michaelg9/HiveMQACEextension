package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.ExtendedAuthenticator;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.OAuthExtMain;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.utils.AuthData;
import com.hivemq.extensions.oauth.utils.Constants;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;
import com.hivemq.extensions.oauth.utils.dataclasses.PendingAuthenticationDetails;
import com.nimbusds.jose.JOSEException;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;
import static com.hivemq.extensions.oauth.utils.StringUtils.bytesToHex;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v5 ACE authentication
 */

public class OAuthAuthenticatorV5 extends OAuthAuthenticator implements SimpleAuthenticator, ExtendedAuthenticator {
    private Map<String, PendingAuthenticationDetails> nonceMap = new ConcurrentHashMap<>();

    @Override
    boolean isInputValid(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        if (!super.isInputValid(simpleAuthInput, simpleAuthOutput)) {
            return false;
        }
        if (simpleAuthInput.getConnectPacket().getSessionExpiryInterval() != 0) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.PAYLOAD_FORMAT_INVALID, "Session expiry interval should be 0");
            return false;
        }
        if (!isAuthenticationMethodValid(simpleAuthInput)) {
            simpleAuthOutput.nextExtensionOrDefault();
            return false;
        }

        if (simpleAuthInput.getConnectPacket().getAuthenticationData().isEmpty() &&
                simpleAuthInput.getConnectPacket().getUserName().isEmpty() &&
                simpleAuthInput.getConnectPacket().getPassword().isEmpty()) {
            // missing authentication data, AS server discovery
            final String asServer = OAuthExtMain.getServerConfig().getAsServerIP();
            //TODO: parameter names? cnonce use?
            simpleAuthOutput.getOutboundUserProperties().addUserProperty("AS", asServer);
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, "Authentication token from provided server expected.");
            return false;
        }
        return (simpleAuthInput.getConnectPacket().getAuthenticationData().isPresent() &&
                simpleAuthInput.getConnectPacket().getAuthenticationData().get().remaining() > 2) ||
                simpleAuthInput.getConnectPacket().getUserName().isPresent() && simpleAuthInput.getConnectPacket().getPassword().isPresent();
    }

    @Override
    @Nullable AuthData parseAuthData(@NotNull SimpleAuthInput simpleAuthInput,
                                     @NotNull SimpleAuthOutput simpleAuthOutput) {
        AuthData authData = null;
        if (simpleAuthInput.getConnectPacket().getUserName().isPresent() && simpleAuthInput.getConnectPacket().getPassword().isPresent()) {
            // v5 client authenticating with username and password
            authData = new AuthData();
            authData.setToken(simpleAuthInput.getConnectPacket().getUserName().get());
            authData.setPop(simpleAuthInput.getConnectPacket().getPassword().get());
        } else if (simpleAuthInput.getConnectPacket().getAuthenticationData().isPresent() &&
                simpleAuthInput.getConnectPacket().getAuthenticationData().get().remaining() > 2) {
            // authenticating with extended method, retrieve token and maybe pop from authentication data
            authData = new AuthData();
            authData.setTokenAndPop(simpleAuthInput.getConnectPacket().getAuthenticationData().get());
        }
        if (authData != null && authData.getToken().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.PROTOCOL_ERROR, "Missing or invalid token");
            authData = null;
        }
        return authData;
    }

    @Override
    void proceedToChallenge(@NotNull IntrospectionResponse introspectionResponse,
                            @NotNull MACCalculator macCalculator,
                            @NotNull SimpleAuthOutput simpleAuthOutput) {
        final byte[] nonce = generateChallengeString();
        final String clientID = introspectionResponse.getSub();
        byte[] expectedPOP;
        try {
            expectedPOP = macCalculator.signNonce(nonce);
        } catch (JOSEException e) {
            e.printStackTrace();
            simpleAuthOutput.failAuthentication(ConnackReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "Algorithm not supported");
            return;
        }
        PendingAuthenticationDetails details = new PendingAuthenticationDetails(introspectionResponse, expectedPOP);
        nonceMap.put(clientID, details);
        simpleAuthOutput.continueToAuth(nonce);
    }

    private boolean isAuthenticationMethodValid(@NotNull SimpleAuthInput simpleAuthInput) {
        return simpleAuthInput.getConnectPacket().getAuthenticationMethod().isPresent() &&
                Constants.ACE.equalsIgnoreCase(simpleAuthInput.getConnectPacket().getAuthenticationMethod().get());
    }

    private byte[] generateChallengeString() {
        byte[] challenge = new byte[32];
        new Random().nextBytes(challenge);
        return challenge;
    }

    public void onAUTH(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        LOGGER.log(Level.FINE, String.format("Received client AUTH:\t%s", simpleAuthInput.getAuthPacket()));
        if (!simpleAuthInput.getAuthPacket().getAuthenticationMethod().equalsIgnoreCase(Constants.ACE)) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_AUTHENTICATION_METHOD, "Should be ACE");
            return;
        }
        final String clientID = simpleAuthInput.getConnectPacket().getClientId();
        if (!nonceMap.containsKey(clientID)) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.PROTOCOL_ERROR, "Need to send CONNECT packet first");
            return;
        }
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
        final PendingAuthenticationDetails details = nonceMap.remove(clientID);
        byte[] expectedPOP = details.getExpectedPOP();
        AuthData authData = new AuthData();
        authData.setPop(simpleAuthInput.getAuthPacket().getAuthenticationData());
        if (authData.getPOP().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.PROTOCOL_ERROR, "POP not found or invalid format.");
            return;
        }
        boolean isValidPOP = Arrays.equals(authData.getPOP().get(), expectedPOP);
        if (isValidPOP) {
            authenticateClient(simpleAuthOutput, details.getIntrospectionResponse());
        } else {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, POP_FAILED);
        }
    }
}
