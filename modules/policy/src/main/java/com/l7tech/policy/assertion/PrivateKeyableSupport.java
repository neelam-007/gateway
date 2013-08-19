package com.l7tech.policy.assertion;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.migration.Migration;
import com.l7tech.objectmodel.migration.MigrationMappingSelection;
import com.l7tech.objectmodel.migration.PropertyResolver;

import java.io.Serializable;

/**
 * Support class for PrivateKeyable.
 */
public class PrivateKeyableSupport implements OptionalPrivateKeyable, Serializable {

    //- PUBLIC

    public PrivateKeyableSupport() {
        this(false);
    }

    public PrivateKeyableSupport( boolean usesNoKeyAllowed ) {
        this.usesNoKeyAllowed = usesNoKeyAllowed;
    }

    public PrivateKeyableSupport( final PrivateKeyableSupport privateKeyableSupport ) {
        this(privateKeyableSupport != null && privateKeyableSupport.isUsesNoKeyAllowed());
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
    public Goid getNonDefaultKeystoreId() {
        return nonDefaultKeystoreId;
    }

    @Override
    public void setNonDefaultKeystoreId( Goid nonDefaultKeystoreId ) {
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

    @Override
    public boolean isUsesNoKeyAllowed() {
        return usesNoKeyAllowed;
    }

    @Override
    public boolean isUsesNoKey() {
        return usesNoKey;
    }

    @Override
    public void setUsesNoKey(boolean usesNoKey) {
        this.usesNoKey = usesNoKey;
    }

    public void copyFrom( final PrivateKeyableSupport other ) {
        this.usesDefaultKeyStore = other.usesDefaultKeyStore;
        this.nonDefaultKeystoreId = other.nonDefaultKeystoreId;
        this.keyAlias = other.keyAlias;
        this.usesNoKey = other.usesNoKey;
    }

    //- PRIVATE

    private final boolean usesNoKeyAllowed;

    private boolean usesDefaultKeyStore = true;
    private Goid nonDefaultKeystoreId;
    private String keyAlias;
    private boolean usesNoKey = false;

}
