package com.hivemq.extensions.oauth.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.exceptions.RSUnauthenticatedException;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static com.hivemq.extensions.oauth.utils.Constants.AUTHORIZATION_HEADER;
import static com.hivemq.extensions.oauth.utils.Constants.CONTENT_TYPE;
import static com.hivemq.extensions.oauth.utils.Constants.CONTENT_TYPE_APP_JSON;
import static com.hivemq.extensions.oauth.utils.Constants.ErrorMessages.AUTH_SERVER_UNAVAILABLE;

public class OauthHttpClient {
    @NotNull
    private EndpointRetriever endpointRetriever;
    private ObjectMapper objectMapper = new ObjectMapper();
    private java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();

    public OauthHttpClient(String protocol, String OauthServerAddress, String OauthServerPort) {
        endpointRetriever = new EndpointRetriever(protocol, OauthServerAddress, OauthServerPort);
    }

    public OauthHttpClient() {
        endpointRetriever = new EndpointRetriever("http", "127.0.0.1", "3001");
    }

    public @NotNull IntrospectionResponse tokenIntrospectionRequest(@NotNull String authorizationHeader,
                                                             @NotNull String token)
            throws ASUnreachableException {
        Map<String, String> body = new HashMap<>(1);
        body.put("token", token);
        return this.tokenIntrospectionRequest(authorizationHeader, body);
    }

    public @NotNull IntrospectionResponse tokenIntrospectionRequest(@NotNull String authorizationHeader,
                                                                     @NotNull Map<String, String> body)
            throws ASUnreachableException {
        String encodedAuth = Base64.getEncoder().encodeToString((authorizationHeader).getBytes());
        String stringifiedBody;
        try {
            stringifiedBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            // Should never happen
            throw new IllegalArgumentException("Failed to parse POST request body", e);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointRetriever.getEndpoint(EndpointRetriever.ASEndpoint.TOKEN_INTROSPECTION)))
                .header(CONTENT_TYPE, CONTENT_TYPE_APP_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(stringifiedBody))
                .setHeader(AUTHORIZATION_HEADER, "Basic " + encodedAuth)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            // unable to contact AS server
            throw new ASUnreachableException(AUTH_SERVER_UNAVAILABLE);
        }
        if (response.statusCode() != 200) {
            // token introspection failed, invalid token
            throw new RSUnauthenticatedException();
        }
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = objectMapper.readValue(response.body(), IntrospectionResponse.class);
        } catch (JsonProcessingException e) {
            // Should never happen
            throw new IllegalArgumentException("Failed to parse POST response", e);
        }
        return introspectionResponse;
    }
}
