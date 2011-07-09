package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.AccessibleObject;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * AttributeExtensible extension for ManagedObjectReference[] properties.
 */
@XmlType(name="ReferenceListPropertyType")
public class AttributeExtensibleReferenceList  extends AttributeExtensibleType.AttributeExtensible<ManagedObjectReference[]> {

    private String referenceUri;
    private ManagedObjectReference[] value;
    
    @XmlAttribute(name="resourceUri")
    public String getReferenceUri() {
        return referenceUri;
    }

    public void setReferenceUri( final String referenceUri ) {
        this.referenceUri = referenceUri;
    }

    public void setReferenceType( final Class<? extends AccessibleObject> typeClass ) {
        setReferenceUri( AccessorSupport.getResourceUri(typeClass) );
    }

    @Override
    @XmlElement(name="Reference")
    public ManagedObjectReference[] getValue() {
        return value;
    }

    @Override
    public void setValue( final ManagedObjectReference[] value ) {
        this.value = value;
    }
}
