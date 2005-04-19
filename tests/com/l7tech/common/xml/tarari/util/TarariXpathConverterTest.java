/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
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

    public void testNestedPredicate() throws Exception {
        Map m = new HashMap();
        m.put("s", "urn:s");
        String got;
        String gave;

        try {
            gave = "/foo[@*[local-name()=\"blah\"]]";
            got = TarariXpathConverter.convertToTarariXpath(m, gave);
            fail("Expected exception was not thrown for nested predicate.  got=" + got);

            // If this worked, it'd pass through the nested predicate unchanged, but nested preds don't work on Tarari
            //assertEquals("/foo[@*[local-name()=\"blah\"]]", got);
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testAttributePredicate() throws Exception {
        Map m = new HashMap();
        m.put("s", "urn:s");
        String got;
        String gave;

        try {
            gave = "/foo[@s:blah]";
            got = TarariXpathConverter.convertToTarariXpath(m, gave);
            fail("Expected exception was not thrown:\ngave=" + gave + "\n got=" + got);
            // If this worked, it'd look like this, which is incompatible with Tarari due to the nested predicate:
            //assertEquals("/foo[@*[local-name() = \"blah\"  and namespace-uri() =\"urn:s\" ]]", got);
        } catch (ParseException e) {
            // ok
        }

        gave = "/foo[@undeclared:blah]";
        got = TarariXpathConverter.convertToTarariXpath(m, gave);
        assertEquals(gave, got);

    }

    public void testSimple() throws Exception {
        Map m = new HashMap();
        m.put("s", "urn:s");
        String got;
        String gave;

        gave = "/foo/@*[local-name()=\"blah\"]";
        got = TarariXpathConverter.convertToTarariXpath(m, gave);
        assertEquals(gave, got);

        gave = "/foo/*[local-name()=\"blah\"]/s:bar/@s:bletch";
        got = TarariXpathConverter.convertToTarariXpath(m, gave);
        assertEquals("/foo/*[local-name()=\"blah\"]/*[local-name() = \"bar\"  and namespace-uri() =\"urn:s\" ]/@*[local-name() = \"bletch\"  and namespace-uri() =\"urn:s\" ]", got);

        gave = "/s:Envelope/s:Body";
        got = TarariXpathConverter.convertToTarariXpath(m, gave);
        assertEquals("/*[local-name() = \"Envelope\"  and namespace-uri() =\"urn:s\" ]/*[local-name() = \"Body\"  and namespace-uri() =\"urn:s\" ]", got);
    }

    public void testConverter() throws Exception {
        {
            Map smallMap = new HashMap();
            smallMap.put("e", "http://junk.com/emp");

            String got;
            got = TarariXpathConverter.convertToTarariXpath(smallMap, "/e:employees/e:emp");
            assertEquals("/*[local-name() = \"employees\"  and namespace-uri() =\"http://junk.com/emp\" ]/*[local-name() = \"emp\"  and namespace-uri() =\"http://junk.com/emp\" ]", got);

            got = TarariXpathConverter.convertToTarariXpath(smallMap, "//e:emp");
            assertEquals("//*[local-name() = \"emp\"  and namespace-uri() =\"http://junk.com/emp\" ]", got);

            got = TarariXpathConverter.convertToTarariXpath(smallMap, "//e:*");
            assertEquals("//*[namespace-uri() =\"http://junk.com/emp\" ]", got);

            got = TarariXpathConverter.convertToTarariXpath(smallMap, "/foo/*:bar");
            assertEquals("/foo/*[local-name() = \"bar\" ]", got);

            got = TarariXpathConverter.convertToTarariXpath(smallMap, "/foo/bar:*");
            assertEquals("/foo/bar:*", got); // should pass through undeclared NS as literal match

            got = TarariXpathConverter.convertToTarariXpath(smallMap, "/foo/e:*");
            assertEquals("/foo/*[namespace-uri() =\"http://junk.com/emp\" ]", got);
        }

        {
            Map bigMap = new HashMap();
            bigMap.put("e", "http://junk.com/emp");
            bigMap.put("foo", "http://www.foo.com");
            bigMap.put("foo1", "http://www.food.com");

            String got;
            got = TarariXpathConverter.convertToTarariXpath(bigMap, "/e:employees/e:emp");
            assertEquals("/*[local-name() = \"employees\"  and namespace-uri() =\"http://junk.com/emp\" ]/*[local-name() = \"emp\"  and namespace-uri() =\"http://junk.com/emp\" ]", got);

            got = TarariXpathConverter.convertToTarariXpath(bigMap, "/foo:ducksoup[1]/foo1:*/foo:ducksoup[position()=1]");
            assertEquals("/*[local-name() = \"ducksoup\"  and namespace-uri() =\"http://www.foo.com\"  and (position() = 1)]/*[namespace-uri() =\"http://www.food.com\" ]/*[local-name() = \"ducksoup\"  and namespace-uri() =\"http://www.foo.com\"  and (position()=1)]", got);

            got = TarariXpathConverter.convertToTarariXpath(bigMap, "//e:emp");
            assertEquals("//*[local-name() = \"emp\"  and namespace-uri() =\"http://junk.com/emp\" ]", got);

            got = TarariXpathConverter.convertToTarariXpath(bigMap, "//e:*");
            assertEquals("//*[namespace-uri() =\"http://junk.com/emp\" ]", got);
        }
    }

    public void testIntegers() throws Exception {
        Map m = new HashMap();
        String gave;
        String got;

        try {
            gave = "/foo[3=3]";
            got = TarariXpathConverter.convertToTarariXpath(m, gave);
            fail("Expected exception was not thrown.  got=" + got);

            // If this worked, it would pass through unchanged:
            //assertEquals(gave, got);
            // Note that there is no reason this coudn't work with Tarari -- that this fails is a bug in xparser.g
            // TODO fix this bug
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testRelativeXpath() throws Exception {
        Map m = new HashMap();
        String gave;
        String got;

        try {
            gave = "foo/bar";
            got = TarariXpathConverter.convertToTarariXpath(m, gave);
            fail("Expected exception was not thrown.  got=" + got);
        } catch (ParseException e) {
            // Ok
        }
    }

    public void testBadSyntax() throws Exception {
        Map m = new HashMap();
        String gave;
        String got;

        String[] badones = new String[] {
            "/foo/@@bar",
            "/foo[@bar[]",
        };

        for (int i = 0; i < badones.length; i++) {
            gave = badones[i];
            try {
                got = TarariXpathConverter.convertToTarariXpath(m, gave);
                fail("Expected exception was not thrown.  got=" + got);
            } catch (ParseException e) {
                // Ok
            }
        }
    }

    public void testUndeclaredPrefix() throws Exception {
        Map smallMap = new HashMap();
        smallMap.put("asdf", "http://junk.com/emp");
        String gave;
        String got;

        gave = "/e:employees/e:emp";
        got = TarariXpathConverter.convertToTarariXpath(smallMap, gave);
        assertEquals("Undeclared prefix should pass-through as literal match", gave, got);
    }

    public void testDashInPrefixBug1711() throws Exception {
        Map map = new HashMap();
        map.put("SOAP-ENV", "http://blah");
        map.put("soapenv", "http://blah");
        map.put("typens", "http://bletch");

        String gave = "/soapenv:Envelope/soapenv:Body/typens:BrowseNodeSearchRequest";
        String got = TarariXpathConverter.convertToTarariXpath(map, gave);
        assertTrue("Unused namespace prefix containing a dash should be ignored", got != null && got.length() > 1);

        gave = "/soapenv:Envelope/SOAP-ENV:Body/typens:BrowseNodeSearchRequest";
        got = TarariXpathConverter.convertToTarariXpath(map, gave);
        assertTrue("Namespace prefix containing a dash should work", got != null && got.length() > 1);

        gave = "/BLEE-BLOO:Envelope/FOO-BAR:Body/BLETCH-BLOTCH:BrowseNodeSearchRequest";
        got = TarariXpathConverter.convertToTarariXpath(map, gave);
        assertEquals("Undeclared namespaces with dashes should pass through unchanged", gave, got);                
    }
}
