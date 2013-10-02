package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.Map;

/**
 * The RbacRolePredicateMO object represents an rbac role predicate entity.
 */
@XmlRootElement(name = "RolePredicate")
@XmlType(name = "RolePredicateType", propOrder = {"type", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "rolePredicate")
public class RbacRolePredicateMO extends ElementExtendableAccessibleObject {
    private Type type;
    private Map<String, String> properties = new HashMap<>();

    protected RbacRolePredicateMO() {
    }

    /**
     * This is the type of predicate.
     *
     * @return The type of role predicate
     */
    @XmlElement(name = "type", required = true)
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of role predicate
     *
     * @param type The type of role predicate
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * This is the properties associated with this predicate.
     * <p/>
     * The properties used are:
     * <pre>
     * AttributePredicate: 'attribute', 'value', 'mode'
     * EntityFolderAncestryPredicate: 'entityId', 'entityType'
     * FolderPredicate: 'folderId', 'transitive'
     * ObjectIdentityPredicate: 'entityId'
     * SecurityZonePredicate: 'securityZoneId'
     * </pre>
     *
     * @return The properties associated with this predicate
     */
    @XmlElement(name = "Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Sets the properties associated with this predicate
     *
     * @param properties The properties associated with this predicate
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    /**
     * These are the different types of predicates.
     */
    @XmlEnum(String.class)
    @XmlType(name = "RbacRolePredicateType")
    public enum Type {
        @XmlEnumValue("AttributePredicate")AttributePredicate,
        @XmlEnumValue("EntityFolderAncestryPredicate")EntityFolderAncestryPredicate,
        @XmlEnumValue("FolderPredicate")FolderPredicate,
        @XmlEnumValue("ObjectIdentityPredicate")ObjectIdentityPredicate,
        @XmlEnumValue("SecurityZonePredicate")SecurityZonePredicate;
    }
}
