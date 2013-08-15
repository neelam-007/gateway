package com.l7tech.gateway.common.security.rbac;

import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.HasFolder;
import com.l7tech.objectmodel.imp.NamedGoidEntityImp;
import com.l7tech.util.TextUtils;
import org.hibernate.annotations.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.persistence.CascadeType;
import javax.persistence.*;
import javax.persistence.Table;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class Role extends NamedGoidEntityImp implements Comparable<Role> {
    public static enum Tag { ADMIN }
    private static final Logger logger = Logger.getLogger(Role.class.getName());
    private Set<Permission> permissions = new HashSet<Permission>();
    private Set<RoleAssignment> roleAssignments = new HashSet<RoleAssignment>();
    private String description;

    private EntityType entityType;
    @Deprecated
    private Long entityOid;
    private Goid entityGoid;
    private Entity cachedSpecificEntity;
    private Tag tag;
    private boolean userCreated = false;

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

    /**
     * Check if this Role was created by an administrator (true) or else by the Gateway itself (false).  Roles
     * pre-supplied or created by the Gateway cannot normally be edited or deleted by an admin user.
     *
     * @return true if this role was created by an administrator, rather than being pre-supplied as part of the Gateway
     *              schema or auto-created by the Gateway.
     */
    @Column(name="user_created", nullable=true)
    public Boolean isUserCreated() {
        return userCreated;
    }

    public void setUserCreated(@Nullable Boolean userCreated) {
        this.userCreated = Boolean.TRUE.equals(userCreated);
    }

    @Override
    public void setName(String name) {
        name = TextUtils.truncStringMiddle(name, 128);
        super.setName(name);
    }

    /**
     * Adds a new Permission granting the specified privilege with its scope set to an
     * {@link ObjectIdentityPredicate} for the provided ID, or no scope (allowing any instance of the supplied type)
     * if id == null.
     */
    public void addEntityPermission(OperationType operation, EntityType etype, @Nullable String id) {
        Permission perm = new Permission(this, operation, etype);
        if (id != null) perm.getScope().add(new ObjectIdentityPredicate(perm, id));
        permissions.add(perm);
    }

    /**
     * Adds a new Permission granting the specified privilege with its scope set to an
     * {@link ObjectIdentityPredicate} for the provided ID, or no scope (allowing any instance of the supplied type)
     * if id == null.
     */
    public void addEntityOtherPermission( EntityType etype, String id, String operationName) {
        Permission perm = new Permission(this, OperationType.OTHER, etype);
        perm.setOtherOperationName(operationName);
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
     * @param entityGoid the ID of the entity whose folder ancestry should be made readable
     */
    public void addEntityFolderAncestryPermission(EntityType etype, Goid entityGoid) {
        Permission perm = new Permission(this, OperationType.READ, EntityType.FOLDER);
        perm.getScope().add(new EntityFolderAncestryPredicate(perm, etype, entityGoid));
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
     * Adds a new Permission granting the specified privilege with its scope set to
     * a {@link SecurityZonePredicate} for the provided entity security zone.
     *
     * @param operation  the operation that the permission grants access to
     * @param etype      the type of entity to which the permission applies
     * @param securityZone the security zone entities must be in for the new permission to apply to them
     */
    public void addSecurityZonePermission(OperationType operation, EntityType etype, SecurityZone securityZone) {
        Permission perm = new Permission(this, operation, etype);
        perm.getScope().add(new SecurityZonePredicate(perm, securityZone));
        permissions.add(perm);
    }

    /**
     * Creates and adds a new {@link RoleAssignment} assigning the provided {@link User} to this Role.
     */
    public void addAssignedUser(User user) {
        if (user != null && !isUserAssigned(user)) {
            roleAssignments.add(new RoleAssignment(this, user.getProviderId(), user.getId(), EntityType.USER));
        } else {
            logger.log(Level.FINE, "User is null or already assigned: " + user);
        }
    }

    /**
     * Creates and adds a new {@link RoleAssignment} assigning the provided {@link Group} to this Role.
     */
    public void addAssignedGroup(@NotNull final Group group) {
        if (!isGroupAssigned(group)) {
            roleAssignments.add(new RoleAssignment(this, group.getProviderId(), group.getId(), EntityType.GROUP));
        } else {
            logger.log(Level.FINE, "Group is already assigned: " + group);
        }
    }

    /**
     * Removes an {@link RoleAssignment} that was assigned by the provided {@link User} to this Role.
     *
     * @param user  The user that will be used to remove the user's assignment roles.
     */
    public void removeAssignedUser(User user) {
        if (user != null) {
            for (Iterator<RoleAssignment> i = roleAssignments.iterator(); i.hasNext();) {
                RoleAssignment roleAssignment = i.next();
                if (assignmentMatchesUser(roleAssignment, user)) {
                    i.remove();
                }
            }
        }
    }

     /**
     * Removes an {@link RoleAssignment} that was assigned by the provided {@link Group} to this Role.
     *
     * @param group  The user that will be used to remove the user's assignment roles.
     */
    public void removeAssignedGroup(Group group) {
        if (group != null) {
            for (Iterator<RoleAssignment> i = roleAssignments.iterator(); i.hasNext();) {
                RoleAssignment roleAssignment = i.next();
                if (assignmentMatchesGroup(roleAssignment, group)) {
                    i.remove();
                }
            }
        }
    }

    public String toString() {
        return _name;
    }

    @Override
    public int compareTo(Role that) {
        if (this.equals(that))
            return 0;

        int ret = Boolean.compare(this.isUserCreated(), that.isUserCreated());
        if (ret != 0)
            return ret;

        return String.CASE_INSENSITIVE_ORDER.compare( this.getName(), that.getName() );
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
    @Deprecated
    @Column(name="entity_oid")
    public Long getEntityOid() {
        return entityOid;
    }

    @Deprecated
    public void setEntityOid(Long entityOid) {
        this.entityOid = entityOid;
    }

    /**
     * If this Role is scoped to a particular entity, this property will contain the OID of that entity.
     * Otherwise, it will be null.
     */
    @Column(name="entity_goid")
    @Type(type = "com.l7tech.server.util.GoidType")
    public Goid getEntityGoid() {
        return entityGoid;
    }

    public void setEntityGoid(Goid entityGoid) {
        this.entityGoid = entityGoid;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        if (!super.equals(o)) return false;

        Role role = (Role) o;

        if (userCreated != role.userCreated) return false;
        if (tag != role.tag) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (tag != null ? tag.hashCode() : 0);
        result = 31 * result + (userCreated ? 1 : 0);
        return result;
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

    /**
     * Construct description from the role name and folder path of the Entity, if it has one. The contextual
     * description can be used to distinguish entities with the same name.
     *
     * TODO delete me and replace with calls to EntityNameResolver
     */
    @Transient
    public String getContextualDescriptiveName() {
        if(cachedSpecificEntity instanceof HasFolder) {
            final String PATH_SEPARATOR = "/";
            final String ELLIPSIS = "...";

            LinkedList<String> folderPath = new LinkedList<>();
            HasFolder folderEntity = (HasFolder) cachedSpecificEntity;

            while (null != folderEntity.getFolder()) {
                folderPath.add(folderEntity.getFolder().getName());
                folderEntity = folderEntity.getFolder();
            }

            StringBuilder description = new StringBuilder();
            description.append(getDescriptiveName());
            description.append(" (").append(PATH_SEPARATOR);

            if (folderPath.size() > 1 && folderPath.size() <= 4) {
                for (int i = folderPath.size() - 2; i >= 0; i--) {
                    description.append(folderPath.get(i));
                    description.append(PATH_SEPARATOR);
                }
            } else if(folderPath.size() > 4) {
                description.append(folderPath.get(3));
                description.append(PATH_SEPARATOR).append(ELLIPSIS).append(PATH_SEPARATOR);
                description.append(folderPath.get(0));
                description.append(PATH_SEPARATOR);
            }

            description.append(((NamedEntity) cachedSpecificEntity).getName()).append(")");

            return description.toString();
        } else {
            // can extend later to create contextual descriptive names for other entity types
            return getDescriptiveName();
        }
    }

    private boolean assignmentMatchesUser(@NotNull final RoleAssignment roleAssignment, @NotNull final User user) {
        return roleAssignment.getIdentityId().equals(user.getId()) && roleAssignment.getProviderId().equals(user.getProviderId())
                            && roleAssignment.getEntityType().equals(EntityType.USER.getName());
    }

    private boolean assignmentMatchesGroup(@NotNull final RoleAssignment roleAssignment, @NotNull final Group group) {
        return roleAssignment.getIdentityId().equals(group.getId()) && roleAssignment.getProviderId().equals(group.getProviderId())
                            && roleAssignment.getEntityType().equals(EntityType.GROUP.getName());
    }

    private boolean isUserAssigned(@NotNull final User user) {
        boolean assigned = false;
        for (Iterator<RoleAssignment> i = roleAssignments.iterator(); i.hasNext();) {
            final RoleAssignment roleAssignment = i.next();
            if(assignmentMatchesUser(roleAssignment, user)) {
                assigned = true;
                break;
            }
        }
        return assigned;
    }

    private boolean isGroupAssigned(@NotNull final Group group) {
        boolean assigned = false;
            for (Iterator<RoleAssignment> i = roleAssignments.iterator(); i.hasNext();) {
                final RoleAssignment roleAssignment = i.next();
                if(assignmentMatchesGroup(roleAssignment, group)) {
                    assigned = true;
                    break;
                }
            }
            return assigned;
    }
}
