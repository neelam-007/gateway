package com.l7tech.server.config;

/**
 * User: megery
 * Date: Aug 29, 2006
 * Time: 1:32:46 PM
 */
public enum KeystoreType {
    DEFAULT_KEYSTORE_NAME("Default (PKCS12) Keystore"),
    LUNA_KEYSTORE_NAME("Luna Keystore"),
    NO_KEYSTORE(""),
    UNDEFINED(""),
    ;
    private String ksName;

    KeystoreType(String ksName) {
        this.ksName = ksName;
    }

    public String getName() {
        return ksName;
    }

    public String toString() {
        return ksName;
    }
}
