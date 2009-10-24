/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.tarari.util;

import com.l7tech.test.BugNumber;
import com.l7tech.util.ComparisonOperator;
import com.l7tech.xml.xpath.FastXpath;
import static org.junit.Assert.*;
import org.junit.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Test case for TarariXpathConverter.
 */
public class TarariXpathConverterTest {
    @Test
    public void testNestedPredicate() throws Exception {
        Map<String, String> m = new HashMap<String, String>();
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

    @Test
    public void testAttributePredicate() throws Exception {
        Map<String, String> m = new HashMap<String, String>();
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

    @Test
    public void testSimple() throws Exception {
        Map<String, String> m = new HashMap<String, String>();
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

    @Test
    public void testConverter() throws Exception {
        {
            Map<String, String> smallMap = new HashMap<String, String>();
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
            Map<String, String> bigMap = new HashMap<String, String>();
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

    @SuppressWarnings({"UnusedDeclaration", "UnusedAssignment"})
    @Test(expected = ParseException.class)
    public void testIntegers() throws Exception {
        Map m = new HashMap();
        String gave;
        String got;

        gave = "/foo[3=3]";
        got = TarariXpathConverter.convertToTarariXpath(m, gave);

        // If this worked, it would pass through unchanged:
        //assertEquals(gave, got);
        // Note that there is no reason this coudn't work with Tarari -- that this fails is a bug in xparser.g
        // TODO fix this bug
    }

    @Test(expected = ParseException.class)
    public void testSimpleVariableReference() throws Exception {
        TarariXpathConverter.convertToTarariXpath(new HashMap(), "$foo");
    }

    @Test(expected = ParseException.class)
    public void testPredicateVariableReference() throws Exception {
        TarariXpathConverter.convertToTarariXpath(new HashMap(), "/*[local-name() = $foo]");
    }

    @Test(expected = ParseException.class)
    public void testRelativeXpath() throws Exception {
        TarariXpathConverter.convertToTarariXpath(new HashMap(), "foo/bar");
    }

    @Test(expected = ParseException.class)
    public void testNonRootDescendantOrSelfXpath() throws Exception {
        TarariXpathConverter.convertToTarariXpath(new HashMap(), "/foo//bar");
    }

    @Test
    public void testBadSyntax() throws Exception {
        Map m = new HashMap();
        String gave;
        String got;

        String[] badones = new String[] {
            "/foo/@@bar",
            "/foo[@bar[]",
        };

        for (String badone : badones) {
            gave = badone;
            try {
                got = TarariXpathConverter.convertToTarariXpath(m, gave);
                fail("Expected exception was not thrown.  got=" + got);
            } catch (ParseException e) {
                // Ok
            }
        }
    }

    @Test
    public void testUndeclaredPrefix() throws Exception {
        Map<String, String> smallMap = new HashMap<String, String>();
        smallMap.put("asdf", "http://junk.com/emp");
        String gave;
        String got;

        gave = "/e:employees/e:emp";
        got = TarariXpathConverter.convertToTarariXpath(smallMap, gave);
        assertEquals("Undeclared prefix should pass-through as literal match", gave, got);
    }

    @Test
    @BugNumber(1711)
    public void testDashInPrefixBug1711() throws Exception {
        Map<String, String> map = new HashMap<String, String>();
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

    @Test(expected = ParseException.class)
    public void testAlexBofaBug() throws Exception {
        Map<String, String> map = nsmap();
        String gave="/soapenv:Envelope/soapenv:Body/api:RetrieveTransactionHistoryV001/api:osaRequestHeader/osa:rulesBag/osa:sets/osa:set[osa:name=\"routingRules\"]/osa:attributes/osa:attribute/osa:value";
        TarariXpathConverter.convertToTarariXpath(map, gave);
    }

    private static Map<String, String> nsmap() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("soapenv", "http://blah/soapenv");
        map.put("api", "http://blah/api");
        map.put("osa", "http://blah/osa");
        return map;
    }

    @Test
    public void testZeroEqCount() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "0 = count(/soapenv:Envelope/soapenv:Body/api:Blah)");
        assertEquals(fx.getCountComparison(), ComparisonOperator.EQ);
        assertEquals(fx.getCountValue(), new Integer(0));
        assertEquals(fx.getExpression(), "/*[local-name() = \"Envelope\"  and namespace-uri() =\"http://blah/soapenv\" ]/*[local-name() = \"Body\"  and namespace-uri() =\"http://blah/soapenv\" ]/*[local-name() = \"Blah\"  and namespace-uri() =\"http://blah/api\" ]");
    }

    @Test
    public void testCountNePos() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "count(/soapenv:Envelope/soapenv:Body/api:Blah) != 4");
        assertEquals(fx.getCountComparison(), ComparisonOperator.NE);
        assertEquals(fx.getCountValue(), new Integer(4));
    }

    @Test
    public void testCountNeNeg() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "count(/soapenv:Envelope/soapenv:Body/api:Blah) != -4");
        assertEquals(fx.getCountComparison(), ComparisonOperator.NE);
        assertEquals(fx.getCountValue(), new Integer(-4));
    }

    @Test
    public void testNegEqCount() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "   -4  != count(/soapenv:Envelope/soapenv:Body/api:Blah)");
        assertEquals(fx.getCountComparison(), ComparisonOperator.NE);
        assertEquals(fx.getCountValue(), new Integer(-4));
    }

    @Test
    public void testCountLtPos() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "count(/soapenv:Envelope/soapenv:Body/api:Blah)<59");
        assertEquals(fx.getCountComparison(), ComparisonOperator.LT);
        assertEquals(fx.getCountValue(), new Integer(59));
    }

    @Test
    public void testCountLtNeg() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "count(/soapenv:Envelope/soapenv:Body/api:Blah)<-59");
        assertEquals(fx.getCountComparison(), ComparisonOperator.LT);
        assertEquals(fx.getCountValue(), new Integer(-59));
    }

    @Test
    public void testNegGtCount() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "-59>count(/soapenv:Envelope/soapenv:Body/api:Blah)");
        assertEquals(fx.getCountComparison(), ComparisonOperator.LT);
        assertEquals(fx.getCountValue(), new Integer(-59));
    }

    @Test
    public void testPosLeCount() throws Exception {
        FastXpath fx = TarariXpathConverter.convertToFastXpath(nsmap(), "  53   <=count(/soapenv:Envelope/soapenv:Body/api:Blah)");
        assertEquals(fx.getCountComparison(), ComparisonOperator.GE);
        assertEquals(fx.getCountValue(), new Integer(53));
    }
}
