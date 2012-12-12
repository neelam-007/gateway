package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.testuncust.UncustomizedMetadataAssertion;
import com.l7tech.policy.wsp.InvalidPolicyStreamException;
import com.l7tech.policy.wsp.TypeMapping;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import org.junit.Test;

import java.util.Arrays;
import java.util.logging.Logger;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static org.junit.Assert.*;

/**
 * Test for Assertion#meta() and DefaultAssertionMetadata.
 */
public class AssertionMetadataTest {
    private static final Logger log = Logger.getLogger(AssertionMetadataTest.class.getName());

    static {
        AssertionRegistry.installEnhancedMetadataDefaults();
    }

    @Test
    public void testDefaults() throws Exception {
        Assertion ass = new UncustomizedMetadataAssertion();
        AssertionMetadata am = ass.meta();
        assertNotNull(am);

        assertEquals("UncustomizedMetadata", am.get(BASE_NAME));
        assertEquals("Uncustomized Metadata", am.get(SHORT_NAME));
        assertEquals("com.l7tech.policy.testuncust", am.get(BASE_PACKAGE));
        assertEquals("Uncustomized Metadata Assertion", am.get(LONG_NAME));
        assertEquals("Uncustomized Metadata Assertion", am.get(DESCRIPTION));
        assertEquals("Uncustomized Metadata", am.get(SHORT_NAME));
        assertEquals(Boolean.FALSE, am.get(USED_BY_CLIENT));
        assertEquals("com.l7tech.policy.testuncust.wsp.UncustomizedMetadataAssertionMapping", am.get(WSP_TYPE_MAPPING_CLASSNAME));
        assertEquals("com.l7tech.policy.wsp.AssertionMapping", am.get(WSP_TYPE_MAPPING_INSTANCE).getClass().getName());
        assertEquals("com.l7tech.policy.testuncust.console.UncustomizedMetadataPropertiesAction", am.get(PROPERTIES_ACTION_CLASSNAME));
        assertEquals("com.l7tech.policy.testuncust.console.UncustomizedMetadataPaletteNode", am.get(PALETTE_NODE_CLASSNAME));
        assertEquals("com.l7tech.policy.testuncust.console.UncustomizedMetadataPropertiesDialog", am.get(PROPERTIES_EDITOR_CLASSNAME));
        assertEquals("com.l7tech.policy.testuncust.console.UncustomizedMetadataAdvice", am.get(POLICY_ADVICE_CLASSNAME));
        assertEquals("com.l7tech.policy.testuncust.console.UncustomizedMetadataPolicyNode", am.get(POLICY_NODE_CLASSNAME));
        assertEquals("UncustomizedMetadata", am.get(WSP_EXTERNAL_NAME));
        assertEquals("com.l7tech.policy.testuncust.server.ServerUncustomizedMetadataAssertion", am.get(SERVER_ASSERTION_CLASSNAME));
        assertEquals("com.l7tech.policy.testuncust.client.ClientUncustomizedMetadataAssertion", am.get(CLIENT_ASSERTION_CLASSNAME));
        assertNull(am.get(BASE_64_NODE_IMAGE));
        log.info("Generated defaults test OK");
    }

    @Test
    public void testMemoization() throws Exception {
        // Ensures that caching works
        testDefaults();
        AssertionMetadata am2 = new UncustomizedMetadataAssertion().meta();
        TypeMapping tm2 = (TypeMapping)am2.get(WSP_TYPE_MAPPING_INSTANCE);
        testDefaults();
        AssertionMetadata am4 = new UncustomizedMetadataAssertion().meta();
        TypeMapping tm4 = (TypeMapping)am4.get(WSP_TYPE_MAPPING_INSTANCE);
        testDefaults();
        AssertionMetadata am6 = new UncustomizedMetadataAssertion().meta();
        TypeMapping tm6 = (TypeMapping)am6.get(WSP_TYPE_MAPPING_INSTANCE);
        assertTrue(am2 == am4 && am4 == am6);
        assertTrue(tm2 == tm4 && tm4 == tm6);
    }

    @Test
    public void testCoreAssertionWspNames() throws Exception {

        assertEquals("com.l7tech.server.policy.assertion.identity.ServerAuthenticationAssertion",
                     new AuthenticationAssertion().meta().get(SERVER_ASSERTION_CLASSNAME));

        assertEquals("com.l7tech.server.policy.assertion.ServerTrueAssertion",
                     new TrueAssertion().meta().get(SERVER_ASSERTION_CLASSNAME));

        assertEquals("com.l7tech.proxy.policy.assertion.composite.ClientOneOrMoreAssertion",
                     new OneOrMoreAssertion().meta().get(CLIENT_ASSERTION_CLASSNAME));
    }
    
    @Test
    public void testDyanmicWsp() throws Exception {
        WspWriter.getPolicyXml(new UncustomizedMetadataAssertion());

        Assertion root = new AllAssertion(Arrays.<Assertion>asList(new OneOrMoreAssertion(Arrays.<Assertion>asList(new UncustomizedMetadataAssertion()))));

        // Writer should Just Work, since the assertion knows its own external name and will use the default mapping
        String policyXml = WspWriter.getPolicyXml(root);

        // Strict reader should fail, since we haven't told it about the new assertion
        try {
            new WspReader(null).parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
            fail("Expected exception not caught");
        } catch (InvalidPolicyStreamException e) {
            // Ok
        }
        
        // Strict reader should succeed once it's hooked up to an AssertionRegistry that knows about our new assertion
        final AssertionRegistry reg = new AssertionRegistry();
        reg.registerAssertion(UncustomizedMetadataAssertion.class);
        Assertion out = new WspReader(reg).parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertEquals(UncustomizedMetadataAssertion.class.getName(), firstKid(firstKid(out)).getClass().getName());
    }

    @Test
    public void testCompositeAssertionFolders() {
        assertEquals("com/l7tech/console/resources/folder.gif", new AllAssertion().meta().get(AssertionMetadata.POLICY_NODE_ICON));
    }

    private Assertion firstKid(Assertion parent) {
        if (parent instanceof CompositeAssertion) {
            CompositeAssertion comp = (CompositeAssertion)parent;
            return comp.getChildren().get(0);
        }
        throw new IllegalArgumentException("not a CompositeAssertion: " + parent.getClass().getName());
    }
}
