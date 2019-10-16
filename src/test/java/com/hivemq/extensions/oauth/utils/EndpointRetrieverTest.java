package com.hivemq.extensions.oauth.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EndpointRetrieverTest {
    private EndpointRetriever endpointRetriever;

    @Test
    void getTokenIntrospectionEndpoint() {
        endpointRetriever = new EndpointRetriever("http", "1.1.1.1", "30");
        assertEquals(endpointRetriever.getTokenIntrospectionEndpoint(), "http://1.1.1.1:30/api/rs/introspect");
    }
}