package com.l7tech.policy.assertion.ext;

import java.util.*;
import java.util.logging.Logger;

/**
 * The utility class that keeps track of the registered ext assertions.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class CustomAssertions {
    static Logger logger = Logger.getLogger(CustomAssertions.class.getName());

    /**
     * cannot instanitate
     */
    private CustomAssertions() {}

    public static void register(CustomAssertionDescriptor eh) {
        logger.fine("registering " + eh);
        assertions.put(eh.getName(), eh);
    }

    /**
     * Return client assertion for a given assertion or <b>null<b>
     *
     * @param a the assertion class
     * @return the client assertion class or <b>null</b>
     */
    public static Class getClientAssertion(Class a) {
        for (Iterator iterator = assertions.keySet().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor eh = (CustomAssertionDescriptor)iterator.next();
            if (a.equals(eh.getAssertion())) {
                return eh.getClientAssertion();
            }
        }
        return null;
    }

    /**
     * Return server assertion for a given assertion or <b>null<b>
     *
     * @param a the assertion class
     * @return the server assertion class or <b>null</b>
     */
    public static Class getServerAssertion(Class a) {
        for (Iterator iterator = assertions.keySet().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor eh = (CustomAssertionDescriptor)iterator.next();
            if (a.equals(eh.getAssertion())) {
                return eh.getServerAssertion();
            }
        }
        return null;
    }

    /**
     * @return the set of all assertions registered
     */
    public static Set getAssertions() {
        Set allAssertions = new HashSet();
        for (Iterator iterator = assertions.keySet().iterator(); iterator.hasNext();) {
            CustomAssertionDescriptor eh = (CustomAssertionDescriptor)iterator.next();
            allAssertions.add(eh.getAssertion());
        }
        return allAssertions;
    }

    private static Map assertions = new HashMap();
}