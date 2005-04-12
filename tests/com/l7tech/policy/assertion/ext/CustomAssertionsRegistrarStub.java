package com.l7tech.policy.assertion.ext;

import com.l7tech.identity.StubDataStore;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.policy.assertion.ClientTrueAssertion;
import com.l7tech.service.PublishedService;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarStub implements CustomAssertionsRegistrar {
    static Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());

    static {
        loadTestCustomAssertions();
    }

    private static void loadTestCustomAssertions() {
        CustomAssertionDescriptor eh =
          new CustomAssertionDescriptor("Test.Assertion",
            TestAssertionProperties.class,
            ClientTrueAssertion.class,
            TestServiceInvocation.class, Category.ACCESS_CONTROL, null);
        CustomAssertions.register(eh);
    }

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        Set customAssertionDescriptors = CustomAssertions.getAllDescriptors();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        final Set customAssertionDescriptors = CustomAssertions.getDescriptors(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    public Assertion resolvePolicy(EntityHeader eh) throws RemoteException, ObjectNotFoundException {
        final Map publishedServices = StubDataStore.defaultStore().getPublishedServices();
        PublishedService svc = (PublishedService)publishedServices.get(new Long(eh.getOid()));
        if (svc == null) {
            throw new ObjectNotFoundException("service " + eh);
        }
        try {
            return WspReader.parsePermissively(svc.getPolicyXml());
        } catch (IOException e) {
            ServerException se = new ServerException(e.getMessage());
            se.initCause(e);
            throw se;
        }
    }

    /**
     * Resolve the policy in the xml string format with the custom assertions
     * support. The server is asked will resolve registered custom elements.
     *
     * @param xml the netity header representing the service
     * @return the policy tree
     * @throws java.rmi.RemoteException on remote invocation error
     * @throws IOException              on policy format error
     */
    public Assertion resolvePolicy(String xml) throws RemoteException, IOException {
        return WspReader.parsePermissively(xml);
    }

    private Collection asCustomAssertionHolders(final Set customAssertionDescriptors) {
          Collection result = new ArrayList();
          Iterator it = customAssertionDescriptors.iterator();
          while (it.hasNext()) {
              try {
                  CustomAssertionDescriptor cd = (CustomAssertionDescriptor)it.next();
                  Class ca = cd.getAssertion();
                  CustomAssertionHolder ch = new CustomAssertionHolder();
                  final CustomAssertion cas = (CustomAssertion)ca.newInstance();
                  ch.setCustomAssertion(cas);
                  ch.setCategory(cd.getCategory());
                  result.add(ch);
              } catch (Exception e) {
                  logger.log(Level.WARNING, "Unable to instantiate custom assertion", e);
              }
          }
          return result;
      }
}