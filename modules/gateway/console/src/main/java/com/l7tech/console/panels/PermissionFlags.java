/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import java.util.Collection;
import java.util.Collections;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.console.util.Registry;
import com.l7tech.console.security.SecurityProvider;

/**
 * @author alex
*/
public class PermissionFlags {
    private final boolean createAll;
    private final boolean updateAll;
    private final boolean deleteAll;
    private final boolean readAll;

    private final boolean createSome;
    private final boolean updateSome;
    private final boolean deleteSome;
    private final boolean readSome;

    private PermissionFlags(boolean createAll, boolean readAll, boolean updateAll, boolean deleteAll,
                            boolean createSome, boolean readSome, boolean updateSome, boolean deleteSome) {
        this.createAll = createAll;
        this.readAll = readAll;
        this.updateAll = updateAll;
        this.deleteAll = deleteAll;

        this.createSome = createSome;
        this.readSome = readSome;
        this.updateSome = updateSome;
        this.deleteSome = deleteSome;
    }

    public static PermissionFlags get(EntityType etype) {
        boolean createSome = false;
        boolean readSome = false;
        boolean updateSome = false;
        boolean deleteSome = false;

        boolean createAll = false;
        boolean readAll = false;
        boolean updateAll = false;
        boolean deleteAll = false;

        Collection<Permission> userPermissions = Collections.emptyList();

        Registry registry = Registry.getDefault();
        if (registry != null) {
            SecurityProvider securityProvider = registry.getSecurityProvider();

            if (securityProvider != null)
                userPermissions = securityProvider.getUserPermissions();
        }

        for (Permission perm : userPermissions) {
            if (perm.getEntityType() == etype || perm.getEntityType() == EntityType.ANY) {
                switch (perm.getOperation()) {
                    case CREATE:
                        createSome = true;
                        if (perm.getScope().isEmpty()) createAll = true;
                        break;
                    case READ:
                        readSome = true;
                        if (perm.getScope().isEmpty()) readAll = true;
                        break;
                    case UPDATE:
                        updateSome = true;
                        if (perm.getScope().isEmpty()) updateAll = true;
                        break;
                    case DELETE:
                        deleteSome = true;
                        if (perm.getScope().isEmpty()) deleteAll = true;
                        break;
                }
            }
        }

        return new PermissionFlags(createAll, readAll, updateAll, deleteAll, createSome, readSome, updateSome, deleteSome);
    }

    /**
     * @return true if the current user can create any object of the specified type
     */
    public boolean canCreateAll() {
        return createAll;
    }

    /**
     * @return true if the current user can update any object of the specified type
     */
    public boolean canUpdateAll() {
        return updateAll;
    }

    /**
     * @return true if the current user can delete any object of the specified type
     */
    public boolean canDeleteAll() {
        return deleteAll;
    }

    /**
     * @return true if the current user can read any object of the specified type
     */
    public boolean canReadAll() {
        return readAll;
    }

    /**
     * @return true if the current user can create some objects of the specified type
     */
    public boolean canCreateSome() {
        return createSome;
    }

    /**
     * @return true if the current user can update some objects of the specified type
     */
    public boolean canUpdateSome() {
        return updateSome;
    }

    /**
     * @return true if the current user can delete some objects of the specified type
     */
    public boolean canDeleteSome() {
        return deleteSome;
    }

        /**
     * @return true if the current user can read some objects of the specified type
     */
    public boolean canReadSome() {
        return readSome;
    }
}
