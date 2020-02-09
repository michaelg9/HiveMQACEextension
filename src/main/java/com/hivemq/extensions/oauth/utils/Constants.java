package com.hivemq.extensions.oauth.utils;

public final class Constants {
    public static final String ACE = "ace";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_APP_JSON = "application/json";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AS_SERVER_PROTOCOL = "ASServerProtocol";
    public static final String LOCAL_CONFIG_FILENAME = "config.properties";
    public static final String CONFIG_DIR = "ConfigDir";
    public static final String AS_SERVER_IP = "ASServerIP";
    public static final String PUBLIC_AS_SERVER_IP = "PublicASServerIP";
    public static final String AS_SERVER_PORT = "ASServerPort";
    public static final String CLIENT_ID = "ClientID";
    public static final String CLIENT_SECRET = "ClientSecret";
    public static final String CLIENT_URI = "ClientUri";
    public static final String CLIENT_USERNAME = "ClientUsername";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String BROKER_CONFIG_DIR = "BROKER_CONFIG_DIR";

    public static final class ErrorMessages {
        public static final String AUTH_SERVER_UNAVAILABLE = "Authorization server is unavailable. Please try later";
        public static final String EXPIRED_TOKEN = "Token expired.";
        public static final String MISSING_TOKEN = "Authentication token expected when using ACE authentication";
        public static final String POP_FAILED = "Unable to proof possession of token";
        public static final String USERNAME_PASSWORD_MISSING = "Username must be set to access token and password must be the MAC/DiSig";

    }
}
