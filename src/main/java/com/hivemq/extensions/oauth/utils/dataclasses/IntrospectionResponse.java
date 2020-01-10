package com.hivemq.extensions.oauth.utils.dataclasses;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;

public final class IntrospectionResponse {
    public enum SCOPE {
        sub,
        pub;
    }
    private boolean active;
    private String profile;
    private long exp;
    private String sub;
    private String aud;
    private SCOPE[] scope;
    private CNF cnf;

    public boolean isActive() {
        return active;
    }

    public String getProfile() {
        return profile;
    }

    public long getExp() {
        return exp;
    }

    public String getSub() {
        return sub;
    }

    public String getAud() {
        return aud;
    }

    public SCOPE[] getScope() {
        return scope;
    }

    public CNF getCnf() {
        return cnf;
    }

    public static class CNF {
        private JWK jwk;
        public JWK getJwk() {
            return jwk;
        }
    }

    public static class JWK {
        private String alg;
        private String kty;
        private byte[] k;

        public String getAlg() {
            return alg;
        }

        public String getKty() {
            return kty;
        }

        public byte[] getK() {
            return k;
        }
    }

    public @NotNull TopicPermission.MqttActivity parseScope() {
        TopicPermission.MqttActivity result = null;
        for (final IntrospectionResponse.SCOPE action: scope) {
            if (action.equals(IntrospectionResponse.SCOPE.sub)) {
                result = result == null ? TopicPermission.MqttActivity.SUBSCRIBE : TopicPermission.MqttActivity.ALL;
            }
            if (action.equals(IntrospectionResponse.SCOPE.pub)) {
                result = result == null ? TopicPermission.MqttActivity.PUBLISH : TopicPermission.MqttActivity.ALL;
            }
        }
        if (result == null) throw new IllegalStateException("Scope should be either sub or pub or both");
        return result;
    }
}
