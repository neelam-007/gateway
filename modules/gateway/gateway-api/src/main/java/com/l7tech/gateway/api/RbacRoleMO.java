package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * The RbacRoleMO object represents an rbac role entity.
 */
@XmlRootElement(name = "Role")
@XmlType(name = "RbacRoleType", propOrder = {"name", "description", "userCreated", "entityType", "entityID", "permissions", "assignments", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "roles")
public class RbacRoleMO extends ElementExtendableAccessibleObject {

    private String name;
    private String description;
    private boolean userCreated = true;
    private String entityType;
    private String entityID;
    private List<RbacRolePermissionMO> permissions = new ArrayList<>();
    private List<RbacRoleAssignmentMO> assignments = new ArrayList<>();

    protected RbacRoleMO() {
    }

    /**
     * This is the name of the role. It must be unique.
     *
     * @return The name of the role.
     */
    @XmlElement(name = "name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the role. The name must be unique
     *
     * @param name The name of the role.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * This is the role description
     *
     * @return The role description
     */
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    /**
     * Sets the role description
     *
     * @param description The role description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * This is the user created flag on the role. On new roles or role updated this value is ignored and automatically
     * set to true. For Gateway managed roles this will be false. Gateway managed roles cannot be updated or deleted.
     * You can only add or remove assignments from gateway managed roles.
     *
     * @return True if the role is user created. False if it is no user created (gateway managed)
     */
    @XmlElement(name = "userCreated")
    public boolean isUserCreated() {
        return userCreated;
    }

    /**
     * Sets the user created flag.
     *
     * @param userCreated The user created flag
     */
    public void setUserCreated(boolean userCreated) {
        this.userCreated = userCreated;
    }

    /**
     * This is the list of permissions associated with this role.
     *
     * @return The list of permissions for this role.
     */
    @XmlElementWrapper(name = "permissions", required = false)
    @XmlElement(name = "permission", required = false)
    public List<RbacRolePermissionMO> getPermissions() {
        return permissions;
    }

    /**
     * Sets the list of permissions for this role.
     *
     * @param permissions The list of permissions for this role.
     */
    public void setPermissions(List<RbacRolePermissionMO> permissions) {
        this.permissions = permissions;
    }

    /**
     * This is the list of assignments associated with this role.
     *
     * @return The list of assignments for this role
     */
    @XmlElementWrapper(name = "assignments", required = false)
    @XmlElement(name = "assignment", required = false)
    public List<RbacRoleAssignmentMO> getAssignments() {
        return assignments;
    }

    /**
     * Sets the list of assignments for this role.
     *
     * @param assignments This list of assignments for this role.
     */
    public void setAssignments(List<RbacRoleAssignmentMO> assignments) {
        this.assignments = assignments;
    }

    /**
     * The entity type that this role is associated with. This is used for auto generated roles to associate them with a specific entity.
     * @return The entity type that this role is associated with
     */
    @XmlElement(name = "entityType", required = false)
    public String getEntityType() {
        return entityType;
    }

    /**
     * Returns the entity type that this role is associated with
     * @param entityType The entity type that this role is associated with
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * The entity id that this role is associated with. This is used for auto generated roles to associate them with a specific entity.
     * @return The entity id that this role is associated with
     */
    @XmlElement(name = "entityID", required = false)
    public String getEntityID() {
        return entityID;
    }

    /**
     * Returns the entity id that this role is associated with
     * @param entityID The entity id that this role is associated with
     */
    public void setEntityID(String entityID) {
        this.entityID = entityID;
    }
}
