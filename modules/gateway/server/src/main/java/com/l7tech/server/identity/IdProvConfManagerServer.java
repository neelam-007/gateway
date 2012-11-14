package com.l7tech.server.identity;

import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ConfigFactory;
import org.springframework.dao.DataAccessException;

import java.text.MessageFormat;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.l7tech.objectmodel.EntityType.*;

/**
 * This IdentityProviderConfigManager is the server side manager who manages the one and only
 * internal identity provider as well as the other providers (ldap & federated) configured by the administrator.
 *
 * @author flascelles
 */
public class IdProvConfManagerServer
    extends HibernateEntityManager<IdentityProviderConfig, EntityHeader>
    implements IdentityProviderConfigManager
{
    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger(IdProvConfManagerServer.class.getName());
    private RoleManager roleManager;

    private static final Pattern replaceRoleName =
        Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, IdentityAdmin.ROLE_NAME_TYPE_SUFFIX));

    public void setRoleManager(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Override
    public long save(IdentityProviderConfig identityProviderConfig) throws SaveException {

        // For the moment don't allow the name etc to be changed
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            fixInternalConfig(identityProviderConfig);
        }

        return super.save(identityProviderConfig);
    }

    @Override
    public void update(IdentityProviderConfig identityProviderConfig) throws UpdateException {
        // For the moment don't allow the name etc to be changed
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            fixInternalConfig(identityProviderConfig);
        } else {
            try {
                roleManager.renameEntitySpecificRoles(ID_PROVIDER_CONFIG, identityProviderConfig, replaceRoleName);
            } catch (FindException e) {
                throw new UpdateException("Couldn't find Role to rename", e);
            }
        }

        try {
            super.update(identityProviderConfig);
        } catch (DataAccessException se) {
            throw new UpdateException(se.toString(), se);
        }
    }

    @Override
    public void delete( long oid ) throws DeleteException, FindException {
        findAndDelete( oid );
    }

    @Override
    public void delete(IdentityProviderConfig identityProviderConfig) throws DeleteException {
        // we should not accept deleting an internal type
        if (identityProviderConfig.type() == IdentityProviderType.INTERNAL) {
            logger.warning("Attempt to delete internal id provider");
            throw new DeleteException("this type of config cannot be deleted");
        }

        try {
            super.delete(identityProviderConfig);
        } catch (DataAccessException se) {
            throw new DeleteException(se.toString(), se);
        }
    }

    @Override
    public Class<IdentityProviderConfig> getImpClass() {
        return IdentityProviderConfig.class;
    }

    @Override
    public Class<IdentityProviderConfig> getInterfaceClass() {
        return IdentityProviderConfig.class;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ID_PROVIDER_CONFIG;
    }

    @Override
    public void createRoles( final IdentityProviderConfig config ) throws SaveException {
        addManageProviderRole( config );
    }

    @Override
    public void updateRoles( final IdentityProviderConfig entity ) throws UpdateException {
        // handled in update method
    }

    @Override
    public void deleteRoles( final long entityOid ) throws DeleteException {
        roleManager.deleteEntitySpecificRoles(ID_PROVIDER_CONFIG, entityOid);
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
    private void addManageProviderRole(IdentityProviderConfig config) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        String name = MessageFormat.format(IdentityAdmin.ROLE_NAME_PATTERN, config.getName(), config.getOid());

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        // RUD this IPC
        newRole.addEntityPermission(OperationType.READ, ID_PROVIDER_CONFIG, config.getId());
        newRole.addEntityPermission(OperationType.UPDATE, ID_PROVIDER_CONFIG, config.getId());
        newRole.addEntityPermission(OperationType.DELETE, ID_PROVIDER_CONFIG, config.getId());

        // CRUD users in this IdP
        newRole.addAttributePermission(OperationType.CREATE, USER, "providerId", config.getId());
        newRole.addAttributePermission(OperationType.READ, USER, "providerId", config.getId());
        newRole.addAttributePermission(OperationType.UPDATE, USER, "providerId", config.getId());
        newRole.addAttributePermission(OperationType.DELETE, USER, "providerId", config.getId());

        // CRUD groups in this IdP
        newRole.addAttributePermission(OperationType.CREATE, GROUP, "providerId", config.getId());
        newRole.addAttributePermission(OperationType.READ, GROUP, "providerId", config.getId());
        newRole.addAttributePermission(OperationType.UPDATE, GROUP, "providerId", config.getId());
        newRole.addAttributePermission(OperationType.DELETE, GROUP, "providerId", config.getId());

        // permission to all keystore
        newRole.addEntityPermission(OperationType.READ, SSG_KEY_ENTRY, null);
        newRole.addEntityPermission(OperationType.READ, SSG_KEYSTORE, null);

        newRole.setEntityType(ID_PROVIDER_CONFIG);
        newRole.setEntityOid(config.getOid());
        newRole.setDescription("Users assigned to the {0} role have the ability to read, update and delete the {1} provider, and create, search, update and delete its users and groups.");

        // Assignees will need to search TrustedCerts if this is a FIP
        boolean fip = config.type() == IdentityProviderType.FEDERATED;
        if (fip) {
            newRole.addEntityPermission(OperationType.READ, TRUSTED_CERT, null);
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

            if (!omnipotent && shouldAutoAssignToNewRole()) {
                logger.info("Assigning current User to new Role");
                newRole.addAssignedUser(currentUser);
            }
        }

        roleManager.save(newRole);
    }

    private boolean shouldAutoAssignToNewRole() {
        return ConfigFactory.getBooleanProperty("rbac.autoRole.manageProvider.autoAssign", true);
    }
}
