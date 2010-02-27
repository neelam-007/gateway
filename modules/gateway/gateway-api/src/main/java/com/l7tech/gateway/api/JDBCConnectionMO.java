package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;
import com.l7tech.gateway.api.impl.PropertiesMapType;

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
 * <p>The Accessor for JDBC connections is read only. JDBC connections can be
 * accessed by name or identifier.</p>
 *
 * @see ManagedObjectFactory#createJDBCConnection()
 */
@XmlRootElement(name="JDBCConnection")
@XmlType(name="JDBCConnectionType", propOrder={"name","enabled","extensions","properties"})
@AccessorFactory.AccessibleResource(name ="jdbcConnections")
public class JDBCConnectionMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the jdbc connection (case insensitive, required)
     *
     * @return The name (may be null)
     */
    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the jdbc connection.
     *
     * @param name The name to use
     */
    public void setName( final String name ) {
        this.name = name;
    }

    /**
     * Is the jdbc connection enabled? (required)
     *
     * @return True if enabled.
     */
    @XmlElement(name="Enabled", required=true)
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the enabled state of the jdbc connection.
     *
     * @param enabled True for enabled.
     */
    public void setEnabled( final boolean enabled ) {
        this.enabled = enabled;
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

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    JDBCConnectionMO() {
    }

    //- PRIVATE

    private String name;
    private boolean enabled;
    private Map<String,Object> properties;
}
