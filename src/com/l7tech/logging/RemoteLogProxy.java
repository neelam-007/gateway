package com.l7tech.logging;

import com.l7tech.adminws.logging.Client;
import java.rmi.RemoteException;

/**
 * Layer 7 technologies, inc.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 2:32:17 PM
 *
 * console entry point to retrieve ssg server logs
 */
public class RemoteLogProxy {

    public String[] getSSGLogs(int offset, int size) throws RemoteException {
        return getStub().getSystemLog(offset, size);
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private Client getStub() throws RemoteException {
        if (localStub == null) {
            localStub = new Client();
            if (localStub == null) throw new java.rmi.RemoteException("Exception getting admin ws stub");
        }
        return localStub;
    }
    private Client localStub = null;
}
