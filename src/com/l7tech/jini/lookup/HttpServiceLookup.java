package com.l7tech.jini.lookup;

import com.l7tech.console.util.Preferences;
import net.jini.config.ConfigurationException;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.security.ProxyPreparer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.logging.Level;

/**
 * <code>ServiceLookup</code> is the utility class that  performs the
 * jini registrar service download over http.
 * <br>
 * Jini lookup and dscovery uses additional port, and this* lookup avoids
 * exposing additional port, as it reuses the servlet.
 * This approach effectively disables the "spontaneous" aspect (multicast
 * discovery) on the client side.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpServiceLookup extends ServiceLookup {
    private final String serviceUrl;

    /**
     * Instantiate http serrvice lookup.
     *
     * @throws ConfigurationException if problem encountered with
     * configuration
     * @throws IOException on io erro reading configuration
     */
    public HttpServiceLookup()
     throws ConfigurationException, IOException {
        super();
        serviceUrl = Preferences.getPreferences().getServiceUrl();
        if (serviceUrl == null) {
            throw new ConfigurationException("The service url cannot be null");
        }
    }

    /**
     * create the service proxy instance of the class cl with
     * optional context <code>Collection</code>.
     * <p>
     * Returns <b>null</b> if cannot locate the service or there
     * was an error in instantiating the service proxy.
     *
     * @param cl the class that
     * @param context optional context collection
     * @return the object instance of the class type
     */
    public Object getInstance(Class cl, Collection context) {
        /* Look up the remote server */
        try {
            ServiceMatches matches =
              getRegistrar().lookup(
                new ServiceTemplate(null, new Class[]{cl}, null), timeout);

            if (matches == null || matches.totalMatches == 0) {
                return null;
            }
            Object server = matches.items[0].service;
            /* Prepare the server proxy */
            ProxyPreparer preparer = getProxyPreparer(cl);
            return preparer.prepareProxy(server);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to obtain the service proxy", e);
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, "Unable to obtain the service proxy", e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Unable to obtain the service proxy", e);
        }
        return null;
    }

    /**
     * Get the service discovery manager.
     *
     * @return
     * @throws IOException
     * @throws ConfigurationException
     */
    protected ServiceRegistrar getRegistrar()
      throws IOException, ConfigurationException, ClassNotFoundException {
        URLConnection conn = null;
        URL url = new URL(serviceUrl+"/registrar");
        conn = url.openConnection();
        // for both input and output
        conn.setUseCaches(false);
        conn.setDefaultUseCaches(false);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setAllowUserInteraction(false);

        ObjectInputStream oi = new ObjectInputStream(conn.getInputStream());

        try {
            return (ServiceRegistrar) oi.readObject();
        } finally {
            if (oi !=null) oi.close();
        }
    }
}
