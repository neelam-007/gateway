/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import junit.framework.Assert;
import org.junit.Test;

public class ComparisonAssertionTest {
    @Test
    public void testSerialization() throws Exception {
        AssertionRegistry assreg = new AssertionRegistry();
        assreg.registerAssertion(ComparisonAssertion.class);
        WspConstants.setTypeMappingFinder(assreg);
        Assertion ass = WspReader.getDefault().parseStrictly(POLICY_XML, WspReader.INCLUDE_DISABLED);

        String what = WspWriter.getPolicyXml(ass);
        Assert.assertEquals("what", what, POLICY_XML);
    }

    public static final String POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:ComparisonAssertion>\n" +
            "            <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
            "            <L7p:Expression1 stringValue=\"${blargle}\"/>\n" +
            "            <L7p:Operator operatorNull=\"null\"/>\n" +
            "            <L7p:Predicates predicates=\"included\">\n" +
            "                <L7p:item binary=\"included\">\n" +
            "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
            "                    <L7p:Negated booleanValue=\"true\"/>\n" +
            "                    <L7p:Operator operator=\"GT\"/>\n" +
            "                    <L7p:RightValue stringValue=\"blah\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item cardinality=\"included\">\n" +
            "                    <L7p:Max intValue=\"15\"/>\n" +
            "                    <L7p:Min intValue=\"4\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item regex=\"included\">\n" +
            "                    <L7p:Pattern stringValue=\"f.*$\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item stringLength=\"included\">\n" +
            "                    <L7p:Max intValue=\"44454\"/>\n" +
            "                    <L7p:Min intValue=\"5\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item binary=\"included\">\n" +
            "                    <L7p:Negated booleanValue=\"true\"/>\n" +
            "                    <L7p:Operator operator=\"GE\"/>\n" +
            "                    <L7p:RightValue stringValue=\"343\"/>\n" +
            "                </L7p:item>\n" +
            "            </L7p:Predicates>\n" +
            "        </L7p:ComparisonAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";
}
