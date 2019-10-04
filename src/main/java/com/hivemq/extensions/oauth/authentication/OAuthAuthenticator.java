package com.hivemq.extensions.oauth.authentication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extensions.oauth.utils.EndpointRetriever;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Michaelides
 */

public class OAuthAuthenticator implements SimpleAuthenticator {
    @NotNull
    private EndpointRetriever apiRetriever = new EndpointRetriever("http", "127.0.0.1", "3001");
    @NotNull
    private HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
//                .connectTimeout(Duration.ofSeconds(20))
//                .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
//                .authenticator(java.net.Authenticator.)
            .build();

    @Override
    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        String auth = "g0uZrCzzonfyIU3R:td5l3yHaHHsOYcuhdO0MnEqIlpElf9q0301jRk6B7oM=";
        String encodedAuth = Base64.getEncoder().encodeToString((auth).getBytes());
        String token = simpleAuthInput.getConnectPacket().getUserName().orElse("");
        String pop = StandardCharsets.UTF_8.
                decode(simpleAuthInput.getConnectPacket().getPassword().orElse(ByteBuffer.allocate(0))).toString();
        if (isEmpty(token) || isEmpty(pop)) {
            simpleAuthOutput.nextExtensionOrDefault();
            return;
        }
        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        ObjectMapper m = new ObjectMapper();
        HttpResponse<String> response = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiRetriever.getTokenIntrospectionEndpoint()))
//                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(m.writerWithDefaultPrettyPrinter().writeValueAsString(body)))
                    .setHeader("Authorization", "Basic " + encodedAuth)
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        IntrospectionResponse r = null;
        try {
            r = m.readValue(response.body(), IntrospectionResponse.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (r != null && r.active) {
            simpleAuthOutput.authenticateSuccessfully();
        } else {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Invalid or expired token");
        }
    }

    @NotNull
    private boolean isEmpty(@Nullable String text) {
        return text == null || "".equals(text);
    }
}
