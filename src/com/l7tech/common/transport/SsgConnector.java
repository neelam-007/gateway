package com.l7tech.common.transport;

import com.l7tech.objectmodel.imp.NamedEntityImp;

import java.util.HashSet;
import java.util.Set;

/**
 * Describes a port on which the Gateway will listen for incoming requests.
 */
public class SsgConnector extends NamedEntityImp {
    /** Indicates that a client certificate challenge will never be sent. */
    public static final int CLIENT_AUTH_NEVER = 0;

    /** Indicates that a client certificate challenge will always be sent, and the handshake will fail if the client declines it. */
    public static final int CLIENT_AUTH_ALWAYS = 1;

    /** Indicates that a client certificate challenge will always be sent, but the handshake will succeed anyway if the client declines it. */
    public static final int CLIENT_AUTH_OPTIONAL = 2;

    public static final String SCHEME_HTTP = "HTTP";
    public static final String SCHEME_HTTPS = "HTTPS";
    public static final String SCHEME_FTP = "FTP";
    public static final String SCHEME_FTPS = "FTPS";

    /** Custom cipher list.  If set, should be a comma-separated ordered list, ie "TLS_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA". */
    public static final String PROP_CIPHERLIST = "cipherList";

    /** If a port range is required, this property holds the first port in the range. */
    public static final String PROP_PORT_RANGE_START = "portRangeStart";

    /** If a port range is required, this property holds the number of ports in the range. */
    public static final String PROP_PORT_RANGE_COUNT = "portRangeCount";

    /** If specified, this is a specified interface IP address to bind the listen port.  Otherwise, it will bind INADDR_ANY. */
    public static final String PROP_BIND_ADDRESS = "bindAddress";

    private boolean enabled = true;
    private int port;
    private String scheme = SCHEME_HTTP;
    private boolean secure;
    private int clientAuth;
    private Long keystoreOid;
    private String keyAlias;
    private Set<SsgConnectorProperty> properties = new HashSet<SsgConnectorProperty>();

    public SsgConnector() {
    }

    public SsgConnector(long oid, String name, int port, String scheme, boolean secure, int clientAuth, Long keystoreOid, String keyAlias) {
        super();
        setOid(oid);
        setName(name);
        this.port = port;
        this.scheme = scheme;
        this.secure = secure;
        this.clientAuth = clientAuth;
        this.keystoreOid = keystoreOid;
        this.keyAlias = keyAlias;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the TCP port on which the connector shall listen.
     *
     * @return a TCP port from 1-65535.
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the TCP port on which the connector shall listen.
     *
     * @param port a TCP port from 1-65535.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get the scheme that this connector should use.
     *
     * @return a scheme; current one of "http" or "https".
     */
    public String getScheme() {
        return scheme;
    }

    /**
     * Set the scheme that this connector should use.
     *
     * @param scheme a scheme.  currently must be one of "http" or "https".
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

    /**
     * Get the secure flag for this connector.
     * Requests that arrive on a connector flagged as secure can return true from
     * HttpServletRequest.isSecure().
     *
     * @return the secure flag.
     */
    public boolean isSecure() {
        return secure;
    }

    /**
     * Set the secure flag for this connector.
     * Requests that arrive on a connector flagged as secure can return true from
     * HttpServletRequest.isSecure().
     *
     * @param secure the secure flag.
     */
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    /**
     * Check the client auth state for this connector.
     *
     * @return the current client auth setting, typically one of {@link #CLIENT_AUTH_NEVER},
               {@link #CLIENT_AUTH_ALWAYS}, or {@link #CLIENT_AUTH_OPTIONAL}.
     */
    public int getClientAuth() {
        return clientAuth;
    }

    /**
     * Set the client auth state for this connector.
     *
     * @param clientAuth one of {@link #CLIENT_AUTH_NEVER}, {@link #CLIENT_AUTH_ALWAYS}, or
     *         {@link #CLIENT_AUTH_OPTIONAL}.
     */
    public void setClientAuth(int clientAuth) {
        this.clientAuth = clientAuth;
    }

    /**
     * Get the OID of the KeystoreFile instance in which can be found the server certificate and private key,
     * if this connector will be using SSL.
     * <p/>
     * <b>Note:</b> caller should be prepared for the corresponding keystore to be unavailable on the current system.
     * This can occur (for example) if a configuration is moved from a Gateway with an HSM to a Gateway without one.
     * If this happens, the system should honor the "keyStoreSearchForAlias" ServerConfig setting (possibly searching
     * other keystores for a matching alias).
     *
     * @return the OID of the KeystoreFile instance that provides this connector's SSL server cert and private key,
     *         or null if one isn't set.
     */
    public Long getKeystoreOid() {
        return keystoreOid;
    }

    /**
     * Set the OID of the KeystoreFile instance in which can be found the server certificate and private key,
     * if this connector will be using SSL.
     * <p/>
     * See {@link #getKeystoreOid()} for more information.
     *
     * @param keystoreOid the OID of the KeystoreFile instance in which to find the private key with alias
     *        {@link #keyAlias}, or null if one is not configured.
     */
    public void setKeystoreOid(Long keystoreOid) {
        this.keystoreOid = keystoreOid;
    }

    /**
     * Get the alias of the private key to use for the SSL server socket, if this connector will be using SSL.
     *
     * @return the private key alias, or null if one is not configured.
     */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Set the alias of the priate key to use for the SSL server socket, if this connector will be using SSL.
     *
     * @param keyAlias the private key alias, or null if one is not to be used.
     */
    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    /**
     * Get an arbitrary connector property.
     *
     * @param key  the name of the property to get
     * @return the requested property, or null if it is not set
     */
    public String getProperty(String key) {
        for (SsgConnectorProperty property : properties) {
            if (key.equals(property.getName()))
                return property.getValue();
        }
        return null;
    }

    /**
     * Set an arbitrary connector property.  Set a property to null to remove it.
     *
     * @param key  the name of the property to set
     * @param value the value to set it to, or null to remove the property
     */
    public void putProperty(String key, String value) {
        SsgConnectorProperty found = null;
        for (SsgConnectorProperty property : properties) {
            if (key.equals(property.getName())) {
                found = property;
                break;
            }
        }

        if (value == null) {
            // Remove it
            if (found != null)
                properties.remove(found);
            return;
        }

        // Add or update it
        if (found == null)
            properties.add(new SsgConnectorProperty(this, key, value));
        else
            found.setValue(value);
    }

    /**
     * Get the extra properties of this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @return a Set containing the extra connector properties.  May be empty but never null.
     */
    protected Set<SsgConnectorProperty> getProperties() {
        return properties;
    }

    /**
     * Set the extra properties for this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @param properties the properties set to use
     */
    protected void setProperties(Set<SsgConnectorProperty> properties) {
        this.properties = properties;
    }

    /** @noinspection RedundantIfStatement*/
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SsgConnector that = (SsgConnector)o;

        if (clientAuth != that.clientAuth) return false;
        if (enabled != that.enabled) return false;
        if (port != that.port) return false;
        if (secure != that.secure) return false;
        if (keyAlias != null ? !keyAlias.equals(that.keyAlias) : that.keyAlias != null) return false;
        if (keystoreOid != null ? !keystoreOid.equals(that.keystoreOid) : that.keystoreOid != null) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + port;
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        result = 31 * result + (secure ? 1 : 0);
        result = 31 * result + clientAuth;
        result = 31 * result + (keystoreOid != null ? keystoreOid.hashCode() : 0);
        result = 31 * result + (keyAlias != null ? keyAlias.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
