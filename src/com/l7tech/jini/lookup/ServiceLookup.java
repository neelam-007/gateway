package com.l7tech.jini.lookup;

import com.l7tech.util.locator.ObjectFactory;
import net.jini.config.*;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.core.lookup.ServiceMatches;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.core.discovery.LookupLocator;
import net.jini.discovery.LookupDiscovery;
import net.jini.lookup.ServiceDiscoveryManager;
import net.jini.security.BasicProxyPreparer;
import net.jini.security.ProxyPreparer;
import net.jini.security.policy.DynamicPolicyProvider;
import net.jini.security.policy.PolicyInitializationException;

import java.io.IOException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.Policy;
import java.net.URL;

/**
 * <code>ServiceLookup</code> is the utility class that
 * performs the jini service lookup.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServiceLookup implements ObjectFactory {
    protected static final Logger logger = Logger.getLogger(ServiceLookup.class.getName());
    protected int timeout = 5000;
    private final Configuration config;
    private static final String CLIENT_CONFIG = "etc/jini/client.config";
    String[] configOptions = {CLIENT_CONFIG};
    private ServiceDiscoveryManager discoveryManager;
    private ServiceRegistrar registrar;


    static {
        try {
            Policy policy = Policy.getPolicy();
            if (policy == null) {
                Policy.setPolicy(new DynamicPolicyProvider());
            }
        } catch (Exception e) {
            throw new InitializationException("Unable to intialize service lookup policy", e);
        }
    }

    public ServiceLookup() throws ConfigurationException {
        Configuration cf;
        try {
            cf = ConfigurationProvider.getInstance(configOptions);
        } catch (ConfigurationNotFoundException e) {
            cf = ConfigurationProvider.getInstance(null);
        }
        config = cf;
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
            /*       } catch (InterruptedException e) {
                       logger.log(Level.WARNING, "Unable to obtain the service proxy", e);*/
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to obtain the service proxy", e);
        } catch (ConfigurationException e) {
            logger.log(Level.SEVERE, "Unable to obtain the service proxy", e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
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
    protected ServiceDiscoveryManager getServiceDiscovery()
      throws IOException, ConfigurationException {
        if (discoveryManager != null) return discoveryManager;

        String entry = ServiceLookup.class.getName();
        try {
            discoveryManager =
              (ServiceDiscoveryManager) config.getEntry(
                entry,
                "serviceDiscovery", ServiceDiscoveryManager.class);
            return discoveryManager;
        } catch (NoSuchEntryException e) {
            // use default
        }
        discoveryManager =
          new ServiceDiscoveryManager(
            new LookupDiscovery(new String[]{""}, config),
            null, config);
        return discoveryManager;
    }

    protected ServiceRegistrar getRegistrar()
      throws ConfigurationException, IOException, ClassNotFoundException {
        if (registrar == null) {
            registrar = getLookupLocator().getRegistrar(timeout);
        }
        return registrar;
    }


    /**
     * Get the service discovery manager.
     *
     * @return
     * @throws IOException
     * @throws ConfigurationException
     */
    protected LookupLocator getLookupLocator()
      throws IOException, ConfigurationException {
        String entry = ServiceLookup.class.getName();
        try {
            LookupLocator locator =
              (LookupLocator) config.getEntry(
                entry,
                "lookupLocator", LookupLocator.class);
            return locator;
        } catch (NoSuchEntryException e) {
            // use default
        }
        return new LookupLocator("jini://localhost");
    }

    /**
     * Get the proxy preparer.
     *
     * @param cl
     * @return
     * @throws IOException
     * @throws ConfigurationException
     */
    protected ProxyPreparer getProxyPreparer(Class cl)
      throws IOException, ConfigurationException {
        ProxyPreparer preparer;
        String[] components = {cl.getName(), ServiceLookup.class.getName()};

        for (int i = 0; i < components.length; i++) {
            String entry = components[i];
            try {
                preparer =
                  (ProxyPreparer) config.getEntry(entry,
                    "preparer",
                    ProxyPreparer.class);
                return preparer;
            } catch (NoSuchEntryException e) {
                continue;
            }
        }
        preparer = new BasicProxyPreparer();
        return preparer;
    }

    /**
     * special purpose exception to indicate failure intializing service lookup.
     */
    public static final class InitializationException extends RuntimeException {
        private InitializationException(String message) {
            this(message, null);
        }

        private InitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
