package com.l7tech.policy.assertion.ext;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * The interface <code>CustomAssertionsRegistrar</code> implementations
 * provide access to the custom assertions that have been registered with
 * the runtime.
 *
 * @author emil
 * @version Feb 13, 2004
 */
public interface CustomAssertionsRegistrar extends Remote {
    /**
     * @return the list of all assertions known to the runtime
     * @throws RemoteException
     */
    List getAssertions() throws RemoteException;

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws RemoteException
     */
    List getAssertions(Category c) throws RemoteException;
}