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
 * The details for a JMSDestinationMO
 *
 * @see ManagedObjectFactory#createJMSDestinationDetails()
 */
@XmlType(name="JMSDestinationDetailsType", propOrder={"destinationName", "inbound", "enabled", "extensions", "properties"})
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
    @XmlElement(name="DestinationName", required=true)
    public String getDestinationName() {
        return destinationName;
    }

    /**
     * Set the destination name.
     *
     * @param destinationName The destination name to use.
     */
    public void setDestinationName( final String destinationName ) {
        this.destinationName = destinationName;
    }

    /**
     * Flag for inbound/outbound destinations (required)
     *
     * <p>Inbound destinations are for JMS transport, outbound destinations can
     * be used with the JMS routing assertion.</p>
     *
     * @return True if the destination is inbound
     */
    @XmlElement(name="Inbound", required=true)
    public boolean isInbound() {
        return inbound;
    }

    /**
     * Set the inbound/outbound flag.
     *
     * @param inbound True for an inbound destination.
     */
    public void setInbound( final boolean inbound ) {
        this.inbound = inbound;
    }

    /**
     * Flag to enable/disable the destination (required)
     *
     * <p>If an inbound destination is disabled no messages are processed for
     * the destination.</p>
     *
     * @return True if enabled.
     */
    @XmlElement(name="Enabled", required=true)
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the enabled flag for the destination.
     *
     * @param enabled True to enable.
     */
    public void setEnabled( final boolean enabled ) {
        this.enabled = enabled;
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
    private Integer version;
    private String destinationName;
    private boolean inbound;
    private boolean enabled;
    private Map<String,Object> properties;
    private List<Object> extensions;
    private Map<QName,Object> attributeExtensions;
}
