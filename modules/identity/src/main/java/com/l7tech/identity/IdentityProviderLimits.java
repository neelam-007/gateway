package com.l7tech.identity;

/**
 * Contains a list of constants used across different user/password policy management dialogs
 */
public enum IdentityProviderLimits {
    /**
     * Maximum supported user id length
     */
    MAX_ID_LENGTH(128),
    /**
     * Minimum allowed password length
     */
    MIN_PASSWORD_LENGTH(3),
    /**
     * Maximum supported password length
     */
    MAX_PASSWORD_LENGTH(128),
    /**
     * Maximum supported X509 Subject DN length
     */
    MAX_X509_SUBJECT_DN_LENGTH(255),
    /**
     * Maximum supported email length
     */
    MAX_EMAIL_LENGTH(128),
    /**
     * Maximum supported group id/group name length
     */
    MAX_GROUP_ID_LENGTH(128);

   
    private final int value;
    /**
     * Instantiate IdentityProviderLimits object
     */
    private IdentityProviderLimits( int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }

}
