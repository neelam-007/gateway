package com.l7tech.server.identity;

import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.common.security.rbac.*;
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
    private RoleManager roleManager;

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

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

    /**
     * Create a new Role for the specified IdentityProviderConfig.
     *
     * @param config  the config for which a new Role is to be created.  Must not be null, and must not already have a Role.
     * @throws SaveException if there was a problem saving the new Role.
     */
    public void addManageProviderRole(IdentityProviderConfig config) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        String name = "Manage " + config.getName() + " Identity Provider (#" + config.getOid() + ")";

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        // RUD this IPC
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.ID_PROVIDER_CONFIG, config.getId());
        newRole.addPermission(OperationType.UPDATE, com.l7tech.common.security.rbac.EntityType.ID_PROVIDER_CONFIG, config.getId());
        newRole.addPermission(OperationType.DELETE, com.l7tech.common.security.rbac.EntityType.ID_PROVIDER_CONFIG, config.getId());

        // CRUD users in this IdP
        newRole.addPermission(OperationType.CREATE, com.l7tech.common.security.rbac.EntityType.USER, "providerId", config.getId());
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.USER, "providerId", config.getId());
        newRole.addPermission(OperationType.UPDATE, com.l7tech.common.security.rbac.EntityType.USER, "providerId", config.getId());
        newRole.addPermission(OperationType.DELETE, com.l7tech.common.security.rbac.EntityType.USER, "providerId", config.getId());

        // CRUD groups in this IdP
        newRole.addPermission(OperationType.CREATE, com.l7tech.common.security.rbac.EntityType.GROUP, "providerId", config.getId());
        newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.GROUP, "providerId", config.getId());
        newRole.addPermission(OperationType.UPDATE, com.l7tech.common.security.rbac.EntityType.GROUP, "providerId", config.getId());
        newRole.addPermission(OperationType.DELETE, com.l7tech.common.security.rbac.EntityType.GROUP, "providerId", config.getId());

        // Assignees will need to search TrustedCerts if this is a FIP
        boolean fip = config.type() == IdentityProviderType.FEDERATED;
        if (fip) {
            newRole.addPermission(OperationType.READ, com.l7tech.common.security.rbac.EntityType.TRUSTED_CERT, null);
        }

        if (currentUser != null) {
            // Check if current user can already do everything this Role will grant
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForEntity(currentUser, config, OperationType.READ, null);
                omnipotent &= roleManager.isPermittedForEntity(currentUser, config, OperationType.UPDATE, null);
                omnipotent &= roleManager.isPermittedForEntity(currentUser, config, OperationType.DELETE, null);

                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.USER, OperationType.CREATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.USER, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.USER, OperationType.UPDATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.USER, OperationType.DELETE);

                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.GROUP, OperationType.CREATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.GROUP, OperationType.READ);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.GROUP, OperationType.UPDATE);
                omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.GROUP, OperationType.DELETE);
                if (fip) omnipotent &= roleManager.isPermittedForAllEntities(currentUser, com.l7tech.common.security.rbac.EntityType.TRUSTED_CERT, OperationType.READ);
            } catch (FindException e) {
                throw new SaveException("Coudln't get existing permissions", e);
            }

            if (!omnipotent) {
                logger.info("Assigning current User to new Role");
                newRole.addAssignedUser(currentUser);
            }
        }

        roleManager.save(newRole);
    }
}
