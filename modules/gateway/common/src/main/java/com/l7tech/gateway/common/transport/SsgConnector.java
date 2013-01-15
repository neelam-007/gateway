package com.l7tech.gateway.common.transport;

import com.l7tech.common.io.PortOwner;
import com.l7tech.common.io.PortRange;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlEnumValue;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Describes a port on which the Gateway will listen for incoming requests.
 */
@Entity
@Proxy(lazy=false)
@Table(name="connector")
public class SsgConnector extends NamedEntityImp implements PortOwner {
    protected static final Logger logger = Logger.getLogger(SsgConnector.class.getName());

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
    public static final String SCHEME_NA = "N/A";
    /** Custom cipher list.  If set, should be a comma-separated ordered list, ie "TLS_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA". */
    public static final String PROP_TLS_CIPHERLIST = "cipherList";

    /** GUI-managed TLS versions list.  If set, should be a comma-separated list of JSSE protocol names to enable, ie "TLSv1,TLSv1.1,TLSv1.2".  Default is to enable only "TLSv1". */
    public static final String PROP_TLS_PROTOCOLS = "protocols";

    /** Custom TLS versions list, overridding those from the GUI.  If set, should be a comma-separated list of JSSE protocol names. */
    public static final String PROP_TLS_OVERRIDE_PROTOCOLS = "overrideProtocols";

    /** Custom SSL/TLS protocol name.  If set, should be the first argument to pass to {@link javax.net.ssl.SSLContext#getInstance(String, String)}, ie "TLS".  Defaults to "TLS". */
    public static final String PROP_TLS_PROTOCOL = "protocol";

    /**
     * Name of specific SSLContext provider to use.  Can be used to force the Gateway to use a specific registered JSSE provider instead of selecting one automatically.
     * If set, this should be the second argument to pass to {@link javax.net.ssl.SSLContext#getInstance(String, String)}, ie "SunJSSE" or "RsaJsse".
     */
    public static final String PROP_TLS_PROTOCOL_PROVIDER = "protocolProvider";

    /** If set to true, we will NOT automatically prepend "SSLv2Hello" to the list of enabled protocols whenever SSL 3.0 or TLS 1.0 are enabled. */
    public static final String PROP_TLS_NO_SSLV2_HELLO = "noSSLv2Hello";

    /** Can be used to override the session cache size.  If not set, defaults to 0, which means no limit. */
    public static final String PROP_TLS_SESSION_CACHE_SIZE = "sessionCacheSize";

    /** Can be used to override the session cache timeout.  Note that this is in seconds, not milliseconds.  If not set, defaults to 86400 seconds. */
    public static final String PROP_TLS_SESSION_CACHE_TIMEOUT = "sessionCacheTimeout";

    /**
     * Can be set to "true" to disable the crude work around for CVE-2009-3555 (removing all cipher suites after handshake to prevent re-handshakes).
     * Currently only has an effect for HTTPS listeners.  Not necessary when using RSA SSL-J 5.1.1 or later as the TLS provider. 
     */
    public static final String PROP_TLS_ALLOW_UNSAFE_LEGACY_RENEGOTIATION = "allowUnsafeLegacyRenegotiation";

    /** If a port range is required, this property holds the first port in the range. */
    public static final String PROP_PORT_RANGE_START = "portRangeStart";

    /** If a port range is required, this property holds the number of ports in the range. */
    public static final String PROP_PORT_RANGE_COUNT = "portRangeCount";

    /** If specified, this is a specified interface IP address to bind the listen port.  Otherwise, it will bind INADDR_ANY. */
    public static final String PROP_BIND_ADDRESS = "bindAddress";

    /** If specified, this is the size of the thread pool for the connector (Currently HTTP(S) only). */
    public static final String PROP_THREAD_POOL_SIZE = "threadPoolSize";

    /** If specified, service resolution should be bypassed for incoming requests, and they should be immediately routed to the specified service OID. */
    public static final String PROP_HARDWIRED_SERVICE_ID = "hardwiredServiceId";

    /** If specified, incoming messages should be assumed to use the specified content type. */
    public static final String PROP_OVERRIDE_CONTENT_TYPE = "overrideContentType";

    /**If specified, the request xml size limit is overridden */
    public static final String PROP_REQUEST_SIZE_LIMIT = "requestSizeLimit";

    /** Recognized endpoint names. */
    public static enum Endpoint {
        /** Message processor. */
        @XmlEnumValue( "Published service message input" )
        MESSAGE_INPUT,

        /** Connections from standalone SSM. */
        @XmlEnumValue( "Policy Manager access" )
        ADMIN_REMOTE_SSM,

        /** Connections from Enterprise Service Manager or clients of ESM specific services. */
        @XmlEnumValue( "Enterprise Manager access" )
        ADMIN_REMOTE_ESM,

        /** Connections from standalone SSM or the ESM. */
        @XmlEnumValue( "Administrative access" )
        ADMIN_REMOTE(ADMIN_REMOTE_SSM, ADMIN_REMOTE_ESM),

        /** Administration services offered over HTTP(S), i.e. the admin applet and backup services. */
        @XmlEnumValue( "Browser-based administration" )
        ADMIN_APPLET,

        /** Certificate and policy discovery. */
        @XmlEnumValue( "Policy download service" )
        POLICYDISCO,

        @XmlEnumValue( "Ping service" )
        PING,

        /** The WS-Trust security token service. */
        @XmlEnumValue( "WS-Trust security token service" )
        STS,

        /** The built-in CA service. */
        @XmlEnumValue( "Certificate signing service" )
        CSRHANDLER,

        /** The Bridge password change service. */
        @XmlEnumValue( "Password changing service" )
        PASSWD,

        /** The WSDL proxy service. */
        @XmlEnumValue( "WSDL download service" )
        WSDLPROXY,

        /** The HTTP-based SNMP query service. */
        @XmlEnumValue( "SNMP Query service" )
        SNMPQUERY,

        /** Agent web service for HP SOA Manager. */
        @XmlEnumValue( "HP SOA Manager agent service" )
        HPSOAM,

        /**
         * All built-in servlets other than the first three.  This includes POLICYDISCO, PING, STS, PASSWD etc.
         */
        @XmlEnumValue( "Built-in services" )
        OTHER_SERVLETS(POLICYDISCO, PING, STS, CSRHANDLER, PASSWD, WSDLPROXY, SNMPQUERY, HPSOAM),

        /** Process Controller Service Node API*/
        @XmlEnumValue( "Node Control" )
        PC_NODE_API,

        /** Node to Node Remoting  */
        @XmlEnumValue( "Inter-Node Communication" )
        NODE_COMMUNICATION;

        private Endpoint[] enabledKids;
        private Set<Endpoint> enabledSet;
        private Endpoint(Endpoint... enabled) { this.enabledKids = enabled; }

        /**
         * Get the set of Endpoints enabled by enabling this Endpoint.
         *
         * @return the set of Endpoints that should be enabled if this endpoint is found to be enabled.
         *         This set will always include at least this endpoint itself.
         */
        public Set<Endpoint> enabledSet() {
            if (enabledSet == null)
                enabledSet = Collections.unmodifiableSet(EnumSet.of(this, enabledKids));
            //noinspection ReturnOfCollectionOrArrayField
            return enabledSet;
        }

        private static final Pattern PATTERN_WS_COMMA_WS = Pattern.compile("\\s*,\\s*");

        /**
         * Parse a comma-delimited list of endpoint names into a set of Endpoint instances.
         *
         * @param commaDelimitedList a comma-delimited list of zero or more endpoint names.  May be empty but mustn't be null.
         * @return a new EnumSet of Endpoint instances.  May be empty but never null.
         */
        public static Set<Endpoint> parseCommaList(String commaDelimitedList) {
            String[] names = PATTERN_WS_COMMA_WS.split(commaDelimitedList);
            Set<Endpoint> ret = EnumSet.noneOf(Endpoint.class);
            for (String name : names) {
                if(name != null || !name.isEmpty()){
                    try {
                        ret.add(Endpoint.valueOf(name));
                    } catch (IllegalArgumentException iae) {
                        logger.log(Level.WARNING, "Ignoring unrecognized endpoint name: " + name);
                    }
                }
            }
            return ret;
        }

        /**
         * Convert the specified set of endpoints into a comma-delimited list of their names.
         *
         * @param endpoints a set of endpoints. Required.
         * @return a comma-delimited list, ie "WSDLPROXY,STS".  Never null.
         */
        public static String asCommaList(Set<Endpoint> endpoints) {
            StringBuilder sb = new StringBuilder(128);
            Set<Endpoint> canon = EnumSet.copyOf(endpoints);
            boolean first = true;
            for (Endpoint endpoint : canon) {
                if (!first) sb.append(',');
                sb.append(endpoint.toString());
                first = false;
            }
            return sb.toString();
        }
    }

    private boolean enabled = true;
    private int port = -1;
    private String scheme = SCHEME_HTTP;
    private boolean secure;
    private String endpoints = "";
    private int clientAuth;
    private Long keystoreOid;
    private String keyAlias;

    private Map<String,String> properties = new HashMap<String,String>();

    // Fields not saved by hibernate
    private Set<Endpoint> endpointSet;

    public SsgConnector() {
    }

    public SsgConnector(long oid, String name, int port, String scheme, boolean secure, String endpoints, int clientAuth, Long keystoreOid, String keyAlias) {
        setOid(oid);
        setName(name);
        this.port = port;
        this.scheme = scheme;
        this.secure = secure;
        this.endpoints = endpoints;
        this.clientAuth = clientAuth;
        this.keystoreOid = keystoreOid;
        this.keyAlias = keyAlias;
    }

    @Size(min=1, max=128)
    @Transient
    @Override
    public String getName() {
        return super.getName();
    }

    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        checkLocked();
        this.enabled = enabled;
    }

    /**
     * Get the TCP port on which the connector shall listen.
     *
     * @return a TCP port from 1-65535.
     */
    @Min(1025) //This is the minimum permitted in the UI
    @Max(65535)
    @Column(name="port")
    public int getPort() {
        return port;
    }

    /**
     * Set the TCP port on which the connector shall listen.
     *
     * @param port a TCP port from 1-65535.
     */
    public void setPort(int port) {
        checkLocked();
        this.port = port;
    }

    /**
     * Get the scheme that this connector should use.
     *
     * @return a scheme; current one of "http" or "https".
     */
    @NotNull
    @Size(min=1, max=128)
    @Column(name="scheme", length=128, nullable=false)
    public String getScheme() {
        return scheme;
    }

    /**
     * Set the scheme that this connector should use.
     *
     * @param scheme a scheme.  currently must be one of "http" or "https".
     */
    public void setScheme(String scheme) {
        checkLocked();
        this.scheme = scheme;
    }

    /**
     * Get the secure flag for this connector.
     * Requests that arrive on a connector flagged as secure can return true from
     * HttpServletRequest.isSecure().
     *
     * @return the secure flag.
     */
    @Column(name="secure")
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
        checkLocked();
        this.secure = secure;
    }

    /**
     * Check the client auth state for this connector.
     *
     * @return the current client auth setting, typically one of {@link #CLIENT_AUTH_NEVER},
               {@link #CLIENT_AUTH_ALWAYS}, or {@link #CLIENT_AUTH_OPTIONAL}.
     */
    @Column(name="client_auth")
    @Min(0)
    @Max(2)
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
        checkLocked();
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
     * <p/>
     * <b>Note:</b> A connector with a null key alias or keystore OID can still be configured as an HTTPS
     * listener -- such listeners will just use the current default SSL key as their server cert.
     *
     * @return the OID of the KeystoreFile instance that provides this connector's SSL server cert and private key,
     *         or null if one isn't set.
     */
    @Column(name="keystore_oid")
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
        checkLocked();
        this.keystoreOid = keystoreOid;
    }

    /**
     * Get the alias of the private key to use for the SSL server socket, if this connector will be using SSL.
     * <p/>
     * <b>Note:</b> A connector with a null key alias or keystore OID can still be configured as an HTTPS
     * listener -- such listeners will just use the current default SSL key as their server cert.
     *
     * @return the private key alias, or null if one is not configured.
     */
    @Column(name="key_alias", length=255)
    @Size(min = 1, max=255)
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Set the alias of the priate key to use for the SSL server socket, if this connector will be using SSL.
     * <p/>
     * See {@link #getKeyAlias()} for more information.
     *
     * @param keyAlias the private key alias, or null if one is not configured.
     */
    public void setKeyAlias(String keyAlias) {
        checkLocked();
        this.keyAlias = keyAlias;
    }

    /**
     * Get an arbitrary connector property.
     *
     * @param key  the name of the property to get
     * @return the requested property, or null if it is not set
     */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Convenience method to get a property as a boolean.
     *
     * @param key the name of the property to get
     * @return boolean represented by the requested property value
     */
    public boolean getBooleanProperty(String key) {
        return Boolean.parseBoolean(getProperty(key));
    }

    /**
     * Convenience method to get a property as an int.
     *
     * @param key  the name of the property to get
     * @param dflt the default value to return if the property is not set or if it is not a valid integer
     * @return the requested property value, or null if it is not set
     */
    public int getIntProperty(String key, int dflt) {
        String val = getProperty(key);
        if (val == null)
            return dflt;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException nfe) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Invalid integer property value for listen port " + getPort() + " property " + key + ": " + val);
            return dflt;
        }
    }

    /**
     * Convenience method to get a property as a long.
     *
     * @param key  the name of the property to get
     * @param dflt the default value to return if the property is not set or if it is not a valid long
     * @return the requested property value, or null if it is not set
     */
    public long getLongProperty(String key, long dflt) {
        String val = getProperty(key);
        if (val == null)
            return dflt;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException nfe) {
            if (logger.isLoggable(Level.WARNING))
                logger.log(Level.WARNING, "Invalid long integer property value for listen port " + getPort() + " property " + key + ": " + val);
            return dflt;
        }
    }

    /**
     * Get a list of all extra properties set on this connector.
     *
     * @return a List of Strings.  May be empty, but never null.
     */
    @Transient
    public List<String> getPropertyNames() {
        return new ArrayList<String>(properties.keySet());
    }

    /**
     * Set an arbitrary connector property.  Set a property to null to remove it.
     *
     * @param key  the name of the property to set
     * @param value the value to set it to, or null to remove the property
     */
    public void putProperty(String key, String value) {
        checkLocked();

        properties.put(key, value);
    }

    /**
     * Remove some property associated with the name,  ${propertyName} from the property list.
     * @param propertyName the property name whose property will be removed from the list.
     */
    public void removeProperty(String propertyName) {
        checkLocked();

        properties.remove(propertyName);
    }

    /**
     * Get the endpoints to enable on this connector.
     * The format is a comma-delimited string of endpoint names.
     *
     * @return endpoint names, ie "MESSAGE_INPUT,ADMIN_APPLET,STS".  If null, the connector should be treated as disabled.
     */
    @Column(name="endpoints", length=255, nullable=false)
    public String getEndpoints() {
        return endpoints;
    }

    /**
     * Set the endpoints to enable on this connector.
     * The format is a comma-delimited string of endpoint names.
     *
     * @param endpoints endpoint names to enable, ie "MESSAGE_INPUT,ADMIN_REMOTE,CSRHANDLER".
     */
    public void setEndpoints(String endpoints) {
        checkLocked();

        this.endpoints = endpoints;
        endpointSet = null;
    }

    /**
     * Check if this connector offers access to the specified endpoint.
     *
     * @param endpoint the endpoint to check.  Required.
     * @return  true if this connector grants access to the specified endpoint.
     */
    public boolean offersEndpoint(Endpoint endpoint) {
        return endpointSet().contains(endpoint);
    }

    /**
     * Get the endpoints to enable on this connector as a set.
     * The returned set will be expanded to include all endpoints implied by the endpoints
     * that are enabled explicitly; for example, if OTHER_SERVLETS is enabled, the returned set
     * will include STS.
     * <p/>
     * The returned set is read-only.
     *
     * @return a read-only expanded Set of enabled endpoints.  Never null.
     */
    public Set<Endpoint> endpointSet() {
        if (endpointSet == null) {
            String endpointList = getEndpoints();
            Set<Endpoint> es = endpointList == null ? EnumSet.noneOf(Endpoint.class) : Endpoint.parseCommaList(endpointList);
            Set<Endpoint> ret = EnumSet.copyOf(es);
            for (Endpoint e : es)
                ret.addAll(e.enabledSet());
            endpointSet = Collections.unmodifiableSet(ret);
        }
        return endpointSet;
    }

    /**
     * Get the extra properties of this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @return a Set containing the extra connector properties.  May be empty but never null.
     */
    @Fetch(FetchMode.SUBSELECT)
    @ElementCollection(fetch=FetchType.EAGER)
    @JoinTable(name="connector_property",
               joinColumns=@JoinColumn(name="connector_oid", referencedColumnName="objectid"))
    @MapKeyColumn(name="name",length=128)
    @Column(name="value", nullable=false, length=32672)
    protected Map<String,String> getProperties() {
        //noinspection ReturnOfCollectionOrArrayField
        return properties;
    }

    /**
     * Set the extra properties for this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @param properties the properties set to use
     */
    protected void setProperties(Map<String,String> properties) {
        checkLocked();

        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.properties = properties;
    }

    /**
     * @return a list of all TCP port ranges claimed by this connector.
     */
    @Transient
    public List<PortRange> getTcpPortsUsed() {
        List<PortRange> ret = new ArrayList<PortRange>();
        ret.add(new PortRange(port, port, false, getProperty(PROP_BIND_ADDRESS)));
        PortRange range = getPortRange();
        if (range != null)
            ret.add(range);
        return ret;
    }

    /** @return the configured TCP port range, or null if it is not configured. */
    @Transient
    private PortRange getPortRange() {
        try {
            String startstr = getProperty(PROP_PORT_RANGE_START);
            if (startstr == null)
                return null;
            int start = Integer.parseInt(startstr);
            String countstr = getProperty(PROP_PORT_RANGE_COUNT);
            if (countstr == null)
                return null;
            int count = Integer.parseInt(countstr);
            if (count < 1)
                throw new IllegalArgumentException("Invalid port count: " + count);
            return new PortRange(start, start + count -1, false, getProperty(PROP_BIND_ADDRESS));
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Ignoring invalid port range settings for connector oid #" + getOid() + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    @Override
    public boolean isPortUsed(int port, boolean udp, String device) {
        if (udp)
            return false;

        if (device != null) {
            String bindAddress = getProperty(PROP_BIND_ADDRESS);
            if (bindAddress != null && !device.equals(bindAddress))
                return false;
        }

        if (getPort() == port)
            return true;

        PortRange range = getPortRange();
        return range != null && range.isPortUsed(port, udp, device);
    }

    @Override
    public boolean isOverlapping(PortRange range) {
        if (range.isPortUsed(getPort(), false, getProperty(PROP_BIND_ADDRESS)))
            return true;
        final PortRange ourRange = getPortRange();
        return ourRange != null && range.isOverlapping(ourRange);
    }

    @Override
    public Pair<PortRange, PortRange> getFirstOverlappingPortRange(PortOwner owner) {
        return PortRange.getFirstOverlappingPortRange(this, owner);
    }

    @Override
    @Transient
    public List<PortRange> getUsedPorts() {
        // Currently an SsgConnector can only use TCP ports
        return getTcpPortsUsed();
    }

    /**
     * Initialize any lazily-computed fields and mark this instance as read-only.
     */
    private void setReadOnly() {
        this.endpointSet();
        this.getPortRange();
        this.getClientAuth();
        this.getKeyAlias();
        this.getKeystoreOid();
        this.getPort();
        this.getPropertyNames();
        this.getScheme();
        this.lock();
    }

    @Transient
    public SsgConnector getCopy() {
        try {
            SsgConnector copy = new SsgConnector();
            BeanUtils.copyProperties(this, copy,
                                     BeanUtils.omitProperties(BeanUtils.getProperties(getClass()), "properties"));
            copy.setProperties(new HashMap<String, String>(getProperties()));
            return copy;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Transient
    public SsgConnector getReadOnlyCopy() {
        SsgConnector copy = getCopy();
        copy.setReadOnly();
        return copy;
    }

    /** @noinspection RedundantIfStatement*/
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        SsgConnector that = (SsgConnector)o;

        if (clientAuth != that.clientAuth) return false;
        if (enabled != that.enabled) return false;
        if (port != that.port) return false;
        if (secure != that.secure) return false;
        if (endpoints != null ? !endpoints.equals(that.endpoints) : that.endpoints != null) return false;
        if (keyAlias != null ? !keyAlias.equals(that.keyAlias) : that.keyAlias != null) return false;
        if (keystoreOid != null ? !keystoreOid.equals(that.keystoreOid) : that.keystoreOid != null) return false;
        if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;
        if (scheme != null ? !scheme.equals(that.scheme) : that.scheme != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (enabled ? 1 : 0);
        result = 31 * result + port;
        result = 31 * result + (scheme != null ? scheme.hashCode() : 0);
        result = 31 * result + (secure ? 1 : 0);
        result = 31 * result + (endpoints != null ? endpoints.hashCode() : 0);
        result = 31 * result + clientAuth;
        result = 31 * result + (keystoreOid != null ? keystoreOid.hashCode() : 0);
        result = 31 * result + (keyAlias != null ? keyAlias.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[SsgConnector ");
        sb.append(port);
        sb.append(' ').append(scheme).append(secure ? " secure" : " noSecure");
        sb.append(' ').append(endpoints).append(" clientAuth=").append(clientAuth);
        sb.append(" keystoreOid=").append(keystoreOid).append(" keyAlias=").append(keyAlias);
        List<String> props = getPropertyNames();
        for (String prop : props)
            sb.append(" P:").append(prop).append('=').append(getProperty(prop));
        sb.append(']');
        return sb.toString();
    }
}
