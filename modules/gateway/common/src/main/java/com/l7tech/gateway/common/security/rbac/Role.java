/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.NamedEntity;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.TextUtils;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * A Role groups zero or more {@link Permission}s so they can be assigned as a whole
 * to individual users using {@link RoleAssignment}s.  Roles do not point back to
 * identities, because:
 * <ul>
 * <li>They can be assigned to multiple identites;
 * <li>They may in future be assigned at runtime, rather than statically.
 * </ul>
 */
@javax.persistence.Entity
@Proxy(lazy=false)
@Table(name="rbac_role")
public class Role extends NamedEntityImp implements Comparable<Role> {
    public static enum Tag { ADMIN }

    private Set<Permission> permissions = new HashSet<Permission>();
    private Set<RoleAssignment> roleAssignments = new HashSet<RoleAssignment>();
    private String description;

    private EntityType entityType;
    private Long entityOid;
    private Entity cachedSpecificEntity;
    private Tag tag; 

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

    @Enumerated(EnumType.STRING)
    @Column(name="tag", length= 36)
    public Tag getTag() {
        return tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
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
    public void addEntityPermission(OperationType operation, EntityType etype, String id) {
        Permission perm = new Permission(this, operation, etype);
        if (id != null) perm.getScope().add(new ObjectIdentityPredicate(perm, id));
        permissions.add(perm);
    }

    /**
     * Adds a new Permission granting the specified privilege with its scope set to a {@link FolderPredicate} for the
     * provided folder.
     *
     * @param operation  the operation that the permission grants access to
     * @param etype      the type of entity to which the permission applies
     * @param folder     the folder containing entities to which the permission applies
     * @param transitive <code>true</code> if the permission should also apply to entities in subfolders of the
     *                   provided folder; <code>false</code> if it only applies to entities directly in the provided
     *                   folder.
     */
    public void addFolderPermission(OperationType operation, EntityType etype, Folder folder, boolean transitive) {
        Permission perm = new Permission(this, operation, etype);
        perm.getScope().add(new FolderPredicate(perm, folder, transitive));
        permissions.add(perm);
    }

    /**
     * Adds a new Permission granting read access to the folder ancestry of an entity.
     *
     * @param etype    the type of entity whose folder ancestry should be made readable
     * @param entityId the ID of the entity whose folder ancestry should be made readable
     */
    public void addEntityFolderAncestryPermission(EntityType etype, String entityId) {
        Permission perm = new Permission(this, OperationType.READ, EntityType.FOLDER);
        perm.getScope().add(new EntityFolderAncestryPredicate(perm, etype, entityId));
        permissions.add(perm);
    }


    /**
     * Adds a new Permission granting the specified privilege with its scope set to an
     * {@link AttributePredicate} for the provided attribute name and value.
     * @throws IllegalArgumentException if the provided attribute name doesn't match a getter on the class represented 
     *         by the provided EntityType.
     */
    public void addAttributePermission(OperationType operation, EntityType etype, String attr, String name)
        throws IllegalArgumentException
    {
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

    /**
     * Removes an {@link RoleAssignment} that was assigned by the provided {@link User} to this Role.
     *
     * @param user  The user that will be used to remove the user's assignment roles.
     */
    public void removeAssignedUser(User user) {
        for (Iterator<RoleAssignment> i = roleAssignments.iterator(); i.hasNext();) {
            RoleAssignment roleAssignment = i.next();
            if (roleAssignment.getIdentityId().equals(user.getId()) && roleAssignment.getProviderId() == user.getProviderId()
                    && roleAssignment.getEntityType().equals(EntityType.USER.getName())) {
                i.remove();
            }
        }
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
    @Deprecated
    @Column(name="entity_type", length=255)
    protected String getEntityTypeName() {
        if (entityType == null) return null;
        return entityType.name();
    }

    /** @deprecated only here to hide enums from Hibernate */
    @Deprecated
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
