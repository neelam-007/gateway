package com.l7tech.common.util;

import java.rmi.RemoteException;

/**
 * Signifies that an admin operation was attempted without the proper admin role.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 1, 2004<br/>
 * $Id$<br/>
 */
public class UnauthorizedAdminOperation extends RemoteException {
    public UnauthorizedAdminOperation(String msg) {
        super(msg);
    }
}
