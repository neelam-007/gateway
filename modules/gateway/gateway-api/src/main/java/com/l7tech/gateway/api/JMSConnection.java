package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.naming.InitialContext;
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
 * JMS Connection represents the information required to create a {@code ConnectionFactory}
 *
 * <p> The following properties can be set:
 * <ul>
 *   <li><code>jndi.initialContextFactoryClassname</code> (required): The fully qualified
 *       Java class name of the JNDI initial context factory (see
 *       {@link javax.naming.Context#INITIAL_CONTEXT_FACTORY INITIAL_CONTEXT_FACTORY})</li>
 *   <li><code>jndi.providerUrl</code> (required): URL to identify the JNDI service provider
 *       (see {@link javax.naming.Context#PROVIDER_URL PROVIDER_URL})</li>
 *   <li><code>username</code>: The connection username (see
 *       {@link javax.jms.ConnectionFactory#createConnection( String, String ) createConnection})</li>
 *   <li><code>password</code>: The connection password (see
 *       {@link javax.jms.ConnectionFactory#createConnection( String, String ) createConnection})</li>
 *   <li><code>queue.connectionFactoryName</code> (required): The name to lookup the queue connection
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
@XmlType(name="JMSConnectionType", propOrder={"extensions","properties","contextPropertiesTemplate"})
public class JMSConnection {

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
