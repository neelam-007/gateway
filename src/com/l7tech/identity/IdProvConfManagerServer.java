package com.l7tech.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.internal.imp.InternalIdentityProviderImp;

import java.util.Collection;
import java.util.ArrayList;
import java.sql.SQLException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 20, 2003
 *
 * This IdentityProviderConfigManager is the server side manager who manages the one and only
 * internal identity provider as well as the other providers (ldap) configured by the administrator.
 *
 */
public class IdProvConfManagerServer extends HibernateEntityManager implements IdentityProviderConfigManager {

    // since this provider config is not persisted, we need a special id to identify it for certain operations
    public static final long INTERNALPROVIDER_SPECIAL_OID = -2;

    public IdProvConfManagerServer() {
        super();
        // construct the internal id provider
        internalProvider = new InternalIdentityProviderImp();
        IdentityProviderConfig cfg = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        cfg.setOid(INTERNALPROVIDER_SPECIAL_OID);
        internalProvider.initialize(cfg);
    }

    public IdentityProvider getInternalIdentityProvider() {
        return internalProvider;
    }

    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        if (oid == INTERNALPROVIDER_SPECIAL_OID) return internalProvider.getConfig();
        else try {
            return (IdentityProviderConfig)_manager.findByPrimaryKey( getContext(), getImpClass(), oid );
        } catch ( SQLException se ) {
            throw new FindException( se.toString(), se );
        }
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        // we should not accept saving an internal type
        if (identityProviderConfig.type() != IdentityProviderType.LDAP) throw new SaveException("this type of config cannot be saved");
        try {
            return _manager.save(getContext(), identityProviderConfig);
        } catch (SQLException se) {
            throw new SaveException(se.toString(), se);
        }
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // we should not accept saving an internal type
        if (identityProviderConfig.type() != IdentityProviderType.LDAP) throw new UpdateException("this type of config cannot be updated");
        try {
            _manager.update( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new UpdateException( se.toString(), se );
        }
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        // we should not accept deleting an internal type
        if (identityProviderConfig.type() != IdentityProviderType.LDAP) throw new DeleteException("this type of config cannot be deleted");
        try {
            _manager.delete( getContext(), identityProviderConfig );
        } catch ( SQLException se ) {
            throw new DeleteException( se.toString(), se );
        }
    }

    public Collection findAllIdentityProviders() throws FindException {
        return IdentityProviderFactory.findAllIdentityProviders(this);
    }

    public Collection findAllHeaders() throws FindException {
        // return collection of ldap ones and add the internal one
        // this is an unmodifiable collection so i must construct a new one
        Collection out = new ArrayList(super.findAllHeaders());
        out.add(headerFromConfig(internalProvider.getConfig()));
        return out;
    }

    public Collection findAllHeaders(int offset, int windowSize) throws FindException {
        // return collection of ldap ones and add the internal one
        Collection out = new ArrayList(super.findAllHeaders(offset, windowSize-1));
        out.add(headerFromConfig(internalProvider.getConfig()));
        return out;
    }

    public Collection findAll() throws FindException {
        Collection out = new ArrayList(super.findAll());
        out.add(internalProvider.getConfig());
        return out;
    }

    public Collection findAll(int offset, int windowSize) throws FindException {
        Collection out = new ArrayList(super.findAll(offset, windowSize));
        out.add(internalProvider.getConfig());
        return out;
    }


    public Class getImpClass() {
        return IdentityProviderConfig.class;
    }

    public Class getInterfaceClass() {
        return IdentityProviderConfig.class;
    }

    public String getTableName() {
        return "identity_provider";
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    protected EntityHeader headerFromConfig(IdentityProviderConfig cfg) {
        EntityHeader out = new EntityHeader();
        out.setDescription(cfg.getDescription());
        out.setName(cfg.getName());
        out.setOid(cfg.getOid());
        out.setType(EntityType.ID_PROVIDER_CONFIG);
        return out;
    }
    protected InternalIdentityProviderImp internalProvider;

}
