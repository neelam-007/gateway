package com.l7tech.server.partition;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.tomcat.ServerXmlParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class encapsulates the code for initializing the connectors table from the contents of server.xml.
 */
public class DefaultHttpConnectors {
    protected static final Logger logger = Logger.getLogger(DefaultHttpConnectors.class.getName());
    static final String defaultEndpoints = "MESSAGE_INPUT,ADMIN_REMOTE,ADMIN_APPLET,OTHER_SERVLETS";

    private static File findServerXml(String pathToServerXml) throws FileNotFoundException {
        if (pathToServerXml == null)
            throw new FileNotFoundException("No server.xml path configured.");
        return new File(pathToServerXml);
    }

    /**
     * Create connectors from server.xml if possible, or by creating some hardcoded defaults.
     *
     * @param pathToServerXml the fully qualified path used for locating server.xml
     * @return a Set of connectors.  Never null or empty.
     */
    public static Collection<SsgConnector> makeFallbackConnectors(String pathToServerXml) {
        List<Map<String, String>> connectors;
        try {
            ServerXmlParser serverXml = new ServerXmlParser();
            serverXml.load(findServerXml(pathToServerXml));
            connectors = serverXml.getConnectors();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unable to load connectors from server.xml (will use default connectors): " + ExceptionUtils.getMessage(e), e);
            return makeDefaultConnectors();
        }

        List<SsgConnector> ret = new ArrayList<SsgConnector>();
        for (Map<String, String> connector : connectors) {
            try {
                ret.add(makeConnectorFromServerXml(connector));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unable to create connector read from server.xml: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return ret;
    }

    /**
     * Last-ditch emergency fallback if there's no connectors in database and we can't read some from server.xml.
     * We'll create the traditional default connectors.
     *
     * @return a Set of SsgConnector instances to add.  Never null.
     */
    private static Collection<SsgConnector> makeDefaultConnectors() {
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

        return ret;
    }

    // Create an old-school connector, from properties read from server.xml
    private static SsgConnector makeConnectorFromServerXml(Map<String, String> connector) {
        SsgConnector c = new SsgConnector();
        c.setEnabled(true);
        int port = Integer.parseInt(connector.get("port"));
        c.setPort(port);
        c.setSecure(Boolean.valueOf(connector.get("secure")));
        String scheme = connector.get("scheme");
        scheme = scheme == null ? "HTTP" : scheme.toUpperCase();
        c.setScheme(scheme);
        c.setEndpoints(defaultEndpoints);
        c.setName("Legacy port " + port);

        String auth = connector.get("clientAuth");
        if ("want".equals(auth))
            c.setClientAuth(SsgConnector.CLIENT_AUTH_OPTIONAL);
        else if ("true".equals(auth))
            c.setClientAuth(SsgConnector.CLIENT_AUTH_ALWAYS);

        return c;
    }
}
