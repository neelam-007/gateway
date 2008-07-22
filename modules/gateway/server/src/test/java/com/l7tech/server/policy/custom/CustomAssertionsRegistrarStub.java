package com.l7tech.server.policy.custom;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.proxy.policy.assertion.ClientTrueAssertion;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.custom.CustomAssertionDescriptor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author emil
 * @version 16-Feb-2004
 */
public class CustomAssertionsRegistrarStub implements CustomAssertionsRegistrar {
    static Logger logger = Logger.getLogger( CustomAssertionsRegistrar.class.getName());

    static {
        loadTestCustomAssertions();
    }

    private static void loadTestCustomAssertions() {
        CustomAssertionDescriptor eh =
          new CustomAssertionDescriptor("Test.Assertion",
            TestAssertionProperties.class,
            null,
            TestServiceInvocation.class, Category.ACCESS_CONTROL, "", null);
        CustomAssertions.register(eh);
    }

    public byte[] getAssertionClass(String className) {
        return null;
    }

    public byte[] getAssertionResourceBytes(String path) {
        return null;
    }

    /**
     * @return the list of all assertions known to the runtime
     */
    public Collection getAssertions() {
        Set customAssertionDescriptors = CustomAssertions.getAllDescriptors();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     */
    public Collection getAssertions(Category c) {
        final Set customAssertionDescriptors = CustomAssertions.getDescriptors(c);
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * Return the <code>CustomAssertionUI</code> class for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion UI class or <b>null</b>
     */
    public CustomAssertionUI getUI(String a) {
        return CustomAssertions.getUI(a);
    }

    /**
     * Resolve the policy in the xml string format with the custom assertions
     * support. The server is asked will resolve registered custom elements.
     *
     * @param xml the netity header representing the service
     * @return the policy tree
     * @throws IOException              on policy format error
     */
    public Assertion resolvePolicy(String xml) throws IOException {
        return WspReader.getDefault().parsePermissively(xml);
    }

    /**
     * Return the <code>CustomAssertionDescriptor</code> for a given assertion or
     * <b>null<b>
     *
     * @param a the assertion class
     * @return the custom assertion descriptor class or <b>null</b>
     */
    public CustomAssertionDescriptor getDescriptor(Class a) {
        return CustomAssertions.getDescriptor(a);
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