package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * This IdentityProviderConfigManager is the server side manager who manages the one and only
 * internal identity provider as well as the other providers (ldap & federated) configured by the administrator.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 20, 2003
 */
public class IdProvConfManagerServer
    extends HibernateEntityManager<IdentityProviderConfig, EntityHeader>
    implements IdentityProviderConfigManager
{
    private static final Logger logger = Logger.getLogger(IdProvConfManagerServer.class.getName());

    public IdentityProviderConfig findByPrimaryKey(long oid) throws FindException {
        if (oid == INTERNALPROVIDER_SPECIAL_OID)
            return internalProvider.getConfig();
        else
            return super.findByPrimaryKey(oid);
    }

    /**
     * @param oid the identity provider id to look for
     * @return the identoty provider for a given id, or <code>null</code>
     * @throws FindException if there was an persistence error
     */
    public IdentityProvider getIdentityProvider(long oid) throws FindException {
        return identityProviderFactory.getProvider(oid);
    }

    public void test(IdentityProviderConfig identityProviderConfig)
      throws InvalidIdProviderCfgException {
        IdentityProvider provider = identityProviderFactory.makeProvider(identityProviderConfig);
        provider.test();
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {
        // we should not accept saving an internal type
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            logger.warning("Attempt to save internal id provider");
            throw new SaveException("this type of config cannot be saved");
        }

        return super.save(identityProviderConfig);
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // we should not accept saving an internal type
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            logger.warning("Attempt to update internal id provider");
            throw new UpdateException("this type of config cannot be updated");
        }
        try {
            identityProviderFactory.dropProvider(identityProviderConfig);
            super.update(identityProviderConfig);
        } catch (DataAccessException se) {
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
            identityProviderFactory.dropProvider(identityProviderConfig);
            super.delete(identityProviderConfig);
        } catch (DataAccessException se) {
            throw new DeleteException(se.toString(), se);
        }
    }

    public Collection<IdentityProvider> findAllIdentityProviders() throws FindException {
        return identityProviderFactory.findAllIdentityProviders(this);
    }

    public Collection<IdentityProviderConfig> findAll() throws FindException {
        Collection<IdentityProviderConfig> out = new ArrayList<IdentityProviderConfig>(super.findAll());
        out.add(internalProvider.getConfig());
        return out;
    }

    public Collection<IdentityProviderConfig> findAll(int offset, int windowSize) throws FindException {
        Collection<IdentityProviderConfig> out = new ArrayList<IdentityProviderConfig>(super.findAll(offset, windowSize));
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

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        return ldapTemplateManager.getTemplates();
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public EntityType getEntityType() {
        return EntityType.ID_PROVIDER_CONFIG;
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

    protected InternalIdentityProvider internalProvider;
    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();

    private IdentityProviderFactory identityProviderFactory;

    protected void initDao() throws Exception {
        // construct the internal id provider
        IdentityProviderConfig cfg = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        cfg.setName("Internal Identity Provider");
        cfg.setDescription("Internal Identity Provider");
        cfg.setOid(INTERNALPROVIDER_SPECIAL_OID);
        internalProvider = (InternalIdentityProvider)identityProviderFactory.makeProvider(cfg);

    }
}
