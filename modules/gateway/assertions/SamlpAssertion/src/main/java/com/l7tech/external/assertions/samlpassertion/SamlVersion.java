package com.l7tech.external.assertions.samlpassertion;

/**
 * Typesafe enum for SAML version (1.1 or 2.0).
 */
public enum SamlVersion {
    SAML2(2, "2.0"), SAML1_1(1, "1.1");

    SamlVersion(int versionInt, String versionString) {
        this.versionInt = versionInt;
        this.versionString = versionString;
    }

    private final int versionInt;
    private final String versionString;

    /**
     * @return version integer -- 1 for SAML 1.1, 2 for SAML 2.0
     */
    public int getVersionInt() {
        return versionInt;
    }

    @Override
    public String toString() {
        return versionString;
    }

    public static SamlVersion valueOf(int versionInt) {
        switch (versionInt) {
            case 1:
                return SAML1_1;
            case 2:
                return SAML2;
            default:
                throw new IllegalArgumentException("Unrecognized SAML version integer: " + versionInt);
        }
    }
}
