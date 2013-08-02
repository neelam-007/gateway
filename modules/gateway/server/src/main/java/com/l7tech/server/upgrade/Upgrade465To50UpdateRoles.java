/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.EntityFolderAncestryPredicate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.identity.IdProvConfManagerServer;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.EntityType.*;

/**
 * A database upgrade task that adds folder roles to the database.
 */
public class Upgrade465To50UpdateRoles implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade465To50UpdateRoles.class.getName());
    private ApplicationContext applicationContext;

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    private Object getBean( final String name, final Class beanClass ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        return applicationContext.getBean(name, beanClass);
    }

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        FolderManager folderManager = (FolderManager)getBean("folderManager",FolderManager.class);
        RoleManager roleManager = (RoleManager)getBean("roleManager",RoleManager.class);
        IdProvConfManagerServer identityManager =
                (IdProvConfManagerServer) getBean("identityProviderConfigManager", IdProvConfManagerServer.class);

        try {
            addRolesForFolders(folderManager, roleManager);
        } catch (FindException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        } catch (SaveException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }

        try {
            updateEntityRolesForFolderTraversal(roleManager, EntityType.SERVICE);
            updateEntityRolesForFolderTraversal(roleManager, EntityType.POLICY);
        } catch (ObjectModelException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }

        try {
            upgradeManageIdentityProviderRoles(roleManager, identityManager);
        } catch (FindException fe) {
            throw new NonfatalUpgradeException(fe); // rollback, but continue boot, and try again another day
        } catch (UpdateException se) {
            throw new NonfatalUpgradeException(se); // rollback, but continue boot, and try again another day
        }

    }

    /**
     * Upgrades all 'Manage X Identity Provider' roles to have permission access to all key stores.
     * @param roleManager
     */
    private void upgradeManageIdentityProviderRoles(final RoleManager roleManager, final IdProvConfManagerServer identityManager)
            throws FindException, UpdateException {
        //find all identity provider
        Collection<IdentityProviderConfig> providers = identityManager.findAll();
        for (IdentityProviderConfig provider : providers) {
            //find all roles that have entity_type = ID_PROVIDER_CONFIG
            Collection<Role> roles = roleManager.findEntitySpecificRoles(EntityType.ID_PROVIDER_CONFIG, provider.getOid());
            for (Role role : roles) {
                role.addEntityPermission(OperationType.READ, SSG_KEY_ENTRY, null);
                role.addEntityPermission(OperationType.READ, SSG_KEYSTORE, null);
                roleManager.update(role);
            }
        }
    }

    private void addRolesForFolders( final FolderManager folderManager, final RoleManager roleManager )
            throws FindException, SaveException
    {
        // Find all folders
        // If any of them doesn't have a role, try to create it
        Collection<Folder> folders = folderManager.findAll();
        for ( Folder folder : folders ) {
            if ( folder.getFolder() == null ) {
                continue; // no roles for root folder
            }

            Collection<Role> roles = roleManager.findEntitySpecificRoles(FOLDER, folder.getGoid());
            if (roles == null || roles.isEmpty() ) {
                logger.info("Auto-creating missing admin Roles for folder " + folder.getName() + " (#" + folder.getGoid() + ")");
                folderManager.addManageFolderRole( folder );
                folderManager.addReadonlyFolderRole( folder );
            }
        }
    }

    private void updateEntityRolesForFolderTraversal( final RoleManager roleManager, final EntityType roleTargetEntityType )
        throws FindException, UpdateException
    {
        Collection<Role> roles = roleManager.findAll();
        role:
        for ( Role role : roles ) {
            if ( role.getEntityType()==roleTargetEntityType && role.getEntityGoid() != null ) {
                Set<Permission> permissions = role.getPermissions();
                for ( Permission permission : permissions ) {
                    if ( permission.getOperation()==OperationType.READ &&
                         permission.getEntityType()==EntityType.FOLDER &&
                         permission.getScope() != null &&
                         permission.getScope().size()==1 &&
                         permission.getScope().iterator().next() instanceof EntityFolderAncestryPredicate) {
                        continue role;
                    }
                }
                logger.info("Auto-creating missing folder traversal permission for role " + role.getName() + ".");
                role.addEntityFolderAncestryPermission( role.getEntityType(), role.getEntityGoid() );
                roleManager.update( role );
            }
        }
    }
}