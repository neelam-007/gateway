package com.l7tech.external.assertions.echorouting;

import com.ibm.xml.dsig.transform.W3CCanonicalizer2WC;
import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.echorouting.server.ServerEchoRoutingAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.server.MockServletApi;
import com.l7tech.server.SoapMessageProcessingServlet;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.composite.ServerCompositeAssertion;
import com.l7tech.server.service.ServicesHelper;
import com.l7tech.server.transport.http.HttpTransportModuleTester;
import com.l7tech.util.SoapConstants;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapMessageGenerator;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author emil
 * @version 21-Mar-2005
 */
public class EchoAssertionTest {
    private static MockServletApi servletApi;
    private SoapMessageProcessingServlet messageProcessingServlet;
    private static ServicesHelper servicesHelper;
    private static AssertionRegistry assReg;

    @Before
    public void setUp() throws Exception {
        if (servletApi == null) {
            servletApi = MockServletApi.defaultMessageProcessingServletApi("com/l7tech/server/resources/testApplicationContext.xml");
            assReg = (AssertionRegistry) servletApi.getApplicationContext().getBean("assertionRegistry");
            servicesHelper = new ServicesHelper((ServiceAdmin)servletApi.getApplicationContext().getBean("serviceAdmin"));
            assReg.registerAssertion(EchoRoutingAssertion.class);
        }
        initializeServicesAndPolicies();
        HttpTransportModuleTester.setGlobalConnector(new SsgConnector() {
            public boolean offersEndpoint(Endpoint endpoint) {
                return true;
            }
        });
    }

    /**
     * Test request response copying
     *
     * @throws Exception
     */
    @Test
    public void testRequestResponseEqual() throws Exception {
        ServicesHelper.ServiceDescriptor[] serviceDescriptors = servicesHelper.getAlllDescriptors();
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
                mhreq.addHeader( SoapConstants.SOAPACTION, request.getSOAPAction());
                MockHttpServletResponse mhres = servletApi.getServletResponse();
                messageProcessingServlet = new SoapMessageProcessingServlet();
                messageProcessingServlet.init(servletApi.getServletConfig());
                messageProcessingServlet.doPost(mhreq, mhres);
                W3CCanonicalizer2WC canonicalizer = new W3CCanonicalizer2WC();
                ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());

                ByteArrayOutputStream canonicalizedRequest = new ByteArrayOutputStream();
                canonicalizer.canonicalize( XmlUtil.parse(bi), canonicalizedRequest);

                byte[] responseByteArray = mhres.getContentAsByteArray();
                ByteArrayOutputStream canonicalizedResponse = new ByteArrayOutputStream();
                canonicalizer.canonicalize(XmlUtil.parse(new ByteArrayInputStream(responseByteArray)), canonicalizedResponse);

                assertTrue(Arrays.equals(canonicalizedRequest.toByteArray(), canonicalizedResponse.toByteArray()));
            }
        }
    }

    @Test
    public void testBug4570RoutingStatus() throws Exception {
        Message request = new Message(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        Message response = new Message();
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
        context.setService(new PublishedService());

        ServerAssertion sa = new ServerEchoRoutingAssertion(new EchoRoutingAssertion(), servletApi.getApplicationContext());
        AssertionStatus result = sa.checkRequest(context);
        assertEquals(result, AssertionStatus.NONE);
        assertEquals("Routing status must be routed after an echo (Bug #4570)", context.getRoutingStatus(), RoutingStatus.ROUTED);
    }

    private static void initializeServicesAndPolicies()
            throws IOException, FindException, DeleteException, UpdateException,
            SaveException, VersionException, SAXException, PolicyAssertionException {
        servicesHelper.deleteAllServices();
        servicesHelper.publish(TestDocuments.WSDL_DOC_LITERAL, TestDocuments.WSDL_DOC_LITERAL, getPolicy());
        servicesHelper.publish(TestDocuments.WSDL_DOC_LITERAL2, TestDocuments.WSDL_DOC_LITERAL2, getPolicy());
        servicesHelper.publish(TestDocuments.WSDL_DOC_LITERAL3, TestDocuments.WSDL_DOC_LITERAL3, getPolicy());
    }

    private static Assertion getPolicy() {
        return new AllAssertion(Arrays.asList(new EchoRoutingAssertion()));
    }

    /**
     * Test the Echo individually - this assertion is part of the
     * test source tree only
     *
     * @throws Exception
     */
    @Test
    public void testInstantiateEchoAssertion() throws Exception {
        AllAssertion echo = new AllAssertion(Arrays.asList(new EchoRoutingAssertion()));
        ServerPolicyFactory pfac = servletApi.getApplicationContext().getBean("policyFactory", ServerPolicyFactory.class);
        ServerAssertion serverAll = pfac.compilePolicy(echo, true);
        assertTrue(((ServerCompositeAssertion)serverAll).getChildren().get(0) instanceof ServerEchoRoutingAssertion);
    }

}
