package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

/**
 * The details for a JMSDestinationMO
 *
 * @see ManagedObjectFactory#createJMSDestinationDetails()
 */
@XmlType(name="JMSDestinationDetailsType", propOrder={"destinationNameValue", "inboundValue", "enabledValue", "properties", "extension", "extensions"})
public class JMSDestinationDetails {

    //- PUBLIC

    /**
     * The identifier for the details.
     *
     * @return The identifier or null
     */
    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    /**
     * Set the identifier for the details.
     *
     * @param id The identifier to use.
     */
    public void setId( final String id ) {
        this.id = id;
    }

    /**
     * Get the version for the details.
     *
     * @return The version or null
     */
    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    /**
     * Set the version for the details.
     *
     * @param version The version to use.
     */
    public void setVersion( final Integer version ) {
        this.version = version;
    }

    /**
     * Get the destination name (required)
     *
     * <p>The destination name is used to look up the JMS destination in the JNDI
     * context.</p>
     *
     * @return the name or null
     */
    @XmlTransient
    public String getDestinationName() {
        return get(destinationName);
    }

    /**
     * Set the destination name.
     *
     * @param destinationName The destination name to use.
     */
    public void setDestinationName( final String destinationName ) {
        this.destinationName = set(this.destinationName, destinationName);
    }

    /**
     * Flag for inbound/outbound destinations (required)
     *
     * <p>Inbound destinations are for JMS transport, outbound destinations can
     * be used with the JMS routing assertion.</p>
     *
     * @return True if the destination is inbound
     */
    @XmlTransient
    public boolean isInbound() {
        return get(inbound, false);
    }

    /**
     * Set the inbound/outbound flag.
     *
     * @param inbound True for an inbound destination.
     */
    public void setInbound( final boolean inbound ) {
        this.inbound = set(this.inbound,inbound);
    }

    /**
     * Flag to enable/disable the destination (required)
     *
     * <p>If an inbound destination is disabled no messages are processed for
     * the destination.</p>
     *
     * @return True if enabled.
     */
    @XmlTransient
    public boolean isEnabled() {
        return get(enabled,false);
    }

    /**
     * Set the enabled flag for the destination.
     *
     * @param enabled True to enable.
     */
    public void setEnabled( final boolean enabled ) {
        this.enabled = set(this.enabled,enabled);
    }

    /**
     * Get the properties for the destination.
     *
     * @return The properties or null
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the destination.
     *
     * @param properties The properties to use
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="DestinationName", required=true)
    protected AttributeExtensibleString getDestinationNameValue() {
        return destinationName;
    }

    protected void setDestinationNameValue( final AttributeExtensibleString destinationName ) {
        this.destinationName = destinationName;
    }

    @XmlElement(name="Inbound", required=true)
    protected AttributeExtensibleBoolean getInboundValue() {
        return inbound;
    }

    protected void setInboundValue( final AttributeExtensibleBoolean inbound ) {
        this.inbound = inbound;
    }

    @XmlElement(name="Enabled", required=true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue( final AttributeExtensibleBoolean enabled ) {
        this.enabled = enabled;
    }

    @XmlAnyAttribute
    protected Map<QName, Object> getAttributeExtensions() {
        return attributeExtensions;
    }

    protected void setAttributeExtensions( final Map<QName, Object> attributeExtensions ) {
        this.attributeExtensions = attributeExtensions;
    }

    @XmlElement(name="Extension")
    protected Extension getExtension() {
        return extension;
    }

    protected void setExtension( final Extension extension ) {
        this.extension = extension;
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
    private Integer version;
    private AttributeExtensibleString destinationName;
    private AttributeExtensibleBoolean inbound = new AttributeExtensibleBoolean(false);
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);
    private Map<String,Object> properties;
    private Extension extension;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;
}
