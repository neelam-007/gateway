/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.upgrade;

import static com.l7tech.objectmodel.EntityType.FOLDER;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.EntityFolderAncestryPredicate;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.folder.FolderManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

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

            Collection<Role> roles = roleManager.findEntitySpecificRoles(FOLDER, folder.getOid());
            if (roles == null || roles.isEmpty() ) {
                logger.info("Auto-creating missing admin Roles for folder " + folder.getName() + " (#" + folder.getOid() + ")");
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
            if ( role.getEntityType()==roleTargetEntityType && role.getEntityOid() != null ) {
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
                role.addEntityFolderAncestryPermission( role.getEntityType(), Long.toString(role.getEntityOid()) );
                roleManager.update( role );
            }
        }
    }
}