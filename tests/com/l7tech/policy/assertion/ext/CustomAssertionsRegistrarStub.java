package com.l7tech.policy.assertion.ext;

import java.util.List;
import java.util.ArrayList;
import java.rmi.RemoteException;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarStub implements CustomAssertionsRegistrar {
    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public List getAssertions() throws RemoteException {
        return new ArrayList();
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public List getAssertions(Category c) throws RemoteException {
        return new ArrayList();
    }
}