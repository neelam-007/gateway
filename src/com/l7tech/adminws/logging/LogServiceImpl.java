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


    /**
     * Retrieve the system logs in between the startMsgNumber and endMsgNumber specified
     * up to the specified size.
     * NOTE: the log messages whose message number equal to startMsgNumber and endMsgNumber
     * are not returned.
     *
     * @param startMsgNumber the message number to locate the start point.
     *                       Start from beginning of the message buffer if it equals to -1.
     * @param endMsgNumber   the message number to locate the end point.
     *                       Retrieve messages until the end of the message buffer is hit if it equals to -1.
     * @param size  the max. number of messages retrieved
     * @return String[] the array of messages retrieved
     * @throws RemoteException
     */
    public String[] getSystemLog(long startMsgNumber, long endMsgNumber, int size) throws RemoteException {
            return delegate.getSystemLog(startMsgNumber, endMsgNumber, size);
        }

    public UptimeMetrics getUptime() throws RemoteException {
        return delegate.getUptime();
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private Log delegate = null;
}
