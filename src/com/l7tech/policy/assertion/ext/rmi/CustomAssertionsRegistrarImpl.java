package com.l7tech.policy.assertion.ext.rmi;

import com.l7tech.common.LicenseException;
import com.l7tech.common.LicenseManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
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

    private final LicenseManager licenseManager;

    public CustomAssertionsRegistrarImpl(LicenseManager licenseManager) {
        if (licenseManager == null)
            throw new IllegalArgumentException("License manager required");
        this.licenseManager = licenseManager;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature("service:Admin");
        } catch (LicenseException e) {
            throw new RemoteException(e.getMessage());
        }
    }

    public void setDelegate(CustomAssertionsRegistrar delegate) {
        this.delegate = delegate;
    }

    public byte[] getAssertionClass(String className) throws RemoteException {
        checkLicense();
        return delegate.getAssertionClass(className);
    }

    /**
     * @return the list of all assertions known to the runtime
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions() throws RemoteException {
        checkLicense();
        return delegate.getAssertions();
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     * @throws java.rmi.RemoteException
     */
    public Collection getAssertions(Category c) throws RemoteException {
        checkLicense();
        return delegate.getAssertions(c);
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    public CustomAssertionDescriptor getDescriptor(Class a) {
        return delegate.getDescriptor(a);
    }

    /**
     * Return the <code>CustomAssertionUI</code> class for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class name
     * @return the custom assertion UI class or <b>null</b>
     */
    public CustomAssertionUI getUI(String a) {
        return delegate.getUI(a);
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
        checkLicense();
        return delegate.resolvePolicy(xml);
    }


    public void afterPropertiesSet() throws Exception {
        if (delegate == null) {
            throw new IllegalArgumentException("custom assertion registrar delegate required");
        }
    }
}
