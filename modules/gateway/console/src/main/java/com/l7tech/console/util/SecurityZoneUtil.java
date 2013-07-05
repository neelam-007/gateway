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
import org.apache.commons.lang.Validate;
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
    private static final Map<EntityType, Collection<EntityType>> TYPES_WITH_INHERITED_ZONES;
    private static final Set<EntityType> HIDDEN_TYPES;
    private static final String ELLIPSIS = "...";

    static {
        TYPES_WITH_INHERITED_ZONES = new HashMap<>();
        // do not support audits as there may be a LOT of them in the zone
        // user is not aware of the UDDI entities under the hood - they inherit the security zone from the published service
        TYPES_WITH_INHERITED_ZONES.put(EntityType.SERVICE, Arrays.asList(EntityType.AUDIT_MESSAGE, EntityType.UDDI_PROXIED_SERVICE_INFO, EntityType.UDDI_SERVICE_CONTROL));
        // user is not aware that JMS involves two entity types - they share the same security zone
        TYPES_WITH_INHERITED_ZONES.put(EntityType.JMS_CONNECTION, Arrays.asList(EntityType.JMS_ENDPOINT));
        // key metadata is fronted by ssg key entry
        TYPES_WITH_INHERITED_ZONES.put(EntityType.SSG_KEY_ENTRY, Arrays.asList(EntityType.SSG_KEY_METADATA));

        HIDDEN_TYPES = new HashSet<>();
        for (final Collection<EntityType> typesThatInherit : TYPES_WITH_INHERITED_ZONES.values()) {
            HIDDEN_TYPES.addAll(typesThatInherit);
        }
    }

    /**
     * Semaphore used internally that represents a null security zone.
     */
    public static final SecurityZone NULL_ZONE = new SecurityZone() {
        {
            setOid(-1);
            setName("no security zone");
            setDescription("%%%NULL_ZONE%%%");
            setPermittedEntityTypes(Collections.singleton(EntityType.ANY));
        }
    };

    /**
     * Use this if the currently set zone is not readable by the user.
     */
    public static final SecurityZone CURRENT_UNAVAILABLE_ZONE = new SecurityZone() {
        {
            setOid(-2);
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
    public static Set<SecurityZone> getSortedReadableSecurityZones() {
        final Set<SecurityZone> sorted = new TreeSet<>(new NamedEntityComparator());
        sorted.addAll(getSecurityZones());
        return sorted;
    }

    @Nullable
    public static SecurityZone getSecurityZoneByOid(final long oid) {
        return loadMap().get(oid);
    }

    /**
     * Get a sorted list of zones which the user is allowed to use for the given operation and entity types.
     * <p/>
     * Examples:
     * - get a list of zones which the user can select when updating policies
     * - get a list of zones which the user can select when creating jms connections and jms endpoints.
     *
     * @param operation the optional operation that the zones must be valid for. If null, no filtering will be done.
     * @param types     the optional entity types that must be valid for the zones. If null, no filtering will be done.
     * @return a sorted list of zones which the user is allowed to use for the given operation and entity types.
     */
    public static List<SecurityZone> getSortedZonesForOperationAndEntityType(@Nullable final OperationType operation, @Nullable final Collection<EntityType> types) {
        final Collection<Permission> userPermissions = Registry.getDefault().getSecurityProvider().getUserPermissions();
        final List<SecurityZone> validZones = new ArrayList<>();
        if (operation == null || types == null || SecurityZoneUtil.isZoneValidForOperation(SecurityZoneUtil.NULL_ZONE, types, operation, userPermissions)) {
            validZones.add(SecurityZoneUtil.NULL_ZONE);
        }
        for (final SecurityZone zone : SecurityZoneUtil.getSortedReadableSecurityZones()) {
            if (operation == null || types == null || SecurityZoneUtil.isZoneValidForOperation(zone, types, operation, userPermissions)) {
                validZones.add(zone);
            }
        }
        return validZones;
    }

    /**
     * @return a sorted set of all zoneable entity types. Some types do not allow the user to set their zone via the SSM.
     */
    @NotNull
    public static Set<EntityType> getAllZoneableEntityTypes() {
        final Set<EntityType> ret = new TreeSet<>(EntityType.NAME_COMPARATOR);
        for (EntityType type : EntityType.values()) {
            if (type.isSecurityZoneable())
                ret.add(type);
        }
        return ret;
    }

    /**
     * @return zoneable EntityTypes for which the user can set a zone via SSM.
     * @see #getHiddenZoneableEntityTypes()
     */
    @NotNull
    public static Set<EntityType> getNonHiddenZoneableEntityTypes() {
        final Set<EntityType> types = getAllZoneableEntityTypes();
        types.removeAll(getHiddenZoneableEntityTypes());
        return types;
    }

    /**
     * @return EntityTypes that are zoneable but should be hidden from the user because they cannot have their zone set via SSM.
     */
    @NotNull
    public static Set<EntityType> getHiddenZoneableEntityTypes() {
        return HIDDEN_TYPES;
    }

    /**
     * Some EntityTypes cannot have their SecurityZone set via the SSM. These EntityTypes 'inherit' their SecurityZone from other entities.
     *
     * @return a map of EntityTypes that inherit their SecurityZone from other EntityTypes where key = the EntityType that is inherited from
     *         and value = collection of EntityTypes which inherit the SecurityZone from the parent.
     */
    public static Map<EntityType, Collection<EntityType>> getEntityTypesWithInheritedZones() {
        return TYPES_WITH_INHERITED_ZONES;
    }

    /**
     * @param resource     the ResourceBundle.
     * @param propertyName the name of the property which holds an integer.
     * @return the Integer value of the property from the resource or null if the property is not an Integer.
     */
    @Nullable
    public static Integer getIntFromResource(@NotNull ResourceBundle resource, @NotNull final String propertyName) {
        Integer max = null;
        final String maxStr = resource.getString(propertyName);
        try {
            max = Integer.valueOf(maxStr);
        } catch (final NumberFormatException e) {
            logger.log(Level.WARNING, "Invalid integer: " + maxStr);
        }
        return max;
    }

    /**
     * Get the name of the given Security Zone, truncating with ellipsis if over a maximum length.
     *
     * @param securityZone the SecurityZone for which to get a name.
     * @param maxChars     the max number of characters to return without truncating.
     * @return the name of the given Security Zone which could be truncated with ellipsis.
     */
    @NotNull
    public static String getSecurityZoneName(@NotNull final SecurityZone securityZone, @Nullable final Integer maxChars) {
        Validate.isTrue(maxChars == null || maxChars > 0, "MaxChars must be null or greater than zero: " + maxChars);
        String name = securityZone.getName();
        if (maxChars != null && name.length() > maxChars) {
            name = name.substring(0, maxChars) + ELLIPSIS;
        }
        return name;
    }

    /**
     * Determine if a given SecurityZone is valid for a user operation on an entity.
     * <p/>
     * Can be used to filter SecurityZones that the user cannot successfully use for an entity operation.
     *
     * @param zone                the SecurityZone to evaluate. Required (use NULL_ZONE instead of null).
     * @param requiredEntityTypes the EntityTypes that the SecurityZone must support to be valid or null to skip entity type validation.
     * @param requiredOperation   the OperationType used to evaluate whether the given Permissions support the given SecurityZone or null to skip permission validation.
     * @param userPermissions     the Permissions to evaluate against if a requiredOperation is also provided. Optional if no requiredOperation is provided.
     * @return true if the given SecurityZone is valid for the user operation on an entity.
     */
    static boolean isZoneValidForOperation(@NotNull final SecurityZone zone, @Nullable final Collection<EntityType> requiredEntityTypes, @Nullable final OperationType requiredOperation, @Nullable final Collection<Permission> userPermissions) {
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

    private static boolean matchScope(@NotNull final SecurityZone zone, @Nullable Set<ScopePredicate> predicates) {
        boolean match = false;
        if (predicates == null || predicates.isEmpty()) {
            // user has no predicates on the entity they are operating on
            match = true;
        } else {
            // there might be a limit to which zone the user can set
            boolean hasZonePredicate = false;
            for (final ScopePredicate predicate : predicates) {
                if (predicate instanceof SecurityZonePredicate) {
                    hasZonePredicate = true;
                    final SecurityZonePredicate zonePredicate = (SecurityZonePredicate) predicate;
                    final SecurityZone requiredZone = zonePredicate.getRequiredZone();
                    if (zone.equals(NULL_ZONE) && requiredZone == null || zone.equals(requiredZone)) {
                        match = true;
                        break;
                    }
                }
            }
            if (!hasZonePredicate) {
                // can't tell if the user is restricted by zone so give them the benefit of the doubt
                match = true;
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
