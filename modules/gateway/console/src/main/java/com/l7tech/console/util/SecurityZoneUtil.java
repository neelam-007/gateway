package com.l7tech.console.util;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.security.rbac.SecurityZonePredicate;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.comparator.NamedEntityComparator;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods for working with security zones within the SSM.
 */
public class SecurityZoneUtil {
    private static final Logger logger = Logger.getLogger(SecurityZoneUtil.class.getName());

    private static final AtomicReference<Map<Long, SecurityZone>> securityZones = new AtomicReference<>();

    /**
     * Semaphore used internally that represents a null security zone.
     */
    public static final SecurityZone NULL_ZONE = new SecurityZone() {
        {
            setOid(-1);
            setName("No security zone");
            setDescription("%%%NULL_ZONE%%%");
            setPermittedEntityTypes(Collections.singleton(EntityType.ANY));
        }
    };

    /**
     * Use this if the currently set zone is not readable by the user.
     */
    public static final SecurityZone CURRENT_UNAVAILABLE_ZONE = new SecurityZone() {
        {
            setOid(-1);
            setName("Current zone (zone details are unavailable)");
            setDescription("%%%UNAVAILABLE_ZONE%%%");
            setPermittedEntityTypes(Collections.singleton(EntityType.ANY));
        }
    };

    /**
     * Check if any security zones exist and are visible to the current admin user.
     *
     * @return true iff. we can see at least one security zone.
     */
    public static boolean isAnySecurityZonesPresent() {
        return !getSecurityZones().isEmpty();
    }

    public static void flushCachedSecurityZones() {
        securityZones.set(null);
    }

    /**
     * @return all security zones visible to the current admin.  Never null.
     */
    @NotNull
    public static Set<SecurityZone> getSecurityZones() {
        final Map<Long, SecurityZone> ret = loadMap();
        return ret != null ? new HashSet<>(ret.values()) : Collections.<SecurityZone>emptySet();
    }

    /**
     * @return all security zones visible to the current admin sorted by name. Never null.
     */
    @NotNull
    public static Set<SecurityZone> getSortedSecurityZones() {
        final Set<SecurityZone> sorted = new TreeSet<>(new NamedEntityComparator());
        sorted.addAll(getSecurityZones());
        return sorted;
    }

    @Nullable
    public static SecurityZone getSecurityZoneByOid(final long oid) {
        return loadMap().get(oid);
    }

    /**
     * Determine if a given SecurityZone is valid for a user operation on an entity.
     *
     * Can be used to filter SecurityZones that the user cannot successfully use for an entity operation.
     *
     * @param zone the SecurityZone to evaluate. Required (use NULL_ZONE instead of null).
     * @param requiredEntityTypes the EntityTypes that the SecurityZone must support to be valid or null to skip entity type validation.
     * @param requiredOperation the OperationType used to evaluate whether the given Permissions support the given SecurityZone or null to skip permission validation.
     * @param userPermissions the Permissions to evaluate against if a requiredOperation is also provided. Optional if no requiredOperation is provided.
     * @return true if the given SecurityZone is valid for the user operation on an entity.
     */
    public static boolean isZoneValidForOperation(@NotNull final SecurityZone zone, @Nullable final Collection<EntityType> requiredEntityTypes, @Nullable final OperationType requiredOperation, @Nullable final Collection<Permission> userPermissions) {
        boolean match = false;
        if (matchEntityTypes(zone, requiredEntityTypes)) {
            if (requiredOperation == null) {
                match = true;
            } else if (userPermissions != null) {
                for (final Permission permission : userPermissions) {
                    if (requiredOperation == permission.getOperation() && (permission.getEntityType() == EntityType.ANY || (requiredEntityTypes == null || requiredEntityTypes.contains(permission.getEntityType())))) {
                        match = matchScope(zone, permission.getScope());
                        if (match) {
                            break;
                        }
                    }
                }
            }
        }
        return match;
    }

    public static Set<EntityType> getAllZoneableEntityTypes() {
        final Set<EntityType> ret = new TreeSet<>(EntityType.NAME_COMPARATOR);
        for (EntityType type : EntityType.values()) {
            if (type.isSecurityZoneable())
                ret.add(type);
        }
        return ret;
    }

    private static boolean matchScope(@NotNull final SecurityZone zone, @Nullable Set<ScopePredicate> predicates) {
        boolean match = false;
        if (predicates == null || predicates.isEmpty()) {
            // user has no predicates on the entity they are operating on
            match = true;
        } else {
            // there might be a limit to which zone the user can set
            for (final ScopePredicate predicate : predicates) {
                if (predicate instanceof SecurityZonePredicate) {
                    final SecurityZonePredicate zonePredicate = (SecurityZonePredicate) predicate;
                    final SecurityZone requiredZone = zonePredicate.getRequiredZone();
                    if (zone.equals(NULL_ZONE) && requiredZone == null || zone.equals(requiredZone)) {
                        match = true;
                        break;
                    }
                }
            }
        }
        return match;
    }

    private static boolean matchEntityTypes(@NotNull final SecurityZone zone, @Nullable Collection<EntityType> entityTypes) {
        boolean match = true;
        if (entityTypes != null) {
            for (final EntityType entityType : entityTypes) {
                if (!zone.permitsEntityType(entityType)) {
                    match = false;
                    break;
                }
            }
        }
        return match;
    }

    private static Map<Long, SecurityZone> loadMap() {
        Map<Long, SecurityZone> ret = securityZones.get();
        if (ret == null) {
            try {
                if (Registry.getDefault().isAdminContextPresent()) {
                    final Collection<SecurityZone> zones = Registry.getDefault().getRbacAdmin().findAllSecurityZones();
                    ret = new HashMap<>(zones.size());
                    for (final SecurityZone zone : zones) {
                        ret.put(zone.getOid(), zone);
                    }
                    securityZones.set(ret);
                }
            } catch (final FindException e) {
                logger.log(Level.WARNING, "Unable to load security zones: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return ret;
    }
}
