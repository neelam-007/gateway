/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.util.TextUtils;

import javax.persistence.Table;
import javax.persistence.OneToMany;
import javax.persistence.Column;
import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Cascade;

/**
 * A Role groups zero or more {@link Permission}s so they can be assigned as a whole
 * to individual users using {@link RoleAssignment}s.  Roles do not point back to
 * identities, because:
 * <ul>
 * <li>They can be assigned to multiple identites;
 * <li>They may in future be assigned at runtime, rather than statically.
 * </ul>
 * @author alex
 */
@javax.persistence.Entity
@Table(name="rbac_role")
public class Role extends NamedEntityImp implements Comparable<Role> {
    public static final int ADMIN_ROLE_OID = -100;

    private Set<Permission> permissions = new HashSet<Permission>();
    private Set<RoleAssignment> roleAssignments = new HashSet<RoleAssignment>();
    private String description;

    private EntityType entityType;
    private Long entityOid;
    private Entity cachedSpecificEntity;

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="role")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<Permission> getPermissions() {
        return permissions;
    }

    protected void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    @OneToMany(cascade=CascadeType.ALL, fetch=FetchType.EAGER, mappedBy="role")
    @Fetch(FetchMode.SUBSELECT)
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    public Set<RoleAssignment> getRoleAssignments() {
        return roleAssignments;
    }

    protected void setRoleAssignments(Set<RoleAssignment> roleAssignments) {
        this.roleAssignments = roleAssignments;
    }

    public void setName(String name) {
        name = TextUtils.truncStringMiddle(name, 128);
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
     * Creates and adds a new {@link RoleAssignment} assigning the provided {@link User} to this Role.
     */
    public void addAssignedUser(User user) {
        roleAssignments.add(new RoleAssignment(this, user.getProviderId(), user.getId(), EntityType.USER));
    }

    public String toString() {
        return _name;
    }

    public int compareTo(Role that) {
        if (this.equals(that))
            return 0;
        
        return this.getName().compareTo(that.getName());
    }

    @Column(name="description", length=255)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * If this Role is scoped to a particular entity, this property will contain the type of that entity.
     * Otherwise, it will be null.
     */
    @Transient
    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    /** @deprecated only here to hide enums from Hibernate */
    @Column(name="entity_type", length=255)
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
     */
    @Column(name="entity_oid")
    public Long getEntityOid() {
        return entityOid;
    }

    public void setEntityOid(Long entityOid) {
        this.entityOid = entityOid;
    }

    /**
     * If this Role is scoped to a particular entity, this property will contain a recent copy of that Entity.
     */
    @Transient
    public Entity getCachedSpecificEntity() {
        return cachedSpecificEntity;
    }

    public void setCachedSpecificEntity(Entity cachedSpecificEntity) {
        this.cachedSpecificEntity = cachedSpecificEntity;
    }

    @Transient
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
