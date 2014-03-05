package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.api.impl.ElementExtensionSupport;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import com.l7tech.util.Functions;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The details for a JMSDestinationMO
 *
 * @see ManagedObjectFactory#createJMSDestinationDetails()
 */
@XmlType(name="JMSDestinationDetailType", propOrder={"nameValue", "destinationNameValue", "inboundValue", "enabledValue", "templateValue", "passthroughMessageRules", "jmsEndpointMessagePropertyRuleList", "properties", "extension", "extensions"})
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
     * Set the passthrougMessageRules flag for the destination
     * @return passthroughMessageRules True for passthrough
     */
    public Boolean isPassthroughMessageRules() {
        return get(passthroughMessageRules, Boolean.TRUE);
    }

    public void setPassthroughMessageRules(final Boolean passthroughMessageRules) {
        this.passthroughMessageRules = set(this.passthroughMessageRules, passthroughMessageRules);
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

    @XmlElement(name="PassthroughMessageRules")
    public AttributeExtensibleBoolean getPassthroughMessageRules() {
        return passthroughMessageRules;
    }

    public void setPassthroughMessageRules(final AttributeExtensibleBoolean passthroughMessageRules) {
        this.passthroughMessageRules = passthroughMessageRules;
    }

    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);

    //- PACKAGE

    JMSDestinationDetail() {
    }


    @XmlRootElement(name="JmsEndpointMessagePropertyRule")
    @XmlType(name="JmsEndpointMessagePropertyRuleType",propOrder={"rulenameValue","passThruValue","customPatternValue","extension","extensions"})
    public static class JmsEndpointMessagePropertyRuleSupport extends ElementExtensionSupport {
        private AttributeExtensibleString rulename;
        private AttributeExtensibleString customPattern;
        private AttributeExtensibleBoolean passThru;

        protected  JmsEndpointMessagePropertyRuleSupport(){

        }

        public String getRulename() {
            return get(rulename);
        }

        public void setRulename(String rulename) {
            this.rulename = set(this.rulename, rulename);
        }

        public Boolean getPassThru() {
            return get(passThru);
        }

        public void setPassThru(Boolean passThru) {
            this.passThru = set(this.passThru, passThru);
        }


        public String getCustomPattern() {
            return get(customPattern);
        }

        public void setCustomPattern(String customPattern) {
            this.customPattern = set(this.customPattern, customPattern);
        }

        @XmlElement(name = "RuleName")
        protected AttributeExtensibleString getRulenameValue() {
            return rulename;
        }

        protected void setRulenameValue(AttributeExtensibleString rulenameValue) {
            this.rulename = rulenameValue;
        }

        @XmlElement(name="PassThru")
        protected  AttributeExtensibleBoolean getPassThruValue() {
            return passThru;
        }

        protected void setPassThruValue(AttributeExtensibleBoolean passThruValue) {
            this.passThru = passThruValue;
        }

        @XmlElement(name="CustomPattern")
        protected AttributeExtensibleString getCustomPatternValue() {
            return customPattern;
        }

        protected void setCustomPatternValue(AttributeExtensibleString customPatternValue) {
            this.customPattern = customPatternValue;
        }
    }

    @XmlType(name="JmsEndpointMessagePropertyRulesType", propOrder={"value"})
    public static class JmsEndpointMessagePropertyRuleList extends AttributeExtensibleType.AttributeExtensible<List<JmsEndpointMessagePropertyRuleSupport>> {

        protected List<JmsEndpointMessagePropertyRuleSupport> value = new ArrayList<>();

        protected  JmsEndpointMessagePropertyRuleList(){

        }
        /**
         * Get the value of the attribute.
         *
         * @return the value (may be null)
         */
        @Override
        @XmlElement(name="JmsEndpointMessagePropertyRule")
        public List<JmsEndpointMessagePropertyRuleSupport> getValue() {
            return value;
        }

        /**
         * Set the value of the attribute.
         *
         * @param value The value to use (should not be null)
         */
        @Override
        public void setValue(List<JmsEndpointMessagePropertyRuleSupport> value) {
            this.value = value;
        }

        private static final Functions.Nullary<JmsEndpointMessagePropertyRuleList> Builder =
                new Functions.Nullary<JmsEndpointMessagePropertyRuleList>(){
                    @Override
                    public JmsEndpointMessagePropertyRuleList call() {
                        return new JmsEndpointMessagePropertyRuleList();
                    }
                };

    }


    public List<JmsEndpointMessagePropertyRuleSupport> getJmsEndpointMessagePropertyRules() {
        return get(jmsEndpointMessagePropertyRuleList, new ArrayList<JmsEndpointMessagePropertyRuleSupport>() );
    }

    public void setJmsEndpointMessagePropertyRules(List<JmsEndpointMessagePropertyRuleSupport> rules) {
        this.jmsEndpointMessagePropertyRuleList = set(this.jmsEndpointMessagePropertyRuleList, rules, JmsEndpointMessagePropertyRuleList.Builder);

    }
    @XmlElement(name="JmsEndpointMessagePropertyRules")
    public JmsEndpointMessagePropertyRuleList getJmsEndpointMessagePropertyRuleList() {
        return jmsEndpointMessagePropertyRuleList;
    }

    public void setJmsEndpointMessagePropertyRuleList(JmsEndpointMessagePropertyRuleList jmsEndpointMessagePropertyRuleList) {
        this.jmsEndpointMessagePropertyRuleList = jmsEndpointMessagePropertyRuleList;
    }
    //- PRIVATE
    private String id;
    private Integer version;
    private AttributeExtensibleString name;
    private AttributeExtensibleString destinationName;

    private AttributeExtensibleBoolean inbound = new AttributeExtensibleBoolean(false);

    private AttributeExtensibleBoolean template;
    private AttributeExtensibleBoolean passthroughMessageRules;

    private Map<String,Object> properties;

    private JmsEndpointMessagePropertyRuleList jmsEndpointMessagePropertyRuleList;
}
