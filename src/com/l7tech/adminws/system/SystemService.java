package com.l7tech.adminws.system;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Oct 2, 2003
 * Time: 4:00:53 PM
 * $Id$
 *
 * System related operations such as shutdown and restart.
 * This is only available through RMI interface (not through axis admin ws)
 *
 */
public interface SystemService extends Remote {
    void shutdown(long secondsToShutdown) throws RemoteException;
    void restart(long secondsToRestart) throws RemoteException;
}
