package com.l7tech.policy.assertion;

/**
 * Support class for PrivateKeyable.
 */
public class PrivateKeyableSupport implements PrivateKeyable {

    //- PUBLIC

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    public void setKeyAlias( String keyAlias ) {
        this.keyAlias = keyAlias;
    }

    @Override
    public long getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId( long nonDefaultKeystoreId ) {
        this.nonDefaultKeystoreId = nonDefaultKeystoreId;
    }

    @Override
    public boolean isUsesDefaultKeyStore() {
        return usesDefaultKeyStore;
    }

    @Override
    public void setUsesDefaultKeyStore( boolean usesDefaultKeyStore ) {
        this.usesDefaultKeyStore = usesDefaultKeyStore;
    }

    public void copyFrom( final PrivateKeyableSupport other ) {
        this.usesDefaultKeyStore = other.usesDefaultKeyStore;
        this.nonDefaultKeystoreId = other.nonDefaultKeystoreId;
        this.keyAlias = other.keyAlias;
    }

    //- PRIVATE

    private boolean usesDefaultKeyStore = true;
    private long nonDefaultKeystoreId;
    private String keyAlias;

}
