package com.l7tech.external.assertions.mongodb;

/**
 * Created by mobna01 on 07/01/15.
 */
public enum MongoDBEncryption {

    NO_ENCRYPTION("None"),
    SSL("Certificate Based"),
    X509_Auth("Certificate and Key Based");

    private String displayName;

    private MongoDBEncryption(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}