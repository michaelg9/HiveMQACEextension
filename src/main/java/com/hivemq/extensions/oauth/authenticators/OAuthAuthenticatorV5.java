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
import com.hivemq.extensions.oauth.OAuthExtMain;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.utils.AuthData;
import com.hivemq.extensions.oauth.utils.Constants;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;
import com.hivemq.extensions.oauth.utils.dataclasses.PendingAuthenticationDetails;
import com.nimbusds.jose.JOSEException;

import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.EXPIRED_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;
import static com.hivemq.extensions.oauth.utils.StringUtils.bytesToHex;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v5 ACE authentication
 */

public class OAuthAuthenticatorV5 extends OAuthAuthenticator implements EnhancedAuthenticator {
    private Map<String, PendingAuthenticationDetails> nonceMap = new ConcurrentHashMap<>();

    @Override
    public void onConnect(@NotNull EnhancedAuthConnectInput enhancedAuthConnectInput, @NotNull EnhancedAuthOutput enhancedAuthOutput) {
        ConnectPacket connectPacket = enhancedAuthConnectInput.getConnectPacket();
        LOGGER.log(Level.FINE, String.format("Received client CONNECT:\t%s", connectPacket));
        if (isASDiscovery(connectPacket)) {
            final URL asServerUri = OAuthExtMain.getServerConfig().getAsServerURI();
            final String asServer = asServerUri == null ? "" : asServerUri.toString();
            //TODO: parameter names? cnonce use?
            enhancedAuthOutput.getOutboundUserProperties().addUserProperty("AS", asServer);
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, "Authentication token from provided server expected.");
            return;
        }
        Optional<String> errors = isInputValid(connectPacket);
        if (errors.isPresent()) {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.PAYLOAD_FORMAT_INVALID, errors.get());
            return;
        }
        AuthData authData = parseAuthData(connectPacket);
        if (authData == null) {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.PAYLOAD_FORMAT_INVALID, "Authentication data malformed");
            return;
        }
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = super.introspectToken(authData.getToken().get());
        } catch (ASUnreachableException e) {
            LOGGER.severe("Unable to access AS at " + OAuthExtMain.getServerConfig().getAsServerURI());
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.SERVER_UNAVAILABLE, AUTH_SERVER_UNAVAILABLE);
            return;
        }
        if (!introspectionResponse.isActive()) {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, EXPIRED_TOKEN);
            return;
        }
        String clientid = enhancedAuthConnectInput.getClientInformation().getClientId();
        if (!clientid.equals(introspectionResponse.getSub())) {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, "Invalid Client ID");
            return;
        }
        MACCalculator macCalculator = new MACCalculator(
                introspectionResponse.getCnf().getJwk().getK(),
                introspectionResponse.getCnf().getJwk().getAlg());
        if (authData.getPOP().isEmpty()) {
            final byte[] nonce = generateChallengeString();
            byte[] expectedPOP;
            try {
                expectedPOP = macCalculator.signNonce(nonce);
            } catch (JOSEException e) {
                e.printStackTrace();
                enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "POP algorithm not supported");
                return;
            }
            PendingAuthenticationDetails details = new PendingAuthenticationDetails(introspectionResponse, expectedPOP);
            nonceMap.put(clientid, details);
            ByteBuffer buf = ByteBuffer.allocate(nonce.length+2);
            buf.putShort((short) nonce.length);
            buf.put(nonce);
            enhancedAuthOutput.continueAuthentication(buf);
            return;
        }
        boolean isValidPOP = macCalculator.isMacValid(authData.getPOP().get(), authData.getTokenAsBytes().get());
        if (isValidPOP) {
            authenticateClient(enhancedAuthOutput, introspectionResponse);
        } else {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, POP_FAILED);
        }
    }

    boolean isACEAuthMethod(@NotNull ConnectPacket connectPacket) {
        return connectPacket.getAuthenticationMethod().isPresent() && connectPacket.getAuthenticationMethod().get().equalsIgnoreCase(Constants.ACE);
    }

    Optional<String> isInputValid(@NotNull ConnectPacket connectPacket) {
        Optional<String> errors = super.isInputValid(connectPacket);
        if (errors.isPresent()) {
            return errors;
        }
        String reason = null;
        if (!isACEAuthMethod(connectPacket)) {
            reason = "Should be ACE";
        }
        if (connectPacket.getSessionExpiryInterval() != 0) {
            reason = "Session expiry interval should be 0";
        }
        if ((connectPacket.getAuthenticationData().isEmpty() || connectPacket.getAuthenticationData().get().remaining() <= 2) &&
                (connectPacket.getUserName().isEmpty() || connectPacket.getPassword().isEmpty())) {
            reason = "Malformed Authentication data";
        }
        return Optional.ofNullable(reason);
    }

    private boolean isASDiscovery(ConnectPacket connectPacket) {
        return (isACEAuthMethod(connectPacket) &&
                connectPacket.getAuthenticationData().isEmpty() &&
                connectPacket.getUserName().isEmpty() &&
                connectPacket.getPassword().isEmpty());
    }

    private @Nullable AuthData parseAuthData(@NotNull ConnectPacket connectPacket) {
        AuthData authData = null;
        if (connectPacket.getUserName().isPresent() && connectPacket.getPassword().isPresent()) {
            // v5 client authenticating with username and password
            authData = new AuthData();
            authData.setToken(connectPacket.getUserName().get());
            authData.setPop(connectPacket.getPassword().get());
        } else if (connectPacket.getAuthenticationData().isPresent() &&
                connectPacket.getAuthenticationData().get().remaining() > 2) {
            // authenticating with extended method, retrieve token and maybe pop from authentication data
            authData = new AuthData();
            authData.setTokenAndPop(connectPacket.getAuthenticationData().get());
        }
        if (authData != null && authData.getToken().isEmpty()) {
            authData = null;
        }
        return authData;
    }

    private byte[] generateChallengeString() {
        byte[] challenge = new byte[32];
        new Random().nextBytes(challenge);
        return challenge;
    }

    @Override
    public void onAuth(@NotNull EnhancedAuthInput enhancedAuthInput, @NotNull EnhancedAuthOutput enhancedAuthOutput) {
        LOGGER.log(Level.FINE, String.format("Received client AUTH:\t%s", enhancedAuthInput.getAuthPacket()));
        if (!enhancedAuthInput.getAuthPacket().getAuthenticationMethod().equalsIgnoreCase(Constants.ACE)) {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.BAD_AUTHENTICATION_METHOD, "Should be ACE");
            return;
        }
        final String clientID = enhancedAuthInput.getClientInformation().getClientId();
        if (enhancedAuthInput.getAuthPacket().getAuthenticationData().isEmpty()) {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, "Authentication data expected");
            return;
        }

        LOGGER.log(Level.FINE, String.format("Received client AUTH:\t%s\n" +
                        "reasonCode:\t%s\n" +
                        "reasonString:\t%s\n" +
                        "method:\t%s\n" +
                        "data:\t%s",
                enhancedAuthInput.getAuthPacket(),
                enhancedAuthInput.getAuthPacket().getReasonCode(),
                enhancedAuthInput.getAuthPacket().getReasonString(),
                enhancedAuthInput.getAuthPacket().getAuthenticationMethod(),
                bytesToHex(enhancedAuthInput.getAuthPacket().getAuthenticationData().get())));

        final PendingAuthenticationDetails details = nonceMap.remove(clientID);
        byte[] expectedPOP = details.getExpectedPOP();
        AuthData authData = new AuthData();
        authData.setPop(enhancedAuthInput.getAuthPacket().getAuthenticationData().get());
        if (authData.getPOP().isEmpty()) {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, "POP not found or invalid format.");
            return;
        }
        boolean isValidPOP = Arrays.equals(authData.getPOP().get(), expectedPOP);
        if (isValidPOP) {
            authenticateClient(enhancedAuthOutput, details.getIntrospectionResponse());
        } else {
            enhancedAuthOutput.failAuthentication(DisconnectedReasonCode.NOT_AUTHORIZED, POP_FAILED);
        }
    }

    void authenticateClient(@NotNull EnhancedAuthOutput enhancedAuthOutput, @NotNull final IntrospectionResponse introspectionResponse) {
        TopicPermission permission = super.registerClient(introspectionResponse);
        enhancedAuthOutput.getDefaultPermissions().add(permission);
        enhancedAuthOutput.authenticateSuccessfully();
    }

    @Override
    public void onReAuth(@NotNull EnhancedAuthInput enhancedAuthInput, @NotNull EnhancedAuthOutput enhancedAuthOutput) {

    }
}
