package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests of SQL Attack Protection Assertion
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class CodeInjectionProtectionAssertionTest {

    private static final String CURRENT_POLICY_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:CodeInjectionProtectionAssertion>\n" +
            "        <L7p:IncludeBody booleanValue=\"true\"/>\n" +
            "        <L7p:IncludeUrlPath booleanValue=\"true\"/>\n" +
            "        <L7p:IncludeUrlQueryString booleanValue=\"true\"/>\n" +
            "        <L7p:Protections codeInjectionProtectionTypeArray=\"included\">\n" +
            "            <L7p:item protectionType=\"htmlJavaScriptInjection\"/>\n" +
            "            <L7p:item protectionType=\"phpEvalInjection\"/>\n" +
            "            <L7p:item protectionType=\"shellInjection\"/>\n" +
            "            <L7p:item protectionType=\"ldapDnInjection\"/>\n" +
            "            <L7p:item protectionType=\"ldapSearchInjection\"/>\n" +
            "            <L7p:item protectionType=\"xpathInjection\"/>\n" +
            "        </L7p:Protections>\n" +
            "        <L7p:Target target=\"REQUEST\"/>\n" +
            "    </L7p:CodeInjectionProtectionAssertion>\n" +
            "</wsp:Policy>\n";

    // Serialization

    /**
     * Read current (Icefish) policy as xml to create assertion, then create xml from the assertion and
     * ensure it is identical
     */
    @Test
    public void testSerialization() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(CodeInjectionProtectionAssertion.class);

        WspReader wspReader = new WspReader(registry);

        CodeInjectionProtectionAssertion assertion = (CodeInjectionProtectionAssertion)
                wspReader.parseStrictly(CURRENT_POLICY_XML, WspReader.INCLUDE_DISABLED);

        assertEquals(CURRENT_POLICY_XML, WspWriter.getPolicyXml(assertion));
    }

    @Test
    public void testSerialization_PreMessageTargetablePolicy_IncludeRequestBody() throws Exception {
        String oldPolicy =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <L7p:CodeInjectionProtectionAssertion>\n" +
                "        <L7p:IncludeRequestBody booleanValue=\"true\"/>\n" +
                "        <L7p:Protections codeInjectionProtectionTypeArray=\"included\">\n" +
                "            <L7p:item protectionType=\"htmlJavaScriptInjection\"/>\n" +
                "            <L7p:item protectionType=\"phpEvalInjection\"/>\n" +
                "            <L7p:item protectionType=\"shellInjection\"/>\n" +
                "            <L7p:item protectionType=\"ldapDnInjection\"/>\n" +
                "            <L7p:item protectionType=\"ldapSearchInjection\"/>\n" +
                "            <L7p:item protectionType=\"xpathInjection\"/>\n" +
                "        </L7p:Protections>\n" +
                "    </L7p:CodeInjectionProtectionAssertion>\n" +
                "</wsp:Policy>";

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(CodeInjectionProtectionAssertion.class);

        WspReader wspReader = new WspReader(registry);

        CodeInjectionProtectionAssertion assertion = (CodeInjectionProtectionAssertion)
                wspReader.parseStrictly(oldPolicy, WspReader.INCLUDE_DISABLED);

        assertTrue(assertion.isIncludeBody());
        assertFalse(assertion.isIncludeUrlPath());
        assertFalse(assertion.isIncludeUrlQueryString());

        assertEquals(TargetMessageType.REQUEST, assertion.getTarget());
    }

    @Test
    public void testSerialization_PreMessageTargetablePolicy_IncludeRequestUrl() throws Exception {
        String oldPolicy =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <L7p:CodeInjectionProtectionAssertion>\n" +
                "        <L7p:IncludeRequestUrl booleanValue=\"true\"/>\n" +
                "        <L7p:Protections codeInjectionProtectionTypeArray=\"included\">\n" +
                "            <L7p:item protectionType=\"htmlJavaScriptInjection\"/>\n" +
                "            <L7p:item protectionType=\"phpEvalInjection\"/>\n" +
                "            <L7p:item protectionType=\"shellInjection\"/>\n" +
                "            <L7p:item protectionType=\"ldapDnInjection\"/>\n" +
                "            <L7p:item protectionType=\"ldapSearchInjection\"/>\n" +
                "            <L7p:item protectionType=\"xpathInjection\"/>\n" +
                "        </L7p:Protections>\n" +
                "    </L7p:CodeInjectionProtectionAssertion>\n" +
                "</wsp:Policy>";

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(CodeInjectionProtectionAssertion.class);

        WspReader wspReader = new WspReader(registry);

        CodeInjectionProtectionAssertion assertion = (CodeInjectionProtectionAssertion)
                wspReader.parseStrictly(oldPolicy, WspReader.INCLUDE_DISABLED);

        assertFalse(assertion.isIncludeBody());
        assertFalse(assertion.isIncludeUrlPath());
        assertTrue(assertion.isIncludeUrlQueryString());

        assertEquals(TargetMessageType.REQUEST, assertion.getTarget());
    }

    @Test
    public void testSerialization_PreMessageTargetablePolicy_IncludeResponseBody() throws Exception {
        String oldPolicy =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <L7p:CodeInjectionProtectionAssertion>\n" +
                "        <L7p:IncludeResponseBody booleanValue=\"true\"/>\n" +
                "        <L7p:Protections codeInjectionProtectionTypeArray=\"included\">\n" +
                "            <L7p:item protectionType=\"htmlJavaScriptInjection\"/>\n" +
                "            <L7p:item protectionType=\"phpEvalInjection\"/>\n" +
                "            <L7p:item protectionType=\"shellInjection\"/>\n" +
                "            <L7p:item protectionType=\"ldapDnInjection\"/>\n" +
                "            <L7p:item protectionType=\"ldapSearchInjection\"/>\n" +
                "            <L7p:item protectionType=\"xpathInjection\"/>\n" +
                "        </L7p:Protections>\n" +
                "    </L7p:CodeInjectionProtectionAssertion>\n" +
                "</wsp:Policy>";

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(CodeInjectionProtectionAssertion.class);

        WspReader wspReader = new WspReader(registry);

        CodeInjectionProtectionAssertion assertion = (CodeInjectionProtectionAssertion)
                wspReader.parseStrictly(oldPolicy, WspReader.INCLUDE_DISABLED);

        assertTrue(assertion.isIncludeBody());
        assertFalse(assertion.isIncludeUrlPath());
        assertFalse(assertion.isIncludeUrlQueryString());

        assertEquals(TargetMessageType.RESPONSE, assertion.getTarget());
    }
}
