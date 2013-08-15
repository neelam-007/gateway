package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import java.util.List;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The SecurityZoneMO object represents a Security Zone configuration.
 */
@XmlRootElement(name="SecurityZone")
@XmlType(name="SecurityZoneType", propOrder={"nameValue", "descriptionValue", "permittedEntityTypesValue", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "securityZones")
public class SecurityZoneMO extends AccessibleObject {

    //- PUBLIC

    public String getName() {
        return get(name);
    }

    public void setName(String name) {
        this.name = set(this.name, name);
    }

    public String getDescription() {
        return get(description);
    }

    public void setDescription(String description) {
        this.description = set(this.description, description);
    }

    public List<String> getPermittedEntityTypes() {
        return unwrap(get( permittedEntityTypes ));
    }

    public void setPermittedEntityTypes(List<String> permittedEntityTypes) {
        this.permittedEntityTypes = set(this.permittedEntityTypes, wrap(permittedEntityTypes, AttributeExtensibleStringBuilder));
    }

    //- PROTECTED

    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @XmlElement(name = "Name", required = true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name = "Description", required = true)
    protected AttributeExtensibleString getDescriptionValue() {
        return description;
    }

    protected void setDescriptionValue( final AttributeExtensibleString description ) {
        this.description = description;
    }

    @XmlElement(name = "PermittedEntityTypes", required = true)
    protected AttributeExtensibleStringList getPermittedEntityTypesValue() {
        return permittedEntityTypes;
    }

    protected void setPermittedEntityTypesValue(AttributeExtensibleStringList permittedEntities) {
        this.permittedEntityTypes = permittedEntities;
    }

    //- PACKAGE

    SecurityZoneMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString description;
    private AttributeExtensibleStringList permittedEntityTypes;

}
