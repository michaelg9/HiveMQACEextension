package com.hivemq.extensions.oauth.utils.dataclasses;

public final class IntrospectionResponse {
    public boolean active;
    public String profile;
    public String exp;
    public String sub;
    public String aud;
    public String[] scope;
    public CNF cnf;

    public static class CNF {
        public JWK jwk;
    }

    public static class JWK {
        public String alg;
        public String kty;
        public String k;
    }

}
