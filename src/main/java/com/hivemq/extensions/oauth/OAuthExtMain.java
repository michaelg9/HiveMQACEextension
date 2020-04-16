/*
 * Copyright 2018 dc-square GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hivemq.extensions.oauth;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.parameter.ExtensionInformation;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extensions.oauth.authenticators.ACEAuthenticatorProvider;
import com.hivemq.extensions.oauth.authorizer.AceAuthorizerProvider;
import com.hivemq.extensions.oauth.authorizer.AcePublishOutboundInterceptor;
import com.hivemq.extensions.oauth.http.HttpsClient;
import com.hivemq.extensions.oauth.utils.ServerConfig;
import com.hivemq.extensions.oauth.utils.dataclasses.ClientRegistrationRequest;
import com.hivemq.extensions.oauth.utils.dataclasses.ClientRegistrationResponse;

import java.util.logging.Logger;

import static com.hivemq.extensions.oauth.utils.Constants.BROKER_CONFIG_DIR;
import static com.hivemq.extensions.oauth.utils.Constants.HTTPS_PROTOCOL;

/**
 * This is the main class of the extension,
 * which is instantiated either during the HiveMQ start up process (if extension is enabled)
 * or when HiveMQ is already started by enabling the extension.
 *
 * @author Florian Limpöck
 * @since 4.0.0
 */
public class OAuthExtMain implements ExtensionMain {
    final static Logger LOGGER = Logger.getLogger(ExtensionMain.class.getName());
    private static ServerConfig serverConfig;

    public OAuthExtMain() {
    }

    @Override
    public void extensionStart(final @NotNull ExtensionStartInput extensionStartInput, final @NotNull ExtensionStartOutput extensionStartOutput) {
        String configDir = System.getenv(BROKER_CONFIG_DIR);
        if (configDir == null) {
            throw new IllegalStateException("Unable to find broker configuration");
        }
        serverConfig = ServerConfig.getConfig(configDir);
        if (!serverConfig.doesASinfoExist()) {
            throw new IllegalStateException("AS information missing");
        }
        if (!serverConfig.isBrokerRegistered()) {
            if (!serverConfig.canRegisterToAS()) throw new IllegalStateException("Missing client credentials");
            ClientRegistrationResponse response = new HttpsClient(HTTPS_PROTOCOL, serverConfig.getAsServerIP(), serverConfig.getAsServerPort())
                    .registerClient(new ClientRegistrationRequest(serverConfig.getClientUsername(), serverConfig.getClientUri()));
            serverConfig.setClientID(response.getClientID(), true);
            serverConfig.setClientSecret(response.getClientSecret(), true);
        }
        Services.securityRegistry().setEnhancedAuthenticatorProvider(new ACEAuthenticatorProvider());
        Services.securityRegistry().setAuthorizerProvider(new AceAuthorizerProvider());
        try {

            Services.initializerRegistry().setClientInitializer((initializerInput, clientContext) -> clientContext.addPublishOutboundInterceptor(new AcePublishOutboundInterceptor()));

        } catch (Exception e) {
            LOGGER.warning("Exception thrown at extension start: " + e);
        }
    }

    @Override
    public void extensionStop(final @NotNull ExtensionStopInput extensionStopInput, final @NotNull ExtensionStopOutput extensionStopOutput) {
        final ExtensionInformation extensionInformation = extensionStopInput.getExtensionInformation();
        LOGGER.info("Stopped " + extensionInformation.getName() + ":" + extensionInformation.getVersion());
    }

    public static ServerConfig getServerConfig() {
        return serverConfig;
    }
}
