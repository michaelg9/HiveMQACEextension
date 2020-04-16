package com.hivemq.extensions.oauth.authenticators;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.EnhancedAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extensions.oauth.ClientRegistry;
import com.hivemq.extensions.oauth.OAuthExtMain;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.http.HttpsClient;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.util.Optional;
import java.util.logging.Logger;

public abstract class ACEAuthenticator implements EnhancedAuthenticator {
    final static Logger LOGGER = Logger.getLogger(ACEAuthenticator.class.getName());
    private static final ClientRegistry clientRegistry = new ClientRegistry();

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
        HttpsClient httpsClient = new HttpsClient(protocol, asServer, port);
        return httpsClient.tokenIntrospectionRequest(clientSecret, token);
    }

    public static ClientRegistry getClientRegistry() {
        return clientRegistry;
    }

}
