package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.CustomAssertionHolder;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarStub implements CustomAssertionsRegistrar {
    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        return new ArrayList();
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        CustomAssertionHolder ca = new CustomAssertionHolder();
        ca.setCustomAssertion(new TestAssertionProperties());
        List list = new ArrayList();
        list.add(ca);
        return list;

    }
}