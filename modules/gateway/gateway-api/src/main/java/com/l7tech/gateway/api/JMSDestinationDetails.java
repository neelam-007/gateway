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
@XmlType(name="JMSDestinationDetailsType", propOrder={"destinationName", "inbound", "enabled", "extensions", "properties"})
public class JMSDestinationDetails {

    //- PUBLIC

    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    public void setId( final String id ) {
        this.id = id;
    }

    @XmlAttribute(name="version")
    public int getVersion() {
        return version;
    }

    public void setVersion( final int version ) {
        this.version = version;
    }

    @XmlElement(name="DestinationName", required=true)
    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName( final String destinationName ) {
        this.destinationName = destinationName;
    }

    @XmlElement(name="Inbound", required=true)
    public boolean isInbound() {
        return inbound;
    }

    public void setInbound( final boolean inbound ) {
        this.inbound = inbound;
    }

    @XmlElement(name="Enabled", required=true)
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled( final boolean enabled ) {
        this.enabled = enabled;
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
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

    JMSDestinationDetails() {        
    }

    //- PRIVATE

    private String id;
    private int version;
    private String destinationName;
    private boolean inbound;
    private boolean enabled;
    private Map<String,Object> properties;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;
}
