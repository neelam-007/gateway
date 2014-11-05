package com.l7tech.external.assertions.retrieveservicewsdl.server;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

/**
 * Test the RetrieveServiceWsdlAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerRetrieveServiceWsdlAssertionTest {
    private static final String ACME_WAREHOUSE_WSDL = "ACMEWarehouse.wsdl";

    @Mock
    private PublishedService service;

    @Mock
    private ServiceCache serviceCache;

    private TestAudit testAudit;

    private static SecurityManager originalSecurityManager = System.getSecurityManager();

    @BeforeClass
    public static void init() {
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        testAudit = new TestAudit();
    }

    @AfterClass
    public static void dispose() {
        System.setSecurityManager(originalSecurityManager);
    }

    @Test
    public void testCheckRequest_SpecifyingNonExistentServiceIdVariable_UnsupportedVariableAudited() throws Exception {
        String serviceIdVariable = "serviceId";
        String hostnameVariable = "hostname";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${" + serviceIdVariable + "}");
        assertion.setHostname("${" + hostnameVariable + "}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // don't set serviceId
        // context.setVariable(serviceIdVariable, "ffffffffffffffffffffffffffffffffffff");

        context.setVariable(hostnameVariable, "localhost");

        try {
            serverAssertion.checkRequest(context);
            fail("Expected VariableNameSyntaxException");
        } catch (VariableNameSyntaxException e) {
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), serviceIdVariable)));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingNonExistentHostnameVariable_UnsupportedVariableAudited() throws Exception {
        String serviceIdVariable = "serviceId";
        String hostnameVariable = "hostname";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${" + serviceIdVariable + "}");
        assertion.setHostname("${" + hostnameVariable + "}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable(serviceIdVariable, "ffffffffffffffffffffffffffffffff");

        // don't set hostname
        // context.setVariable(hostnameVariable, "localhost");

        try {
            serverAssertion.checkRequest(context);
            fail("Expected VariableNameSyntaxException");
        } catch (VariableNameSyntaxException e) {
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), hostnameVariable)));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingInvalidCharInServiceId_InvalidServiceIdAudited() throws Exception {
        String invalidServiceId = "Xfffffffffffffffffffffffffffffff"; // includes invalid character 'X'

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", invalidServiceId);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHostname("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);

        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.RETRIEVE_WSDL_INVALID_SERVICE_ID.getMessage(),
                        "Cannot create goid from this String. Invalid hex data: " + invalidServiceId)));
    }

    @Test
    public void testCheckRequest_SpecifyingTooLongServiceId_InvalidServiceIdAudited() throws Exception {
        String invalidServiceId = "ffffffffffffffffffffffffffffffffff"; // length is 34 chars - a multiple of 2, but too long

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", invalidServiceId);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHostname("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);

        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.RETRIEVE_WSDL_INVALID_SERVICE_ID.getMessage(),
                        "Cannot create a goid from this String, it does not decode to a 16 byte array")));
    }

    @Test
    public void testCheckRequest_SpecifyingNonExistentService_ServiceNotFoundAudited() throws Exception {
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(null);

        String nonExistentServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid format, but no matching service

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", nonExistentServiceId);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHostname("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);

        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.RETRIEVE_WSDL_SERVICE_NOT_FOUND.getMessage(), nonExistentServiceId)));
    }

    @Test
    public void testCheckRequest_SpecifyingNonSoapService_ServiceNotSoapAudited() throws Exception {
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(false);

        String nonSoapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching non-SOAP service

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", nonSoapServiceId);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHostname("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_SERVICE_NOT_SOAP));
    }

    // TODO jwilliams: write test for SAXException on invalid WSDL xml

    @Test
    public void testCheckRequest_SpecifyingValidServiceAndResponseTarget_WsdlStoredToResponse() throws Exception {
        String acmeWsdlXmlString = getTestDocumentAsString(ACME_WAREHOUSE_WSDL);

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlXml()).thenReturn(acmeWsdlXmlString);

        String nonSoapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", nonSoapServiceId);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHostname("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);

        Document storedWsdl = context.getResponse().getXmlKnob().getDocumentReadOnly();

        assertTrue(storedWsdl.isEqualNode(XmlUtil.parse(acmeWsdlXmlString)));
    }

    @Test
    public void testCheckRequest_SpecifyingServiceWithPoorlyFormedXml_ServerErrorStatusReturned() throws Exception {
        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlXml()).thenReturn("<wsdl:definitions INVALID XML></wsdl:definitions>");

        String nonSoapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", nonSoapServiceId);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHostname("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.SERVER_ERROR, status);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO.getMessage(),
                        "Attribute name \"INVALID\" associated with an element type \"wsdl:definitions\" " +
                                "must be followed by the ' = ' character.")));
    }

    private ServerRetrieveServiceWsdlAssertion createServer(RetrieveServiceWsdlAssertion assertion) {
        ServerRetrieveServiceWsdlAssertion serverAssertion = new ServerRetrieveServiceWsdlAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                        .put("auditFactory", testAudit.factory())
                        .put("serviceCache", serviceCache)
                        .unmodifiableMap()
        );

        return serverAssertion;
    }

    private static PolicyEnforcementContext createPolicyEnforcementContext() {
        Message request = new Message();
        Message response = new Message();

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private String getTestDocumentAsString(String testDocument) throws IOException {
        return new String(IOUtils.slurpStream(getClass().getResourceAsStream(testDocument)));
    }
}
