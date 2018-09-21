package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.server.util.nameresolver.EntityNameResolver;

/**
 * Name resolver for Role Entity
 */
public class RoleNameResolver extends EntityNameResolver {

    public RoleNameResolver() {
        super(null);
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof Role;
    }

    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        final Role role = (Role) entity;
        String name = role.getDescriptiveName();
        final Entity roleEntity = role.getCachedSpecificEntity();
        if (includePath && roleEntity instanceof HasFolder && roleEntity instanceof NamedEntity) {
            name = name + " (" + getPath((HasFolder) roleEntity) + ((NamedEntity) roleEntity).getName() + ")";
        }
        return name;
    }
}
