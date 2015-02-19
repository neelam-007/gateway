package com.l7tech.server.transport.http;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.util.TextUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class encapsulates the code for initializing the connectors table from the contents of server.xml.
 */
public class DefaultHttpConnectors {
    protected static final Logger logger = Logger.getLogger(DefaultHttpConnectors.class.getName());

    private static final String PROP_INIT_ADDR = "com.l7tech.server.listener.initaddr";
    private static final String PROP_INIT_PORT = "com.l7tech.server.listener.initport";
    private static final String PROP_INIT_LISTENER_HTTP_ENDPOINTS = "com.l7tech.server.listener.initendpoints.http";
    private static final String PROP_INIT_LISTENER_HTTPS_ENDPOINTS = "com.l7tech.server.listener.initendpoints";
    private static final String PROP_INIT_LISTENER_CIPHERS = "com.l7tech.server.listener.initciphers";
    private static final String PROP_INIT_INTERNODE_CIPHERS = "com.l7tech.server.listener.initinternodeciphers";
    private static final String PROP_INIT_INTERNODE_POOLSIZE = "com.l7tech.server.listener.initinternodepoolsize";

    // Strong cipher suites for RSA server certs
    // We omit _ECDHE_RSA_ for now since it has problems in SunJSSE with our EC providers (Certicom, RSA, and Luna)
    private static final String RSA_256 = "TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_RSA_WITH_AES_256_CBC_SHA";
    private static final String RSA_128 = "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA";

    // Weaker/older cipher suites for RSA server certs.  Needed in order to support older/non-Mozilla web browsers.
    // We have to draw a line somewhere: we won't (by default) enable any suites that use DES or MD5 or RC4
    private static final String RSA_3DES = "SSL_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA";

    // Strong cipher suites for ECC server certs.  We omit the _ECDH_ECDSA_ suites for now for two reasons:
    //   1) They can only do client auth with client cert keys on exactly the same EC curve as the server cert key
    //   2) These suites trigger fatal bugs with SunJSSE using our EC providers (Certicom, RSA, and Luna)
    private static final String ECC_256 = "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA";
    private static final String ECC_128 = "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA";

    private static final String RSA_ECC = TextUtils.join(",", RSA_256, ECC_256, RSA_128, ECC_128).toString();
    private static final String RSA_ECC_3DES = TextUtils.join(",", RSA_ECC, RSA_3DES ).toString();

    static final String defaultHttpEndpoints = ConfigFactory.getProperty( PROP_INIT_LISTENER_HTTP_ENDPOINTS, "MESSAGE_INPUT, POLICYDISCO, PING, STS, WSDLPROXY, SNMPQUERY" ); // Other two built-in endpoints (CSRHANDLER and PASSWD) are not available for HTTP protocol.
    static final String defaultHttpsEndpoints = ConfigFactory.getProperty( PROP_INIT_LISTENER_HTTPS_ENDPOINTS, "MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS" );
    static final String defaultListenerStrongCiphers = ConfigFactory.getProperty( PROP_INIT_LISTENER_CIPHERS, RSA_ECC_3DES );
    static final String defaultInternodeStrongCiphers = ConfigFactory.getProperty( PROP_INIT_INTERNODE_CIPHERS, RSA_ECC );
    static final String defaultInternodePoolSize = ConfigFactory.getProperty( PROP_INIT_INTERNODE_POOLSIZE, "10" );

    /**
     * Create connectors from server.xml if possible, or by creating some hardcoded defaults.
     *
     * @return a Set of connectors.  Never null or empty.
     */
    public static Collection<SsgConnector> getDefaultConnectors() {
        List<SsgConnector> ret = new ArrayList<SsgConnector>();

        boolean enableOtherConnectors = true;
        int initPort = ConfigFactory.getIntProperty( PROP_INIT_PORT, 0 );
        if ( initPort > 0 ) {
            enableOtherConnectors = false;
            SsgConnector https = new SsgConnector();
            https.setName("Default HTTPS ("+initPort+")");
            https.setScheme(SsgConnector.SCHEME_HTTPS);
            https.setEndpoints(defaultHttpsEndpoints);
            setListenerCiphers(https);
            https.setPort(initPort);
            https.setKeyAlias(null);
            https.setKeystoreGoid(null);
            https.setSecure(true);
            https.setClientAuth(SsgConnector.CLIENT_AUTH_OPTIONAL);
            https.setEnabled(true);

            String ipAddr = ConfigFactory.getProperty( PROP_INIT_ADDR );
            if ( ipAddr != null ) {
                // Fail if an ip address is specified but does not exist.
                validateIpAddress( ipAddr );
                https.putProperty( SsgConnector.PROP_BIND_ADDRESS, ipAddr );
            }

            ret.add(https);
        } else {
            SsgConnector https = new SsgConnector();
            https.setName("Default HTTPS (8443)");
            https.setScheme(SsgConnector.SCHEME_HTTPS);
            https.setEndpoints(defaultHttpsEndpoints);
            setListenerCiphers(https);
            https.setPort(8443);
            https.setKeyAlias(null);
            https.setKeystoreGoid(null);
            https.setSecure(true);
            https.setClientAuth(SsgConnector.CLIENT_AUTH_OPTIONAL);
            https.setEnabled(enableOtherConnectors);
            ret.add(https);
        }

        SsgConnector http = new SsgConnector();
        http.setName("Default HTTP (8080)");
        http.setScheme(SsgConnector.SCHEME_HTTP);
        http.setEndpoints(defaultHttpEndpoints);
        http.setPort(8080);
        http.setEnabled(true);
        ret.add(http);

        SsgConnector httpsNocc = new SsgConnector();
        httpsNocc.setName("Default HTTPS (9443)");
        httpsNocc.setScheme(SsgConnector.SCHEME_HTTPS);
        httpsNocc.setEndpoints(defaultHttpsEndpoints);
        setListenerCiphers(httpsNocc);
        httpsNocc.setPort(9443);
        httpsNocc.setKeyAlias(null);
        httpsNocc.setKeystoreGoid(null);
        httpsNocc.setSecure(true);
        httpsNocc.setClientAuth(SsgConnector.CLIENT_AUTH_NEVER);
        httpsNocc.setEnabled(enableOtherConnectors);
        ret.add(httpsNocc);

        SsgConnector nodeHttps = buildNodeHttpsConnector(2124);
        nodeHttps.setEnabled(enableOtherConnectors);
        ret.add(nodeHttps);

        return ret;
    }

    private static void setListenerCiphers(SsgConnector https) {
        if (defaultListenerStrongCiphers != null && defaultListenerStrongCiphers.trim().length() > 0)
            https.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, defaultListenerStrongCiphers);
    }

    public static Collection<SsgConnector> getRequiredConnectors( final Collection<SsgConnector> existingConnectors ) {
        List<SsgConnector> ret = new ArrayList<SsgConnector>();

        if ( !connectorExists( existingConnectors, SsgConnector.Endpoint.NODE_COMMUNICATION ) ) {
            int port = ConfigFactory.getIntProperty( "clusterPortOld", 0 );

            if ( port > 0 ) {
                // If port > 0 then they have a cluster property for the inter-node port
                // this is the case for upgraded systems
                ret.add(buildNodeHttpsConnector(port));
            }
        }

        return ret;
    }


    private static SsgConnector buildNodeHttpsConnector( int port ) {
        SsgConnector nodeHttps = new SsgConnector();
        nodeHttps.setName("Node HTTPS ("+port+")");
        nodeHttps.setScheme(SsgConnector.SCHEME_HTTPS);
        nodeHttps.setEndpoints(SsgConnector.Endpoint.NODE_COMMUNICATION.name() + "," + SsgConnector.Endpoint.PC_NODE_API.name());
        nodeHttps.setPort(port);
        nodeHttps.setKeyAlias(null);
        nodeHttps.setKeystoreGoid(null);
        nodeHttps.setSecure(true);
        nodeHttps.setClientAuth(SsgConnector.CLIENT_AUTH_OPTIONAL);
        nodeHttps.putProperty(SsgConnector.PROP_TLS_CIPHERLIST, defaultInternodeStrongCiphers);
        if ( !"0".equals(defaultInternodePoolSize) )
            nodeHttps.putProperty(SsgConnector.PROP_THREAD_POOL_SIZE, defaultInternodePoolSize);
        return nodeHttps;
    }

    /*
     * Does any current connector have the given endpoint? (even if disabled)
     */
    private static boolean connectorExists( final Collection<SsgConnector> connectors, final SsgConnector.Endpoint endpoint ) {
        boolean exists = false;

        for ( SsgConnector connector: connectors ) {
            if ( connector.offersEndpoint(endpoint) ) {
                exists = true;
                break;
            }
        }

        return exists;
    }

    /*
     * Validate that the specified IP address actually exists.
     */
    private static void validateIpAddress( final String ipAddress ) {
        if ( ipAddress != null ) {
            boolean isOk = false;
            try {
                isOk = InetAddressUtil.isAnyHostAddress(ipAddress) || NetworkInterface.getByInetAddress( InetAddress.getByName(ipAddress) ) != null;
            } catch (UnknownHostException uhe) {
                // not ok
            } catch (SocketException e) {
                // not ok
            }

            if ( !isOk ) {
                throw new RuntimeException("Invalid Listener IP addresss '"+ipAddress+"'.");
            }
        }
    }

}
