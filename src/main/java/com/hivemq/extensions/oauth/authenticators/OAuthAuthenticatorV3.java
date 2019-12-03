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
import com.hivemq.extensions.oauth.utils.ServerConfig;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.EXPIRED_TOKEN;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.POP_FAILED;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.USERNAME_PASSWORD_MISSING;
import static com.hivemq.extensions.oauth.utils.StringUtils.hexStringToByteArray;

/**
 * @author Michael Michaelides
 * OAuth2 authenticator for ACE.
 * Supports v3.1.1 ACE authentication
 */

public class OAuthAuthenticatorV3 implements SimpleAuthenticator {
    private final static Logger LOGGER = Logger.getLogger(OAuthAuthenticatorV3.class.getName());

    @Override
    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput,
                          @NotNull SimpleAuthOutput simpleAuthOutput) {
        LOGGER.log(Level.FINE, String.format("Received CONNECT:\t%s", simpleAuthInput.getConnectPacket()));
        AuthData authData = new AuthData();
        if (simpleAuthInput.getConnectPacket().getUserName().isPresent() && simpleAuthInput.getConnectPacket().getPassword().isPresent()) {
            authData.setToken(simpleAuthInput.getConnectPacket().getUserName().get());
            authData.setPop(simpleAuthInput.getConnectPacket().getPassword().get());
        }
        if (authData.getToken().isEmpty() || authData.getPOP().isEmpty()) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD, USERNAME_PASSWORD_MISSING);
            return;
        }
        IntrospectionResponse introspectionResponse;
        try {
            final OauthHttpClient oauthHttpClient = new OauthHttpClient();
            introspectionResponse = oauthHttpClient.tokenIntrospectionRequest(ServerConfig.getConfig().getClientSecrets(), authData.getToken().get());
        } catch (ASUnreachableException|IOException e) {
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
        boolean isValidPOP = macCalculator.isMacValid(authData.getPOP().get(), authData.getToken().get().getBytes());
        if (!isValidPOP) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.NOT_AUTHORIZED, POP_FAILED);
        } else {
            simpleAuthOutput.authenticateSuccessfully();
        }
    }
}
