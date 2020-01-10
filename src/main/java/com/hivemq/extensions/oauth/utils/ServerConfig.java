package com.hivemq.extensions.oauth.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ServerConfig {
    private final String asServerIP;
    private final String asServerPort;
    private final String clientID;
    private final String clientSecret;
    private ServerConfig(String asServerIP, String asServerPort, String clientID, String clientSecret) {
        this.asServerIP = asServerIP;
        this.asServerPort = asServerPort;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    public String getAsServerIP() {
        return asServerIP;
    }

    public String getAsServerPort() {
        return asServerPort;
    }

    public byte[] getClientSecrets() {
        return (clientID + ":" + clientSecret).getBytes();
    }

    public static ServerConfig getConfig() {
        Properties properties = readConfig();
        return new ServerConfig(properties.getProperty("ASServerIP"), properties.getProperty("ASServerPort"),
                properties.getProperty("ClientID"), properties.getProperty("ClientSecret"));
    }

    private static Properties readConfig() {
        try (final InputStream input = ServerConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            final Properties prop = new Properties();
            if (input == null) {
                throw new IOException("Unable to find config.properties");
            }
            prop.load(input);
            return prop;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
