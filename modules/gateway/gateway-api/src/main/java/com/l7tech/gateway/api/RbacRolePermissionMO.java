package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The RbacRolePermissionMO object represents an rbac role permission entity.
 */
@XmlRootElement(name = "RolePermission")
@XmlType(name = "RolePermissionType", propOrder = {"operation", "otherOperationName", "entityType", "scope", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "rolePermission")
public class RbacRolePermissionMO extends ElementExtendableAccessibleObject {
    private OperationType operation;
    private String otherOperationName;
    private String entityType;
    private List<RbacRolePredicateMO> scope = new ArrayList<>();

    protected RbacRolePermissionMO() {
    }

    /**
     * These are the different operation types that the permission can have.
     */
    @XmlEnum(String.class)
    @XmlType(name = "RbacRolePermissionOperationType")
    public enum OperationType {
        @XmlEnumValue("CREATE")CREATE,
        @XmlEnumValue("READ")READ,
        @XmlEnumValue("UPDATE")UPDATE,
        @XmlEnumValue("DELETE")DELETE,
        @XmlEnumValue("OTHER")OTHER,
        @XmlEnumValue("NONE")NONE
    }

    /**
     * This is the operation type of the permission.
     *
     * @return The operation type associated with this permission
     */
    @XmlElement(name = "operationType", required = true)
    public OperationType getOperation() {
        return operation;
    }

    /**
     * Sets the operation type of the permission
     *
     * @param operation The operation type of the permission
     */
    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    /**
     * If the operation type is OTHER the operation name needs to be specified
     *
     * @return The operation name of the 'OTHER' operation type
     */
    @XmlElement(name = "otherOperationName")
    public String getOtherOperationName() {
        return otherOperationName;
    }

    /**
     * Sets the other operation name
     *
     * @param otherOperationName The other operation name
     */
    public void setOtherOperationName(String otherOperationName) {
        this.otherOperationName = otherOperationName;
    }

    /**
     * The entity type of the permission.
     *
     * @return The entity type associated with this permission
     */
    @XmlElement(name = "entityType")
    public String getEntityType() {
        return entityType;
    }

    /**
     * Sets the entity type of the permission
     *
     * @param entityType The entity type of the permission
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    /**
     * The list of predicates that this permission has.
     *
     * @return The list of predicates of associated with this permission
     */
    @XmlElementWrapper(name = "predicates", required = false)
    @XmlElement(name = "predicate", required = false)
    public List<RbacRolePredicateMO> getScope() {
        return scope;
    }

    /**
     * Sets the list of predicated for this permission
     *
     * @param scope The list of predicates for this permission
     */
    public void setScope(List<RbacRolePredicateMO> scope) {
        this.scope = scope;
    }
}
