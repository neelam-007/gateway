package com.l7tech.cluster;

import com.l7tech.remote.jini.lookup.ServiceLookup;
import net.jini.config.ConfigurationException;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.security.ProxyPreparer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Utility class that  performs the jini registrar service download over http.
 *
 * This is the entry point for rmi connections between nodes inside a SSG cluster.
 *
 * It differs from com.l7tech.remote.jini.lookup.HttpServiceLookup in that it allows
 * the retrieval of the registrar over ssl without checking the remote server's ssl
 * cert's name (because we connect to direct ip address).
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 12, 2004<br/>
 * $Id$<br/>
 *
 */
public class InterNodeServiceLookup extends ServiceLookup {
    /**
     * Instantiate http service lookup.
     *
     * @throws net.jini.config.ConfigurationException if problem encountered with
     * configuration
     * @throws net.jini.config.ConfigurationException on configuration error
     */
    public InterNodeServiceLookup() throws ConfigurationException {
        super();
    }

    /**
     * create the service proxy instance of the class cl with
     * optional context <code>Collection</code>.
     * <p>
     * Returns <b>null</b> if cannot locate the service or there
     * was an error in instantiating the service proxy.
     *
     * @param cl the class that
     * @param host the host name
     * @return the object instance of the class type
     */
    public Object lookup(Class cl, String host) {
        /* Look up the remote server */
        try {
            ServiceMatches matches = getRegistrar(host).lookup(
                                        new ServiceTemplate(null, new Class[]{cl}, null), timeout);

            if (matches == null || matches.totalMatches == 0) {
                return null;
            }
            Object server = matches.items[0].service;
            ProxyPreparer preparer = getProxyPreparer(cl);
            return preparer.prepareProxy(server);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the service discovery manager.
     *
     * @return
     * @throws java.io.IOException
     * @throws ConfigurationException
     */
    protected synchronized ServiceRegistrar getRegistrar(String host)
      throws IOException, ConfigurationException, ClassNotFoundException {

        ServiceRegistrar cached = (ServiceRegistrar)registrars.get(host);
        if (cached != null) {
            return cached;
        }

        HttpURLConnection conn = null;
        URL url = new URL(host2URL(host));
        conn = (HttpURLConnection)url.openConnection();
        addLooseSslHostNameVerifier(conn);
        setAuthorizationHeader(conn);
        conn.setAllowUserInteraction(false);

        ObjectInputStream oi = new ObjectInputStream(conn.getInputStream());
        try {
            cached = (ServiceRegistrar)oi.readObject();
            registrars.put(host, cached);
            // todo, change locator host to the host value (READ ONLY?!)
            return cached;
        } finally {
            if (oi != null) oi.close();
        }
    }

    private String host2URL(String hostname) {
        // todo, make this configurable
        return "https://" + hostname + ":8443/ssg/registrar";
    }

    /**
     * set the authorizaition properties for the connection
     *
     * @param conn the connection
     */
    private void setAuthorizationHeader(URLConnection conn) {
        // todo, use the admin credentials from the database
        conn.setRequestProperty("Authorization", "Basic YWRtaW46cGFzc3dvcmQ=");
    }

    /**
     * stolen from LogonDialog
     * @param conn
     */
    private void addLooseSslHostNameVerifier(HttpURLConnection conn) {
        if (conn instanceof HttpsURLConnection) {
            ((HttpsURLConnection)conn).setHostnameVerifier(
              new HostnameVerifier() {
                  public boolean verify(String host, String peerHost) {
                      return true;
                  }
                  public boolean verify(String s, SSLSession sslSession) {
                      return true;
                  }
              });
        }
    }

    // instead of caching one registrar, we cache one per node
    // key is hostname value is ServiceRegistrar
    private final HashMap registrars = new HashMap();
    private final Logger logger = Logger.getLogger(getClass().getName());
}
