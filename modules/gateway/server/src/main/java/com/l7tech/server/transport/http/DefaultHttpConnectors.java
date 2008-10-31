package com.l7tech.server.transport.http;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.ServerConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class encapsulates the code for initializing the connectors table from the contents of server.xml.
 */
public class DefaultHttpConnectors {
    protected static final Logger logger = Logger.getLogger(DefaultHttpConnectors.class.getName());
    static final String defaultEndpoints = "MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS";
    static final String defaultStrongCiphers = "TLS_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_DSS_WITH_AES_256_CBC_SHA";

    /**
     * Create connectors from server.xml if possible, or by creating some hardcoded defaults.
     *
     * @return a Set of connectors.  Never null or empty.
     */
    public static Collection<SsgConnector> getDefaultConnectors() {
        List<SsgConnector> ret = new ArrayList<SsgConnector>();

        SsgConnector http = new SsgConnector();
        http.setName("Default HTTP (8080)");
        http.setScheme(SsgConnector.SCHEME_HTTP);
        http.setEndpoints(defaultEndpoints);
        http.setPort(8080);
        http.setEnabled(true);
        ret.add(http);

        SsgConnector https = new SsgConnector();
        https.setName("Default HTTPS (8443)");
        https.setScheme(SsgConnector.SCHEME_HTTPS);
        https.setEndpoints(defaultEndpoints);
        https.setPort(8443);
        https.setKeyAlias("SSL");
        https.setSecure(true);
        https.setClientAuth(SsgConnector.CLIENT_AUTH_OPTIONAL);
        https.setEnabled(true);
        ret.add(https);

        SsgConnector httpsNocc = new SsgConnector();
        httpsNocc.setName("Default HTTPS (9443)");
        httpsNocc.setScheme(SsgConnector.SCHEME_HTTPS);
        httpsNocc.setEndpoints(defaultEndpoints);
        httpsNocc.setPort(9443);
        httpsNocc.setKeyAlias("SSL");
        httpsNocc.setSecure(true);
        httpsNocc.setClientAuth(SsgConnector.CLIENT_AUTH_NEVER);
        httpsNocc.setEnabled(true);
        ret.add(httpsNocc);

        SsgConnector nodeHttps = buildNodeHttpsConnector(2124);
        ret.add(nodeHttps);

        return ret;
    }

    public static Collection<SsgConnector> getRequiredConnectors( final Collection<SsgConnector> existingConnectors ) {
        List<SsgConnector> ret = new ArrayList<SsgConnector>();

        if ( !connectorExists( existingConnectors, SsgConnector.Endpoint.NODE_COMMUNICATION ) ) {
            int port = ServerConfig.getInstance().getIntPropertyCached("clusterPortOld", 0, 30000);

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
        nodeHttps.setKeyAlias("SSL");
        nodeHttps.setSecure(true);
        nodeHttps.setClientAuth(SsgConnector.CLIENT_AUTH_OPTIONAL);
        nodeHttps.putProperty(SsgConnector.PROP_CIPHERLIST, defaultStrongCiphers);
        nodeHttps.setEnabled(true);
        return nodeHttps;
    }

    /**
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
}
