package com.l7tech.server.policy;

import com.l7tech.common.security.Keys;
import com.l7tech.common.util.Locator;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.servlet.MockHttpServletResponse;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Class SamlPolicyTest.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlPolicyTest extends TestCase {
    private SoapMessageProcessingServlet messageProcessingServlet;
    private ServiceAdmin serviceAdmin;
    private SoapMessageGenerator.Message[] soapRequests;
    private PublishedService publishedService;

    /**
     * test <code>SamlPolicyTest</code> constructor
     */
    public SamlPolicyTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SamlPolicyTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * sets the test environment
             * 
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                System.setProperty("com.l7tech.common.locator.properties", "/com/l7tech/common/locator/test.properties");
                Keys.createTestSsgKeystoreProperties();
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void setUp() throws Exception {
        serviceAdmin = (ServiceAdmin)Locator.getDefault().lookup(ServiceAdmin.class);
        if (serviceAdmin == null) {
            throw new AssertionError("could not retrieve admin service");
        }
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        assertTrue("no services have been returned could not execute test", headers.length > 0);
        publishedService = serviceAdmin.findServiceByPrimaryKey(headers[0].getOid());
        Wsdl wsdl = publishedService.parsedWsdl();

        SoapMessageGenerator sg = new SoapMessageGenerator();
        soapRequests = sg.generateRequests(wsdl);
        assertTrue("no operations could be located in the wsdlt", soapRequests.length > 0);


    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    public void testSecurityElementCheck() throws Exception {
        for (int i = 0; i < soapRequests.length; i++) {
            MockServletApi servletApi = MockServletApi.defaultMessageProcessingServletApi();
            SoapMessageGenerator.Message soapRequest = soapRequests[i];
            prepareServicePolicy(new SamlSecurity());
            servletApi.setPublishedService(publishedService);
            Document samlHeader = getDocument("com/l7tech/common/security/saml/saml1.xml");
            SOAPMessage soapMessage = soapRequest.getSOAPMessage();
            attachAssertionHeader(soapMessage, samlHeader);

            soapMessage.writeTo(System.out);
            
            servletApi.setSoapRequest(soapMessage, soapRequest.getSOAPAction());
            HttpServletRequest mhreq = servletApi.getServletRequest();
            MockHttpServletResponse mhres = new MockHttpServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
        }
    }

    public void xtestSenderVouches() throws Exception {
        for (int i = 0; i < soapRequests.length; i++) {
            MockServletApi servletApi = MockServletApi.defaultMessageProcessingServletApi();
            SoapMessageGenerator.Message soapRequest = soapRequests[i];
            HttpRoutingAssertion assertion = new HttpRoutingAssertion();
            assertion.setAttachSamlSenderVouches(true);
            assertion.setProtectedServiceUrl("http://localhost:8081");
            prepareServicePolicy(assertion);
            servletApi.setPublishedService(publishedService);
            Mock sreqMock = servletApi.getServletRequestMock();
            sreqMock.matchAndReturn("getAttribute", Request.PARAM_HTTP_SOAPACTION, soapRequest.getSOAPAction());
            sreqMock.matchAndReturn("getCookies", null);

            SOAPMessage soapMessage = soapRequest.getSOAPMessage();
            servletApi.setSoapRequest(soapMessage, soapRequest.getSOAPAction());
            HttpServletRequest mhreq = servletApi.getServletRequest();
            MockHttpServletResponse mhres = new MockHttpServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
        }
    }


    public void xtestSamlSecurityDateRange() throws Exception {
        for (int i = 0; i < soapRequests.length; i++) {
            MockServletApi servletApi = MockServletApi.defaultMessageProcessingServletApi();
            SoapMessageGenerator.Message soapRequest = soapRequests[i];
            SamlSecurity assertion = new SamlSecurity();
            assertion.setValidateValidityPeriod(true);
            prepareServicePolicy(assertion);
            servletApi.setPublishedService(publishedService);
            Mock sreqMock = servletApi.getServletRequestMock();
            sreqMock.matchAndReturn("getAttribute", Request.PARAM_HTTP_SOAPACTION, soapRequest.getSOAPAction());
            sreqMock.matchAndReturn("getCookies", null);
            SOAPMessage soapMessage = soapRequest.getSOAPMessage();
            Document samlHeader = getDocument("com/l7tech/common/security/saml/saml1.xml");
            attachAssertionHeader(soapMessage, samlHeader);

            servletApi.setSoapRequest(soapMessage, soapRequest.getSOAPAction());
            HttpServletRequest mhreq = servletApi.getServletRequest();
            MockHttpServletResponse mhres = new MockHttpServletResponse();
            messageProcessingServlet = new SoapMessageProcessingServlet();
            messageProcessingServlet.init(servletApi.getServletConfig());
            messageProcessingServlet.doPost(mhreq, mhres);
        }
    }


    private void prepareServicePolicy(Assertion assertion)
      throws IOException, SaveException, VersionException, UpdateException, InterruptedException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        WspWriter.writePolicy(assertion, bo);
        publishedService.setPolicyXml(bo.toString());
        serviceAdmin.savePublishedService(publishedService);
        ServiceCache.getInstance().cache(publishedService);
    }

    private Document getDocument(String resourceName)
      throws IOException, ParserConfigurationException, SAXException {
        ClassLoader cl = getClass().getClassLoader();
        InputStream is = cl.getResourceAsStream(resourceName);
        if (is == null) {
            throw new FileNotFoundException(resourceName);
        }
        DocumentBuilderFactory df = DocumentBuilderFactory.newInstance();
        df.setNamespaceAware(true);
        return df.newDocumentBuilder().parse(is);
    }

    private void attachAssertionHeader(SOAPMessage sm, Document assertionDocument)
      throws SOAPException {
        SOAPEnvelope envelope = sm.getSOAPPart().getEnvelope();
        envelope.addNamespaceDeclaration("wsse", "http://schemas.xmlsoap.org/ws/2002/xx/secext");
        envelope.addNamespaceDeclaration("ds", "http://www.w3.org/2000/09/xmldsig#");
        SOAPHeader sh = envelope.getHeader();
        if (sh == null) {
            sh = envelope.addHeader();
        }
        Element domNode = assertionDocument.getDocumentElement();
        SOAPHeaderElement she = null;
        SOAPFactory sf = SOAPFactory.newInstance();
        Name headerName = sf.createName("Security", "wsse", "http://schemas.xmlsoap.org/ws/2002/xx/secext");

        she = sh.addHeaderElement(headerName);
        Name assertionName = sf.createName(domNode.getLocalName(), domNode.getPrefix(), domNode.getNamespaceURI());

        SOAPElement assertionElement = she.addChildElement(assertionName);
        SoapUtil.domToSOAPElement(assertionElement, domNode);
    }


    /**
     * Test <code>ServerPolicyFactoryTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
