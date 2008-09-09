package com.l7tech.server.transport.http;

import com.l7tech.gateway.common.transport.SsgConnector;
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

        // TODO assign this port from configuration
        SsgConnector pcApi = new SsgConnector();
        pcApi.setName("Process Controller API (8766)");
        pcApi.setScheme(SsgConnector.SCHEME_HTTP);
        pcApi.setEndpoints(SsgConnector.Endpoint.PC_NODE_API.name());
        pcApi.setPort(8766);
        pcApi.setEnabled(true);
        ret.add(pcApi);

        return ret;
    }
}
