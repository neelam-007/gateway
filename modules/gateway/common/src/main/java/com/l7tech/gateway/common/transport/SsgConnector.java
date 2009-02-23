package com.l7tech.gateway.common.transport;

import com.l7tech.common.io.PortOwner;
import com.l7tech.common.io.PortRange;
import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Proxy;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
@XmlRootElement
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

    /** Custom cipher list.  If set, should be a comma-separated ordered list, ie "TLS_RSA_WITH_AES_128_CBC_SHA,SSL_RSA_WITH_3DES_EDE_CBC_SHA". */
    public static final String PROP_CIPHERLIST = "cipherList";

    /** If a port range is required, this property holds the first port in the range. */
    public static final String PROP_PORT_RANGE_START = "portRangeStart";

    /** If a port range is required, this property holds the number of ports in the range. */
    public static final String PROP_PORT_RANGE_COUNT = "portRangeCount";

    /** If specified, this is a specified interface IP address to bind the listen port.  Otherwise, it will bind INADDR_ANY. */
    public static final String PROP_BIND_ADDRESS = "bindAddress";

    /** Recognized endpoint names. */
    public static enum Endpoint {
        /** Message processor. */
        MESSAGE_INPUT,

        /** Connections from standalone SSM. */
        ADMIN_REMOTE,

        /** Administration services offered over HTTP(S), i.e. the admin applet, ping and backup services. */
        ADMIN_APPLET,

        /** Certificate and policy discovery. */
        POLICYDISCO,

        /** The WS-Trust security token service. */
        STS,

        /** The built-in CA service. */
        CSRHANDLER,

        /** The Bridge password change service. */
        PASSWD,

        /** The WSDL proxy service. */
        WSDLPROXY,

        /** The HTTP-based SNMP query service. */
        SNMPQUERY,

        /** Agent web service for HP SOA Manager. */
        HPSOAM,

        /**
         * All built-in servlets other than the first three.  This includes POLICYDISCO, STS, PASSWD etc.
         * This does NOT include the PingServlet since the PingServlet has its own access rules.
         */
        OTHER_SERVLETS(POLICYDISCO, STS, CSRHANDLER, PASSWD, WSDLPROXY, SNMPQUERY, HPSOAM),

        /** Process Controller Service Node API*/
        PC_NODE_API,

        /** Node to Node Remoting  */
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
                try {
                    ret.add(Endpoint.valueOf(name));
                } catch (IllegalArgumentException iae) {
                    logger.log(Level.WARNING, "Ignoring unrecognized endpoint name: " + name);
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
    
    @XmlTransient
    private Map<String,String> properties = new HashMap<String,String>();

    // Fields not saved by hibernate
    private Set<Endpoint> endpointSet;
    private boolean inetAddressSet = false;
    private InetAddress inetAddress;

    public SsgConnector() {
    }

    public SsgConnector(long oid, String name, int port, String scheme, boolean secure, String endpoints, int clientAuth, Long keystoreOid, String keyAlias) {
        super();
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

    @Column(name="enabled")
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if ( isLocked() ) throw new IllegalStateException("readonly");
        this.enabled = enabled;
    }

    /**
     * Get the TCP port on which the connector shall listen.
     *
     * @return a TCP port from 1-65535.
     */
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
        if ( isLocked() ) throw new IllegalStateException("readonly");
        this.port = port;
    }

    /**
     * Get the scheme that this connector should use.
     *
     * @return a scheme; current one of "http" or "https".
     */
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
        if ( isLocked() ) throw new IllegalStateException("readonly");
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
        if ( isLocked() ) throw new IllegalStateException("readonly");
        this.secure = secure;
    }

    /**
     * Check the client auth state for this connector.
     *
     * @return the current client auth setting, typically one of {@link #CLIENT_AUTH_NEVER},
               {@link #CLIENT_AUTH_ALWAYS}, or {@link #CLIENT_AUTH_OPTIONAL}.
     */
    @Column(name="client_auth")
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
        if ( isLocked() ) throw new IllegalStateException("readonly");
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
        if ( isLocked() ) throw new IllegalStateException("readonly");
        this.keystoreOid = keystoreOid;
    }

    /**
     * Get the alias of the private key to use for the SSL server socket, if this connector will be using SSL.
     *
     * @return the private key alias, or null if one is not configured.
     */
    @Column(name="key_alias", length=255)
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
     * Set the alias of the priate key to use for the SSL server socket, if this connector will be using SSL.
     *
     * @param keyAlias the private key alias, or null if one is not to be used.
     */
    public void setKeyAlias(String keyAlias) {
        if ( isLocked() ) throw new IllegalStateException("readonly");
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
        if ( isLocked() ) throw new IllegalStateException("readonly");

        if (PROP_BIND_ADDRESS.equals(key)) {
            inetAddressSet = false;
            inetAddress = null;
        }

        properties.put(key, value);
    }

    /**
     * Remove some property associated with the name,  ${propertyName} from the property list.
     * @param propertyName the property name whose property will be removed from the list.
     */
    public void removeProperty(String propertyName) {
        if ( isLocked() ) throw new IllegalStateException("readonly");

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
        if ( isLocked() ) throw new IllegalStateException("readonly");

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
    private Set<Endpoint> endpointSet() {
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
     * Convenience accessor for the property {@link #PROP_BIND_ADDRESS} that converts to InetAddress,
     * with caching.
     *
     * @return an InetAddress instance, or null if this connector should bind to all addresses.
     */
    @Transient
    public InetAddress getBindAddress() {
        if (inetAddressSet)
            return inetAddress;

        String bindAddr = getProperty(PROP_BIND_ADDRESS);
        final InetAddress result;
        if (bindAddr == null) {
            result = null;
        } else {
            try {
                result = InetAddress.getByName(bindAddr);
            } catch (UnknownHostException e) {
                logger.log(Level.WARNING, "Bad bindAddr hostname in connector oid " + getOid() + ": " + ExceptionUtils.getMessage(e));
                try {
                    return InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
                } catch (UnknownHostException e1) {
                    throw new RuntimeException(e1); // can't happen
                }
            }
        }

        inetAddressSet = true;
        inetAddress = result;
        return inetAddress;
    }

    /**
     * Get the extra properties of this connector.
     * <p/>
     * Should only be used by Hibernate, for serialization.
     *
     * @return a Set containing the extra connector properties.  May be empty but never null.
     */
    @CollectionOfElements(fetch=FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @JoinTable(name="connector_property",
               joinColumns=@JoinColumn(name="connector_oid", referencedColumnName="objectid"))
    @org.hibernate.annotations.MapKey(columns={@Column(name="name",length=128)})
    @Column(name="value", nullable=false, length=Integer.MAX_VALUE)
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
        if ( isLocked() ) throw new IllegalStateException("readonly");

        inetAddressSet = false;
        inetAddress = null;
        //noinspection AssignmentToCollectionOrArrayFieldFromParameter
        this.properties = properties;
    }

    /**
     * @return a list of all TCP port ranges claimed by this connector.
     */
    @Transient
    public List<PortRange> getTcpPortsUsed() {
        List<PortRange> ret = new ArrayList<PortRange>();
        ret.add(new PortRange(port, port, false, getBindAddress()));
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
            return new PortRange(start, start + count, false, getBindAddress());
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Ignoring invalid port range settings for connector oid #" + getOid() + ": " + ExceptionUtils.getMessage(e), e);
            return null;
        }
    }

    public boolean isPortUsed(int port, boolean udp, InetAddress device) {
        if (udp)
            return false;

        if (device != null) {
            InetAddress bindAddress = getBindAddress();
            if (bindAddress != null && !device.equals(bindAddress))
                return false;
        }

        if (getPort() == port)
            return true;

        PortRange range = getPortRange();
        return range != null && range.isPortUsed(port, udp, device);
    }

    public boolean isOverlapping(PortRange range) {
        if (range.isPortUsed(getPort(), false, getBindAddress()))
            return true;
        final PortRange ourRange = getPortRange();
        return ourRange != null && range.isOverlapping(ourRange);
    }

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
        this.getBindAddress();
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
    public SsgConnector getReadOnlyCopy() {
        try {
            SsgConnector copy = new SsgConnector();
            BeanUtils.copyProperties(this, copy,
                                     BeanUtils.omitProperties(BeanUtils.getProperties(getClass()), "properties"));
            copy.setProperties(new HashMap<String, String>(copy.getProperties()));
            copy.setReadOnly();
            return copy;
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
        if (endpoints != null ? !endpoints.equals(that.endpoints) : that.endpoints != null) return false;
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
        result = 31 * result + (endpoints != null ? endpoints.hashCode() : 0);
        result = 31 * result + clientAuth;
        result = 31 * result + (keystoreOid != null ? keystoreOid.hashCode() : 0);
        result = 31 * result + (keyAlias != null ? keyAlias.hashCode() : 0);
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }

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
