package com.l7tech.wsdl;

import com.l7tech.common.TestDocuments;

import javax.wsdl.BindingOperation;
import javax.wsdl.Operation;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.Binding;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

import com.l7tech.test.BugNumber;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Class WsdlTest tests the {@link Wsdl}
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class WsdlTest {
    private static final Logger log = Logger.getLogger(WsdlTest.class.getName());
    public static final String WSDL = TestDocuments.WSDL;
    public static final String WSDL2PORTS = TestDocuments.WSDL2PORTS;
    public static final String WSDL2SERVICES = TestDocuments.WSDL2SERVICES;

    public Reader getWsdlReader(String resourcetoread) {
        if (resourcetoread == null) {
            resourcetoread = WSDL;
        }
        InputStream i = WsdlTest.class.getClassLoader().getResourceAsStream(resourcetoread);
        return new InputStreamReader(i);
    }

    /**
     * Read the well formed WSDL using StringReader.
     *
     * @throws Exception on test errors
     */
    @Test
    public void testReadWsdlFromString() throws Exception {
        Reader fr = getWsdlReader(WSDL);
        StringWriter sw = new StringWriter();
        char[] buf = new char[500];
        int len;
        while ((len = fr.read(buf)) != -1) {
            sw.write(buf, 0, len);
        }

        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(sw.toString()));
        assertTrue("Type != 0", wsdl.getTypes().size() == 0);
        assertTrue("Bindings != 1", wsdl.getBindings().size() == 1);
        assertTrue("Messages != 2", wsdl.getMessages().size() == 2);
        assertTrue("Port Types != 1", wsdl.getPortTypes().size() == 1);
        assertTrue("Service != 1", wsdl.getServices().size() == 1);
    }

    /**
     * Read the well formed WSDL using FileReader.
     *
     * @throws Exception on test errors
     */
    @Test
    public void testReadWsdlFromFile() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(WSDL));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
    }

    @Test
    public void testReadWsdl2PortsFromFile() throws Exception {
        log.info("Enter testReadWsdl2PortsFromFile");
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(WSDL2PORTS));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
        wsdl.getSoapPort();

        assertTrue("Type != 0", wsdl.getTypes().size() == 0);
        assertTrue("Bindings != 1", wsdl.getBindings().size() == 1);
        assertTrue("Messages != 4", wsdl.getMessages().size() == 4);
        assertTrue("Port Types != 1", wsdl.getPortTypes().size() == 1);
        assertTrue("Service != 1", wsdl.getServices().size() == 1);
        assertTrue("SOAP port name does not match", wsdl.getSoapPort().getName().equals("GetQuoteKira"));
    }

    @Test
    public void testReadWsdl2ServicesFromFile() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(WSDL2SERVICES));
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
        wsdl.getSoapPort();

        assertTrue("Type != 0", wsdl.getTypes().size() == 0);
        assertTrue("Bindings != 2", wsdl.getBindings().size() == 2);
        assertTrue("Messages != 4", wsdl.getMessages().size() == 4);
        assertTrue("Port Types != 2", wsdl.getPortTypes().size() == 2);
        assertTrue("Service != 2", wsdl.getServices().size() == 2);
        assertTrue("SOAP port name does not match", wsdl.getSoapPort().getName().equals("GetQuoteKira"));
    }

    @Test
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
     * Test determine the Soap Encoded binding
     *
     * @throws Exception on test errors
     */
    @Test
    public void testDetermineSoapEncodedBinding() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(WSDL));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
        Collection bindings = wsdl.getBindings();
        for ( final Object binding1 : bindings ) {
            Binding binding = (Binding) binding1;
            assertEquals( Wsdl.USE_ENCODED, wsdl.getSoapUse( binding ) );
        }
    }

    /**
     * Test determine the Soap Literal binding
     *
     * @throws Exception on test errors
     */
    @Test
    public void testDetermineSoapDocLiteralBinding() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(TestDocuments.WSDL_DOC_LITERAL));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
        Collection bindings = wsdl.getBindings();
        for ( final Object binding1 : bindings ) {
            Binding binding = (Binding) binding1;
            assertEquals( Wsdl.USE_LITERAL, wsdl.getSoapUse( binding ) );
        }
    }

    /**
     * Test determine the Soap Literal binding from rpc-literla service
     *
     * @throws Exception on test errors
     */
    @Test
    public void testDetermineSoapRpcLiteralBinding() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(TestDocuments.WSDL_RPC_LITERAL));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
        Collection bindings = wsdl.getBindings();
        for ( final Object binding1 : bindings ) {
            Binding binding = (Binding) binding1;
            assertEquals( Wsdl.USE_LITERAL, wsdl.getSoapUse( binding ) );
        }
    }


    /**
     * Test non soap binding throws. Uses the .NET style wsdl that describes http
     * get/post bindings and that is not supported.
     *
     * @throws Exception on test errors
     */
    @Test
    public void testNonSoapBindingThrows() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(TestDocuments.WSDL_DOC_LITERAL));
        wsdl.setShowBindings(Wsdl.HTTP_BINDINGS);
        Collection bindings = wsdl.getBindings();
        for ( final Object binding1 : bindings ) {
            Binding binding = (Binding) binding1;
            try {
                wsdl.getSoapUse( binding );
                fail( "IllegalArgumentException should have been thrown" );
            } catch (IllegalArgumentException e) {
                //
            }
        }
    }

    /**
     * Test unsupported wsdl with mixed use (encoded and literal).
     *
     * @throws Exception on test errors
     */
    @Test
    public void testInvalidWsdlMixedSoapBindingUse() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader(TestDocuments.WSDL_STOCK_QUOTE_INVALID_USE));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
        Collection bindings = wsdl.getBindings();
        for ( final Object binding1 : bindings ) {
            Binding binding = (Binding) binding1;
            try {
                wsdl.getSoapUse( binding );
                fail( "WSDLException should have been thrown" );
            } catch (WSDLException e) {
                //
            }
        }
    }

    /**
     * Test WSDLs with circular imports.
     * 
     * @throws Exception on test at errors
     */
    @Test
    public void testWsdlsWithCircularImports() throws Exception {
        // Step 1: load the resources from the classpath and put them in a CachedDocumentResolver with appropriate urls
        Map<String, String> urisToResources = new HashMap<String, String>();

        // Resource A
        String resourceToReadA = TestDocuments.BUG6944_WSDLS_WITH_CIRCULAR_IMPORTS_A_IMPORTS_B;
        String contentA = TestDocuments.getTestDocumentAsXml(resourceToReadA);
        String baseUriA =  resourceToReadA.replace(TestDocuments.DIR, "");
        urisToResources.put(baseUriA, contentA);

        // Resource B
        String resourceToReadB = TestDocuments.BUG6944_WSDLS_WITH_CIRCULAR_IMPORTS_B_IMPORTS_A;
        String contentB = TestDocuments.getTestDocumentAsXml( resourceToReadB );
        String baseUriB =  resourceToReadB.replace(TestDocuments.DIR, "");
        urisToResources.put(baseUriB, contentB);

        // Step 2: Create two WSDLs
        // WSDL A
        Wsdl wsdlA = Wsdl.newInstance(Wsdl.getWSDLFactory(false), Wsdl.getWSDLLocator(baseUriA, urisToResources, log));
        wsdlA.setShowBindings(Wsdl.SOAP_BINDINGS);

        // WSDL B
        Wsdl wsdlB = Wsdl.newInstance(Wsdl.getWSDLFactory(false), Wsdl.getWSDLLocator(baseUriB, urisToResources, log));
        wsdlB.setShowBindings(Wsdl.SOAP_BINDINGS);

        // Step 3: Test WSDLs with circular imports
        try {
            wsdlA.getTypes();
            wsdlA.getMessages();
            wsdlA.getPortTypes();
            wsdlA.getBindings();
            wsdlA.getServices();

            wsdlB.getTypes();
            wsdlB.getMessages();
            wsdlB.getPortTypes();
            wsdlB.getBindings();
            wsdlB.getServices();
        } catch (StackOverflowError err) {
            fail("WSDLs with circular imports has been handled, so Stack Overflow Error should not happen here.");
        }
    }

    @Test
    @BugNumber(9848)
    public void testCircularWcf() throws Exception {
        final Map<String, String> urisToResources = new HashMap<String, String>();
        urisToResources.put("http://localhost/wsdl1.wsdl", TestDocuments.getTestDocumentAsXml( TestDocuments.BUG9848_CIRCULAR_WCF_WSDL1 ));
        urisToResources.put("http://localhost/wsdl2.wsdl", TestDocuments.getTestDocumentAsXml( TestDocuments.BUG9848_CIRCULAR_WCF_WSDL2 ));
        final Wsdl wsdl = Wsdl.newInstance(Wsdl.getWSDLFactory(false), Wsdl.getWSDLLocator("http://localhost/wsdl1.wsdl", urisToResources, log));

        for ( final Binding binding : wsdl.getBindings() ) {
            assertFalse( "Binding operations not found", binding.getBindingOperations().isEmpty() );

            for ( final BindingOperation bindingOperation : (List<BindingOperation>) binding.getBindingOperations() ) {
                assertNotNull( "Binding operation input not found", bindingOperation.getBindingInput() );
                assertNotNull( "Binding operation output not found", bindingOperation.getBindingOutput() );
            }

            assertNotNull( "Bindng port type not found", binding.getPortType() );
            assertFalse( "Binding port type operations not found", binding.getPortType().getOperations().isEmpty() );

            for ( final Operation operation : (List<Operation>) binding.getPortType().getOperations() ) {
                assertNotNull( "Operation input not found", operation.getInput() );
                assertNotNull( "Operation output not found", operation.getOutput() );

                // Messages were null prior to the bug fix
                assertNotNull( "Operation input message not found", operation.getInput().getMessage() );
                assertNotNull( "Operation output message not found", operation.getOutput().getMessage() );
            }
        }
    }
}
