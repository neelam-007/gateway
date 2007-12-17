package com.l7tech.server.identity;

import static com.l7tech.common.security.rbac.EntityType.*;
import com.l7tech.common.security.rbac.*;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.dao.DataAccessException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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

    private static final Pattern replaceRoleName =
        Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, IdentityAdmin.ROLE_NAME_TYPE_SUFFIX));

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
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
        provider.test(false);
    }

    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {

        // For the moment don't allow the name etc to be changed
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            fixInternalConfig(identityProviderConfig);
        }

        return super.save(identityProviderConfig);
    }

    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // For the moment don't allow the name etc to be changed
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            fixInternalConfig(identityProviderConfig);
        } else {
            try {
                roleManager.renameEntitySpecificRole(ID_PROVIDER_CONFIG, identityProviderConfig, replaceRoleName);
            } catch (FindException e) {
                throw new UpdateException("Couldn't find Role to rename", e);
            }
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
        return out;
    }

    public Collection<IdentityProviderConfig> findAll(int offset, int windowSize) throws FindException {
        Collection<IdentityProviderConfig> out = new ArrayList<IdentityProviderConfig>(super.findAll(offset, windowSize));
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

    private final LdapConfigTemplateManager ldapTemplateManager = new LdapConfigTemplateManager();

    private IdentityProviderFactory identityProviderFactory;

    private void fixInternalConfig(IdentityProviderConfig cfg) {
        cfg.setName("Internal Identity Provider");
        cfg.setDescription("Internal Identity Provider");
    }

    /**
     * Create a new Role for the specified IdentityProviderConfig.
     *
     * @param config  the config for which a new Role is to be created.  Must not be null, and must not already have a Role.
     * @throws SaveException if there was a problem saving the new Role.
     */
    public void addManageProviderRole(IdentityProviderConfig config) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        String name = MessageFormat.format(IdentityAdmin.ROLE_NAME_PATTERN, config.getName(), config.getOid());

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        // RUD this IPC
        newRole.addPermission(OperationType.READ, ID_PROVIDER_CONFIG, config.getId());
        newRole.addPermission(OperationType.UPDATE, ID_PROVIDER_CONFIG, config.getId());
        newRole.addPermission(OperationType.DELETE, ID_PROVIDER_CONFIG, config.getId());

        // CRUD users in this IdP
        newRole.addPermission(OperationType.CREATE, USER, "providerId", config.getId());
        newRole.addPermission(OperationType.READ, USER, "providerId", config.getId());
        newRole.addPermission(OperationType.UPDATE, USER, "providerId", config.getId());
        newRole.addPermission(OperationType.DELETE, USER, "providerId", config.getId());

        // CRUD groups in this IdP
        newRole.addPermission(OperationType.CREATE, GROUP, "providerId", config.getId());
        newRole.addPermission(OperationType.READ, GROUP, "providerId", config.getId());
        newRole.addPermission(OperationType.UPDATE, GROUP, "providerId", config.getId());
        newRole.addPermission(OperationType.DELETE, GROUP, "providerId", config.getId());
        newRole.setEntityType(ID_PROVIDER_CONFIG);
        newRole.setEntityOid(config.getOid());
        newRole.setDescription("Users assigned to the {0} role have the ability to read, update and delete the {1} provider, and create, search, update and delete its users and groups.");

        // Assignees will need to search TrustedCerts if this is a FIP
        boolean fip = config.type() == IdentityProviderType.FEDERATED;
        if (fip) {
            newRole.addPermission(OperationType.READ, TRUSTED_CERT, null);
        }

        if (currentUser != null) {
            // Check if current user can already do everything this Role will grant
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForEntity(currentUser, config, OperationType.READ, null);
                omnipotent &= roleManager.isPermittedForEntity(currentUser, config, OperationType.UPDATE, null);
                omnipotent &= roleManager.isPermittedForEntity(currentUser, config, OperationType.DELETE, null);

                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.CREATE, USER);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.READ, USER);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.UPDATE, USER);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.DELETE, USER);

                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.CREATE, GROUP);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.READ, GROUP);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.UPDATE, GROUP);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.DELETE, GROUP);
                if (fip) omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, OperationType.READ, TRUSTED_CERT);
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
