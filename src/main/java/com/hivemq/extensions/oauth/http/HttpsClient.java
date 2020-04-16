package com.hivemq.extensions.oauth.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.oauth.exceptions.ASUnreachableException;
import com.hivemq.extensions.oauth.exceptions.RSUnauthenticatedException;
import com.hivemq.extensions.oauth.utils.dataclasses.ClientRegistrationRequest;
import com.hivemq.extensions.oauth.utils.dataclasses.ClientRegistrationResponse;
import com.hivemq.extensions.oauth.utils.dataclasses.IntrospectionResponse;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpsClient {
    private final static Logger LOGGER = Logger.getLogger(HttpsClient.class.getName());
    private final EndpointRetriever endpointRetriever;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpsClient(String protocol, String OauthServerAddress, String OauthServerPort) {
        endpointRetriever = new EndpointRetriever(protocol, OauthServerAddress, OauthServerPort);
    }

    @NotNull
    public ClientRegistrationResponse registerClient(@NotNull final ClientRegistrationRequest requestBody) {
        final String stringifiedBody;
        try {
            stringifiedBody = new ObjectMapper().writeValueAsString(requestBody);
        } catch (final JsonProcessingException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        final HttpsURLConnection con;
        try {
            con = getHttpsClient(
                    endpointRetriever.getEndpoint(EndpointRetriever.ASEndpoint.CLIENT_REG),
                    "POST",
                    true);
        } catch (ASUnreachableException e) {
            throw new IllegalStateException("AS should be reachable from RS", e);
        }
        con.setDoOutput(true);
        con.setDoInput(true);
        con.setRequestProperty("Content-Type", "application/json");
        LOGGER.log(
                Level.FINE,
                String.format("Request:\t%s\nHeaders:\t%s\nBody:\t%s", con.toString(), con.getRequestProperties(),
                        stringifiedBody));
        final String responseString;
        try {
            final StringBuilder response = new StringBuilder();
            send(stringifiedBody, con);
            final int responseCode = receive(con, response);
            if ((responseCode / 100) != 2) throw new ASUnreachableException("AS returned error code "+ responseCode);
            responseString = response.toString();
        } catch (ASUnreachableException e) {
            throw new IllegalStateException("AS should be reachable from RS", e);
        } finally {
            con.disconnect();
        }

        final ClientRegistrationResponse registrationResponse;
        try {
            registrationResponse = objectMapper.readValue(responseString, ClientRegistrationResponse.class);
        } catch (final JsonProcessingException e) {
            // Should never happen
            throw new IllegalArgumentException("Failed to parse POST response", e);
        }
        return registrationResponse;
    }

    public @NotNull IntrospectionResponse tokenIntrospectionRequest(@NotNull byte[] authorizationHeader,
                                                                    @NotNull String token)
            throws ASUnreachableException {
        Map<String, String> body = new HashMap<>(1);
        body.put("token", token);
        return this.secureTokenIntrospectionRequest(authorizationHeader, body);
    }

    public @NotNull IntrospectionResponse secureTokenIntrospectionRequest(@NotNull byte[] authorizationHeader,
                                                                          @NotNull Map<String, String> body)
            throws ASUnreachableException {
        String encodedAuth = Base64.getEncoder().encodeToString(authorizationHeader);
        String stringifiedBody;
        try {
            stringifiedBody = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
        } catch (JsonProcessingException e) {
            // Should never happen
            throw new IllegalArgumentException("Failed to parse POST request body", e);
        }
        HttpsURLConnection connection = getHttpsClient(endpointRetriever.getEndpoint(EndpointRetriever.ASEndpoint.TOKEN_INTROSPECTION), "POST", true);
        connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        LOGGER.log(
                Level.FINE,
                String.format("Request:\t%s\nHeaders:\t%s\nBody:\t%s", connection.toString(), connection.getRequestProperties(),
                        stringifiedBody));
        send(stringifiedBody, connection);
        final StringBuilder response = new StringBuilder();
        final int responseCode = receive(connection, response);
        final String responseString = response.toString();
        connection.disconnect();
        if (responseCode != 200) {
            // token introspection failed, invalid token
            throw new RSUnauthenticatedException();
        }
        IntrospectionResponse introspectionResponse;
        try {
            introspectionResponse = objectMapper.readValue(responseString, IntrospectionResponse.class);
        } catch (IOException e) {
            // Should never happen
            throw new IllegalArgumentException("Failed to parse POST response", e);
        }
        return introspectionResponse;
    }

    /**
     * @return SSLSocketFactory that trusts self signed certificates
     */
    private SSLSocketFactory getSocketFactory() {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustManagers = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(final X509Certificate[] certs, final String authType) {
                    }

                    public void checkServerTrusted(final X509Certificate[] certs, final String authType) {
                    }
                }
        };
        // Install the all-trusting trust manager
        final SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustManagers, new java.security.SecureRandom());
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Unable to set up SSL", e);
        }
        return sc.getSocketFactory();
    }

    /**
     * @param target          the target, including the desired endpoint, to connect to
     * @param method          REST method to be performed
     * @param allowSelfSigned allow connecting to servers with self signed certificates
     * @return https client pointing to the AS server token request endpoint
     * @throws ASUnreachableException if an I/O error happens
     */
    private HttpsURLConnection getHttpsClient(final String target, final String method, final boolean allowSelfSigned)
            throws ASUnreachableException {
        final URL url;
        try {
            url = new URL(target);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("AS server URL malformed", e);
        }
        final HttpsURLConnection con;
        try {
            con = (HttpsURLConnection) url.openConnection();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new ASUnreachableException("Unable to contact AS server");
        }
        try {
            con.setRequestMethod(method);
        } catch (final ProtocolException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("GET method unsupported", e);
        }
        if (allowSelfSigned) {
            con.setHostnameVerifier(getInvalidHostnameVerifier());
            con.setSSLSocketFactory(getSocketFactory());
        }
        return con;
    }

    /**
     * @return a hostname verifier that allows invalid hostnames (for self signed certificates)
     */
    private HostnameVerifier getInvalidHostnameVerifier() {
        return (s, sslSession) -> true;
    }

    private int receive(final HttpsURLConnection con, final StringBuilder response) throws ASUnreachableException {
        final int responseCode;
        try (final BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            responseCode = con.getResponseCode();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new ASUnreachableException("Unable to receive request response from AS server");
        }
        LOGGER.log(
                Level.FINE,
                String.format("Response:\t\nHeaders:\t%s\nBody:\t%s", con.getHeaderFields(), response.toString()));
        return responseCode;
    }

    private void send(final String body, final HttpsURLConnection con) throws ASUnreachableException {
        try (final OutputStreamWriter outputStream = new OutputStreamWriter(con.getOutputStream())) {
            outputStream.write(body);
            outputStream.flush();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new ASUnreachableException("Unable to send request to AS server");
        }
    }
}
