package com.l7tech.server.util;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import org.junit.Test;

import java.util.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@SuppressWarnings({"JavaDoc"})
public class ContextVariableUtilsTest {

    @BugNumber(11642)
    @Test
    public void testExpressionsSupported() throws Exception {
        final String expression = "${myDest}gov:icam:bae:v2:1:7000:0000";
        final Map<String, Object> serverVariables = new HashMap<String, Object>();
        serverVariables.put("mydest", "urn:idmanagement.");

        final TestAudit auditor = new TestAudit();
        final List<String> resolvedStrings = ContextVariableUtils.getAllResolvedStrings(expression,
                serverVariables,
                auditor,
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
                    @Override
                    public void call(Object o) {
                        fail("Unexpected");
                    }
                });

        assertEquals(1, resolvedStrings.size());
        assertEquals("urn:idmanagement.gov:icam:bae:v2:1:7000:0000", resolvedStrings.get(0));
    }

    @Test
    public void testTextOnly() throws Exception {
        final String expression = "gov:icam:bae:v2:1:7000:0000";
        final Map<String, Object> serverVariables = new HashMap<String, Object>();

        final TestAudit auditor = new TestAudit();
        final List<String> resolvedStrings = ContextVariableUtils.getAllResolvedStrings(expression,
                serverVariables,
                auditor,
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
                    @Override
                    public void call(Object o) {
                        fail("Unexpected");
                    }
                });

        assertEquals(1, resolvedStrings.size());
        assertEquals("gov:icam:bae:v2:1:7000:0000", resolvedStrings.get(0));
    }

    /**
     * Tests that expression, text values, variable references with space separated values and multi valued references
     * are processed as expected.
     */
    @Test
    public void testAllSupportedInputMethods() throws Exception {

        String expression = "${myDest}gov:icam:bae:v2:1:7000:0000 http://donal.com ${output} ${multivar}";

        final Map<String, Object> serverVariables = new HashMap<String, Object>();
        // variable with space separated values
        serverVariables.put("output", "http://one.com http://two.com http://three.com");
        // multi var reference
        serverVariables.put("multivar", new ArrayList<String>(Arrays.asList("http://input1.com", "http://input2.com", "http://input3.com")));
        // var with single value
        serverVariables.put("mydest", "urn:idmanagement.");


        final TestAudit auditor = new TestAudit();
        final List<String> resolvedStrings = ContextVariableUtils.getAllResolvedStrings(expression,
                serverVariables,
                auditor,
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
                    @Override
                    public void call(Object o) {
                        fail("Unexpected");
                    }
                });

        for (String s : auditor) {
            System.out.println(s);
        }
        // urn:idmanagement.gov:icam:bae:v2:1:7000:0000, http://donal.com, http://one.com, http://two.com, http://three.com, http://input1.com, http://input2.com, http://input3.com

        assertEquals(8, resolvedStrings.size());
        assertEquals("urn:idmanagement.gov:icam:bae:v2:1:7000:0000", resolvedStrings.get(0));
        assertEquals("http://donal.com", resolvedStrings.get(1));
        assertEquals("http://one.com", resolvedStrings.get(2));
        assertEquals("http://two.com", resolvedStrings.get(3));
        assertEquals("http://three.com", resolvedStrings.get(4));
        assertEquals("http://input1.com", resolvedStrings.get(5));
        assertEquals("http://input2.com", resolvedStrings.get(6));
        assertEquals("http://input3.com", resolvedStrings.get(7));
    }

    @Test
    public void testEmptyStringsIgnored() throws Exception {
        String expression = "";
        List<String> stringList = ContextVariableUtils.getStringsFromList(new ArrayList<Object>(Arrays.asList(expression)),
                TextUtils.URI_STRING_SPLIT_PATTERN,
                new Functions.UnaryVoid<Object>() {
            @Override
            public void call(Object o) {
                fail("unexpected");
            }
        });

        assertEquals("No results expected", 0, stringList.size());
        stringList = ContextVariableUtils.getStringsFromList(new ArrayList<Object>(Arrays.asList(expression)), null, null);
        assertEquals("No results expected", 0, stringList.size());
    }

    @Test
    public void testObjectIgnored() throws Exception {
        String expression = "";
        final boolean[] found = new boolean[1];
        ContextVariableUtils.getStringsFromList(new ArrayList<Object>(Arrays.asList(expression, new Object())),
                null,
                new Functions.UnaryVoid<Object>() {
            @Override
            public void call(Object o) {
                found[0] = true;
            }
        });

        assertTrue("Callback should have been called", found[0]);
    }
}
