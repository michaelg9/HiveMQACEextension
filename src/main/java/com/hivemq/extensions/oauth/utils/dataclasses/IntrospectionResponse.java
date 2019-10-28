package com.hivemq.extensions.oauth.utils.dataclasses;

public final class IntrospectionResponse {
    private boolean active;
    private String profile;
    private String exp;
    private String sub;
    private String aud;
    private String[] scope;
    private CNF cnf;

    public boolean isActive() {
        return active;
    }

    public String getProfile() {
        return profile;
    }

    public String getExp() {
        return exp;
    }

    public String getSub() {
        return sub;
    }

    public String getAud() {
        return aud;
    }

    public String[] getScope() {
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
        private String k;

        public String getAlg() {
            return alg;
        }

        public String getKty() {
            return kty;
        }

        public String getK() {
            return k;
        }

    }

}
