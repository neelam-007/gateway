package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.fed.FederatedIdentityProvider;
import com.l7tech.server.identity.internal.InternalIdentityProviderServer;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * This IdentityProviderConfigManager is the server side manager who manages the one and only
 * internal identity provider as well as the other providers (ldap & federated) configured by the administrator.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 20, 2003
 *
 */
public class IdProvConfManagerServer extends HibernateEntityManager implements IdentityProviderConfigManager {

    public IdProvConfManagerServer() {
        super();
        // construct the internal id provider
        internalProvider = new InternalIdentityProviderServer();
        IdentityProviderConfig cfg = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        cfg.setName("Internal Identity Provider");
        cfg.setDescription("Internal Identity Provider");
        cfg.setOid(INTERNALPROVIDER_SPECIAL_OID);
        internalProvider.initialize(cfg);
    }

    public IdentityProvider getInternalIdentityProvider() {
        return internalProvider;
    }

    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        if (oid == INTERNALPROVIDER_SPECIAL_OID)
            return internalProvider.getConfig();
        else
            try {
                return (IdentityProviderConfig)PersistenceManager.findByPrimaryKey(getContext(), getImpClass(), oid);
            } catch (SQLException se) {
                throw new FindException(se.toString(), se);
            }
    }

    /**
     * @param oid the identity provider id to look for
     * @return the identoty provider for a given id, or <code>null</code>
     * @throws FindException if there was an persistence error 
     */
    public IdentityProvider getIdentityProvider(long oid) throws FindException {
        return IdentityProviderFactory.getProvider(oid);
    }

    public void test(IdentityProviderConfig identityProviderConfig)
                                throws InvalidIdProviderCfgException {
        IdentityProviderType type = identityProviderConfig.type();
        if ( type == IdentityProviderType.INTERNAL ) {
            if (identityProviderConfig.getOid() != INTERNALPROVIDER_SPECIAL_OID) {
                logger.warning("Testing an internal id provider with no good oid. Throwing InvalidIdProviderCfgException");
                throw new InvalidIdProviderCfgException("This internal ID provider config is not valid.");
            }
        } else if ( type.equals(IdentityProviderType.LDAP)) {
            LdapIdentityProvider tmpProvider = new LdapIdentityProvider();
            tmpProvider.initialize(identityProviderConfig);
            tmpProvider.test();
        } else if ( type.equals(IdentityProviderType.FEDERATED)) {
            FederatedIdentityProvider tmpProvider = new FederatedIdentityProvider();
            tmpProvider.initialize(identityProviderConfig);
            tmpProvider.test();
        } else {
            String msg = "Unsupported IdentityProviderConfig type: " + type;
            logger.severe(msg);
            throw new InvalidIdProviderCfgException(msg );
        }
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        // we should not accept saving an internal type
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL ) {
            logger.warning("Attempt to save internal id provider");
            throw new SaveException("this type of config cannot be saved");
        }
        /*if (!LdapConfigSettings.isValidConfigObject(identityProviderConfig)) {
            // not the food additive
            String msg = "This IdentityProviderConfig object does not meet the requirements for the IdentityProviderType.LDAP type.";
            logger.warning("Attempt to save invalid ldap id provider config. " + msg);
            throw new SaveException(msg);
        }*/
/*
        if (!identityProviderConfig.type().equals(IdentityProviderType.LDAP) ||
                !(identityProviderConfig instanceof LdapIdentityProviderConfig)) {
            // not the food additive
            String msg = "This IdentityProviderConfig object is not supported";
            logger.warning("Attempt to save invalid ldap id provider config. " + msg);
            throw new SaveException(msg);
        }
*/

        // first, check that there isn't an existing provider with same name
        try {
            List existingProvidersWithSameName = PersistenceManager.find(getContext(), "from " + getTableName() +
                                                                                       " in class " + getImpClass().getName() +
                                                                                       " where " + getTableName() + ".name = ?",
                                                                            identityProviderConfig.getName(), String.class);
            if (existingProvidersWithSameName != null && !(existingProvidersWithSameName.isEmpty())) {
                logger.fine("sending error back to requestor because existing provider with same name exists");
                throw new SaveException("The name " + identityProviderConfig.getName() + " is already used by " +
                                        "another id provider.");
            }
        } catch (SQLException e) {
            logger.log(Level.INFO, "problem trying to check for provider with same name as " +
                                    identityProviderConfig.getName(), e);
        } catch (FindException e) {
            logger.log(Level.INFO, "problem trying to check for provider with same name as " +
                                    identityProviderConfig.getName(), e);
        }

        // then, try to save it
        try {
            return PersistenceManager.save(getContext(), identityProviderConfig);
        } catch (SQLException se) {
            throw new SaveException(se.toString(), se);
        }
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // we should not accept saving an internal type
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            logger.warning("Attempt to update internal id provider");
            throw new UpdateException("this type of config cannot be updated");
        }
        try {
            IdentityProviderFactory.dropProvider(identityProviderConfig);
            PersistenceManager.update(getContext(), identityProviderConfig);
        } catch (SQLException se) {
            throw new UpdateException(se.toString(), se);
        }
    }

    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        // we should not accept deleting an internal type
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            logger.warning("Attempt to delete internal id provider");
            throw new DeleteException("this type of config cannot be deleted");
        }
        try {
            IdentityProviderFactory.dropProvider(identityProviderConfig);
            PersistenceManager.delete(getContext(), identityProviderConfig);
        } catch (SQLException se) {
            throw new DeleteException(se.toString(), se);
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
        Collection out = new ArrayList(super.findAllHeaders(offset, windowSize - 1));
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
        return LdapIdentityProviderConfig.class;
    }

    public Class getInterfaceClass() {
        return IdentityProviderConfig.class;
    }

    public String getTableName() {
        return "identity_provider";
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        return ldapTemplateManager.getTemplates();
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

    protected InternalIdentityProviderServer internalProvider;
    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();
}
