package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlType(name="JMSConnectionType", propOrder={"extensions","properties","contextPropertiesTemplate"})
public class JMSConnection {

    //- PUBLIC

    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    public void setVersion( final Integer version ) {
        this.version = version;
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    @XmlElement(name="ContextPropertiesTemplate")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getContextPropertiesTemplate() {
        return contextPropertiesTemplate;
    }

    public void setContextPropertiesTemplate( final Map<String, Object> contextPropertiesTemplate ) {
        this.contextPropertiesTemplate = contextPropertiesTemplate;
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

    JMSConnection() {
    }

    //- PRIVATE

    private String id;
    private Integer version;
    private Map<String,Object> properties;
    private Map<String,Object> contextPropertiesTemplate;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;
}
