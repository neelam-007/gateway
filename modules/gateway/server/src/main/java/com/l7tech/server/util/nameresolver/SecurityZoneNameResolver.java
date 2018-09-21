package com.l7tech.server.util.nameresolver;

import com.l7tech.gateway.common.security.rbac.SecurityZonePredicate;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.util.nameresolver.EntityNameResolver;

/**
 * Name resolver for Security Zone Entity
 */
public class SecurityZoneNameResolver extends EntityNameResolver {
    public SecurityZoneNameResolver() {
        super(null);
    }

    @Override
    protected boolean canResolveName(final Entity entity) {
        return entity instanceof SecurityZonePredicate;
    }
    @Override
    public String resolve(final Entity entity, final boolean includePath) throws FindException {
        final SecurityZonePredicate predicate = (SecurityZonePredicate) entity;
        String name = null;
        if (predicate.getRequiredZone() != null) {
            name = "in security zone \"" + predicate.getRequiredZone().getName() + "\"";
        } else {
            name = "without a security zone";
        }
        return name;
    }
}
