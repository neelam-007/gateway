/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.wsp;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.AllAssertions;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;
import java.util.ArrayList;

/**
 * Ensures that everything that can be serialized is also listed in {@link AllAssertions#SERIALIZABLE_EVERYTHING}.
 */
public class WspConstantsTest extends TestCase {
    private static Logger log = Logger.getLogger(WspConstantsTest.class.getName());

    public WspConstantsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(WspConstantsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private String getLocalName(Class c) {
        String fullname = c.getName();
        int lastdot = fullname.lastIndexOf('.');
        return fullname.substring(lastdot + 1);
    }
    
    public void testAllAssertionsIsComplete() throws Exception {
        Set<Class<? extends Assertion>> assertionClasses = new HashSet<Class<? extends Assertion>>();
        Assertion[] all = AllAssertions.SERIALIZABLE_EVERYTHING;
        for (Assertion ass : all)
            assertionClasses.add(ass.getClass());

        boolean failed = false;
        Collection<Class<? extends Assertion>> needed = new ArrayList<Class<? extends Assertion>>();

        TypeMapping[] mappings = WspConstants.typeMappings;
        for (TypeMapping typeMapping : mappings) {
            Class clazz = typeMapping.getMappedClass();
            if (Assertion.class.isAssignableFrom(clazz)) {
                if (!assertionClasses.contains(clazz)) {
                    //noinspection unchecked
                    needed.add(clazz);
                    log.warning("Class not listed in AllAssertions.SERIALIZABLE_EVERYTHING: " + clazz.getName());
                    failed = true;
                }
            }
        }

        if (failed) {
            System.err.println("Add the following lines to AllAssertions.SERIALIZABLE_EVERYTHING: \n");
            for (Class<? extends Assertion> assclass : needed) {
                String assname = getLocalName(assclass);
                System.out.println("        new " + assname + "(),");
            }

            fail("At least one assertion mapping was found for an assertion class not represented in AllAssertions.SERIALIZABLE_EVERYTHING");
        }
    }
}
