package com.hivemq.extensions.oauth.authorizer;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.PublishAuthorizer;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.PublishAuthorizerOutput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extension.sdk.api.packets.general.MqttVersion;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.subscribe.SubackReasonCode;
import com.hivemq.extensions.oauth.authenticators.ACEAuthenticator;

public class Authorizer implements PublishAuthorizer, SubscriptionAuthorizer {

    @Override
    public void authorizePublish(@NotNull PublishAuthorizerInput publishAuthorizerInput, @NotNull PublishAuthorizerOutput publishAuthorizerOutput) {
        final String clientID = publishAuthorizerInput.getClientInformation().getClientId();
        final boolean isClientTokenValid = ACEAuthenticator.getClientRegistry().isClientAuthenticated(clientID);
        final MqttVersion version = publishAuthorizerInput.getConnectionInformation().getMqttVersion();
        if (!isClientTokenValid) {
            if (version.equals(MqttVersion.V_5)) {
                // refuse but do not disconnect.
                // TODO: This will actually also disconnect them after refusing
                publishAuthorizerOutput.failAuthorization(AckReasonCode.NOT_AUTHORIZED);
            } else {
                // refuse and disconnect
                publishAuthorizerOutput.disconnectClient();
            }
        } else {
            publishAuthorizerOutput.nextExtensionOrDefault();
        }
    }

    @Override
    public void authorizeSubscribe(@NotNull SubscriptionAuthorizerInput subscriptionAuthorizerInput, @NotNull SubscriptionAuthorizerOutput subscriptionAuthorizerOutput) {
        final boolean isClientTokenValid = ACEAuthenticator.getClientRegistry().isClientAuthenticated(subscriptionAuthorizerInput.getClientInformation().getClientId());
        final MqttVersion version = subscriptionAuthorizerInput.getConnectionInformation().getMqttVersion();
        if (!isClientTokenValid) {
            if (version.equals(MqttVersion.V_5)) {
                subscriptionAuthorizerOutput.failAuthorization(SubackReasonCode.NOT_AUTHORIZED);
            } else {
                subscriptionAuthorizerOutput.disconnectClient();
            }
        } else {
            subscriptionAuthorizerOutput.nextExtensionOrDefault();
        }
    }
}
