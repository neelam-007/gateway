package com.l7tech.server.saml;

import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ResolutionParameterTooLongException;
import com.l7tech.service.ServiceAdmin;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Class SamlProcessingTest.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlProcessingTest extends TestCase {
    private static MockServletApi servletApi;
    private static ServiceDescriptor[] serviceDescriptors;
    private SoapMessageProcessingServlet messageProcessingServlet;

    /**
     * test <code>SamlProcessingTest</code> constructor
     */
    public SamlProcessingTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * SamlProcessingTest <code>TestCase</code>
     */
    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(SamlProcessingTest.class);
        TestSetup wrapper = new TestSetup(suite) {
            /**
             * sets the test environment
             *
             * @throws Exception on error deleting the stub data store
             */
            protected void setUp() throws Exception {
                servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/common/testApplicationContext.xml");
                ApplicationContext context = servletApi.getApplicationContext();
                initializeServicesAndPolicies(context);
            }

            protected void tearDown() throws Exception {
                ;
            }
        };
        return wrapper;
    }

    public void setUp() throws Exception {
        servletApi.reset();
        messageProcessingServlet = new SoapMessageProcessingServlet();
        messageProcessingServlet.init(servletApi.getServletConfig());
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test the authenticaiton assertion. Create the authentication assertion, attach it to
     * the message and invoke message processor.
     *
     * @throws Exception
     */
    public void testAuthenticationAssertion() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptors[0].wsdlXml));
        wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
        SoapMessageGenerator sm = new SoapMessageGenerator();
        SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
        for (int i = 0; i < requests.length; i++) {
            SoapMessageGenerator.Message request = requests[i];
            SOAPMessage msg = request.getSOAPMessage();
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            msg.writeTo(bo);
            MockHttpServletRequest servletRequest = servletApi.getServletRequest();
            servletRequest.setContentType(XmlUtil.TEXT_XML);
            servletRequest.setContent(bo.toByteArray());
            servletRequest.addHeader(SoapUtil.SOAPACTION, request.getSOAPAction());
            messageProcessingServlet.doPost(servletRequest, servletApi.getServletResponse());
        }
    }

    private static void initializeServicesAndPolicies(ApplicationContext context)
      throws IOException, FindException, DeleteException, UpdateException,
      SaveException, VersionException, ResolutionParameterTooLongException, SAXException {

        ServiceAdmin serviceAdmin = (ServiceAdmin)context.getBean("serviceAdmin");
        EntityHeader[] headers = serviceAdmin.findAllPublishedServices();
        for (int i = 0; i < headers.length; i++) {
            EntityHeader header = headers[i];
            serviceAdmin.deletePublishedService(header.getStrId());
        }

        serviceDescriptors = new ServiceDescriptor[]{
            new ServiceDescriptor(TestDocuments.WSDL_DOC_LITERAL,
                                  TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL_DOC_LITERAL),
                                  getAuthenticationAssertionPolicy()),
            new ServiceDescriptor(TestDocuments.WSDL_DOC_LITERAL2,
                                  TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL_DOC_LITERAL2),
                                  getAuthorizationAssertionPolicy()),
            new ServiceDescriptor(TestDocuments.WSDL_DOC_LITERAL3,
                                  TestDocuments.getTestDocumentAsXml(TestDocuments.WSDL_DOC_LITERAL3),
                                  getAttributeAssertionPolicy())
        };

        for (int i = 0; i < serviceDescriptors.length; i++) {
            ServiceDescriptor descriptor = serviceDescriptors[i];
            PublishedService ps = new PublishedService();
            ps.setName(descriptor.name);
            ps.setWsdlXml(descriptor.wsdlXml);
            ps.setPolicyXml(descriptor.policyXml);
            serviceAdmin.savePublishedService(ps);
        }
    }

    private static String getAttributeAssertionPolicy() {
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Saml Attribute statement:
            new SamlAttributeStatement(),
            // Route:
            new HttpRoutingAssertion()
        }));

        return WspWriter.getPolicyXml(policy);
    }

    private static String getAuthorizationAssertionPolicy() {
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Saml Authorization Statement:
            new SamlAuthorizationStatement(),
            // Route:
            new HttpRoutingAssertion()
        }));

        return WspWriter.getPolicyXml(policy);
    }

    private static String getAuthenticationAssertionPolicy() {
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[]{
            // Saml Authentication Statement:
            new SamlAuthenticationStatement(),
            // Route:
            new HttpRoutingAssertion()
        }));

        return WspWriter.getPolicyXml(policy);
    }

    private static class ServiceDescriptor {
        final String name;
        final String wsdlXml;
        final String policyXml;

        public ServiceDescriptor(String name, String wsdlXml, String policyXml) {
            this.name = name;
            this.policyXml = policyXml;
            this.wsdlXml = wsdlXml;
        }
    }

    /**
     * Test <code>SamlProcessingTest</code> main.
     */
    public static void main(String[] args) throws
      Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
