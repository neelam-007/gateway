package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtensionSupport;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * The JDBCConnectionMO managed object represents a JDBC connection.
 *
 * <p>The Accessor for JDBC connections supports read and write. JDBC
 * connections can be accessed by name or identifier.</p>

 * <p>The following properties can be used:
 * <ul>
 *   <li><code>minimumPoolSize</code>: Optional minimum connection pool size
 *   (integer, default 3)</li>
 *   <li><code>maximumPoolSize</code>: Optional maximum connection pool size
 *   (integer, default 15)</li>
 * </ul>
 * </p>
 *
 * <p>Connection properties are passed to JDBC. Commonly used connection
 * properties are:
 * <ul>
 *   <li><code>user</code>: The username to use for the connection</li>
 *   <li><code>password</code>: The password to use for the connection</li>
 * </ul>
 * </p>

 * @see ManagedObjectFactory#createJDBCConnection()
 */
@XmlRootElement(name="JDBCConnection")
@XmlType(name="JDBCConnectionType", propOrder={"nameValue","enabledValue","properties","jdbcExtension","extensions"})
@AccessorSupport.AccessibleResource(name ="jdbcConnections")
public class JDBCConnectionMO extends SecurityZoneableObject {

    //- PUBLIC

    /**
     * Get the name for the jdbc connection (case insensitive, required)
     *
     * @return The name (may be null)
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the jdbc connection.
     *
     * @param name The name to use
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Is the jdbc connection enabled? (required)
     *
     * @return True if enabled.
     */
    public boolean isEnabled() {
        return get(enabled,false);
    }

    /**
     * Set the enabled state of the jdbc connection.
     *
     * @param enabled True for enabled.
     */
    public void setEnabled( final boolean enabled ) {
        this.enabled = set(this.enabled,enabled);
    }

    /**
     * Get the driver class for the jdbc connection.
     *
     * @return The driver class name or null
     */
    public String getDriverClass() {
        return get(this.jdbcExtension.driverClass);
    }

    /**
     * Set the driver class for the jdbc connection.
     *
     * @param driverClass The name of the driver class to use
     */
    public void setDriverClass( final String driverClass ) {
        this.jdbcExtension.driverClass = set(this.jdbcExtension.driverClass,driverClass);
    }

    /**
     * Get the jdbc URL for the jdbc connection.
     *
     * @return The URL or null
     */
    public String getJdbcUrl() {
        return get(this.jdbcExtension.jdbcUrl);
    }

    /**
     * Set the jdbc URL for the jdbc connection.
     *
     * @param jdbcUrl The URL to use
     */
    public void setJdbcUrl( final String jdbcUrl ) {
        this.jdbcExtension.jdbcUrl = set(this.jdbcExtension.jdbcUrl,jdbcUrl);
    }

    /**
     * Get the connection properties for the jdbc connection.
     *
     * @return The connection properties or null
     */
    public Map<String, Object> getConnectionProperties() {
        return jdbcExtension.connectionProperties;
    }

    /**
     * Set the connection properties for the jdbc connection.
     *
     * @param properties The connection properties to use
     */
    public void setConnectionProperties( final Map<String, Object> properties ) {
        this.jdbcExtension.connectionProperties = properties;
    }

    /**
     * Get the properties for the jdbc connection.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the jdbc connection.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="Enabled", required=true)
    protected AttributeExtensibleBoolean getEnabledValue() {
        return enabled;
    }

    protected void setEnabledValue( final AttributeExtensibleBoolean value ) {
        this.enabled = value;
    }

    @XmlElement(name="Extension")
    protected JdbcExtension getJdbcExtension() {
        return this.jdbcExtension;
    }

    protected void setJdbcExtension( final JdbcExtension extension ) {
        this.jdbcExtension = extension == null ? new JdbcExtension() : extension;
    }
    
    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @XmlType(name="JdbcExtensionType", propOrder={"driverClass","jdbcUrl","connectionProperties","extension", "extensions"})
    protected static class JdbcExtension extends ElementExtensionSupport {
        private AttributeExtensibleString driverClass;
        private AttributeExtensibleString jdbcUrl;
        private Map<String,Object> connectionProperties;

        @XmlElement(name="DriverClass", required=true)
        protected AttributeExtensibleString getDriverClass() {
            return driverClass;
        }

        protected void setDriverClass( final AttributeExtensibleString driverClass ) {
            this.driverClass = driverClass;
        }

        @XmlElement(name="JdbcUrl", required=true)
        protected AttributeExtensibleString getJdbcUrl() {
            return jdbcUrl;
        }

        protected void setJdbcUrl( final AttributeExtensibleString jdbcUrl ) {
            this.jdbcUrl = jdbcUrl;
        }

        @XmlElement(name="ConnectionProperties")
        @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
        public Map<String, Object> getConnectionProperties() {
            return connectionProperties;
        }

        public void setConnectionProperties( final Map<String, Object> connectionProperties ) {
            this.connectionProperties = connectionProperties;
        }
    }

    //- PACKAGE

    JDBCConnectionMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);
    private Map<String,Object> properties;
    private JdbcExtension jdbcExtension = new JdbcExtension();
}
