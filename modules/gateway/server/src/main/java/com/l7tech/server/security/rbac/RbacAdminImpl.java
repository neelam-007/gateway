/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.identity.Group;
import com.l7tech.identity.Identity;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.HasFolderId;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.EncapsulatedAssertion;
import com.l7tech.server.EntityCrud;
import com.l7tech.server.identity.HasDefaultRole;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.server.util.nameresolver.EntityNameResolver;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class RbacAdminImpl implements RbacAdmin {
    private static final Logger LOGGER = Logger.getLogger(RbacAdminImpl.class.getName());

    private final RoleManager roleManager;
    @Inject
    private SecurityZoneManager securityZoneManager;

    @Inject
    private AssertionAccessManager assertionAccessManager;

    @Inject
    private EntityCrud entityCrud;

    @Inject
    private IdentityProviderFactory identityProviderFactory;

    @Inject
    private ProtectedEntityTracker protectedEntityTracker;

    @Inject
    private EntityNameResolver entityNameResolver;

    @Inject
    private IdentityAdmin identityAdmin;

    public static final String PROVIDER_ID = "providerId";
    public static final String ID = "id";
    public static final String SERVICE_ID = "serviceid";
    public static final String NODE_ID = "nodeid";
    public static final String ALL = "<ALL>";
    public static final String SEPARATOR = ", ";
    private static final String COMPLEX_SCOPE = "<complex scope>";
    private static final int COMPLEX_SCOPE_SIZE = 3;

    public RbacAdminImpl(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public Collection<Role> findAllRoles() throws FindException {
        final Collection<Role> roles = roleManager.findAll();
        for (Role role : roles) {
            attachEntities(role);
        }
        return roles;
    }

    public Role findRoleByPrimaryKey(Goid goid) throws FindException {
        return attachEntities(roleManager.findByPrimaryKey(goid));
    }

    @Override
    public Collection<EntityHeader> findAllRoleHeaders() throws FindException {
        final Collection<EntityHeader> allHeaders = roleManager.findAllHeaders();
        return allHeaders;
    }

    @NotNull
    @Override
    public Collection<Permission> findCurrentUserPermissions() throws FindException {
        User u = JaasUtils.getCurrentUser();
        if (u == null){
            throw new FindException("Couldn't get current user");
        }
        Set<Permission> perms = new HashSet<Permission>();
        // No license check--needed for SSM login
        final Collection<Role> assignedRoles = roleManager.getAssignedRoles(u);
        for (Role role : assignedRoles) {
            for (final Permission perm : role.getPermissions()) {
                Permission perm2 = perm.getAnonymousClone();
                perms.add(perm2);
            }
        }

        // TODO move this hack into the roleManager
        if (perms.isEmpty()) {
            // Check for an IDP that declares a default role
            Role role = findDefaultRole(u.getProviderId());
            for (Permission perm : role.getPermissions()) {
                Permission perm2 = perm.getAnonymousClone();
                perms.add(perm2);
            }
        }

        return perms;
    }

    @NotNull
    @Override
    public Map<String, EntityProtectionInfo> findProtectedEntities() throws FindException {
        if (protectedEntityTracker == null) {
            throw new FindException("ProtectedEntityTracker is not available");
        }
        return protectedEntityTracker.getProtectedEntities();
    }

    @NotNull
    @Override
    public Pair<Collection<Permission>, Map<String, EntityProtectionInfo>> findCurrentUserPermissionsAndProtectedEntities() throws FindException {
        final Collection<Permission> permissions = findCurrentUserPermissions();
        final Map<String, EntityProtectionInfo> protectedEntityMap = findProtectedEntities();
        return Pair.pair(permissions, protectedEntityMap);
    }

    private Role findDefaultRole(Goid providerId) throws FindException {
        // TODO move this hack into the roleManager
        IdentityProvider prov = identityProviderFactory.getProvider(providerId);
        if (prov instanceof HasDefaultRole) {
            HasDefaultRole hasDefaultRole = (HasDefaultRole) prov;
            Goid roleId = hasDefaultRole.getDefaultRoleId();
            if (roleId != null)
                return roleManager.findByPrimaryKey(roleId);
        }
        return null;
    }

    /**
     * Note: this method <em>intentionally</em> avoids validating user accounts (e.g. whether they're expired or disabled). 
     * Don't ever use it for authorization!
     */
    public Collection<Role> findRolesForUser(User user) throws FindException {
        if (user == null){
            throw new IllegalArgumentException("User cannot be null.");
        }
        List<Role> assignedRoles = new ArrayList<>();
        assignedRoles.addAll(roleManager.getAssignedRoles(user, true, false));

        for (final Role assignedRole : assignedRoles) {
            attachEntities(assignedRole);
        }
        return assignedRoles;
    }

    @Override
    public Role findDefaultRoleForIdentityProvider(Goid identityProviderId) throws FindException {
        return findDefaultRole(identityProviderId);
    }

    /**
     * Does not validate the group - do not use for authorization.
     */
    @Override
    public Collection<Role> findRolesForGroup(@NotNull final Group group) throws FindException {
        final Collection<Role> assignedRoles = roleManager.getAssignedRoles(group);

        // No support for "default role" hack for groups currently

        for (final Role assignedRole : assignedRoles) {
            attachEntities(assignedRole);
        }

        return assignedRoles;
    }

    private Role attachEntities(Role theRole) {
        Goid entityGoid = theRole.getEntityGoid();
        EntityType entityType = theRole.getEntityType();
        if (entityType != null && PersistentEntity.class.isAssignableFrom(entityType.getEntityClass()) && entityGoid != null) {
            try {
                theRole.setCachedSpecificEntity(entityCrud.find(entityType.getEntityClass(), entityGoid));
            } catch (FindException e) {
                LOGGER.log(Level.WARNING, MessageFormat.format("Could not find {0} (# {1}) to attach to {2} Role", entityType.name(), entityGoid, theRole.getName()), ExceptionUtils.getDebugException(e));
            }
        }
        return theRole;
    }

    public Goid saveRole(Role role) throws SaveException {
        if (role.isUnsaved()) {
            return roleManager.save(role);
        } else {
            Goid goid = role.getGoid();
            try {
                roleManager.update(role);
            } catch (UpdateException e) {
                throw new SaveException(e.getMessage(), e);
            }
            return goid;
        }
    }

    public void deleteRole(Role role) throws DeleteException {
        roleManager.delete(role);
    }

    public EntityHeaderSet<EntityHeader> findEntities(EntityType entityType) throws FindException {
        return entityCrud.findAll(entityType.getEntityClass());
    }

    @Override
    public Collection<SecurityZone> findAllSecurityZones() throws FindException {
        return securityZoneManager.findAll();
    }

    @Override
    public SecurityZone findSecurityZoneByPrimaryKey(Goid goid) throws FindException {
        return securityZoneManager.findByPrimaryKey(goid);
    }

    @Override
    public Goid saveSecurityZone(SecurityZone securityZone) throws SaveException {
        if (SecurityZone.DEFAULT_GOID.equals(securityZone.getGoid())) {
            final Goid goid = securityZoneManager.save(securityZone);
            securityZoneManager.createRoles(securityZone);
            return goid;
        } else {
            Goid goid = securityZone.getGoid();
            try {
                securityZoneManager.update(securityZone);
                securityZoneManager.updateRoles(securityZone);
            } catch (UpdateException e) {
                throw new SaveException(ExceptionUtils.getMessage(e), e);
            }
            return goid;
        }
    }

    @Override
    public void deleteSecurityZone(SecurityZone securityZone) throws DeleteException {
        securityZoneManager.delete(securityZone);
    }

    @Override
    public Collection<EntityHeader> findEntitiesByTypeAndSecurityZoneGoid(@NotNull final EntityType type, final Goid securityZoneGoid) throws FindException {
        return entityCrud.findByEntityTypeAndSecurityZoneGoid(type, securityZoneGoid);
    }

    @Override
    public void setSecurityZoneForEntities(final Goid securityZoneGoid, @NotNull final EntityType entityType, @NotNull final Collection<Serializable> entityIds) throws UpdateException {
        entityCrud.setSecurityZoneForEntities(securityZoneGoid, entityType, entityIds);
    }

    @Override
    public void setSecurityZoneForEntities(final Goid securityZoneGoid, @NotNull final Map<EntityType, Collection<Serializable>> entityIds) throws UpdateException {
        entityCrud.setSecurityZoneForEntities(securityZoneGoid, entityIds);
    }

    @Override
    public Collection<AssertionAccess> findAccessibleAssertions() throws FindException {
        // Return them all, and allow the RBAC interceptor to filter out any the current admin can't see
        return assertionAccessManager.findAllRegistered();
    }

    @Override
    public Goid saveAssertionAccess(AssertionAccess assertionAccess) throws UpdateException {
        String assname = assertionAccess.getName();
        if (assname == null)
            throw new IllegalArgumentException("AssertionAccess must have an assertion class name");

        try {
            Goid oid = assertionAccess.getGoid();
            if (assertionAccess.isUnsaved()) {
                oid = assertionAccessManager.save(assertionAccess);
                assertionAccess.setGoid(oid);
                return oid;
            } else {
                AssertionAccess existing = assertionAccessManager.findByPrimaryKey(oid);
                if (existing != null && !assname.equals(existing.getName()))
                    throw new UpdateException("Unable to change the assertion class name of an existing AssertionAccess");

                assertionAccessManager.update(assertionAccess);
                return oid;
            }
        } catch (FindException | SaveException e) {
            throw new UpdateException("Unable to update assertion access: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public EntityHeader findHeader(EntityType entityType, Serializable pk) throws FindException {
        return entityCrud.findHeader(entityType, pk);
    }

    @Override
    public Entity find(@NotNull EntityHeader header) throws FindException {
        return entityCrud.find(header);
    }
    /**
     * This method returns SecurityZoneEntity transfer object for all the entities of given entity type
     * @param type EntityType
     * @param securityZoneGoid Goid
     * @return Collection<ResolvedEntityHeaderTO>
     * @throws FindException
     */
    @Override
    public Collection<ResolvedEntityHeader> findSecurityZoneByTypeAndSecurityZoneGoid(@NotNull final EntityType type, final Goid securityZoneGoid) throws FindException {
        Collection<EntityHeader> entityHeaders = entityCrud.findByEntityTypeAndSecurityZoneGoid(type, securityZoneGoid);
        Collection<ResolvedEntityHeader> securityZoneEntityTOList = new ArrayList<>();
        for (final EntityHeader header : entityHeaders) {
            if (EntityType.POLICY != type || !(header instanceof PolicyHeader && PolicyType.PRIVATE_SERVICE == ((PolicyHeader) header).getPolicyType())) {
                try {
                    String entityName = entityNameResolver.getNameForHeader(header, false);
                    String path = StringUtils.EMPTY;
                    if (header instanceof HasFolderId) {
                        path = entityNameResolver.getPath((HasFolderId) header);
                    }
                    ResolvedEntityHeader securityZoneEntityTO = new ResolvedEntityHeader();
                    securityZoneEntityTO.setEntityHeader(header);
                    securityZoneEntityTO.setName(entityName);
                    securityZoneEntityTO.setPath(path);
                    securityZoneEntityTOList.add(securityZoneEntityTO);
                }
                catch (final FindException | PermissionDeniedException e)
                {
                    LOGGER.log(Level.WARNING, "Error resolving name for header " + header.toStringVerbose() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    throw e;
                }

            }
        }
        return securityZoneEntityTOList;
    }

    /**
     * This method returns SecurityZoneEntity transfer object for all the given entity headers
     * @param entityHeaderSet EntityHeaderSet<EntityHeader>
     * @return Collection<ResolvedEntityHeaderTO>
     */
    @Override
    public Collection<ResolvedEntityHeader> findSecurityZoneByEntityHeaders(@NotNull final EntityHeaderSet<EntityHeader>  entityHeaderSet) throws FindException {
        Collection<ResolvedEntityHeader> securityZoneEntityTOList = new ArrayList<>();
        for (final EntityHeader header : entityHeaderSet) {
            if(isInvalidEntityHeader(header))
            {
                continue;
            }

            try {
                String name = entityNameResolver.getNameForHeader(header, false);
                String path = StringUtils.EMPTY;
                if (header instanceof HasFolderId) {
                    path = entityNameResolver.getPath((HasFolderId) header);
                }
                ResolvedEntityHeader securityZoneEntityTO = new ResolvedEntityHeader();
                securityZoneEntityTO.setEntityHeader(header);
                securityZoneEntityTO.setName(name);
                securityZoneEntityTO.setPath(path);
                securityZoneEntityTOList.add(securityZoneEntityTO);
            } catch (final FindException | PermissionDeniedException e) {
                // skip entities that we cannot resolve a name for since users shouldn't be able to modify an entity without this information
                LOGGER.log(Level.WARNING, "Error resolving name for header " + header.toStringVerbose() + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                throw e;
            }
        }
        return securityZoneEntityTOList;
    }

    private boolean isInvalidEntityHeader(final EntityHeader header)
    {
        if (header instanceof PolicyHeader) {
            final PolicyHeader policy = (PolicyHeader) header;
            if (PolicyType.PRIVATE_SERVICE == policy.getPolicyType() || !policy.getPolicyType().isSecurityZoneable()) {
                // don't show service policies or non-zoneable policies
                return true;
            }
        }

        if (header.getType() == EntityType.ASSERTION_ACCESS) {
            final String assertionClassName = header.getName();
            if (EncapsulatedAssertion.class.getName().equals(assertionClassName)) {
                // encapsulated assertions are handled by their config entity type
                return true;
            }
        }
        return false;
    }

    /**
     * This method returns names for given entity headers
     * @param entityHeaderSet EntityHeaderSet<EntityHeader>
     * @return Map<EntityHeader, String>
     * @throws FindException
     */
    @Override
    public Map<EntityHeader, String> findNamesForEntityHeaders(@NotNull final EntityHeaderSet<EntityHeader> entityHeaderSet) throws FindException {
        Map<EntityHeader, String> entityHeaderMap = new HashMap<>();
        for (final EntityHeader header : entityHeaderSet) {
            String name = entityNameResolver.getNameForHeader(header, true);
            entityHeaderMap.put(header, name);
        }
        return entityHeaderMap;
    }

    /**
     * This method returns PermissionGroup Descriptions for given set of scope predicates
     * @param scopes Collection<Pair<EntityType, Set<ScopePredicate>>>
     * @return Map<Pair<EntityType, Set<ScopePredicate>>, String>
     */
    @Override
    public Map<Pair<EntityType, Set<ScopePredicate>>, String> findPermissionGroupScopeDescriptions(@NotNull final Collection<Pair<EntityType, Set<ScopePredicate>>> scopes) {
        final Map<Pair<EntityType, Set<ScopePredicate>>, String> scopeDescriptions = new HashMap<>();

        for (final Pair<EntityType, Set<ScopePredicate>> scopeItem : scopes) {
            Set<ScopePredicate> scope = scopeItem.getValue();
            String scopeDescription = null;
            if (!scope.isEmpty()) {
                if (scope.size() < COMPLEX_SCOPE_SIZE) {
                    scopeDescription = getScopeDescription(scopeItem);
                } else {
                    scopeDescription = COMPLEX_SCOPE;
                }

            } else {
                scopeDescription = ALL;
            }
            scopeDescriptions.put(scopeItem, scopeDescription);
        }

        return scopeDescriptions;
    }
    private String getScopeDescription(final Pair<EntityType, Set<ScopePredicate>> scopeItem)
    {
        Set<ScopePredicate> scope = scopeItem.getValue();
        EntityType entityType = scopeItem.getKey();
        String scopeDescription = tryGetAttributeSpecificDescriptions(scopeItem);
        if (scopeDescription == null) {
            final Set<String> predicateDescriptions = new TreeSet<>();
            for (final ScopePredicate predicate : scope) {
                try {
                    if (predicate instanceof ObjectIdentityPredicate) {
                        final ObjectIdentityPredicate oip = (ObjectIdentityPredicate) predicate;
                        final String id = oip.getTargetEntityId();
                        final EntityHeader header = findHeader(entityType, id);
                        oip.setHeader(header);
                    }
                    predicateDescriptions.add(entityNameResolver.getNameForEntity(predicate, true));
                } catch (final FindException | PermissionDeniedException e) {
                    LOGGER.log(Level.WARNING, "Unable to determine name for predicate " + predicate + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
            scopeDescription = org.apache.commons.lang.StringUtils.join(predicateDescriptions.toArray(), SEPARATOR);
        }
        return scopeDescription;
    }
    /**
     * Look for attribute predicate combinations which may identify a specific entity and return a scope description if possible.
     */
    private String tryGetAttributeSpecificDescriptions(final Pair<EntityType, Set<ScopePredicate>> scopeItem) {
        String scopeDescription = null;
        Set<ScopePredicate> scope = scopeItem.getValue();
        EntityType entityType = scopeItem.getKey();
        ObjectIdentityPredicate objectIdentityPredicate = null;
        final Identity identity = tryGetIdentity(entityType, scope);
        if (identity != null) {
            objectIdentityPredicate = new ObjectIdentityPredicate(null, identity.getId());
            objectIdentityPredicate.setHeader(new IdentityHeader(identity.getProviderId(), identity.getId(), entityType, identity.getName(), null, null, null));
        } else {
            final ServiceUsageHeader serviceUsageHeader = tryGetServiceUsage(entityType, scope);
            if (serviceUsageHeader != null) {
                objectIdentityPredicate = new ObjectIdentityPredicate(null, null);
                objectIdentityPredicate.setHeader(serviceUsageHeader);
            }
        }
        if (objectIdentityPredicate != null) {
            try {
                scopeDescription = entityNameResolver.getNameForEntity(objectIdentityPredicate, true);
            } catch (final FindException | PermissionDeniedException e) {
                LOGGER.log(Level.WARNING, "Unable to resolve name for predicate " + objectIdentityPredicate + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }
        return scopeDescription;
    }
    private  ServiceUsageHeader tryGetServiceUsage(final EntityType type, final Set<ScopePredicate> scope) {
        ServiceUsageHeader serviceUsageHeader = null;
        if (type == EntityType.SERVICE_USAGE && scope.size() == 2) {
            final Map<String, String> equalsAttributes = getEqualsAttributes(scope);
            if (equalsAttributes.containsKey(SERVICE_ID) && equalsAttributes.containsKey(NODE_ID)) {
                try {
                    final Goid serviceGoid = Goid.parseGoid(equalsAttributes.get(SERVICE_ID));
                    serviceUsageHeader = new ServiceUsageHeader(serviceGoid, equalsAttributes.get(NODE_ID));
                } catch (final IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "Unable to parse goid from attribute value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return serviceUsageHeader;
    }

    private  Identity tryGetIdentity(final EntityType type, final Set<ScopePredicate> scope) {
        Identity identity = null;
        if ((type == EntityType.USER || type == EntityType.GROUP) && scope.size() == 2) {
            final Map<String, String> equalsAttributes = getEqualsAttributes(scope);
            if (equalsAttributes.containsKey(PROVIDER_ID) && equalsAttributes.containsKey(ID)) {
                try {
                    final Goid providerGoid = Goid.parseGoid(equalsAttributes.get(PROVIDER_ID));
                    final String identityId = equalsAttributes.get(ID);
                    if (type == EntityType.USER) {
                        identity = identityAdmin.findUserByID(providerGoid, identityId);
                    } else if (type == EntityType.GROUP) {
                        identity = identityAdmin.findGroupByID(providerGoid, identityId);
                    }
                } catch (final IllegalArgumentException | FindException | PermissionDeniedException e) {
                    LOGGER.log(Level.WARNING, "Unable to parse goid from attribute value: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return identity;
    }
    private  Map<String, String> getEqualsAttributes(final Set<ScopePredicate> scope) {
        final Map<String, String> equalsAttributes = new HashMap<>();
        for (final ScopePredicate predicate : scope) {
            if (predicate instanceof AttributePredicate) {
                final AttributePredicate attributePredicate = (AttributePredicate) predicate;
                if (attributePredicate.getMode() == null || attributePredicate.getMode().equals(AttributePredicate.EQUALS)) {
                    equalsAttributes.put(attributePredicate.getAttribute(), attributePredicate.getValue());
                }
            }
        }
        return equalsAttributes;
    }
}
