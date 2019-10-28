package com.hivemq.extensions.oauth.http;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EndpointRetrieverTest {
    private EndpointRetriever endpointRetriever;

    @Test
    void getTokenIntrospectionEndpoint() {
        endpointRetriever = new EndpointRetriever("http", "1.1.1.1", "30");
        assertEquals(endpointRetriever.getEndpoint(EndpointRetriever.ASEndpoint.TOKEN_INTROSPECTION),
                "http://1.1.1.1:30/api/rs/introspect");
    }
}