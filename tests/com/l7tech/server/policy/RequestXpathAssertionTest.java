package com.l7tech.server.policy;

import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceCache;
import com.l7tech.service.ServiceManager;
import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.xml.sax.InputSource;

import javax.servlet.ServletOutputStream;
import javax.wsdl.Definition;
import javax.xml.soap.SOAPConstants;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestXpathAssertionTest extends TestCase {
    private MockServletApi _servletApi = MockServletApi.defaultMessageProcessingServletApi();
    private PublishedService _service;
    private SoapMessageProcessingServlet _servlet = new SoapMessageProcessingServlet();
    private ServiceCache _serviceCache;
    private MyServletOutputStream _servletOutputStream = new MyServletOutputStream();

    /**
     * test <code>RequestXpathAssertionTest</code> constructor
     */
    public RequestXpathAssertionTest(String name) throws Exception {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * RequestXpathAssertionTest <code>TestCase</code>
     */
    public static Test suite() {

        TestSuite suite = new TestSuite(RequestXpathAssertionTest.class);
        return suite;
    }

    String newXpathPolicy(String pattern, Map namespaceMap) throws IOException {
        // Set up policy with only RequestXpathAssertion
        RequestXpathAssertion rxa = new RequestXpathAssertion();
        rxa.setXpathExpression(new XpathExpression(pattern, namespaceMap));

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        WspWriter.writePolicy(rxa, baos);

        return baos.toString("UTF-8");
    }

    private class MyServletOutputStream extends ServletOutputStream {
        public MyServletOutputStream() {
            reset();
        }

        public void write(int i) throws IOException {
            _chain.write(i);
        }

        public void reset() {
            _chain = new ByteArrayOutputStream(4096);
        }

        public String toString(String encoding) throws UnsupportedEncodingException {
            return _chain.toString(encoding);
        }

        private ByteArrayOutputStream _chain;
    }

    public void testXmethods() throws Exception {
        final String XMETHODS_WSDL_PATH = "com/l7tech/service/resources/xmethods-StockQuoteService.wsdl";
        final String XMETHODS_URN = "urn:xmethods-delayed-quotes";
        final String XMETHODS_SOAPACTION = XMETHODS_URN + "#getQuote";

        InputStream xmethodsWsdlStream = getClass().getClassLoader().getResourceAsStream( XMETHODS_WSDL_PATH );
        byte[] slurpedWsdl = HexUtils.slurpStream( xmethodsWsdlStream, 65536 );
        String xml = new String(slurpedWsdl, "UTF-8");
        Wsdl xmethodsWsdl = Wsdl.newInstance("", new InputSource(new ByteArrayInputStream(slurpedWsdl)) );
        _service.setWsdlXml(xml);
        _serviceCache.cache(_service);

        SoapMessageGenerator.MessageInputGenerator mig = new SoapMessageGenerator.MessageInputGenerator() {
            public String generate(String messagePartName, String operationName, Definition definition) {
                if (operationName.equals("getQuote")) {
                    if (messagePartName.equals("symbol")) {
                        return "SUNW";
                    }
                }
                return null;
            }
        };

        SoapMessageGenerator srg = new SoapMessageGenerator(mig);

        SoapMessageGenerator.Message[] requests = srg.generateRequests(xmethodsWsdl);

        String[] passingXpaths =
          {
              "//", // sanity
              "/soapenv:Envelope/soapenv:Body/ns1:getQuote/symbol", // contains a value
              "/soapenv:Envelope/soapenv:Body/ns1:getQuote/symbol='SUNW'", // works with proper namespaces
              "/*[local-name(.)='Envelope']/*[local-name(.)='Body']/*[local-name(.)='getQuote']/symbol='SUNW'", // works with no-namespace hack
          };

        // String[] passingXpaths = { "//", "/Envelope/Body/getQuote/c-gensym3=\"SUNW\"" };
        String[] failingXpaths =
          {
              "[", // invalid expression
              "/Envelope/Body/getQuote/symbol='SUNW'", // fails without namespaces
              "foo:Envelope/bar:Body/baz:getQuote/symbol='SUNW'", // fails with bogus namespaces
              "/soapenv:Envelope/soapenv:Body/ns1:getQuote/symbol=\"IBM\"", // wrong value with correct namespaces
              "/Envelope/Body/getQuote/symbol='IBM'", // wrong value without namespaces
          };


        Map namespaceMap = new HashMap();
        namespaceMap.put("ns1", XMETHODS_URN);
        namespaceMap.put("soapenv", SOAPConstants.URI_NS_SOAP_ENVELOPE);

        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];

            for (int j = 0; j < passingXpaths.length; j++) {
                newServletApi();
                String xpath = passingXpaths[j];
                _service.setPolicyXml(newXpathPolicy(xpath, namespaceMap));
                _serviceCache.cache(_service);

                _servletApi.setSoapRequest(request.getSOAPMessage(), XMETHODS_SOAPACTION);
                _servlet.doPost(_servletApi.getServletRequest(), _servletApi.getServletResponse());

                String resp = _servletOutputStream.toString("UTF-8");

                assertTrue("Response contained a fault!", resp.indexOf("Fault") == -1);
                System.err.println(resp);
            }

            for (int j = 0; j < failingXpaths.length; j++) {
                newServletApi();
                String xpath = failingXpaths[j];
                _service.setPolicyXml(newXpathPolicy(xpath, namespaceMap));
                _serviceCache.cache(_service);

                _servletApi.setSoapRequest(request.getSOAPMessage(), XMETHODS_SOAPACTION);
                _servlet.doPost(_servletApi.getServletRequest(), _servletApi.getServletResponse());
                String resp = _servletOutputStream.toString("UTF-8");

                assertTrue("Response did not contain an expected fault!", resp.indexOf("Fault") >= 0);
                System.err.println(resp);
            }


        }
    }

    public void setUp() throws Exception {
        System.setProperty("com.l7tech.common.locator.properties", "/com/l7tech/common/locator/test.properties");

        ServiceManager sman = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
        _service = (PublishedService)sman.findAll().iterator().next();
        //ServiceCache.initialize(); // need to do this, otherwise is a no go
        _serviceCache = ServiceCache.getInstance();
        _servlet.init(_servletApi.getServletConfig());

    }

    private void newServletApi() {
        _servletApi = MockServletApi.defaultMessageProcessingServletApi();
        _servletApi.setPublishedService(_service);
        _servletOutputStream = new MyServletOutputStream();
        Mock mock = _servletApi.getServletResponseMock();
        mock.matchAndReturn("getOutputStream", C.NO_ARGS, _servletOutputStream);
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test <code>RequestXpathAssertionTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}