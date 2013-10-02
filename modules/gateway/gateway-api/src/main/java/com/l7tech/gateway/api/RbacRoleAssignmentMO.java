package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * The RbacRoleAssignmentMO object represents an rbac role assignment entity.
 */
@XmlRootElement(name = "RoleAssignment")
@XmlType(name = "RoleAssignmentType", propOrder = {"providerId", "identityId", "entityType", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "roleAssignment")
public class RbacRoleAssignmentMO extends ElementExtendableAccessibleObject {
    private String providerId;
    private String identityId;
    private String entityType;

    protected RbacRoleAssignmentMO() {
    }

    /**
     * This is the provider Id that the role assignment user or group comes from.
     *
     * @return The provider id of the role assignment
     */
    @XmlElement(name = "providerId", required = true)
    public String getProviderId() {
        return providerId;
    }

    /**
     * Sets the provider id of the role assignment
     *
     * @param providerId The preovider id of the role assignment
     */
    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    /**
     * This is the identity id of the assignment user or group
     *
     * @return The identity id of the assignment user or group
     */
    @XmlElement(name = "identityId", required = true)
    public String getIdentityId() {
        return identityId;
    }

    /**
     * Sets the identity id of the assignment user or group
     *
     * @param identityId The identity id of the assignment user or group
     */
    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    /**
     * This is the entity type of the assignment. It is either 'User' or "Group'
     *
     * @return The entity type of the assignment. Either user or group
     */
    @XmlElement(name = "entityType")
    public String getEntityType() {
        return entityType;
    }

    /**
     * Sets the entity type of the assignment. Either 'User' or 'Group'
     *
     * @param entityType The entity type of the assignment. Either user or group
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
}
