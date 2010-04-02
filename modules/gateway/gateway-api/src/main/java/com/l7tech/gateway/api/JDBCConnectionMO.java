package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
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
@XmlType(name="JDBCConnectionType", propOrder={"nameValue","enabledValue","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="jdbcConnections")
public class JDBCConnectionMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the jdbc connection (case insensitive, required)
     *
     * @return The name (may be null)
     */
    @XmlTransient
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
    @XmlTransient
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
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @Override
    protected void setExtension( final Extension extension ) {
        super.setExtension( extension );
    }
    
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

    private AttributeExtensibleString name;
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);
    private Map<String,Object> properties;
}
