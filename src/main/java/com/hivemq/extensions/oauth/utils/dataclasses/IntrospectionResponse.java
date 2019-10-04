package com.hivemq.extensions.oauth.utils.dataclasses;

public class IntrospectionResponse {
    public boolean active;
    public String profile;
    public String exp;
    public String sub;
    public String aud;
    public String[] scope;
    public CNF cnf;

    static class CNF {
        public JWK jwk;
    }

    static class JWK {
        public String alg;
        public String kty;
        public String k;
    }

}
