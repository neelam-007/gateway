package com.l7tech.console.policy.exporter;

import com.l7tech.util.DomUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Test class for the policy exporter.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 16, 2004<br/>
 * $Id$<br/>
 */
public class PolicyExporterTest extends TestCase {

    public Document testExportToDocument() throws Exception  {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        PolicyExporter exporter = new PolicyExporter();
        Assertion testPolicy = createTestPolicy();
        Document resultingExport = exporter.exportToDocument(testPolicy);
        // visual inspection for now:
        System.out.println( XmlUtil.nodeToFormattedString(resultingExport));
        return resultingExport;
    }

    public void testParseReferences() throws Exception {
        Document exportedPolicy = testExportToDocument();
        Element referencesEl = DomUtils.findOnlyOneChildElementByName(exportedPolicy.getDocumentElement(),
                                                                     ExporterConstants.EXPORTED_POL_NS,
                                                                     ExporterConstants.EXPORTED_REFERENCES_ELNAME);
        ExternalReference[] refs = ExternalReference.parseReferences(referencesEl);
        for (int i = 0; i < refs.length; i++) {
            ExternalReference ref = refs[i];
            System.out.println("Found ref of type " + ref.getClass().getName());
        }
    }

    private Assertion createTestPolicy() {
        AllAssertion root = new AllAssertion();
        SpecificUser suass = new SpecificUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, "john", null, null);
        root.addChild(suass);
        JmsRoutingAssertion jrass = new JmsRoutingAssertion();
        jrass.setEndpointName("blah");
        jrass.setEndpointOid(new Long(25));
        jrass.setResponseTimeout(55);
        root.addChild(jrass);
        // todo, plug in a real custom assertion, the WSPWriter cannot handle this garbage
        CustomAssertionHolder cahass = new CustomAssertionHolder();
        cahass.setCategory(Category.MESSAGE);
        cahass.setCustomAssertion(new CustomAssertion() {
            public String getName() {
                return "Some Custom Assertion Lame name";
            }
        });
        //root.getChildren().add(cahass);
        return root;
    }

    public static Test suite() {
        return new TestSuite(PolicyExporterTest.class);
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        junit.textui.TestRunner.run(suite());
    }
}
