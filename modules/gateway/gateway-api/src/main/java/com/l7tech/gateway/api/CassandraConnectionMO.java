package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtendableAccessibleObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

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
@XmlType(name = "CassandraConnectionType",
        propOrder = {"name", "keyspace", "contactPoint", "port", "username", "compression", "ssl", "tlsciphers", "enabled", "properties", "extension", "extensions"})
@AccessorSupport.AccessibleResource(name = "cassandraConnections")
public class CassandraConnectionMO extends ElementExtendableAccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the Cassandra connection (case insensitive, required)
     *
     * @return The name
     */
    @XmlElement(name = "Name", required = true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the Cassandra connection.
     *
     * @param name The name to use
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the keyspace for the Cassandra connection (case insensitive, required)
     *
     * @return The keyspace
     */
    @XmlElement(name = "Keyspace", required = true)
    public String getKeyspace() {
        return keyspace;
    }

    /**
     * Set the keyspace for the Cassandra connection.
     *
     * @param keyspace The keyspace to use
     */
    public void setKeyspace(final String keyspace) {
        this.keyspace = keyspace;
    }

    /**
     * Get the contact point for the Cassandra connection (case insensitive, required)
     *
     * @return The contact point
     */
    @XmlElement(name = "ContactPoint", required = true)
    public String getContactPoint() {
        return contactPoint;
    }

    /**
     * Set the contact point for the Cassandra connection.
     *
     * @param contactPoint The contact point to use
     */
    public void setContactPoint(final String contactPoint) {
        this.contactPoint = contactPoint;
    }

    /**
     * Get the port for the Cassandra connection (case insensitive, required)
     *
     * @return The port
     */
    @XmlElement(name = "Port", required = true)
    public String getPort() {
        return port;
    }

    /**
     * Set the port for the Cassandra connection.
     *
     * @param port The port to use
     */
    public void setPort(final String port) {
        this.port = port;
    }

    /**
     * Get the username for the Cassandra connection (case insensitive, required)
     *
     * @return The username
     */
    @XmlElement(name = "Username", required = true)
    public String getUsername() {
        return username;
    }

    /**
     * Set the username for the Cassandra connection.
     *
     * @param username The username to use
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Get the compression for the Cassandra connection (case insensitive, required)
     *
     * @return The compression
     */
    @XmlElement(name = "Compression", required = true)
    public String getCompression() {
        return compression;
    }

    /**
     * Set the compression for the Cassandra connection.
     *
     * @param compression The compression to use
     */
    public void setCompression(final String compression) {
        this.compression = compression;
    }

    /**
     * Is SSL for the Cassandra connection enabled? (required)
     *
     * @return True if enabled.
     */
    @XmlElement(name = "Ssl", required = true)
    public boolean isSsl() {
        return ssl;
    }

    /**
     * Set the SSL state of the Cassandra connection.
     *
     * @param ssl True for enabled.
     */
    public void setSsl( final boolean ssl ) {
        this.ssl = ssl;
    }

    /**
     * Get the TLS Acceptable Cipher List, comma separated
     *
     * @return JSSE cipher strings in a list.
     */
    @XmlElement(name = "TlsCiphers")
    public String getTlsciphers() { return tlsciphers;}

    /**
     * Set TLS Acceptable Cipher List, comma separated.
     *
     * @param ciphers JSSE Cipher List (comma separated)
     */
    public void setTlsciphers(String ciphers) { this.tlsciphers = ciphers; }

    /**
     * Is the Cassandra connection enabled? (required)
     *
     * @return True if enabled.
     */
    @XmlElement(name = "Enabled", required = true)
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Set the enabled state of the Cassandra connection.
     *
     * @param enabled True for enabled.
     */
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
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

    private String name;
    private String keyspace;
    private String contactPoint;
    private String port;
    private String username;
    private String compression;
    private Boolean ssl = false;
    private String tlsciphers;
    private Boolean enabled = false;
    private Map<String, String> properties;
}
