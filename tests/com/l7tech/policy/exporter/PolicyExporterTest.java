package com.l7tech.policy.exporter;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;

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

    public void testExportToDocument() throws Exception  {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        PolicyExporter exporter = new PolicyExporter();
        Assertion testPolicy = createTestPolicy();
        Document resultingExport = exporter.exportToDocument(testPolicy);
        // visual inspection for now:
        System.out.println(XmlUtil.nodeToFormattedString(resultingExport));
    }

    private Assertion createTestPolicy() {
        AllAssertion root = new AllAssertion();
        SpecificUser suass = new SpecificUser(125, "john");
        root.getChildren().add(suass);
        // todo, more
        return root;
    }

    public static Test suite() {
        return new TestSuite(PolicyExporterTest.class);
    }

    public static void main(String[] args) {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        junit.textui.TestRunner.run(suite());
    }
}
