package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlType(name="PolicyDetailType", propOrder={"name","policyType","extensions","properties"})
public class PolicyDetail {

    //- PUBLIC

    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    @XmlAttribute(name="guid")
    public String getGuid() {
        return guid;
    }

    public void setGuid( final String guid ) {
        this.guid = guid;
    }

    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    public void setVersion( final Integer version ) {
        this.version = version;
    }

    @XmlAttribute(name="folderId")
    public String getFolderId() {
        return folderId;
    }

    public void setFolderId( final String folderId ) {
        this.folderId = folderId;
    }

    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    public void setName( final String name ) {
        this.name = name;
    }

    @XmlElement(name="PolicyType", required=true)
    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType( final PolicyType policyType ) {
        this.policyType = policyType;
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    @XmlEnum(String.class)
    public enum PolicyType {
        /**
         * Policy Include Fragment
         */
        @XmlEnumValue("Include") INCLUDE,

        /**
         * Internal Use Policy
         */
        @XmlEnumValue("Internal") INTERNAL
    }

    //- PROTECTED

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlAnyElement(lax=true)
    protected List<Object> getExtensions() {
        return extensions;
    }

    protected void setExtensions( final List<Object> extensions ) {
        this.extensions = extensions;
    }

    //- PACKAGE

    PolicyDetail() {
    }

    //- PRIVATE

    private String id;
    private String guid;
    private Integer version;
    private String folderId;
    private String name;
    private PolicyType policyType;
    private Map<String,Object> properties;
    private Map<QName,Object> attributeExtensions;
    private List<Object> extensions;
}
