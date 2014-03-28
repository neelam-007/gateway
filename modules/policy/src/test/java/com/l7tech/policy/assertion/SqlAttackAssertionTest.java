package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.test.BugId;
import org.junit.Test;

import static com.l7tech.policy.assertion.SqlAttackAssertion.policyNameFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests of SQL Attack Protection Assertion
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class SqlAttackAssertionTest {

    // Current policy xml (Icefish)
    private static final String CURRENT_POLICY_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:SqlAttackProtection>\n" +
            "        <L7p:IncludeBody booleanValue=\"false\"/>\n" +
            "        <L7p:IncludeUrlPath booleanValue=\"true\"/>\n" +
            "        <L7p:IncludeUrlQueryString booleanValue=\"true\"/>\n" +
            "        <L7p:Protections stringSetValue=\"included\">\n" +
            "            <L7p:item stringValue=\"MsSql\"/>\n" +
            "            <L7p:item stringValue=\"SqlMetaText\"/>\n" +
            "            <L7p:item stringValue=\"OraSql\"/>\n" +
            "            <L7p:item stringValue=\"SqlMeta\"/>\n" +
            "        </L7p:Protections>\n" +
            "    </L7p:SqlAttackProtection>\n" +
            "</wsp:Policy>\n";

    // Goatfish policy xml
    private static final String GOATFISH_POLICY_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:SqlAttackProtection>\n" +
            "        <L7p:IncludeBody booleanValue=\"false\"/>\n" +
            "        <L7p:IncludeUrl booleanValue=\"true\"/>\n" +
            "        <L7p:Protections stringSetValue=\"included\">\n" +
            "            <L7p:item stringValue=\"MsSql\"/>\n" +
            "            <L7p:item stringValue=\"OraSql\"/>\n" +
            "            <L7p:item stringValue=\"SqlMeta\"/>\n" +
            "        </L7p:Protections>\n" +
            "    </L7p:SqlAttackProtection>\n" +
            "</wsp:Policy>\n";

    // Serialization

    /**
     * Read current (Icefish) policy as xml to create assertion, then create xml from the assertion and
     * ensure it is identical
     */
    @Test
    public void testSerialization() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(SqlAttackAssertion.class);

        WspReader wspReader = new WspReader(registry);

        SqlAttackAssertion assertion =
                (SqlAttackAssertion) wspReader.parseStrictly(CURRENT_POLICY_XML, WspReader.INCLUDE_DISABLED);

        assertEquals(CURRENT_POLICY_XML, WspWriter.getPolicyXml(assertion));
    }

    /**
     * Read Goatfish policy xml to create assertion, ensure the expected values have been maintained/set.
     */
    @BugId("SSG-8252")
    @Test
    public void testSerialization_UpgradeGoatfishToCurrent() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(SqlAttackAssertion.class);

        WspConstants.setTypeMappingFinder(registry);

        SqlAttackAssertion assertion = (SqlAttackAssertion) WspReader.getDefault()
                .parseStrictly(GOATFISH_POLICY_XML, WspReader.INCLUDE_DISABLED);

        assertEquals(3, assertion.getProtections().size());
        assertTrue(assertion.getProtections().contains(SqlAttackAssertion.PROT_META));
        assertTrue(assertion.getProtections().contains(SqlAttackAssertion.PROT_MSSQL));
        assertTrue(assertion.getProtections().contains(SqlAttackAssertion.PROT_ORASQL));
        assertEquals(false, assertion.isIncludeBody());
        assertEquals(false, assertion.isIncludeUrlPath());
        assertEquals(true, assertion.isIncludeUrlQueryString());
    }

    // Decoration

    /**
     * Target: REQUEST
     * Message parts: URL Path + URL Query String + Body
     */
    @BugId("SSG-8252")
    @Test
    public void testDecoration_RequestTarget_IncludeAll() throws Exception {
        SqlAttackAssertion assertion = new SqlAttackAssertion();

        assertion.setTarget(TargetMessageType.REQUEST);
        assertion.setIncludeBody(true);
        assertion.setIncludeUrlPath(true);
        assertion.setIncludeUrlQueryString(true);

        String decoratedName = policyNameFactory.getAssertionName(assertion, true);

        assertEquals("Request: Protect Against SQL Attacks [URL Path + URL Query String + Body]", decoratedName);
    }

    /**
     * Target: RESPONSE
     * Message parts: URL Path + Body
     */
    @BugId("SSG-8252")
    @Test
    public void testDecoration_ResponseTarget_IncludePathAndBody() throws Exception {
        SqlAttackAssertion assertion = new SqlAttackAssertion();

        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setIncludeBody(true);
        assertion.setIncludeUrlPath(true);
        assertion.setIncludeUrlQueryString(false);

        String decoratedName = policyNameFactory.getAssertionName(assertion, true);

        assertEquals("Response: Protect Against SQL Attacks [URL Path + Body]", decoratedName);
    }

    /**
     * Target: OTHER ("testMsgVar")
     * Message parts: URL Path + URL Query String
     */
    @BugId("SSG-8252")
    @Test
    public void testDecoration_VariableTarget_IncludeBody() throws Exception {
        SqlAttackAssertion assertion = new SqlAttackAssertion();

        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("testMsgVar");
        assertion.setIncludeBody(true);
        assertion.setIncludeUrlPath(false);
        assertion.setIncludeUrlQueryString(false);

        String decoratedName = policyNameFactory.getAssertionName(assertion, true);

        assertEquals("${testMsgVar}: Protect Against SQL Attacks [Body]", decoratedName);
    }
}
