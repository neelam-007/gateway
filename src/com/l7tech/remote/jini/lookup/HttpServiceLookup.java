package com.l7tech.remote.jini.lookup;

import com.l7tech.console.util.Preferences;
import net.jini.config.ConfigurationException;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.security.ProxyPreparer;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;

import javax.security.auth.Subject;
import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.rmi.ServerException;
import java.security.AccessController;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;


/**
 * <code>ServiceLookup</code> is the utility class that  performs the
 * jini registrar service download over http.
 * <br>
 * Jini lookup and discovery uses additional TCP port; this lookup (hack?)
 * avoids exposing additional port, as it reuses the HTTP engine.
 * This approach effectively disables the "spontaneous" aspect (multicast
 * discovery) on the client side.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpServiceLookup extends ServiceLookup {
    /**
     * Instantiate http serrvice lookup.
     *
     * @throws ConfigurationException if problem encountered with
     *                                configuration
     * @throws ConfigurationException on configuration error
     */
    public HttpServiceLookup()
      throws ConfigurationException {
        super();
    }

    /**
     * create the service proxy instance of the class cl with
     * optional context <code>Collection</code>.
     * <p/>
     * Returns <b>null</b> if cannot locate the service or there
     * was an error in instantiating the service proxy.
     *
     * @param cl      the class that
     * @param context optional context collection
     * @return the object instance of the class type
     */
    public Object lookup(Class cl, Collection context) {
        /* Look up the remote server */
        try {
            ServiceMatches matches =
              getRegistrar().lookup(new ServiceTemplate(null, new Class[]{cl}, null), timeout);

            if (matches == null || matches.totalMatches == 0) {
                return null;
            }
            Object server = matches.items[0].service;
            if (server == null) {
                logger.severe("Lookup returned null service for '" + cl.getClass() + "' service id '" + matches.items[0].serviceID + "'");
                return null;
            }
            /* Prepare the server proxy */
            ProxyPreparer preparer = getProxyPreparer(cl);
            return preparer.prepareProxy(server);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ConfigurationException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the service discovery manager.
     *
     * @return
     * @throws IOException
     * @throws ConfigurationException
     */
    protected synchronized ServiceRegistrar getRegistrar()
      throws IOException, ConfigurationException, ClassNotFoundException, GeneralSecurityException {
        if (registrar != null) {
            logger.fine("Returning cached service registrar " + registrar);
            return registrar;
        }
        HttpClient client = new HttpClient();
        HttpState state = client.getState();

        String serviceUrl = Preferences.getPreferences().getServiceUrl();
        if (serviceUrl == null) {
            throw new ConfigurationException("The service url cannot be null");
        }
        String registrarUrl = serviceUrl + "/registrar";
        logger.fine("Attempting connection to " + serviceUrl);
        GetMethod getMethod = new GetMethod(registrarUrl);
        getMethod.setDoAuthentication(true);
        state.setAuthenticationPreemptive(true);
        setAuthorizationHeader(state);
        client.setTimeout(5000);
        client.executeMethod(getMethod);

        int status = getMethod.getStatusCode();
        switch (status) {
            case 200:
                ObjectInputStream oi = new ObjectInputStream(getMethod.getResponseBodyAsStream());
                try {
                    registrar = (ServiceRegistrar)oi.readObject();
                    logger.fine("Obtained registrar " + registrar);
                    return registrar;
                } finally {
                    if (oi != null) oi.close();
                }
            case 401:
                throw new FailedLoginException("Invoke '"+registrarUrl+"' returned remote error ("+status+")");
            case 500:
                throw new ServerException("Invoke '"+registrarUrl+"' returned remote error ("+status+")");
            default:
                throw new IOException("Invoke '"+registrarUrl+"' returned remote error ("+status+")");
        }
    }

    /**
     * set the authorizaition properties for the connection
     *
     * @param state the http client state
     */
    private void setAuthorizationHeader(HttpState state) {
        Subject subject = Subject.getSubject(AccessController.getContext());
        if (subject == null) {
            logger.warning("No subject associated. The authentication will not be attempted");
            return;
        }

        Iterator i = subject.getPrincipals().iterator();

        String login = i.hasNext() ? ((Principal)i.next()).getName() : "";
        i = subject.getPrivateCredentials().iterator();
        String password = (i.hasNext() ? (new String((char[])i.next())) : "");

        state.setCredentials(null, null, new UsernamePasswordCredentials(login, password));

    }
}
