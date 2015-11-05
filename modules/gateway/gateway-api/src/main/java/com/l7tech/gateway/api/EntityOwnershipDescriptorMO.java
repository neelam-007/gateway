package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 */
@XmlRootElement(name = "EntityOwnershipDescriptor")
@XmlType(name = "EntityOwnershipDescriptorType", propOrder = {"entityId", "entityType", "readOnly", "versionStamp", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "entityOwnershipDescriptor")
public class EntityOwnershipDescriptorMO extends AccessibleObject{

    //- PUBLIC

    @XmlElement(name = "EntityId", required = true)
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(final String entityId) {
        this.entityId = entityId;
    }

    @XmlElement(name = "EntityType", required = true)
    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(final String entityType) {
        this.entityType = entityType;
    }

    @XmlElement(name = "ReadOnly")
    public Boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(final Boolean readOnly) {
        this.readOnly = readOnly;
    }

    @XmlElement(name = "VersionStamp")
    public Long getVersionStamp() {
        return versionStamp;
    }

    public void setVersionStamp(final Long versionStamp) {
        this.versionStamp = versionStamp;
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


    //- PACKAGE

    EntityOwnershipDescriptorMO() {
    }


    //- PRIVATE

    private String entityId;
    private String entityType;
    private Boolean readOnly;
    private Long versionStamp;
}
