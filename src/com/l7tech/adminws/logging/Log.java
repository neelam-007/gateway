package com.l7tech.adminws.logging;

import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 * Insert comments here.
 *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public interface Log extends Remote {
    String[] getSystemLog(int offset, int size) throws RemoteException;
}
