package com.l7tech.policy.assertion.ext;

import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.identity.StubDataStore;
import com.l7tech.service.PublishedService;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.io.IOException;

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

    public Assertion resolvePolicy(EntityHeader eh) throws RemoteException, ObjectNotFoundException {
        final Map publishedServices = StubDataStore.defaultStore().getPublishedServices();
        PublishedService svc = (PublishedService)publishedServices.get(new Long(eh.getOid()));
        if (svc == null) {
            throw new ObjectNotFoundException("service "+eh);
        }
        try {
            return WspReader.parse(svc.getPolicyXml());
        } catch (IOException e) {
            ServerException se = new  ServerException(e.getMessage());
            se.initCause(e);
            throw se;
        }
    }
}