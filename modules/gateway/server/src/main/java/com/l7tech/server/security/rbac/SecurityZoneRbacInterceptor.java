package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * Custom RBAC interceptor for bulk Security Zone updates.
 * <p/>
 * Can potentially be adapted to apply to other bulk operation scenarios.
 */
public class SecurityZoneRbacInterceptor implements CustomRbacInterceptor {
    private static final int ZONE_OID_ARG = 0;
    private static final int MIN_ARG_LENGTH = 2;
    private static final int MAX_ARG_LENGTH = 3;

    private static final int ENTITY_TYPE_ARG = 1;
    private static final int COLLECTION_ARG = 2;

    private static final int MAP_ARG = 1;

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
     * OR map where key = entity type of entities to update and value = oids (longs) of the entities for which to set the security zone
     * 3. oids (longs) of the entities for which to set the security zone
     * OR no third arg if the 2nd arg is a map
     *
     * @param invocation invocation to invoke (if before-invocation checks pass).  Required.
     * @return whatever the method invocation returns after it proceeds.
     * @throws PermissionDeniedException if the user is not allowed to update at least one of the entities.
     * @throws IllegalArgumentException  if the method invocation arguments are invalid.
     * @throws FindException             if a db error occurs when retrieving an entity.
     * @throws Throwable                 if an unexpected error occurs
     */
    @Override
    public Object invoke(@NotNull final MethodInvocation invocation) throws Throwable {
        final Object[] arguments = invocation.getArguments();
        Validate.isTrue(arguments.length == MIN_ARG_LENGTH || arguments.length == MAX_ARG_LENGTH, "Expected two or three arguments. Received: " + arguments.length);
        Validate.isTrue(arguments[ZONE_OID_ARG] == null || arguments[0] instanceof Long, "Expected a Long or null. Received: " + arguments[ZONE_OID_ARG]);
        final Long securityZoneOid = (Long) arguments[ZONE_OID_ARG];
        if (arguments.length == MAX_ARG_LENGTH) {
            checkBulkUpdateSingleEntityType(arguments, securityZoneOid);
        } else {
            checkBulkUpdateMultipleEntityTypes(arguments, securityZoneOid);
        }
        return invocation.proceed();
    }

    private void checkBulkUpdateMultipleEntityTypes(final Object[] arguments, final Long securityZoneOid) throws FindException {
        Validate.isTrue(arguments[MAP_ARG] instanceof Map, "Expected a Map. Received: " + arguments[1]);
        final Map oidsMap = (Map) arguments[MAP_ARG];
        for (final Object entry : oidsMap.entrySet()) {
            Validate.isTrue(entry instanceof Map.Entry, "Invalid map");
            final Map.Entry mapEntry = (Map.Entry) entry;
            final Object key = mapEntry.getKey();
            final Object value = mapEntry.getValue();
            Validate.isTrue(key instanceof EntityType, "Expected a map key of EntityType. Received: " + key);
            Validate.isTrue(value instanceof Collection, "Expected a map value of Collection. Received: " + value);
            validateCollectionOfSerializable((Collection) value);
        }
        zoneUpdateSecurityChecker.checkBulkUpdatePermitted(user, securityZoneOid, oidsMap);
    }

    private void checkBulkUpdateSingleEntityType(final Object[] arguments, final Long securityZoneOid) throws FindException {
        Validate.isTrue(arguments[ENTITY_TYPE_ARG] instanceof EntityType, "Expected an EntityType. Received: " + arguments[ENTITY_TYPE_ARG]);
        Validate.isTrue(arguments[COLLECTION_ARG] instanceof Collection, "Expected a Collection. Received: " + arguments[COLLECTION_ARG]);
        final EntityType entityType = (EntityType) arguments[ENTITY_TYPE_ARG];
        final Collection oids = (Collection) arguments[COLLECTION_ARG];
        validateCollectionOfSerializable(oids);
        zoneUpdateSecurityChecker.checkBulkUpdatePermitted(user, securityZoneOid, entityType, oids);
    }

    /**
     * Ensure the collection only contains longs.
     */
    private void validateCollectionOfSerializable(@NotNull final Collection ids) {
        for (final Object id : ids) {
            Validate.isTrue(id instanceof Serializable, "oid is not a Serializable: " + id);
        }
    }
}
