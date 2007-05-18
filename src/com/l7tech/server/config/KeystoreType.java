package com.l7tech.server.config;

/**
 * User: megery
 * Date: Aug 29, 2006
 * Time: 1:32:46 PM
 */
public enum KeystoreType {
    DEFAULT_KEYSTORE_NAME("Default (PKCS12) Keystore", "PKCS12"),
    LUNA_KEYSTORE_NAME("Luna Keystore", "Luna"),
    SCA6000_KEYSTORE_NAME("Internal Hardware Security Module", "PKCS11"),
    NO_KEYSTORE("", ""),
    UNDEFINED("", ""),
    ;
    private String ksName;
    private String shortTypeName;

    KeystoreType(String ksName, String shortKeystoreTypeName) {
        this.ksName = ksName;
        this.shortTypeName = shortKeystoreTypeName;
    }

    public String getName() {
        return ksName;
    }

    public String toString() {
        return ksName;
    }

    public String shortTypeName() {
        return shortTypeName;
    }
}
