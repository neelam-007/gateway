package com.l7tech.service;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Class WsdlTest tests the {@link com.l7tech.common.xml.Wsdl}
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class WsdlTest extends TestCase {
    private static final Logger log = Logger.getLogger(WsdlTest.class.getName());
    public static final String WSDL = TestDocuments.WSDL;
    public static final String WSDL2PORTS = TestDocuments.WSDL2PORTS;
    public static final String WSDL2SERVICES = TestDocuments.WSDL2SERVICES;
    public static final String WSDL_DOC_STYLE = TestDocuments.WSDL_DOC_LITERAL;

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

    public Reader getWsdlReader(String resourcetoread) {
        if (resourcetoread == null) {
            resourcetoread = WSDL;
        }
        ClassLoader cl = getClass().getClassLoader();
        InputStream i = cl.getResourceAsStream(resourcetoread);
        InputStreamReader r = new InputStreamReader(i);
        return r;
    }

    /**
     * Read the well formed WSDL using StringReader.
     *
     * @throws Exception on tesat errors
     */
    public void testReadWsdlFromString() throws Exception {
        Reader fr = getWsdlReader(WSDL);
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
     *
     * @throws Exception on tesat errors
     */
    public void testReadWsdlFromFile() throws Exception {
        Wsdl wsdl =
          Wsdl.newInstance(null, getWsdlReader(WSDL));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
    }

    public void testReadWsdl2PortsFromFile() throws Exception {
        log.info("Enter testReadWsdl2PortsFromFile");
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(WSDL2PORTS));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
        wsdl.getSoapPort();
    }

    public void testReadWsdl2ServicesFromFile() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(WSDL2SERVICES));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
        wsdl.getSoapPort();
    }

    public void testGetAndSetPortUrl() throws FileNotFoundException, WSDLException, MalformedURLException {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(WSDL));
        Port port = wsdl.getSoapPort();
        URL url = new URL(wsdl.getUriFromPort(port));
        log.info("Read port URL: " + url);
        wsdl.setPortUrl(port, new URL("http://blee.blah.baz:9823/foo/bar/baz?whoomp=foomp&feez=gleez&atlue=42"));
        log.info("Changed port URL");
        port = wsdl.getSoapPort();
        url = new URL(wsdl.getUriFromPort(port));
        log.info("Read back port URL: " + url);
    }

    /**
     * Test <code>AbstractLocatorTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
