package com.l7tech.server.policy.custom;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertionUI;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.wsp.WspReader;
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
            TestServiceInvocation.class, Category.ACCESS_CONTROL, "", "", "");
        CustomAssertions.register(eh);
    }

    @Override
    public byte[] getAssertionClass(String className) {
        return null;
    }

    @Override
    public byte[] getAssertionResourceBytes(String path) {
        return null;
    }

    @Override
    public AssertionResourceData getAssertionResourceData( String name ) {
        return null;
    }

    /**
     * @return the list of all assertions known to the runtime
     */
    @Override
    public Collection getAssertions() {
        Set customAssertionDescriptors = CustomAssertions.getAllDescriptors();
        return asCustomAssertionHolders(customAssertionDescriptors);
    }

    /**
     * @param c the category to query for
     * @return the list of all assertions known to the runtime
     *         for a give n category
     */
    @Override
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
    @Override
    public CustomAssertionUI getUI(String a) {
        return CustomAssertions.getUI(a);
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
