package com.hivemq.extensions.oauth.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public final class ServerConfig {
    private final String asServerProtocol = "https";
    @NotNull
    private final String asServerIP;
    @NotNull
    private final String asServerPort;
    @NotNull
    private final String clientID;
    @NotNull
    private final String clientSecret;

    private ServerConfig(@NotNull final String asServerIP, @NotNull final String asServerPort, @NotNull final String clientID, @NotNull final String clientSecret) {
        this.asServerIP = asServerIP;
        this.asServerPort = asServerPort;
        this.clientID = clientID;
        this.clientSecret = clientSecret;
    }

    public @NotNull String getAsServerProtocol() {
        return asServerProtocol;
    }

    public @NotNull String getAsServerIP() {
        return asServerIP;
    }

    public @NotNull String getAsServerPort() {
        return asServerPort;
    }

    public @Nullable URL getAsServerURI() {
        try {
            return new URL(asServerProtocol, asServerIP, Integer.parseInt(asServerPort), "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
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
