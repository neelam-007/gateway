/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.common.security.rbac;

import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.common.util.HexUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * A Role groups zero or more {@link Permission}s so they can be assigned as a whole
 * to individual users using {@link UserRoleAssignment}s.  Roles do not point back to
 * identities, because:
 * <ul>
 * <li>They can be assigned to multiple identites;
 * <li>They may in future be assigned at runtime, rather than statically.
 * </ul>
 * @author alex
 */
public class Role extends NamedEntityImp implements Comparable<Role> {
    public static final int ADMIN_ROLE_OID = -100;

    private Set<Permission> permissions = new HashSet<Permission>();
    private Set<UserRoleAssignment> userAssignments = new HashSet<UserRoleAssignment>();
    private String description;

    private EntityType entityType;
    private Long entityOid;
    private Entity cachedSpecificEntity;

    public Set<Permission> getPermissions() {
        return permissions;
    }

    protected void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<UserRoleAssignment> getUserAssignments() {
        return userAssignments;
    }

    protected void setUserAssignments(Set<UserRoleAssignment> userAssignments) {
        this.userAssignments = userAssignments;
    }

    public void setName(String name) {
        name = HexUtils.truncStringMiddle(name, 128);
        super.setName(name);
    }

    /**
     * Adds a new Permission granting the specified privilege with its scope set to an
     * {@link ObjectIdentityPredicate} for the provided ID, or no scope (allowing any instance of the supplied type)
     * if id == null.
     */
    public void addPermission(OperationType operation, EntityType etype, String id) {
        Permission perm = new Permission(this, operation, etype);
        if (id != null) perm.getScope().add(new ObjectIdentityPredicate(perm, id));
        permissions.add(perm);
    }

    /**
     * Adds a new Permission granting the specified privilege with its scope set to an
     * {@link AttributePredicate} for the provided attribute name and value.
     * @throws IllegalArgumentException if the provided attribute name doesn't match a getter on the class represented 
     *         by the provided EntityType.
     */
    public void addPermission(OperationType operation,
                              EntityType etype,
                              String attr,
                              String name)
                       throws IllegalArgumentException {
        Permission perm = new Permission(this, operation, etype);
        perm.getScope().add(new AttributePredicate(perm, attr, name));
        permissions.add(perm);
    }

    /**
     * Creates and adds a new {@link UserRoleAssignment} assigning the provided {@link User} to this Role.
     */
    public void addAssignedUser(User user) {
        userAssignments.add(new UserRoleAssignment(this, user.getProviderId(), user.getId()));
    }

    public String toString() {
        return _name;
    }

    public int compareTo(Role that) {
        if (this.equals(that))
            return 0;
        
        return this.getName().compareTo(that.getName());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * If this Role is scoped to a particular entity, this property will contain the type of that entity.
     * Otherwise, it will be null.
     * @return
     */
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    /** @deprecated only here to hide enums from Hibernate */
    protected String getEntityTypeName() {
        if (entityType == null) return null;
        return entityType.name();
    }

    /** @deprecated only here to hide enums from Hibernate */
    protected void setEntityTypeName(String typeName) {
        if (typeName == null) return;
        entityType = EntityType.valueOf(typeName);
    }

    /**
     * If this Role is scoped to a particular entity, this property will contain the OID of that entity.
     * Otherwise, it will be null.
     * @return
     */
    public Long getEntityOid() {
        return entityOid;
    }

    public void setEntityOid(Long entityOid) {
        this.entityOid = entityOid;
    }

    /**
     * If this Role is scoped to a particular entity, this property will contain a recent copy of that Entity.
     */
    public Entity getCachedSpecificEntity() {
        return cachedSpecificEntity;
    }

    public void setCachedSpecificEntity(Entity cachedSpecificEntity) {
        this.cachedSpecificEntity = cachedSpecificEntity;
    }

    public String getDescriptiveName() {
        StringBuilder sb = new StringBuilder();

        Matcher matcher = RbacUtilities.removeOidPattern.matcher(_name);
        if (matcher.matches()) {
            if (cachedSpecificEntity instanceof PublishedService) {
                sb.append(RbacAdmin.ROLE_NAME_PREFIX).append(" ");
                sb.append(((NamedEntity)cachedSpecificEntity).getName());
                String uri = ((PublishedService)cachedSpecificEntity).getRoutingUri();
                if (uri != null) sb.append(" [").append(uri).append("]");
                sb.append(" ").append(ServiceAdmin.ROLE_NAME_TYPE_SUFFIX);
                return sb.toString();
            } else if (cachedSpecificEntity instanceof IdentityProviderConfig) {
                sb.append(RbacAdmin.ROLE_NAME_PREFIX).append(" ");
                sb.append(((NamedEntity)cachedSpecificEntity).getName());
                sb.append(" ").append(IdentityAdmin.ROLE_NAME_TYPE_SUFFIX);
                return sb.toString();
            } else {
                sb.append(matcher.group(1));
            }
        } else {
            sb.append(_name);
        }

        return sb.toString();
    }
}
