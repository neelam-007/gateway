package com.l7tech.policy.assertion;

import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.service.ResolutionParameterTooLongException;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServicesHelper;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

/**
 * @author emil
 * @version 21-Mar-2005
 */
public class EchoAssertionTest extends TestCase {
    private static MockServletApi servletApi;
    private SoapMessageProcessingServlet messageProcessingServlet;
    private static ServicesHelper servicesHelper;

    /**
     * test <code>EchoAssertionTest</code> constructor
     */
    public EchoAssertionTest(String name) {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> for the
     * ServerPolicyFactoryTest <code>TestCase</code>
     */
    public static Test suite() throws Exception {
        TestSuite suite = new TestSuite(EchoAssertionTest.class);
        servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/common/testApplicationContext.xml");
        servicesHelper = new ServicesHelper((ServiceAdmin)servletApi.getApplicationContext().getBean("serviceAdmin"));
        initializeServicesAndPolicies();
        return suite;
    }

    public void setUp() throws Exception {
    }

    public void tearDown() throws Exception {
        // put tear down code here
    }

    /**
     * Test request response copying
     *
     * @throws Exception
     */
    public void testRequestResponseEqual() throws Exception {
        com.l7tech.service.ServicesHelper.ServiceDescriptor[] serviceDescriptors = servicesHelper.getAlllDescriptors();
        for (int i = 0; i < serviceDescriptors.length; i++) {
            ServicesHelper.ServiceDescriptor serviceDescriptor = serviceDescriptors[i];
            Wsdl wsdl = Wsdl.newInstance(null, new StringReader(serviceDescriptor.getWsdlXml()));
            wsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sm = new SoapMessageGenerator();
            SoapMessageGenerator.Message[] requests = sm.generateRequests(wsdl);
            for (int j = 0; j < requests.length; j++) {
                SoapMessageGenerator.Message request = requests[j];
                SOAPMessage msg = request.getSOAPMessage();

                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                msg.writeTo(bo);

                servletApi.reset();
                MockHttpServletRequest mhreq = servletApi.getServletRequest();
                mhreq.setContent(bo.toByteArray());
                mhreq.addHeader(SoapUtil.SOAPACTION, request.getSOAPAction());
                MockHttpServletResponse mhres = servletApi.getServletResponse();
                messageProcessingServlet = new SoapMessageProcessingServlet();
                messageProcessingServlet.init(servletApi.getServletConfig());
                messageProcessingServlet.doPost(mhreq, mhres);
                W3CCanonicalizer2WC canonicalizer = new W3CCanonicalizer2WC();
                ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());

                ByteArrayOutputStream canonicalizedRequest = new ByteArrayOutputStream();
                canonicalizer.canonicalize(XmlUtil.parse(bi), canonicalizedRequest);

                byte[] responseByteArray = mhres.getContentAsByteArray();
                ByteArrayOutputStream canonicalizedResponse = new ByteArrayOutputStream();
                canonicalizer.canonicalize(XmlUtil.parse(new ByteArrayInputStream(responseByteArray)), canonicalizedResponse);

                assertTrue(Arrays.equals(canonicalizedRequest.toByteArray(), canonicalizedResponse.toByteArray()));
            }
        }
    }

    private static void initializeServicesAndPolicies()
      throws IOException, FindException, DeleteException, UpdateException,
      SaveException, VersionException, ResolutionParameterTooLongException, SAXException {
        servicesHelper.deleteAllServices();
        servicesHelper.publish(TestDocuments.WSDL_DOC_LITERAL, TestDocuments.WSDL_DOC_LITERAL, getPolicy());
        servicesHelper.publish(TestDocuments.WSDL_DOC_LITERAL2, TestDocuments.WSDL_DOC_LITERAL2, getPolicy());
        servicesHelper.publish(TestDocuments.WSDL_DOC_LITERAL3, TestDocuments.WSDL_DOC_LITERAL3, getPolicy());
    }

    private static Assertion getPolicy() {
        Assertion policy = new AllAssertion(Arrays.asList(new Assertion[]{
                    new Echo(),
                  }));

        return policy;
    }


}
