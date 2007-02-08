package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.wsp.TypeMapping;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Test for Assertion#meta() and DefaultAssertionMetadata.
 */
public class AssertionMetadataTest extends TestCase {
    private static final Logger log = Logger.getLogger(AssertionMetadataTest.class.getName());

    static {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    public AssertionMetadataTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(AssertionMetadataTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testDefaults() throws Exception {
        Assertion ass = new UncustomizedMetadataAssertion();
        AssertionMetadata am = ass.meta();
        assertNotNull(am);

        assertEquals("UncustomizedMetadata", am.get(AssertionMetadata.BASE_NAME));
        assertEquals("Uncustomized Metadata Assertion", am.get(AssertionMetadata.LONG_NAME));
        assertEquals("Uncustomized Metadata", am.get(AssertionMetadata.SHORT_NAME));
        assertEquals(Boolean.FALSE, am.get(AssertionMetadata.USED_BY_CLIENT));
        assertEquals("com.l7tech.policy.wsp.UncustomizedMetadataAssertionMapping", am.get(AssertionMetadata.WSP_TYPE_MAPPING_CLASSNAME));
        assertEquals("com.l7tech.policy.wsp.AssertionMapping", am.get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE).getClass().getName());
        assertEquals("com.l7tech.console.action.UncustomizedMetadataPropertiesAction", am.get(AssertionMetadata.PROPERTIES_ACTION_CLASSNAME));
        assertEquals("UncustomizedMetadata", am.get(AssertionMetadata.WSP_EXTERNAL_NAME));
        log.info("Generated defaults test OK");
    }

    public void testMemoization() throws Exception {
        // Ensures that caching works
        testDefaults();
        AssertionMetadata am2 = new UncustomizedMetadataAssertion().meta();
        TypeMapping tm2 = (TypeMapping)am2.get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE);
        testDefaults();
        AssertionMetadata am4 = new UncustomizedMetadataAssertion().meta();
        TypeMapping tm4 = (TypeMapping)am4.get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE);
        testDefaults();
        AssertionMetadata am6 = new UncustomizedMetadataAssertion().meta();
        TypeMapping tm6 = (TypeMapping)am6.get(AssertionMetadata.WSP_TYPE_MAPPING_INSTANCE);
        assertTrue(am2 == am4 && am4 == am6);
        assertTrue(tm2 == tm4 && tm4 == tm6);
    }
    
    public void testDyanmicWsp() throws Exception {
        WspWriter.getPolicyXml(new UncustomizedMetadataAssertion());

        Assertion root = new AllAssertion(Arrays.asList(new OneOrMoreAssertion(Arrays.asList(new UncustomizedMetadataAssertion()))));

        // Writer should Just Work, since the assertion knows its own external name and will use the default mapping
        String policyXml = WspWriter.getPolicyXml(root);

        // Strict reader should fail, since we haven't told it about the new assertion
        try {
            new WspReader(null).parseStrictly(policyXml);
            fail("Expected exception not caught");
        } catch (InvalidPolicyStreamException e) {
            // Ok
        }
        
        // Strict reader should succeed once it's hooked up to an AssertionRegistry that knows about our new assertion
        final AssertionRegistry reg = new AssertionRegistry();
        reg.registerAssertion(UncustomizedMetadataAssertion.class);
        Assertion out = new WspReader(reg).parseStrictly(policyXml);
        assertEquals(UncustomizedMetadataAssertion.class.getName(), firstKid(firstKid(out)).getClass().getName());
    }

    private Assertion firstKid(Assertion parent) {
        if (parent instanceof CompositeAssertion) {
            CompositeAssertion comp = (CompositeAssertion)parent;
            return (Assertion)comp.getChildren().get(0);
        }
        throw new IllegalArgumentException("not a CompositeAssertion: " + parent.getClass().getName());
    }
}
