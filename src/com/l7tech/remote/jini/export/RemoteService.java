package com.l7tech.remote.jini.export;

import com.sun.jini.start.LifeCycle;
import net.jini.config.Configuration;
import net.jini.config.ConfigurationException;
import net.jini.config.ConfigurationProvider;
import net.jini.config.NoSuchEntryException;
import net.jini.core.lookup.ServiceID;
import net.jini.discovery.DiscoveryManagement;
import net.jini.discovery.LookupDiscovery;
import net.jini.export.Exporter;
import net.jini.export.ProxyAccessor;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lookup.JoinManager;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>RemoteService</code> is extended by the concrete Jini services.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class RemoteService implements Remote, ProxyAccessor {
    private String[] configOptions;
    private final LifeCycle lifeCycle;
    private static final Logger logger = Logger.getLogger(RemoteService.class.getName());
    private final String[] components = {getClass().getName(),
                                         RemoteService.class.getName()};

    /**
     * If the impl gets GC'ed, then the server will be unexported.
     * Store the server instances here to prevent this.
     */
    private static Map serverImpls =
      Collections.synchronizedMap(new HashMap());

    /* The configuration to use for configuring the server */
    protected final Configuration config;

    /** The server proxy (for use by getProxyVerifier) */
    protected Object serverProxy;
    protected Exporter exporter;

    public RemoteService(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        this.configOptions = options;
        this.lifeCycle = lifeCycle;
        config = ConfigurationProvider.getInstance(configOptions);
        init();
        serverImpls.put(this.getClass(), this);
        logger.info("The Service '" + getClass().getName() + "' is ready");
    }

    public static void unexportAll() {
        for (Iterator iterator = serverImpls.keySet().iterator(); iterator.hasNext();) {
            Object key = (Object) iterator.next();
            RemoteService rs = (RemoteService)serverImpls.get(key);
            logger.finer("Unexporting service " + key);

            try {
                rs.exporter.unexport(true);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while unexporting service " + key, e);
            }
        }
    }

    protected DiscoveryManagement getDiscoveryManager()
      throws ConfigurationException, IOException {
        /* Get the discovery manager, for discovering lookup services */

        DiscoveryManagement discoveryManager;

        for (int i = 0; i < components.length; i++) {
            String entry = components[i];
            try {
                discoveryManager =
                  (DiscoveryManagement) config.getEntry(
                    entry,
                    "discoveryManager", DiscoveryManagement.class);
                return discoveryManager;
            } catch (NoSuchEntryException e) {
                continue;
            }
        }
        /* Use the public group */
        discoveryManager =
          new LookupDiscovery(new String[]{""}, config);
        return discoveryManager;
    }

    /**
     * Initializes the server, including exporting it and storing its proxy in
     * the registry.
     *
     * @throws java.io.IOException  if a problem occurs
     * @throws ConfigurationException  if a problem occurs
     */
    protected void init() throws IOException, ConfigurationException {
        /* Export the server */
        exporter = getExporter();
        serverProxy = exporter.export(this);

        /* Create the smart proxy */
        // Proxy smartProxy = Proxy.create(serverProxy);

      
        /* Get the join manager, for joining lookup services */
        JoinManager joinManager =
          new JoinManager(serverProxy, null /* attrSets */, getServiceID(),
            getDiscoveryManager(), null /* leaseMgr */, config);
    }

    /**
     * Returns the exporter for exporting the server.
     *
     * @throws ConfigurationException if a problem occurs getting the exporter
     *	       from the configuration
     * @throws java.rmi.RemoteException if a remote communication problem occurs
     */
    protected Exporter getExporter()
      throws ConfigurationException, RemoteException {

        Exporter exporter;
        for (int i = 0; i < components.length; i++) {
            String entry = components[i];
            try {
                exporter = (Exporter)config.getEntry(entry, "serverExporter", Exporter.class);
                logger.info("Exporter config: "+exporter);
                return exporter;
            } catch (NoSuchEntryException e) {
                continue;
            }
        }
        exporter =
          new BasicJeriExporter(TcpServerEndpoint.getInstance(0),
            new BasicILFactory());
        logger.warning("No exporter configured, (will use anonymous TCP port) "+ exporter);
        return exporter;
    }

    public Object getProxy() {
        return serverProxy;
    }

    /** Returns the service ID for this server. */
    protected ServiceID getServiceID() {
        return createServiceID();
    }

    /** Creates a new service ID. */
    protected static ServiceID createServiceID() {
        Uuid uuid = UuidFactory.generate();
        return new ServiceID(
          uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }
}
