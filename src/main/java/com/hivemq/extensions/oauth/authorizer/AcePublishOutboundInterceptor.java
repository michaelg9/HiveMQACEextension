package com.hivemq.extensions.oauth.authorizer;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extensions.oauth.authenticators.ACEAuthenticator;

public class AcePublishOutboundInterceptor implements com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor {
    @Override
    public void onOutboundPublish(@NotNull PublishOutboundInput publishOutboundInput, @NotNull PublishOutboundOutput publishOutboundOutput) {
        final String clientID = publishOutboundInput.getClientInformation().getClientId();
        if (!ACEAuthenticator.getClientRegistry().isClientAuthenticated(clientID)) {
            publishOutboundOutput.preventPublishDelivery();
            final ClientService clientService = Services.clientService();
            clientService.disconnectClient(clientID, false, DisconnectReasonCode.NOT_AUTHORIZED, "expired token");
        }
    }
}
