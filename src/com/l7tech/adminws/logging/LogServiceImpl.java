package com.l7tech.adminws.logging;

import com.l7tech.jini.export.RemoteService;
import com.l7tech.common.util.UptimeMetrics;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;

/**
 * <code>Log</code> service implementation.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class LogServiceImpl extends RemoteService implements Log {
    /**
     * Creates the server.
     *
     * @param configOptions options to use when getting the Configuration
     * @throws ConfigurationException if a problem occurs creating the
     *	       configuration
     */
    public LogServiceImpl(String[] configOptions, LifeCycle lc)
      throws ConfigurationException, IOException {
        super(configOptions, lc);
        delegate = new Service();
    }

    public String[] getSystemLog(int offset, int size) throws RemoteException {
        return delegate.getSystemLog(offset, size);
    }

    public UptimeMetrics getUptime() throws RemoteException {
        return delegate.getUptime();
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private Log delegate = null;
}
