package com.l7tech.remote.jini.lookup;

import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;
import net.jini.core.discovery.LookupLocator;
import net.jini.core.lookup.ServiceRegistrar;
import net.jini.export.Exporter;
import net.jini.id.UuidFactory;
import net.jini.jeri.BasicJeriExporter;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 16-May-2004
 */
public class RegistrarLookupImpl extends RemoteService implements RegistrarLookup {
    static final Logger logger = Logger.getLogger(RegistrarLookupImpl.class.getName());

    public RegistrarLookupImpl(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        super(options, lifeCycle);
    }

    /**
     * Obtain the <code>ServiceRegistrar</code> from the remote service implementation.
     *
     * @return the serviice registrar
     * @throws java.rmi.RemoteException on remote related error
     */
    public ServiceRegistrar getRegistrar() throws IOException {
        try {
            return getLookupLocator().getRegistrar();
        } catch (Exception e) {
            IOException ioe = new IOException("Unable to obtain registrar");
            ioe.initCause(e);
            logger.log(Level.SEVERE, ioe.getMessage(), e);
            throw ioe;
        }
    }

    private LookupLocator getLookupLocator() throws ConfigurationException, MalformedURLException {
        return
          (LookupLocator)getExportConfiguration().getConfigEntry("unicastLookupLocator",
            LookupLocator.class,
            new LookupLocator("jini://localhost"));
    }

    /**
     * Returns the exporter for exporting the server. Overiden to set the Uuid. The
     * same Uuid is always set, and the client resolves the service by uuid.
     *
     * @throws net.jini.config.ConfigurationException
     *          if a problem occurs getting the exporter
     *          from the configuration
     */
    protected Exporter getExporter() throws ConfigurationException {
        Exporter configuredExporter = getExportConfiguration().getExporter();
        if (configuredExporter instanceof BasicJeriExporter) {
            BasicJeriExporter be = (BasicJeriExporter)configuredExporter;
            BasicJeriExporter modExporter = new BasicJeriExporter(be.getServerEndpoint(),
              be.getInvocationLayerFactory(),
              be.getEnableDGC(),
              be.getKeepAlive(),
              UuidFactory.create(REGISTRAR_UUID));
            return modExporter;
        }
        throw new IllegalArgumentException("Unknown exporter type " + configuredExporter.getClass());
    }
}
