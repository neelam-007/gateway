package com.l7tech.adminws.system;

import com.l7tech.logging.LogManager;

import java.rmi.RemoteException;
import java.util.logging.Logger;
import java.util.Calendar;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Oct 2, 2003
 * Time: 4:07:53 PM
 * $Id$
 *
 * Server side implementation of the SystemService api.
 */
public class SystemServiceImpl implements SystemService {
    public void shutdown(long secondsToShutdown) throws RemoteException {
        String when = getTimeForShutdownCommand(secondsToShutdown);
        logger.warning("Remote shutdown scheduled for " + when);
         systemShutdown(when, false);
    }

    public void restart(long secondsToRestart) throws RemoteException {
        String when = getTimeForShutdownCommand(secondsToRestart);
        logger.warning("Remote restart scheduled for " + when);
        systemShutdown(when, true);
    }

    private void systemShutdown(String when, boolean restart) throws RemoteException {
        String command = "shutdown";
        if (restart) command += " -r";
        command += " " + when;
        // todo, drop this command somewhere to be pickuped by some cronjob
    }

    private String getTimeForShutdownCommand(long secondsfromnow) {
        if (secondsfromnow <= 0) return "now";
        else {
            // construct some time format understood by shutdown command
            // see man page of shutdown for more details
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(System.currentTimeMillis() + secondsfromnow);
            int hr = time.get(Calendar.HOUR_OF_DAY);
            int min = time.get(Calendar.MINUTE);
            return Integer.toString(hr) + ":" + Integer.toString(min);
        }
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
}
