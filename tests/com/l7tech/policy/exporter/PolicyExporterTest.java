package com.l7tech.policy.exporter;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.policy.assertion.Assertion;
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

    public void exportToDocumentTest() throws Exception  {
        PolicyExporter exporter = new PolicyExporter();
        Assertion testPolicy = createTestPolicy();
        Document resultingExport = exporter.exportToDocument(testPolicy);
        // visual inspection for now:
        System.out.println(XmlUtil.nodeToFormattedString(resultingExport));
    }

    private Assertion createTestPolicy() {
        AllAssertion root = new AllAssertion();
        // todo, more
        return root;
    }

    public static Test suite() {
        return new TestSuite(PolicyExporterTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}
