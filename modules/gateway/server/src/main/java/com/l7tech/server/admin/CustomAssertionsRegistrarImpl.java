package com.l7tech.server.admin;

import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.util.Collection;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarImpl
  extends ApplicationObjectSupport implements CustomAssertionsRegistrar, InitializingBean {
    protected CustomAssertionsRegistrar delegate;

    public CustomAssertionsRegistrarImpl() {
    }

    public void setDelegate(CustomAssertionsRegistrar delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] getAssertionClass(String className) {
        return delegate.getAssertionClass(className);
    }

    @Override
    public byte[] getAssertionResourceBytes(String path) {
        return delegate.getAssertionResourceBytes(path);
    }

    @Override
    public AssertionResourceData getAssertionResourceData( String path ) {
        return delegate.getAssertionResourceData(path);
    }

    /**
     * @return the list of all assertions known to the runtime
     */
    @Override
    public Collection getAssertions() {
        return delegate.getAssertions();
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     */
    @Override
    public Collection getAssertions(Category c) {
        return delegate.getAssertions(c);
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    @Override
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
    @Override
    public CustomAssertionUI getUI(String a) {
        return delegate.getUI(a);
    }

    /**
     * Resolve the policy in the xml string format with the custom assertions
     * support. The server is asked will resolve registered custom elements.
     *
     * @param xml the netity header representing the service
     * @return the policy tree
     * @throws java.io.IOException      on policy format error
     */
    @Override
    public Assertion resolvePolicy(String xml) throws IOException {
        return delegate.resolvePolicy(xml);
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        if (delegate == null) {
            throw new IllegalArgumentException("custom assertion registrar delegate required");
        }
    }
}
