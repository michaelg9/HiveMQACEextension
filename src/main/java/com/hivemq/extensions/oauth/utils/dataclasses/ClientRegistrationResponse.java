package com.hivemq.extensions.oauth.utils.dataclasses;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public final class ClientRegistrationResponse {
    @JsonProperty("client_name")
    private String clientName;
    @JsonProperty("client_uri")
    private String clientUri;
    @JsonProperty("client_id")
    private String clientID;
    @JsonProperty("client_secret")
    private byte[] clientSecret;
    @JsonProperty("client_id_issued_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date issuedAt;
    @JsonProperty("client_secret_expires_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date expiresAt;

    public String getClientName() {
        return clientName;
    }

    public String getClientUri() {
        return clientUri;
    }

    public String getClientID() {
        return clientID;
    }

    public byte[] getClientSecret() {
        return clientSecret;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }
}
