package com.l7tech.gateway.common.security;

/** Describes a type of specially-marked key that can be located by {@link com.l7tech.gateway.common.security.TrustedCertAdmin#findDefaultKey(SpecialKeyType)}. */
public enum SpecialKeyType {
    /** Represents a key marked as the default SSL key. */
    SSL(false),

    /** Represents a key marked as the default CA key. */
    CA(false),

    /** Represents a key marked as the default audit viewer/decryption key. */
    AUDIT_VIEWER(true),

    /** Represents a key marked as the default audit signing key. */
    AUDIT_SIGNING(false);

    private static final long serialVersionUID = 932856867393187255L;
    private final boolean restrictedAccess;

    SpecialKeyType(boolean restrictedAccess) {
        this.restrictedAccess = restrictedAccess;
    }

    public boolean isRestrictedAccess() {
        return restrictedAccess;
    }
}
