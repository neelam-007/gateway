package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

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
    Collection getAssertions() throws RemoteException;

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws RemoteException
     */
    Collection getAssertions(Category c) throws RemoteException;

    /**
     * Resolve the policy with the custom assertions support for a
     * given service. The server is asked will resolve registered
     * custom elements.
     * @param eh the netity header representing the service
     * @return the policy tree
     * @throws RemoteException on remote invocation error
     * @throws ObjectNotFoundException if the service cannot be found
     */
    Assertion resolvePolicy(EntityHeader eh)
      throws RemoteException, ObjectNotFoundException;
}