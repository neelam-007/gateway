package com.l7tech.identity.ldap;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderFactory;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 20, 2003
 *
 */
public class LdapIdentityProviderConfigManager  extends HibernateEntityManager implements IdentityProviderConfigManager {
    public LdapIdentityProviderConfigManager() {
        super();
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        try {
            _manager.delete( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public Collection findAllIdentityProviders() throws FindException {
        //return IdentityProviderFactory.findAllIdentityProviders(this);
        // todo
        return null;
    }

    public IdentityProviderConfig findByPrimaryKey( long oid ) throws FindException {
        try {
            return (IdentityProviderConfig)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public long save( IdentityProviderConfig identityProviderConfig ) throws SaveException {
        try {
            return _manager.save( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new SaveException( se.toString(), se );
        }
    }

    public void update( IdentityProviderConfig identityProviderConfig ) throws UpdateException {
        try {
            _manager.update( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }


    public Collection findAll(int offset, int windowSize) throws FindException {
        throw new IllegalArgumentException( "Not yet implemented!" );
    }

    public Class getImpClass() {
        return LdapIdentityProviderConfig.class;
    }

    public Class getInterfaceClass() {
        return IdentityProviderConfig.class;
    }

    public String getTableName() {
        return "ldap_identity_provider";
    }

}