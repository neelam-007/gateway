package com.l7tech.external.assertions.ldapquery;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import static org.junit.Assert.*;

import com.l7tech.util.GoidUpgradeMapper;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class LDAPQueryAssertionWspTest {
    @Before
    public void setUp() {
        AssertionRegistry assreg = new AssertionRegistry();
        assreg.registerAssertion(LDAPQueryAssertion.class);
        WspConstants.setTypeMappingFinder(assreg);
    }

    @Test
    public void testWspExternalName() throws Exception {
        assertEquals("LDAPQuery", new LDAPQueryAssertion().meta().get(AssertionMetadata.WSP_EXTERNAL_NAME));
    }

    @Test
    public void testSerialization() throws Exception {
        LDAPQueryAssertion ass = new LDAPQueryAssertion();
        ass.setQueryMappings(mappings());
        ass.setEnableCache(true);
        ass.setLdapProviderOid(new Goid(0,443));
        ass.setSearchFilter("blarg");
        ass.setCachePeriod(49384);
        ass.setFailIfNoResults(true);
        String xml = WspWriter.getPolicyXml(ass);
        assertTrue(xml.contains("false_true_variable"));
    }

    @Test
    public void testWspRoundTrip() throws Exception {
        LDAPQueryAssertion ass = new LDAPQueryAssertion();
        ass.setQueryMappings(mappings());
        ass.setEnableCache(true);
        ass.setLdapProviderOid(new Goid(0,353));
        ass.setSearchFilter("qwfasdjfkh");
        ass.setCachePeriod(66633);
        ass.setFailIfNoResults(true);

        String xml = WspWriter.getPolicyXml(ass);
        LDAPQueryAssertion ass2 = (LDAPQueryAssertion) WspReader.getDefault().parseStrictly(xml, WspReader.INCLUDE_DISABLED);

        assertEquals(ass.isEnableCache(), ass2.isEnableCache());
        assertEquals(ass.isFailIfNoResults(), ass2.isFailIfNoResults());
        assertEquals(ass.getCachePeriod(), ass2.getCachePeriod());
        assertEquals(ass.getLdapProviderOid(), ass2.getLdapProviderOid());
        assertEquals(ass.getSearchFilter(), ass2.getSearchFilter());

        for (int i = 0; i < ass.getQueryMappings().length; i++) {
            QueryAttributeMapping q1 = ass.getQueryMappings()[i];
            QueryAttributeMapping q2 = ass2.getQueryMappings()[i];
            assertEquals(q1.getAttributeName(), q2.getAttributeName());
            assertEquals(q1.getMatchingContextVariableName(), q2.getMatchingContextVariableName());
            assertEquals(q1.isJoinMultivalued(), q2.isJoinMultivalued());
        }
    }

    @Test
    public void testQueryMappingsAreNeverNull() throws Exception {
        LDAPQueryAssertion ass = new LDAPQueryAssertion();
        assertNotNull(ass.getQueryMappings());
        ass.setQueryMappings(null);
        assertNotNull(ass.getQueryMappings());

        String xml = WspWriter.getPolicyXml(ass);
        LDAPQueryAssertion ass2 = (LDAPQueryAssertion) WspReader.getDefault().parseStrictly(xml, WspReader.INCLUDE_DISABLED);

        assertNotNull(ass2.getQueryMappings());
    }

    private static final String OLD_PRE_466_FORMAT_ASSERTION_XML =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:LDAPQuery>\n" +
            "        <L7p:AttrNames stringArrayValue=\"included\">\n" +
            "            <L7p:item stringValue=\"false_false_attr\"/>\n" +
            "            <L7p:item stringValue=\"false_true_attr\"/>\n" +
            "            <L7p:item stringValue=\"true_false_attr\"/>\n" +
            "            <L7p:item stringValue=\"true_true_attr\"/>\n" +
            "        </L7p:AttrNames>\n" +
            "        <L7p:CachePeriod longValue=\"49384\"/>\n" +
            "        <L7p:FailIfNoResults booleanValue=\"true\"/>\n" +
            "        <L7p:LdapProviderOid longValue=\"443\"/>\n" +
            "        <L7p:Multivalued bools=\"included\">\n" +
            "            <L7p:item boxedBooleanValue=\"false\"/>\n" +
            "            <L7p:item boxedBooleanValue=\"false\"/>\n" +
            "            <L7p:item boxedBooleanValue=\"true\"/>\n" +
            "            <L7p:item boxedBooleanValue=\"true\"/>\n" +
            "        </L7p:Multivalued>\n" +
            "        <L7p:SearchFilter stringValue=\"blarg\"/>\n" +
            "        <L7p:VarNames stringArrayValue=\"included\">\n" +
            "            <L7p:item stringValue=\"false_false_variable\"/>\n" +
            "            <L7p:item stringValue=\"false_true_variable\"/>\n" +
            "            <L7p:item stringValue=\"true_false_variable\"/>\n" +
            "            <L7p:item stringValue=\"true_true_variable\"/>\n" +
            "        </L7p:VarNames>\n" +
            "    </L7p:LDAPQuery>\n" +
            "</wsp:Policy>";

    @Test
    public void testCompatibilityWithPre466Assertions() throws Exception {
        LDAPQueryAssertion ass = (LDAPQueryAssertion)WspReader.getDefault().parseStrictly(OLD_PRE_466_FORMAT_ASSERTION_XML, WspReader.INCLUDE_DISABLED);
        assertEquals(49384, ass.getCachePeriod());
        assertEquals(GoidUpgradeMapper.mapOid(null,443L), ass.getLdapProviderOid());
        assertEquals("blarg", ass.getSearchFilter());
        assertEquals(true, ass.isFailIfNoResults());
        assertNotNull(ass.getQueryMappings());
        assertEquals(4, ass.getQueryMappings().length);
        assertEquals("false_false_attr", ass.getQueryMappings()[0].getAttributeName());
        assertEquals("false_true_attr", ass.getQueryMappings()[1].getAttributeName());
        assertEquals("false_true_variable", ass.getQueryMappings()[1].getMatchingContextVariableName());
        assertEquals("true_true_variable", ass.getQueryMappings()[3].getMatchingContextVariableName());
        assertEquals(true, ass.getQueryMappings()[3].isMultivalued());
        // Pre 4.6.6 did not support the QueryAttributeMapping.joinMultivalued flag
    }

    private QueryAttributeMapping[] mappings() {
        List<QueryAttributeMapping> mappings = new ArrayList<QueryAttributeMapping>();
        mappings.add(mapping(false, false));
        mappings.add(mapping(false, true));
        mappings.add(mapping(true, false));
        mappings.add(mapping(true, true));
        return mappings.toArray(new QueryAttributeMapping[mappings.size()]);
    }

    private QueryAttributeMapping mapping(boolean multi, boolean join) {
        final String name = multi + "_" + join;
        return new QueryAttributeMapping(name + "_attr", name + "_variable", join, false, multi);
    }
}
