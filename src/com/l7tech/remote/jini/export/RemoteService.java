package com.l7tech.remote.jini.export;

import com.l7tech.common.Authorizer;
import com.l7tech.identity.Group;
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
import net.jini.export.ServerContext;
import net.jini.id.Uuid;
import net.jini.id.UuidFactory;
import net.jini.io.context.ClientSubject;
import net.jini.jeri.BasicILFactory;
import net.jini.jeri.BasicJeriExporter;
import net.jini.jeri.tcp.TcpServerEndpoint;
import net.jini.lookup.JoinManager;

import javax.security.auth.Subject;
import java.io.IOException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.server.ExportException;
import java.rmi.server.ServerNotActiveException;
import java.security.AccessControlException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>RemoteService</code> is extended by the concrete Jini services.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public abstract class RemoteService implements Remote, ProxyAccessor {
    private final LifeCycle lifeCycle;
    private static final Logger logger = Logger.getLogger(RemoteService.class.getName());
    private ExportConfiguration exportConfiguration;
    /**
     * Default services configuraiton file.
     * When changing, note that is used by start-services.config
     */
    public static final String DEFAULT_CONFIG = "services.config";

    /**
     * If the impl gets GC'ed, then the server will be unexported.
     * Store the server instances here to prevent this.
     */
    private static Set serverImpls = Collections.synchronizedSet(new HashSet());


    /**
     * The server proxy (for use by getProxyVerifier)
     */
    protected Object serverProxy;
    protected Exporter exporter;

    public RemoteService(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        this.lifeCycle = lifeCycle;
        exportConfiguration = new ExportConfiguration(options, this.getClass());
        exportAndRegister();

    }

    public static void unexportAll() {
        for (Iterator iterator = serverImpls.iterator(); iterator.hasNext();) {
            RemoteService rs = (RemoteService)iterator.next();

            try {
                rs.exporter.unexport(true);
                logger.finer("Unexported service " + rs);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error while unexporting service " + rs, e);
            }
        }
    }

    /**
     * Exports the specified remote object and returns a <code>Exported</code>
     * instance containing the proxy and the exporter.
     * The exporter may be used to unexport object programatically {@link Exporter#unexport(boolean)}.
     *
     * @param impl      the remote object to export
     * @param enableDGC if <code>true</code>, DGC is enabled to the object
     *                  on this server endpoint
     * @return
     * @throws ConfigurationException on configuraton error
     * @throws ExportException        on export error
     */
    public static Exported export(Remote impl, boolean enableDGC)
      throws ConfigurationException, ExportException {
        if (impl == null) {
            throw new IllegalArgumentException();
        }
        URL url = RemoteService.class.getClassLoader().getResource(DEFAULT_CONFIG);
        if (url == null) {
            throw new ConfigurationException("'" + DEFAULT_CONFIG + "' not found");
        }

        ExportConfiguration cfg =
          new ExportConfiguration(new String[]{url.toString()}, impl.getClass());
        Exporter exporter = enableDGC ? cfg.getDGCExporter() : cfg.getExporter();
        Remote proxy = exporter.export(impl);
        return new Exported(exporter, proxy);
    }

    public static class Exported {
        private Exporter exporter;
        private Remote proxy;

        Exported(Exporter exporter, Remote proxy) {
            this.exporter = exporter;
            this.proxy = proxy;
        }

        public Exporter getExporter() {
            return exporter;
        }

        public Remote getProxy() {
            return proxy;
        }
    }

    protected ExportConfiguration getExportConfiguration() {
        return exportConfiguration;
    }

    /**
     * A single remote server export configuration.
     */
    protected static class ExportConfiguration {
        /* The configuration to use for configuring the server */
        private Configuration config;
        private String[] configOptions;
        private String[] components = {RemoteService.class.getName()};
        private Exporter exporter = null;
        private Exporter dgcEnabledexporter = null;
        private DiscoveryManagement discoveryManager = null;

        public ExportConfiguration(String[] configOptions, Class serviceClass)
          throws ConfigurationException {
            this.configOptions = configOptions;
            config = ConfigurationProvider.getInstance(configOptions);
            if (serviceClass != null) {
                components = new String[]{serviceClass.getName(), RemoteService.class.getName()};
            }
        }

        public Configuration getJiniConfig() {
            return config;
        }

        public DiscoveryManagement getDiscoveryManager()
          throws ConfigurationException, IOException {

            if (discoveryManager != null) {
                return discoveryManager;

            }
            /* Get the discovery manager, for discovering lookup services */
            discoveryManager =
              (DiscoveryManagement)getConfigEntry("discoveryManager",
                DiscoveryManagement.class,
                new LookupDiscovery(new String[]{""}, config));
            return discoveryManager;
        }

        /**
         * Returns the exporter for exporting the server.
         *
         * @throws ConfigurationException if a problem occurs getting the exporter
         *                                from the configuration
         */
        public Exporter getDGCExporter() throws ConfigurationException {
            if (dgcEnabledexporter != null) {
                return dgcEnabledexporter;
            }
            dgcEnabledexporter =
              (Exporter)getConfigEntry("serverDgcEnabledExporter", Exporter.class,
                new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory()));
            return dgcEnabledexporter;
        }

        /**
         * Returns the DGC enabled exporter for exporting the server.
         *
         * @throws ConfigurationException if a problem occurs getting the exporter
         *                                from the configuration
         */
        public Exporter getExporter() throws ConfigurationException {
            if (exporter != null) {
                return exporter;
            }
            exporter =
              (Exporter)getConfigEntry("serverExporter", Exporter.class,
                new BasicJeriExporter(TcpServerEndpoint.getInstance(0), new BasicILFactory()));
            return exporter;
        }


        /**
         * Searches the configuration entry in the <code>RemoteService</code> subclass section
         * and the <code>RemoteService</code> section  in that order
         *
         * @param name  the configuration entry name
         * @param clazz the confuguration entry class
         * @param def   the default value in case the entry is not found
         * @throws ConfigurationException if a problem occurs getting the exporter
         *                                from the configuration
         */
        public final Object getConfigEntry(String name, Class clazz, Object def)
          throws ConfigurationException {

            Object result;
            for (int i = 0; i < components.length; i++) {
                String section = components[i];
                try {
                    result = config.getEntry(section, name, clazz);
                    logger.fine("Found config entry " + result + " for name '" + name + "'");
                    return result;
                } catch (NoSuchEntryException e) {
                    continue;
                }
            }
            logger.fine("Returning default value " + def + " for name '" + name + "'");
            return def;
        }
    }


    /**
     * Initializes the server, including exporting it and storing its proxy in
     * the registry.
     *
     * @throws java.io.IOException    if a problem occurs
     * @throws ConfigurationException if a problem occurs
     */
    protected void exportAndRegister() throws IOException, ConfigurationException {
        /* Export the server */
        exporter = exportConfiguration.getExporter();
        serverProxy = exporter.export(this);

        /* Create the smart proxy */
        // Proxy smartProxy = Proxy.create(serverProxy);

      
        /* Get the join manager, for joining lookup services */
        JoinManager joinManager =
          new JoinManager(serverProxy, null /* attrSets */, getServiceID(),
            exportConfiguration.getDiscoveryManager(), null /* leaseMgr */, exportConfiguration.getJiniConfig());
        serverImpls.add(this);
        logger.fine("The Service '" + getClass().getName() + "' is exported.");
    }


    public Object getProxy() {
        return serverProxy;
    }

    /**
     * Returns the service ID for this server.
     */
    protected ServiceID getServiceID() {
        return createServiceID();
    }

    /**
     * Creates a new service ID.
     */
    protected static ServiceID createServiceID() {
        Uuid uuid = UuidFactory.generate();
        return new ServiceID(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    /**
     * Returns a boolean indicating whether the authenticated user is included in the specified
     * logical "roles".
     *
     * @param roles role - a String array specifying the role names
     * @return a boolean indicating whether the user making this request belongs to one or more given
     *         roles; false if not or the user has not been authenticated
     */
    protected boolean isUserInRole(String[] roles) {
        Subject subject = getCurrentSubject();
        if (subject == null) {
            return false;
        }
        return Authorizer.getAuthorizer().isSubjectInRole(subject, roles);
    }

    /**
     * Get the current <code>Subject</code> that carries out the remote operation
     *
     * @return the current subject or <b>null</b>
     * @throws IllegalStateException if the operation is invoked in wrong time (i.e. service
     *                               not started)
     */
    protected Subject getCurrentSubject() throws IllegalStateException {
        try {
            ClientSubject clientSubject = (ClientSubject)ServerContext.getServerContextElement(ClientSubject.class);
            if (clientSubject == null) return null;
            return clientSubject.getClientSubject();
        } catch (ServerNotActiveException e) {
            IllegalStateException ise = new IllegalStateException();
            ise.initCause(e);
            throw ise;
        }
    }

    /**
     * Makes sure that current subject has full write admin role.
     *
     * @throws AccessControlException if not the case
     */
    protected void enforceAdminRole() throws AccessControlException {
        if (!isUserInRole(new String[]{Group.ADMIN_GROUP_NAME})) {
            throw new AccessControlException("Must be member of " + Group.ADMIN_GROUP_NAME +
              " to perform this operation.");
        }
    }
}
