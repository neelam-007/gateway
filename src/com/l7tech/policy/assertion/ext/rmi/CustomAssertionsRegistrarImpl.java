package com.l7tech.policy.assertion.ext.rmi;

import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionsRegistrar;
import com.l7tech.remote.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;
import net.jini.config.ConfigurationException;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl extends RemoteService implements CustomAssertionsRegistrar {
    static Logger logger = Logger.getLogger(CustomAssertionsRegistrar.class.getName());
    protected CustomAssertionsRegistrar delegate;

    public CustomAssertionsRegistrarImpl(String[] options, LifeCycle lifeCycle)
      throws ConfigurationException, IOException {
        super(options, lifeCycle);
        try {
            getDelegate();
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Error obtaining the custom assertion delegate", e);
        }
    }

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        return getDelegate().getAssertions();
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        return getDelegate().getAssertions(c);
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
        return getDelegate().resolvePolicy(eh);

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
        return getDelegate().resolvePolicy(xml);
    }

    private CustomAssertionsRegistrar getDelegate() {
        if (delegate != null) {
            return delegate;
        }
        delegate = (CustomAssertionsRegistrar)Locator.getDefault().lookup(CustomAssertionsRegistrar.class);
        if (delegate == null) {
            throw new RuntimeException("Unable to obtain the 'custom assertions' registrar implementation");
        }
        return delegate;
    }

}
