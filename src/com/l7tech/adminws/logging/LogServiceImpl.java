package com.l7tech.adminws.logging;

import com.l7tech.jini.export.RemoteService;
import com.l7tech.logging.LogManager;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


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
    }

    public String[] getSystemLog(int offset, int size) throws RemoteException {
        LogRecord[] records = LogManager.getInstance().getRecorded(offset, size);
        return logRecordsToStrings(records);
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private String[] logRecordsToStrings(LogRecord[] logs) {
        String[] output = new String[logs.length];
        for (int i = 0; i < logs.length; i++) {
            output[i] = new Date(logs[i].getMillis()).toString() + " - " +
              logs[i].getLevel().toString() + " - " +
              logs[i].getSourceClassName() + " - " +
              logs[i].getSourceMethodName() + " - " +
              logs[i].getMessage();
            if (logs[i].getThrown() != null)
                output[i] += " Exception: " + logs[i].getThrown().getClass().getName() + " " + logs[i].getThrown().getMessage();
        }
        return output;
    }

}
