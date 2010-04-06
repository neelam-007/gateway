package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.io.Serializable;

/**
 * Support class for PrivateKeyable.
 */
public class PrivateKeyableSupport implements PrivateKeyable, Serializable {

    //- PUBLIC

    public PrivateKeyableSupport() {
    }

    public PrivateKeyableSupport( final PrivateKeyableSupport privateKeyableSupport ) {
        if ( privateKeyableSupport != null ) {
            copyFrom( privateKeyableSupport );
        }
    }

    @Override
    public String getKeyAlias() {
        return keyAlias;
    }

    @Override
    public void setKeyAlias( String keyAlias ) {
        this.keyAlias = keyAlias;
    }

    @Override
    @Migration(mapName = MigrationMappingSelection.REQUIRED, export = false, resolver = PropertyResolver.Type.SSGKEY)
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
