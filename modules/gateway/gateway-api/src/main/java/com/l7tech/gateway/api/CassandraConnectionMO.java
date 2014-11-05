package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * The CassandraConnectionMO managed object represents a Cassandra connection.
 *
 * <p>The Accessor for Cassandra connections supports read and write. Cassandra
 * connections can be accessed by name or identifier.</p>
 *
 * <p>Connection properties are passed to Cassandra. Commonly used connection
 * properties are:
 * <ul>
 *   <li><code>user</code>: The username to use for the connection</li>
 *   <li><code>password</code>: The password to use for the connection</li>
 * </ul>
 * </p>

 * @see ManagedObjectFactory#createCassandraConnectionMO()
 */
@XmlRootElement(name = "CassandraConnection")
@XmlType(name = "CassandraConnectionType", propOrder = {"name", "keyspace", "contactPoint", "port", "username", "compression", "ssl", "properties"})
@AccessorSupport.AccessibleResource(name = "cassandraConnections")
public class CassandraConnectionMO extends SecurityZoneableObject {

    //- PUBLIC

    /**
     * Get the name for the Cassandra connection (case insensitive, required)
     *
     * @return The name
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the Cassandra connection.
     *
     * @param name The name to use
     */
    public void setName(final String name) {
        this.name = set(this.name, name);
    }

    /**
     * Get the keyspace for the Cassandra connection (case insensitive, required)
     *
     * @return The keyspace
     */
    public String getKeyspace() {
        return get(keyspace);
    }

    /**
     * Set the keyspace for the Cassandra connection.
     *
     * @param keyspace The keyspace to use
     */
    public void setKeyspace(final String keyspace) {
        this.keyspace = set(this.keyspace, keyspace);
    }

    /**
     * Get the contact point for the Cassandra connection (case insensitive, required)
     *
     * @return The contact point
     */
    public String getContactPoint() {
        return get(contactPoint);
    }

    /**
     * Set the contact point for the Cassandra connection.
     *
     * @param contactPoint The contact point to use
     */
    public void setContactPoint(final String contactPoint) {
        this.contactPoint = set(this.contactPoint, contactPoint);
    }

    /**
     * Get the port for the Cassandra connection (case insensitive, required)
     *
     * @return The port
     */
    public String getPort() {
        return get(port);
    }

    /**
     * Set the port for the Cassandra connection.
     *
     * @param port The port to use
     */
    public void setPort(final String port) {
        this.port = set(this.port, port);
    }

    /**
     * Get the username for the Cassandra connection (case insensitive, required)
     *
     * @return The username
     */
    public String getUsername() {
        return get(username);
    }

    /**
     * Set the username for the Cassandra connection.
     *
     * @param username The username to use
     */
    public void setUsername(final String username) {
        this.username = set(this.username, username);
    }

    /**
     * Get the compression for the Cassandra connection (case insensitive, required)
     *
     * @return The compression
     */
    public String getCompression() {
        return get(compression);
    }

    /**
     * Set the compression for the Cassandra connection.
     *
     * @param compression The compression to use
     */
    public void setCompression(final String compression) {
        this.compression = set(this.compression, compression);
    }

    /**
     * Is SSL for the Cassandra connection enabled? (required)
     *
     * @return True if enabled.
     */
    public boolean isSsl() {
        return get(ssl, false);
    }

    /**
     * Set the SSL state of the Cassandra connection.
     *
     * @param ssl True for enabled.
     */
    public void setSsl( final boolean ssl ) {
        this.ssl = set(this.ssl, ssl);
    }

    /**
     * Is the Cassandra connection enabled? (required)
     *
     * @return True if enabled.
     */
    public boolean isEnabled() {
        return get(enabled, false);
    }

    /**
     * Set the enabled state of the Cassandra connection.
     *
     * @param enabled True for enabled.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = set(this.enabled, enabled);
    }

    /**
     * Get the properties for the Cassandra connection.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the Cassandra connection.
     *
     * @param properties The properties to use.
     */
    public void setProperties(final Map<String, String> properties) {
        this.properties = properties;
    }

    //- PACKAGE

    CassandraConnectionMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleString keyspace;
    private AttributeExtensibleString contactPoint;
    private AttributeExtensibleString port;
    private AttributeExtensibleString username;
    private AttributeExtensibleString compression;
    private AttributeExtensibleBoolean ssl = new AttributeExtensibleBoolean(false);
    private AttributeExtensibleBoolean enabled = new AttributeExtensibleBoolean(false);
    private Map<String, String> properties;
}
