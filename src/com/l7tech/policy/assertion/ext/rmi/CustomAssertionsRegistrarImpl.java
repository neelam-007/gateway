package com.l7tech.policy.assertion.ext.rmi;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl
  extends ApplicationObjectSupport implements CustomAssertionsRegistrar, InitializingBean {
    protected CustomAssertionsRegistrar delegate;

    public void setDelegate(CustomAssertionsRegistrar delegate) {
        this.delegate = delegate;
    }

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        return delegate.getAssertions();
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        return delegate.getAssertions(c);
    }

    /**
     * Resolve the policy with the custom assertions support for a
     * given service. The server is asked will resolve registered
     * custom elements.
     *
     * @param eh the netity header representing the service
     * @return the policy tree
     * @throws java.rmi.RemoteException on remote invocation error
     * @throws com.l7tech.objectmodel.ObjectNotFoundException
     *                                  if the service cannot be found
     */
    public Assertion resolvePolicy(EntityHeader eh)
      throws RemoteException, ObjectNotFoundException {
        return delegate.resolvePolicy(eh);

    }

    /**
     * Resolve the policy in the xml string format with the custom assertions
     * support. The server is asked will resolve registered custom elements.
     *
     * @param xml the netity header representing the service
     * @return the policy tree
     * @throws java.rmi.RemoteException on remote invocation error
     * @throws java.io.IOException      on policy format error
     */
    public Assertion resolvePolicy(String xml) throws RemoteException, IOException {
        return delegate.resolvePolicy(xml);
    }


    public void afterPropertiesSet() throws Exception {
        if (delegate == null) {
            throw new IllegalArgumentException("custom assertion registrar delegate required");
        }
    }
}
