package com.l7tech.console.xmlviewer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import org.w3c.dom.Document;
import org.dom4j.DocumentHelper;

import java.io.File;

/**
 * Class ExchangerDocumentTest.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ExchangerDocumentTest extends TestCase {
    /**
     * test <code>ExchangerDocumentTest</code> constructor
     */
    public ExchangerDocumentTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ExchangerDocumentTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(ExchangerDocumentTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testCreateEmptyViewer() throws Exception {
    }
    /**
     * Test <code>ExchangerDocumentTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
