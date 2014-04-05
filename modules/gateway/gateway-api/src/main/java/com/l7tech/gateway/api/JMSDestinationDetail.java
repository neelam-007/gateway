package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.ElementExtensionSupport;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * The details for a JMSDestinationMO
 *
 * @see ManagedObjectFactory#createJMSDestinationDetails()
 */
@XmlType(name="JMSDestinationDetailType", propOrder={"nameValue", "destinationNameValue", "inboundValue", "enabledValue", "templateValue", "properties", "extension", "extensions"})
public class JMSDestinationDetail extends ElementExtensionSupport {

    //- PUBLIC

    /**
     * Property value for Queue destination "type" property.
     */
    public static final String TYPE_QUEUE = "Queue";

    /**
     * Property value for Topic destination "type" property.
     */
    public static final String TYPE_TOPIC = "Topic";

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
     * Get the name for this destination.
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for this destination.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the destination name (required for non templates)
     *
     * <p>The destination name is used to look up the JMS destination in the JNDI
     * context.</p>
     *
     * @return the name or null
     */
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
     * Flag for a template destination.
     *
     * @return True if this is a template (may be null)
     */
    public Boolean isTemplate() {
        return get(template);
    }

    /**
     * Set the template flag for the destination.
     *
     * @param template True for a template (may be null)
     */
    public void setTemplate( final Boolean template ) {
        this.template = set(this.template,template);
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

    @XmlElement(name="Name",required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="DestinationName")
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

    @XmlElement(name="Template")
    protected AttributeExtensibleBoolean getTemplateValue() {
        return template;
    }

    protected void setTemplateValue( final AttributeExtensibleBoolean template ) {
        this.template = template;
    }

    //- PACKAGE

    JMSDestinationDetail() {
    }

    //- PRIVATE


    private String id;
    private Integer version;
    private AttributeExtensibleString name;
    private AttributeExtensibleString destinationName;
    private AttributeExtensibleBoolean inbound = new AttributeExtensibleBoolean(false);
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);
    private AttributeExtensibleBoolean template;
    private Map<String,Object> properties;
}
