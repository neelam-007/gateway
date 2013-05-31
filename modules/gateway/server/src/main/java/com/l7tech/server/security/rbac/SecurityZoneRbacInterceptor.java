package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Custom RBAC interceptor for bulk Security Zone updates.
 * <p/>
 * Can potentially be adapted to apply to other bulk operation scenarios.
 */
public class SecurityZoneRbacInterceptor implements CustomRbacInterceptor {
    private User user;
    @Inject
    private ZoneUpdateSecurityChecker zoneUpdateSecurityChecker;

    @Override
    public void setUser(@NotNull User user) {
        this.user = user;
    }

    /**
     * Checks that the user has permission to update the security zone for all of the entities.
     * <p/>
     * Expected method arguments:
     * 1. security zone oid or null
     * 2. entity type of entities to update
     * 3. oids (longs) of the entities for which to set the security zone
     *
     * @param invocation invocation to invoke (if before-invocation checks pass).  Required.
     * @return whatever the method invocation returns after it proceeds.
     * @throws PermissionDeniedException if the user is not allowed to update at least one of the entities.
     * @throws IllegalArgumentException  if the method invocation arguments are invalid.
     * @throws FindException             if a db error occurs when retrieving an entity.
     * @throws Throwable
     */
    @Override
    public Object invoke(@NotNull final MethodInvocation invocation) throws Throwable {
        final Object[] arguments = invocation.getArguments();
        Validate.isTrue(arguments.length == 3, "Expected three arguments. Received: " + arguments.length);
        Validate.isTrue(arguments[0] == null || arguments[0] instanceof Long, "Expected a Long or null. Received: " + arguments[0]);
        Validate.isTrue(arguments[1] instanceof EntityType, "Expected an EntityType. Received: " + arguments[1]);
        Validate.isTrue(arguments[2] instanceof Collection, "Expected a Collection. Received: " + arguments[2]);
        final Long securityZoneOid = (Long) arguments[0];
        final EntityType entityType = (EntityType) arguments[1];
        final Collection oids = (Collection) arguments[2];
        for (final Object oid : oids) {
            Validate.isTrue(oid instanceof Long, "oid is not a Long: " + oid);
        }
        zoneUpdateSecurityChecker.checkBulkUpdatePermitted(user, securityZoneOid, entityType, oids);
        return invocation.proceed();
    }
}
