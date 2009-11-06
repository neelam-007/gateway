package com.l7tech.wsdl;

import com.l7tech.common.TestDocuments;

import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.Binding;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

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
    public static final String WSDL_DOC_STYLE = TestDocuments.WSDL_DOC_LITERAL;

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
     * @throws Exception on tesat errors
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
        wsdl.getTypes();
        wsdl.getBindings();
        wsdl.getMessages();
        wsdl.getPortTypes();
        wsdl.getServices();
        assertEquals( "Hash", "idiLlFDBpP2sEljl54VX1A==", wsdl.getHash() );
    }

    /**
     * Read the well fromed WSDL using FileReader.
     *
     * @throws Exception on tesat errors
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
        assertEquals( "Hash", "FOs/wNJqudFH9j/UwKunyA==", wsdl.getHash() );
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
        assertEquals( "Hash", "5Jrp/fpHC51BLRdNwWoGlw==", wsdl.getHash() );
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
     * @throws Exception on tesat errors
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
     * @throws Exception on tesat errors
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
     * @throws Exception on tesat errors
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
     * @throws Exception on tesat errors
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
        assertEquals( "Hash", "NmjaO9VQRVqjSMCuO/oI6Q==", wsdl.getHash() );
    }

    /**
     * Test unsupported wsdl with mixed use (encoded and literal).
     *
     * @throws Exception on tesat errors
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
        assertEquals( "Hash", "YV+dlG3GNfaptkLbMiur1Q==", wsdl.getHash() );
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
        InputStream inA = WsdlTest.class.getClassLoader().getResourceAsStream(resourceToReadA);
        String contentA = readContentFromInputStream(inA);
        String baseUriA =  resourceToReadA.replace(TestDocuments.DIR, "");
        urisToResources.put(baseUriA, contentA);

        // Resource B
        String resourceToReadB = TestDocuments.BUG6944_WSDLS_WITH_CIRCULAR_IMPORTS_B_IMPORTS_A;
        InputStream inB = WsdlTest.class.getClassLoader().getResourceAsStream(resourceToReadB);
        String contentB = readContentFromInputStream(inB);
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

        assertEquals( "Hash", "ylC488wFxrJz8/LTtJxHwg==", wsdlA.getHash() );
    }

    private String readContentFromInputStream(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }
}
