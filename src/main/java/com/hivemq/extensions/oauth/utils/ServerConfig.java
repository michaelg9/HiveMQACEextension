package com.hivemq.extensions.oauth.utils;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

import static com.hivemq.extensions.oauth.utils.Constants.AS_SERVER_IP;
import static com.hivemq.extensions.oauth.utils.Constants.AS_SERVER_PORT;
import static com.hivemq.extensions.oauth.utils.Constants.AS_SERVER_PROTOCOL;
import static com.hivemq.extensions.oauth.utils.Constants.CLIENT_ID;
import static com.hivemq.extensions.oauth.utils.Constants.CLIENT_SECRET;
import static com.hivemq.extensions.oauth.utils.Constants.CLIENT_URI;
import static com.hivemq.extensions.oauth.utils.Constants.CLIENT_USERNAME;
import static com.hivemq.extensions.oauth.utils.Constants.CONFIG_DIR;
import static com.hivemq.extensions.oauth.utils.Constants.HTTPS_PROTOCOL;
import static com.hivemq.extensions.oauth.utils.Constants.LOCAL_CONFIG_FILENAME;
import static com.hivemq.extensions.oauth.utils.Constants.PUBLIC_AS_SERVER_IP;

public final class ServerConfig {
    private final static Logger LOGGER = Logger.getLogger(ServerConfig.class.getName());
    @NotNull
    private final String configDir;
    @NotNull
    private final String asServerIP;
    @Nullable
    private final String publicASServerIP;
    @NotNull
    private final String asServerPort;
    @NotNull
    private String asServerProtocol;
    @NotNull
    private String clientID;
    @NotNull
    private byte[] clientSecret;
    @Nullable
    private String clientUsername;
    @Nullable
    private String clientUri;
    @NotNull final Properties properties;

    private ServerConfig(@NotNull Properties properties) {
        this.properties = properties;
        this.configDir = properties.getProperty(CONFIG_DIR);
        this.asServerIP = properties.getProperty(AS_SERVER_IP);
        this.publicASServerIP = properties.getProperty(PUBLIC_AS_SERVER_IP);
        this.asServerPort = properties.getProperty(AS_SERVER_PORT);
        this.clientID = properties.getProperty(CLIENT_ID);
        this.clientSecret = properties.getProperty(CLIENT_SECRET).getBytes();
        this.clientUri = properties.getProperty(CLIENT_URI);
        this.clientUsername = properties.getProperty(CLIENT_USERNAME);
        this.asServerProtocol = properties.getProperty(AS_SERVER_PROTOCOL, HTTPS_PROTOCOL);
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
        final String ip =  publicASServerIP == null ? asServerProtocol : publicASServerIP;
        try {
            return new URL(asServerProtocol, ip, Integer.parseInt(asServerPort), "");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public @NotNull boolean doesASinfoExist() {
        return this.asServerIP != null && this.asServerPort != null;
    }

    public @NotNull boolean canRegisterToAS() {
        return this.clientUsername != null && this.clientUri != null;
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public String getClientUri() {
        return clientUri;
    }

    public void setClientID(String clientID, boolean persist) {
        this.clientID = clientID;
        this.properties.setProperty(CLIENT_ID, this.clientID);
        if (persist) persist();
    }

    public void setClientSecret(byte[] clientSecret, boolean persist) {
        this.clientSecret = clientSecret;
        this.properties.setProperty(CLIENT_SECRET, new String(this.clientSecret));
        if (persist) persist();
    }

    private boolean persist() {
        try (final OutputStream out = new FileOutputStream(Paths.get(configDir, LOCAL_CONFIG_FILENAME).toString())) {
            properties.store(out, "---Modified---");
        } catch (final IOException e) {
            LOGGER.severe("Unable to persist configuration: " + e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public @NotNull boolean isBrokerRegistered() {
        return clientID != null && clientSecret != null;
    }

    public byte[] getClientSecrets() {
        return (clientID + ":" + new String(clientSecret)).getBytes();
    }

    public static ServerConfig getConfig(@NotNull final String configDir) {
        Properties properties = readConfig(configDir);
        properties.setProperty(CONFIG_DIR, configDir);
        return new ServerConfig(properties);
    }

    private static Properties readConfig(@NotNull final String configDir) {
        try (final InputStream input = new FileInputStream(Path.of(configDir, LOCAL_CONFIG_FILENAME).toString())) {
            final Properties prop = new Properties();
            prop.load(input);
            return prop;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
