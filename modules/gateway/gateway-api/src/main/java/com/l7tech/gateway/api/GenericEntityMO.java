package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.List;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

@XmlRootElement(name = "GenericEntity")
@XmlType(name = "GenericEntityType", propOrder = {"nameValue", "descriptionValue", "entityClassNameValue", "enabledValue", "valueXmlValue", "extensions"})
@AccessorSupport.AccessibleResource(name = "genericEntities")
//todo validate the required properties and how a missing property is reported as an error
public class GenericEntityMO extends AccessibleObject {

    //- PUBLIC

    /**
     * The name for the generic entity (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the generic entity.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name, name);
    }

    /**
     * The description for the generic entity. Case sensitive.
     *
     * @return The description (may be null).
     */
    public String getDescription() {
        return get(description);
    }

    /**
     * Set the description for the generic entity.
     *
     * @param description The description to use.
     */
    public void setDescription(String description) {
        this.description = set(this.description, description);
    }

    /**
     * The entity class name for the generic entity. Case sensitive.
     *
     * @return The entity class name (may be null).
     */
    public String getEntityClassName() {
        return get(entityClassName);
    }

    /**
     * Set the entity class name for the generic entity (required).
     *
     * @param entityClassName The entity class name to use.
     */
    public void setEntityClassName(String entityClassName) {
        this.entityClassName = set(this.entityClassName, entityClassName);
    }

    /**
     * Whether the generic entity is enabled or not.
     *
     * @return enabled status.
     */
    public boolean getEnabled() {
        return get(enabled);
    }

    /**
     * Set whether the generic entity is enabled or not.
     *
     * @param enabled the value of enabled to use
     */
    public void setEnabled(boolean enabled) {
        this.enabled = set(this.enabled, enabled);
    }

    /**
     * The value xml for the generic entity. Case sensitive.
     *
     * @return The value xml (may be null).
     */
    public String getValueXml() {
        return get(valueXml);
    }

    /**
     * Set the value xml for the generic entity. (required)
     *
     * @param valueXml The value to use.
     */
    public void setValueXml(String valueXml) {
        this.valueXml = set(this.valueXml, valueXml);
    }

    //- PROTECTED

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name = "Description")
    protected AttributeExtensibleString getDescriptionValue() {
        return description;
    }

    protected void setDescriptionValue(final AttributeExtensibleString description) {
        this.description = description;
    }

    @XmlElement(name = "EntityClassName", required = true)
    protected AttributeExtensibleString getEntityClassNameValue() {
        return entityClassName;
    }

    protected void setEntityClassNameValue(final AttributeExtensibleString entityClassName) {
        this.entityClassName = entityClassName;
    }

    @XmlElement(name = "ValueXml", required = true)
    protected AttributeExtensibleString getValueXmlValue() {
        return valueXml;
    }

    protected void setValueXmlValue(final AttributeExtensibleString valueXml) {
        this.valueXml = valueXml;
    }

    @XmlElement(name = "Enabled", required = true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue(final AttributeExtensibleBoolean enabled) {
        this.enabled = enabled;
    }

    //- PACKAGE

    GenericEntityMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString description;
    private AttributeExtensibleString entityClassName;
    private AttributeExtensibleString valueXml;
    private AttributeExtensibleBoolean enabled;

}
