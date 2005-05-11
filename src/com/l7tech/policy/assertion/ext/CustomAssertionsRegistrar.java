package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.Assertion;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * The interface <code>CustomAssertionsRegistrar</code> implementations
 * provide access to the custom assertions that have been registered with
 * the runtime.
 */
public interface CustomAssertionsRegistrar {
    /**
     * @return the list of all assertions known to the runtime
     * @throws RemoteException on remote communication error
     */
    Collection getAssertions() throws RemoteException;

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws RemoteException on remote communication error
     */
    Collection getAssertions(Category c) throws RemoteException;

    /**
     * Resolve the policy in the xml string format with the custom assertions
     * support. The server is asked will resolve registered custom elements.
     *
     * @param xml the netity header representing the service
     * @return the policy tree
     * @throws RemoteException on remote invocation error
     * @throws IOException     on policy format error
     */
    Assertion resolvePolicy(String xml) throws IOException, RemoteException;

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     * Note that this method may not be invoked from management console. The descriptot
     * may not deserialize into the ssm envirnoment since int contains server classes.
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    CustomAssertionDescriptor getDescriptor(Class a);

    /**
     * Return the <code>CustomAssertionUI</code> class for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion UI class or <b>null</b>
     */
    CustomAssertionUI getUI(Class a);
}