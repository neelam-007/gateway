package com.l7tech.service;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.FileReader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Class WsdlTest tests the {@link com.l7tech.service.Wsdl}
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class WsdlTest extends TestCase {
    // this relatinve path from $SRC_ROOT
    private final String WSDL = "tests/com/l7tech/service/StockQuoteService.wsdl";

    /**
     * test <code>AbstractLocatorTest</code> constructor
     */
    public WsdlTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * AbstractLocatorTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(WsdlTest.class);
        return suite;
    }

    public void setUp() throws Exception {
        // put set up code here
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Read the well formed WSDL using StringReader.
     * @throws Exception on tesat errors
     */
    public void testReadWsdlFromString() throws Exception {
        FileReader fr = new FileReader(WSDL);
        StringWriter sw = new StringWriter();
        char[] buf = new char[500];
        int len = 0;
        while ((len = fr.read(buf)) != -1) {
            sw.write(buf, 0, len);
        }

        Wsdl wsdl =
                Wsdl.newInstance(null, new StringReader(sw.toString()));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
    }


    /**
     * Read the well fromed WSDL using FileReader.
     * @throws Exception on tesat errors
     */
    public void testReadWsdlFromFile() throws Exception {
        Wsdl wsdl =
                Wsdl.newInstance(null, new FileReader(WSDL));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
    }

    /**
     * Test <code>AbstractLocatorTest</code> main.
     */
    public static void main(String[] args) throws
            Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
