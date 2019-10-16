package com.hivemq.extensions.oauth.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;

public class OauthHttpClient {
    @NotNull
    private EndpointRetriever endpointRetriever;
    private ObjectMapper objectMapper = new ObjectMapper();
    @NotNull
    private java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .version(java.net.http.HttpClient.Version.HTTP_1_1)
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
//                .connectTimeout(Duration.ofSeconds(20))
//                .proxy(ProxySelector.of(new InetSocketAddress("proxy.example.com", 80)))
//                .authenticator(java.net.Authenticator.)
            .build();

    public OauthHttpClient(String OauthServerAddress, String OauthServerPort) {
        endpointRetriever = new EndpointRetriever("http", OauthServerAddress, OauthServerPort);
    }


    public @Nullable IntrospectionResponse tokenIntrospectionRequest(String authorizationHeader,
                                                                     Map<String, String> body) throws ASUnreachableException {
        String encodedAuth = Base64.getEncoder().encodeToString((authorizationHeader).getBytes());
        String stringifiedBody;
        try {
            stringifiedBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            // Should never happen
            throw new IllegalArgumentException("Failed to parse POST request body", e);
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointRetriever.getTokenIntrospectionEndpoint()))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(stringifiedBody))
                .setHeader("Authorization", "Basic " + encodedAuth)
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            // unable to contact AS server
            throw new ASUnreachableException("Unable to contact AS server");
        }
        if (response.statusCode() != 200) {
            // token introspection failed, invalid token
            return null;
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
