package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.ClientRegistry;
import com.hivemq.extensions.oauth.OAuthExtMain;
import com.hivemq.extensions.oauth.crypto.MACCalculator;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.http.OauthHttpClient;
import com.hivemq.extensions.oauth.utils.AuthData;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.EXPIRED_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;

public abstract class OAuthAuthenticator implements SimpleAuthenticator {
    final static Logger LOGGER = Logger.getLogger(OAuthAuthenticator.class.getName());
    private final ClientRegistry clientRegistry = new ClientRegistry();

    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        LOGGER.log(Level.FINE, String.format("Received client CONNECT:\t%s", simpleAuthInput.getConnectPacket()));
        AuthData authData = parseAuthData(simpleAuthInput, simpleAuthOutput);
        if (authData == null) return;
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = introspectToken(authData.getToken().get());
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
                introspectionResponse.getCnf().getJwk().getAlg());
        if (authData.getPOP().isEmpty()) {
            proceedToChallenge(introspectionResponse, macCalculator, simpleAuthOutput);
            return;
        }
        boolean isValidPOP = macCalculator.isMacValid(authData.getPOP().get(), authData.getTokenAsBytes().get());
        if (isValidPOP) {
            authenticateClient(simpleAuthOutput, introspectionResponse);
        } else {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, POP_FAILED);
        }
    }

    abstract @Nullable AuthData parseAuthData(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput);

    abstract void proceedToChallenge(@NotNull final IntrospectionResponse introspectionResponse, @NotNull final MACCalculator macCalculator, @NotNull SimpleAuthOutput simpleAuthOutput);

    synchronized TopicPermission registerClient(@NotNull final IntrospectionResponse introspectionResponse) {
        final String clientID = introspectionResponse.getSub();
        clientRegistry.addClient(clientID, introspectionResponse);
        return clientRegistry.getClientPermissions(clientID);
    }

    @NotNull IntrospectionResponse introspectToken(@NotNull final String token) throws ASUnreachableException {
        final String asServer = OAuthExtMain.getServerConfig().getAsServerIP();
        final byte[] clientSecret = OAuthExtMain.getServerConfig().getClientSecrets();
        final String port = OAuthExtMain.getServerConfig().getAsServerPort();
        OauthHttpClient oauthHttpClient = new OauthHttpClient(asServer, port);
        return oauthHttpClient.tokenIntrospectionRequest(clientSecret, token);
    }

    void authenticateClient(@NotNull SimpleAuthOutput simpleAuthOutput, @NotNull final IntrospectionResponse introspectionResponse) {
        TopicPermission permission = registerClient(introspectionResponse);
        simpleAuthOutput.getDefaultPermissions().add(permission);
        simpleAuthOutput.authenticateSuccessfully();
    }

}
