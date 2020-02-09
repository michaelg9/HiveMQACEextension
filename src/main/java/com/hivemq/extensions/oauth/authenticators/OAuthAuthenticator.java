package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.oauth.ClientRegistry;
import com.hivemq.extensions.oauth.OAuthExtMain;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.http.OauthHttpsClient;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.util.Optional;
import java.util.logging.Logger;

public abstract class OAuthAuthenticator implements EnhancedAuthenticator {
    final static Logger LOGGER = Logger.getLogger(OAuthAuthenticator.class.getName());
    private final ClientRegistry clientRegistry = new ClientRegistry();

    Optional<String> isInputValid(@NotNull ConnectPacket connectPacket) {
        String result = null;
        if (!connectPacket.getCleanStart()) {
            result = "Expecting clean start flag true";
        }
        return Optional.ofNullable(result);
    }

    synchronized TopicPermission registerClient(@NotNull final IntrospectionResponse introspectionResponse) {
        final String clientID = introspectionResponse.getSub();
        clientRegistry.addClient(clientID, introspectionResponse);
        return clientRegistry.getClientPermissions(clientID);
    }

    @NotNull IntrospectionResponse introspectToken(@NotNull final String token) throws ASUnreachableException {
        final String asServer = OAuthExtMain.getServerConfig().getAsServerIP();
        final byte[] clientSecret = OAuthExtMain.getServerConfig().getClientSecrets();
        final String port = OAuthExtMain.getServerConfig().getAsServerPort();
        final String protocol = OAuthExtMain.getServerConfig().getAsServerProtocol();
        OauthHttpsClient oauthHttpsClient = new OauthHttpsClient(protocol, asServer, port);
        return oauthHttpsClient.tokenIntrospectionRequest(clientSecret, token);
    }

}
