/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.Permission;
import com.l7tech.console.util.Registry;

/**
 * @author alex
*/
public class PermissionFlags {
    private final boolean createAll;
    private final boolean updateAll;
    private final boolean deleteAll;

    private final boolean createSome;
    private final boolean updateSome;
    private final boolean deleteSome;

    private PermissionFlags(boolean createAll, boolean updateAll, boolean deleteAll, boolean createSome, boolean updateSome, boolean deleteSome) {
        this.createAll = createAll;
        this.updateAll = updateAll;
        this.deleteAll = deleteAll;
        this.createSome = createSome;
        this.updateSome = updateSome;
        this.deleteSome = deleteSome;
    }

    public static PermissionFlags get(EntityType etype) {
        boolean createSome = false;
        boolean updateSome = false;
        boolean deleteSome = false;
        boolean createAll = false;
        boolean updateAll = false;
        boolean deleteAll = false;

        for (Permission perm : Registry.getDefault().getSecurityProvider().getUserPermissions()) {
            if (perm.getEntityType() == etype || perm.getEntityType() == EntityType.ANY) {
                switch (perm.getOperation()) {
                    case CREATE:
                        createSome = true;
                        if (perm.getScope().isEmpty()) createAll = true;
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

        return new PermissionFlags(createAll, updateAll, deleteAll, createSome, updateSome, deleteSome);
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
}
