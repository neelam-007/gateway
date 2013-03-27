package com.l7tech.gateway.api;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import com.l7tech.gateway.api.impl.ElementExtensionSupport;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.naming.InitialContext;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * JMS Connection represents the information required to create a {@code ConnectionFactory}
 *
 * <p> The following properties can be set:
 * <ul>
 *   <li><code>jndi.initialContextFactoryClassname</code> (required for non templates): The fully qualified
 *       Java class name of the JNDI initial context factory (see
 *       {@link javax.naming.Context#INITIAL_CONTEXT_FACTORY INITIAL_CONTEXT_FACTORY})</li>
 *   <li><code>jndi.providerUrl</code> (required for non templates): URL to identify the JNDI service provider
 *       (see {@link javax.naming.Context#PROVIDER_URL PROVIDER_URL})</li>
 *   <li><code>username</code>: The connection username (see
 *       {@link javax.jms.ConnectionFactory#createConnection( String, String ) createConnection})</li>
 *   <li><code>password</code>: The connection password (see
 *       {@link javax.jms.ConnectionFactory#createConnection( String, String ) createConnection})</li>
 *   <li><code>queue.connectionFactoryName</code> (required for non templates): The name to lookup the queue connection
 *       factory in the initial JNDI context.</li>
 * </ul>
 * </p>
 *
 * <p>Context template properties are used when creating the initial JNDI context
 * (see {@link InitialContext#InitialContext(java.util.Hashtable) InitialContext}).
 * This can be used for settings such as:
 * <ul>
 *   <li>{@link javax.naming.Context#SECURITY_PRINCIPAL SECURITY_PRINCIPAL}</li>
 *   <li>{@link javax.naming.Context#SECURITY_CREDENTIALS SECURITY_CREDENTIALS}</li>
 * </ul>
 * </p>
 *
 * @see ManagedObjectFactory#createJMSConnection()
 * @see javax.jms.ConnectionFactory ConnectionFactory
 */
@XmlType(name="JMSConnectionType", propOrder={"providerTypeValue", "templateValue", "properties","contextPropertiesTemplate","extension","extensions"})
public class JMSConnection extends ElementExtensionSupport {

    //- PUBLIC

    /**
     * The identifier for the connection.
     *
     * @return The identifier or null
     */
    @XmlAttribute(name="id")
    public String getId() {
        return id;
    }

    /**
     * Set the identifier for the connection.
     *
     * @param id The identifier to use.
     */
    public void setId( final String id ) {
        this.id = id;
    }

    /**
     * Get the version for the connection.
     *
     * @return The version or null
     */
    @XmlAttribute(name="version")
    public Integer getVersion() {
        return version;
    }

    /**
     * Get the version for the connection.
     *
     * @param version The version to use
     */
    public void setVersion( final Integer version ) {
        this.version = version;
    }

    /**
     * Get the provider type for the connection.
     *
     * @return The provider type or null.
     */
    public JMSProviderType getProviderType() {
        return get(providerType);
    }

    /**
     * Set the provider type for the connection.
     *
     * @param providerType The provider type to use (may be null)
     */
    public void setProviderType( final JMSProviderType providerType ) {
        this.providerType = setNonNull( this.providerType==null ? new AttributeExtensibleJMSProviderType() : this.providerType, providerType );
    }

    /**
     * Flag for a template connection.
     *
     * @return True if this is a template (may be null)
     */
    public Boolean isTemplate() {
        return get(template);
    }

    /**
     * Set the template flag for the connection.
     *
     * @param template True for a template (may be null)
     */
    public void setTemplate( final Boolean template ) {
        this.template = set(this.template,template);
    }

    /**
     * Get the properties for the connection.
     *
     * @return The properties map or null
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the connection.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    /**
     * Get the context properties template for the connection.
     *
     * @return The context properties template or null.
     */
    @XmlElement(name="ContextPropertiesTemplate")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getContextPropertiesTemplate() {
        return contextPropertiesTemplate;
    }

    /**
     * Set the context properties template for the connection.
     *
     * @param contextPropertiesTemplate The context properties template to use.
     */
    public void setContextPropertiesTemplate( final Map<String, Object> contextPropertiesTemplate ) {
        this.contextPropertiesTemplate = contextPropertiesTemplate;
    }

    /**
     * Types for JMS providers.
     */
    @XmlEnum(String.class)
    @XmlType(name="JMSProviderTypeType")
    public enum JMSProviderType {

        /**
         * Type for TIBCO EMS.
         */
        @XmlEnumValue("TIBCO EMS") TIBCO_EMS,

        /**
         * Type for WebSphere MQ over LDAP.
         */
        @XmlEnumValue("WebSphere MQ over LDAP") WebSphere_MQ,

        /**
         * Type for FioranoMQ.
         */
        @XmlEnumValue("FioranoMQ") FioranoMQ,

        /**
         * Type for Weblogic
         */
        @XmlEnumValue("WebLogic JMS") Weblogic
    }

    //- PROTECTED

    @XmlElement(name="ProviderType")
    protected AttributeExtensibleJMSProviderType getProviderTypeValue() {
        return providerType;
    }

    protected void setProviderTypeValue( final AttributeExtensibleJMSProviderType providerType ) {
        this.providerType = providerType;
    }

    @XmlElement(name="Template")
    protected AttributeExtensibleBoolean getTemplateValue() {
        return template;
    }

    protected void setTemplateValue( final AttributeExtensibleBoolean template ) {
        this.template = template;
    }

    @XmlType(name="JMSProviderTypePropertyType")
    protected static class AttributeExtensibleJMSProviderType extends AttributeExtensible<JMSProviderType> {
        private JMSProviderType value;

        @XmlValue
        @Override
        public JMSProviderType getValue() {
            return value;
        }

        @Override
        public void setValue( final JMSProviderType value ) {
            this.value = value;
        }
    }

    //- PACKAGE

    JMSConnection() {
    }

    //- PRIVATE

    private String id;
    private Integer version;
    private AttributeExtensibleJMSProviderType providerType;
    private AttributeExtensibleBoolean template;
    private Map<String,Object> properties;
    private Map<String,Object> contextPropertiesTemplate;
}
