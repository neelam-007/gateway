/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.tarari.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Test case for TarariXpathConverter.
 */
public class TarariXpathConverterTest extends TestCase {
    private static Logger log = Logger.getLogger(TarariXpathConverterTest.class.getName());

    public TarariXpathConverterTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(TarariXpathConverterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testConverter() throws Exception {
        {
            Map smallMap = new HashMap();
            smallMap.put("e", "http://junk.com/emp");

            String got;
            got = TarariXpathConverter.convertToTarariXpath(smallMap, "/e:employees/e:emp");
            assertEquals(got, "/employees[namespace-uri() =\"http://junk.com/emp\" ]/emp[namespace-uri() =\"http://junk.com/emp\" ]");

            got = TarariXpathConverter.convertToTarariXpath(smallMap, "//e:emp");
            assertEquals(got, "//emp[namespace-uri() =\"http://junk.com/emp\" ]");

            got = TarariXpathConverter.convertToTarariXpath(smallMap, "//e:*");
            assertEquals(got, "//*[namespace-uri() =\"http://junk.com/emp\" ]");
        }

        {
            Map bigMap = new HashMap();
            bigMap.put("e", "http://junk.com/emp");
            bigMap.put("foo", "http://www.foo.com");
            bigMap.put("foo1", "http://www.food.com");

            String got;
            got = TarariXpathConverter.convertToTarariXpath(bigMap, "/e:employees/e:emp");
            assertEquals(got, "/employees[namespace-uri() =\"http://junk.com/emp\" ]/emp[namespace-uri() =\"http://junk.com/emp\" ]");

            got = TarariXpathConverter.convertToTarariXpath(bigMap, "/foo:ducksoup[1]/foo1:*/foo:ducksoup[position()=1]");
            assertEquals(got, "/ducksoup[namespace-uri() =\"http://www.foo.com\"  and (position() = 1)]/*[namespace-uri() =\"http://www.food.com\" ]/ducksoup[namespace-uri() =\"http://www.foo.com\"  and (position()=1)]");

            got = TarariXpathConverter.convertToTarariXpath(bigMap, "//e:emp");
            assertEquals(got, "//emp[namespace-uri() =\"http://junk.com/emp\" ]");

            got = TarariXpathConverter.convertToTarariXpath(bigMap, "//e:*");
            assertEquals(got, "//*[namespace-uri() =\"http://junk.com/emp\" ]");
        }
    }

    public void testUndeclaredPrefix() throws Exception {
        Map smallMap = new HashMap();
        smallMap.put("asdf", "http://junk.com/emp");

        String got;
        try {
            got = TarariXpathConverter.convertToTarariXpath(smallMap, "/e:employees/e:emp");
            fail("Expected exception was not throws after using undeclared namespace prefix");
        } catch (ParseException e) {
            // Ok
        }
    }
}
