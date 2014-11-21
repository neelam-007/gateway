package com.l7tech.external.assertions.retrieveservicewsdl.server;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.CommonMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.service.ServiceCache;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.IOUtils;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;

/**
 * Test the RetrieveServiceWsdlAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerRetrieveServiceWsdlAssertionTest {
    private static final String ACME_WAREHOUSE_WSDL = "ACMEWarehouse.wsdl";
    private static final String ID_SERVICE_WSDL = "IDService.wsdl";

    private static final String WSDL_QUERY_HANDLER_SERVICE_ROUTING_URI = "/wsdlHandler";

    @Mock
    private PublishedService service;

    @Mock
    private PublishedService wsdlQueryHandlerService;

    @Mock
    private ServiceCache serviceCache;

    @Mock
    private ServiceDocument serviceDocument;

    @Mock
    private ServiceDocumentManager serviceDocumentManager;

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

        when(wsdlQueryHandlerService.getRoutingUri()).thenReturn(WSDL_QUERY_HANDLER_SERVICE_ROUTING_URI);
    }

    @AfterClass
    public static void dispose() {
        System.setSecurityManager(originalSecurityManager);
    }

    @Test
    public void testCheckRequest_SpecifyingNonExistentProtocolVariable_NoSuchVariableAudited() throws Exception {
        String protocolVariable = "protocolVar";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setProtocol(null);
        assertion.setProtocolVariable(protocolVariable);
        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // don't set protocolVariable
        // context.setVariable(protocolVariable, "HTTP");

        try {
            serverAssertion.checkRequest(context);
            fail("Expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.FAILED, e.getAssertionStatus());
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(AssertionMessages.NO_SUCH_VARIABLE_WARNING.getMessage(), protocolVariable)));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingNonStringTypeProtocolVariable_InvalidVariableAudited() throws Exception {
        String protocolVariable = "protocolVar";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setProtocol(null);
        assertion.setProtocolVariable(protocolVariable);
        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // set protocolVariable as integer
        context.setVariable(protocolVariable, 10);

        try {
            serverAssertion.checkRequest(context);
            fail("Expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.FAILED, e.getAssertionStatus());
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(AssertionMessages.VARIABLE_INVALID_VALUE.getMessage(), protocolVariable, "String")));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingEmptyStringForProtocol_NoProtocolAudited() throws Exception {
        String protocolVariable = "protocolVar";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setProtocol(null);
        assertion.setProtocolVariable(protocolVariable);
        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // set protocolVariable as empty String
        context.setVariable(protocolVariable, "");

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_NO_PROTOCOL));
    }

    @Test
    public void testCheckRequest_ProtocolAndProtocolVariableBothNull_NoProtocolAudited() throws Exception {
        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        // protocol and protocol variable properties both null - N.B. not possible in normal use
        assertion.setProtocol(null);
        assertion.setProtocolVariable(null);

        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_NO_PROTOCOL));
    }

    @Test
    public void testCheckRequest_SpecifyingNonIntegerRepresentationPort_InvalidPortAudited() throws Exception {
        String portVariable = "portVar";
        String portValue = "eightyEighty";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setPort("${" + portVariable + "}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // set portVariable to invalid integer representation
        context.setVariable(portVariable, portValue);

        try {
            serverAssertion.checkRequest(context);
            fail("Expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.FAILED, e.getAssertionStatus());
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(AssertionMessages.RETRIEVE_WSDL_INVALID_PORT.getMessage(), portValue)));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingEmptyStringInPortVariable_NoPortAudited() throws Exception {
        String portVariable = "portVar";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setPort("${" + portVariable + "}");
        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // set portVariable to empty String
        context.setVariable(portVariable, "");

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_NO_PORT));
    }

    @Test
    public void testCheckRequest_SpecifyingPortNumberOutsideValidRange_InvalidPortAudited() throws Exception {
        String portVariable = "portVar";
        String portValue = "66666"; // greater than maximum port number of 65535

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setPort("${" + portVariable + "}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable(portVariable, portValue);

        try {
            serverAssertion.checkRequest(context);
            fail("Expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.FAILED, e.getAssertionStatus());
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(AssertionMessages.RETRIEVE_WSDL_INVALID_PORT.getMessage(), portValue)));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingNullOtherTargetMessageVariable_NoSuchVariableAudited() throws Exception {
        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        // set Message Target to variable but set target variable as null - N.B. not possible in normal use
        MessageTargetableSupport messageTarget = new MessageTargetableSupport(TargetMessageType.OTHER, true);
        messageTarget.setOtherTargetMessageVariable(null);

        assertion.setMessageTarget(messageTarget);
        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.NO_SUCH_VARIABLE_WARNING.getMessage(), "Target variable name is null.")));
    }

    @Test
    public void testCheckRequest_SpecifyingNonExistentServiceIdVariable_UnsupportedVariableAudited() throws Exception {
        String serviceIdVariable = "serviceId";
        String hostVariable = "host";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${" + serviceIdVariable + "}");
        assertion.setHost("${" + hostVariable + "}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // don't set serviceId
        // context.setVariable(serviceIdVariable, "ffffffffffffffffffffffffffffffffffff");

        context.setVariable(hostVariable, "localhost");

        try {
            serverAssertion.checkRequest(context);
            fail("Expected VariableNameSyntaxException");
        } catch (VariableNameSyntaxException e) {
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(AssertionMessages.NO_SUCH_VARIABLE_WARNING.getMessage(), serviceIdVariable)));
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), serviceIdVariable)));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingNonExistentHostnameVariable_UnsupportedVariableAudited() throws Exception {
        String serviceIdVariable = "serviceId";
        String hostVariable = "host";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${" + serviceIdVariable + "}");
        assertion.setHost("${" + hostVariable + "}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable(serviceIdVariable, "ffffffffffffffffffffffffffffffff");

        // don't set hostname
        // context.setVariable(hostVariable, "localhost");

        try {
            serverAssertion.checkRequest(context);
            fail("Expected VariableNameSyntaxException");
        } catch (VariableNameSyntaxException e) {
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(AssertionMessages.NO_SUCH_VARIABLE_WARNING.getMessage(), hostVariable)));
            assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                    .format(CommonMessages.TEMPLATE_UNSUPPORTED_VARIABLE.getMessage(), hostVariable)));
        }
    }

    @Test
    public void testCheckRequest_SpecifyingEmptyStringForHost_UnsupportedVariableAudited() throws Exception {
        String hostVariable = "hostVar";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setPort("8080");
        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("${" + hostVariable + "}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // set host variable to empty String
        context.setVariable(hostVariable, "");

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_NO_HOSTNAME));
    }

    @Test
    public void testCheckRequest_SpecifyingInvalidCharInServiceId_InvalidServiceIdAudited() throws Exception {
        String invalidServiceId = "Xfffffffffffffffffffffffffffffff"; // includes invalid character 'X'

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", invalidServiceId);

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHost("localhost");
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
        assertion.setHost("localhost");
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
        assertion.setHost("localhost");
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
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_SERVICE_NOT_SOAP));
    }

    @Test
    public void testCheckRequest_SpecifyingValidServiceAndRequestTarget_WsdlStoredToRequest() throws Exception {
        String acmeWsdlXmlString = getTestDocumentAsString(ACME_WAREHOUSE_WSDL);

        String soapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlXml()).thenReturn(acmeWsdlXmlString);
        when(service.getRoutingUri()).thenReturn("/svc");
        when(service.getId()).thenReturn(soapServiceId);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", soapServiceId);
        context.setVariable("portVar", "8443");

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHost("localhost");
        assertion.setPort("${portVar}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.REQUEST, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);

        Document storedWsdl = context.getRequest().getXmlKnob().getDocumentReadOnly();

        assertFalse(storedWsdl.isEqualNode(XmlUtil.parse(acmeWsdlXmlString)));

        // confirm the endpoints were rewritten correctly
        confirmEndpointsUpdated("http://localhost:8443/service/" + soapServiceId, storedWsdl);
    }

    @Test
    public void testCheckRequest_SpecifyingValidServiceAndResponseTarget_WsdlStoredToResponse() throws Exception {
        String acmeWsdlXmlString = getTestDocumentAsString(ACME_WAREHOUSE_WSDL);

        String soapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlXml()).thenReturn(acmeWsdlXmlString);
        when(service.getRoutingUri()).thenReturn("/svc");
        when(service.getId()).thenReturn(soapServiceId);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", soapServiceId);
        context.setVariable("portVar", "8443");

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHost("localhost");
        assertion.setPort("${portVar}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);

        Document storedWsdl = context.getResponse().getXmlKnob().getDocumentReadOnly();

        assertFalse(storedWsdl.isEqualNode(XmlUtil.parse(acmeWsdlXmlString)));

        // confirm the endpoints were rewritten correctly
        confirmEndpointsUpdated("http://localhost:8443/service/" + soapServiceId, storedWsdl);
    }

    @Test
    public void testCheckRequest_SpecifyingValidServiceAndVariableTarget_WsdlStoredToVariable() throws Exception {
        String acmeWsdlXmlString = getTestDocumentAsString(ACME_WAREHOUSE_WSDL);

        String soapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlXml()).thenReturn(acmeWsdlXmlString);
        when(service.getRoutingUri()).thenReturn("/svc");
        when(service.getId()).thenReturn(soapServiceId);

        String wsdlTargetContextVar = "wsdlTargetVar";

        MessageTargetableSupport otherMessageTarget = new MessageTargetableSupport(TargetMessageType.OTHER, true);
        otherMessageTarget.setOtherTargetMessageVariable(wsdlTargetContextVar);
        otherMessageTarget.setSourceUsedByGateway(false);
        otherMessageTarget.setTargetModifiedByGateway(true);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", soapServiceId);
        context.setVariable("portVar", "8443");

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHost("localhost");
        assertion.setPort("${portVar}");
        assertion.setMessageTarget(otherMessageTarget);

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.NONE, status);

        Message wsdlTargetMessage = (Message) context.getVariable(wsdlTargetContextVar);

        Document storedWsdl = wsdlTargetMessage.getXmlKnob().getDocumentReadOnly();

        assertFalse(storedWsdl.isEqualNode(XmlUtil.parse(acmeWsdlXmlString)));

        // confirm the endpoints were rewritten correctly
        confirmEndpointsUpdated("http://localhost:8443/service/" + soapServiceId, storedWsdl);
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
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        try {
            serverAssertion.checkRequest(context);
            fail("Expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
        }

        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.RETRIEVE_WSDL_ERROR_PARSING_WSDL.getMessage(),
                        "Attribute name \"INVALID\" associated with " +
                                "an element type \"wsdl:definitions\" must be followed by the ' = ' character.")));
    }

    @Test
    public void testCheckRequest_SpecifyingUnrecognizableProtocol_InvalidEndpointAudited() throws Exception {
        String acmeWsdlXmlString = getTestDocumentAsString(ACME_WAREHOUSE_WSDL);

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlXml()).thenReturn(acmeWsdlXmlString);
        when(service.getRoutingUri()).thenReturn("/svc");

        String protocolVariable = "protocolVar";

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setProtocol(null);
        assertion.setProtocolVariable(protocolVariable);
        assertion.setServiceId("ffffffffffffffffffffffffffffffff");
        assertion.setHost("localhost");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        // set protocolVariable as empty String
        context.setVariable(protocolVariable, "wxyz");

        AssertionStatus status = serverAssertion.checkRequest(context);

        assertEquals(AssertionStatus.FAILED, status);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_INVALID_ENDPOINT_URL));
    }

    @Test
    public void testCheckRequest_FindExceptionThrownFromServiceDocumentManager_ServerErrorReturned() throws Exception {
        String acmeWsdlXmlString = getTestDocumentAsString(ACME_WAREHOUSE_WSDL);

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(service.isSoap()).thenReturn(true);
        when(service.isInternal()).thenReturn(true);
        when(service.getWsdlXml()).thenReturn(acmeWsdlXmlString);
        when(service.getRoutingUri()).thenReturn("/svc");
        when(serviceDocumentManager.findByServiceIdAndType(any(Goid.class), any(String.class)))
                .thenThrow(new FindException("data access error"));

        String soapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", soapServiceId);
        context.setVariable("portVar", "8443");

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHost("localhost");
        assertion.setPort("${portVar}");
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        try {
            serverAssertion.checkRequest(context);
            fail("Expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
        }

        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO.getMessage(), "data access error")));
    }

    @Test
    public void testCheckRequest_ExceptionThrownDuringRewriteUrlCreation_ErrorAudited() throws Exception {
        String serviceWsdlXml = getTestDocumentAsString(ID_SERVICE_WSDL);

        // this invalid wsdlUrl will cause a URISyntaxException while attempting to create a url for rewriting
        String wsdlUrl = "://www.predic8.com:8080/base/IDService?wsdl";

        String soapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        String serviceDocumentId = "11111111111111111111111111111111";
        String importUrl = "http://www.predic8.com:8080/base/IDService?xsd=1";

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(serviceDocumentManager.findByServiceIdAndType(any(Goid.class), any(String.class)))
                .thenReturn(Arrays.asList(serviceDocument));

        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlUrl()).thenReturn(wsdlUrl);
        when(service.getWsdlXml()).thenReturn(serviceWsdlXml);
        when(service.getRoutingUri()).thenReturn("/svc");
        when(service.getId()).thenReturn(soapServiceId);

        when(serviceDocument.getId()).thenReturn(serviceDocumentId);
        when(serviceDocument.getUri()).thenReturn(importUrl);
        when(serviceDocument.getContents()).thenReturn("");

        PolicyEnforcementContext context = createPolicyEnforcementContext();

        context.setVariable("serviceId", soapServiceId);
        context.setVariable("portVar", "8443");

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHost("localhost");
        assertion.setPort("${portVar}");
        assertion.setProxyDependencies(true);
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.checkRequest(context);

        // despite the error in rewriting, the assertion should continue and succeed
        assertEquals(AssertionStatus.NONE, status);

        Document storedWsdl = context.getResponse().getXmlKnob().getDocumentReadOnly();

        // confirm the endpoints were rewritten correctly
        confirmEndpointsUpdated("http://localhost:8443/service/" + soapServiceId, storedWsdl);

        NodeList nl = storedWsdl.getElementsByTagName("xsd:import");

        assertEquals(1, nl.getLength());

        Element element = (Element) nl.item(0);

        // should be no change - the rewrite should be skipped because of the error
        assertEquals(importUrl, element.getAttribute("schemaLocation"));

        // error should have been audited
        assertTrue(testAudit.isAuditPresent(AssertionMessages.RETRIEVE_WSDL_PROXY_URL_CREATION_FAILURE));
    }

    @Test
    public void testCheckRequest_ExceptionThrownAttemptingToCreateWsdlProxyBaseUrl_ExceptionAudited() throws Exception {
        String serviceWsdlXml = getTestDocumentAsString(ID_SERVICE_WSDL);

        String wsdlUrl = "http://www.predic8.com:8080/base/IDService?wsdl";

        String serviceDocumentId = "11111111111111111111111111111111";
        String importUrl = "http://www.predic8.com:8080/base/IDService?xsd=1";

        when(serviceCache.getCachedService(any(Goid.class))).thenReturn(service);
        when(serviceDocumentManager.findByServiceIdAndType(any(Goid.class), any(String.class)))
                .thenReturn(Arrays.asList(serviceDocument));

        when(service.isSoap()).thenReturn(true);
        when(service.getWsdlUrl()).thenReturn(wsdlUrl);
        when(service.getWsdlXml()).thenReturn(serviceWsdlXml);
        when(service.getRoutingUri()).thenReturn("/svc");

        when(serviceDocument.getId()).thenReturn(serviceDocumentId);
        when(serviceDocument.getUri()).thenReturn(importUrl);
        when(serviceDocument.getContents()).thenReturn("");

        String soapServiceId = "ffffffffffffffffffffffffffffffff"; // valid goid with matching SOAP service

        MockHttpServletRequest hRequest = mock(MockHttpServletRequest.class);

        when(hRequest.getMethod()).thenReturn("GET");
        // this invalid wsdlUrl will cause a URISyntaxException while attempting to create the base url for proxying
        when(hRequest.getRequestURL()).thenReturn(new StringBuffer("://gateway:80b0"));

        Message request = new Message();
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));
        Message response = createResponse();

        PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        context.setService(wsdlQueryHandlerService);

        context.setVariable("serviceId", soapServiceId);
        context.setVariable("portVar", "8443");

        RetrieveServiceWsdlAssertion assertion = new RetrieveServiceWsdlAssertion();

        assertion.setServiceId("${serviceId}");
        assertion.setHost("localhost");
        assertion.setPort("${portVar}");
        assertion.setProtocol("HTTP");
        assertion.setProtocolVariable(null);
        assertion.setProxyDependencies(true);
        assertion.setMessageTarget(new MessageTargetableSupport(TargetMessageType.RESPONSE, true));

        ServerRetrieveServiceWsdlAssertion serverAssertion = createServer(assertion);

        try {
            serverAssertion.checkRequest(context);
            fail("Expected AssertionStatusException");
        } catch (AssertionStatusException e) {
            assertEquals(AssertionStatus.SERVER_ERROR, e.getAssertionStatus());
        }

        for (String s:testAudit) {
            System.out.println(s);
        }

        // error should have been audited
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat
                .format(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO.getMessage(),
                        "Expected scheme name at index 0: ://gateway:80b0")));
    }

    // ---- EXAMPLE OF EXPECTED ENDPOINT REWRITING ----

    /*
        INPUT DOCUMENT:
        <wsdl:service name="Warehouse">
            <wsdl:port binding="tns:WarehouseSoap" name="WarehouseSoap">
                <soap:address location="http://hugh.l7tech.com/ACMEWarehouseWS/Service1.asmx"/>
            </wsdl:port>
            <wsdl:port binding="tns:WarehouseSoap12" name="WarehouseSoap12">
                <soap12:address location="http://hugh.l7tech.com/ACMEWarehouseWS/Service1.asmx"/>
            </wsdl:port>
        </wsdl:service>

        CHANGES EXPECTED:
        <wsdl:service name="Warehouse">
            <wsdl:port binding="tns:WarehouseSoap" name="WarehouseSoap">
                <soap:address location="http://localhost:8443/svc"/>
            </wsdl:port>
            <wsdl:port binding="tns:WarehouseSoap12" name="WarehouseSoap12">
                <soap12:address location="http://localhost:8443/svc"/>
            </wsdl:port>
        </wsdl:service>
    */

    private void confirmEndpointsUpdated(String endpointUrl, Document storedWsdl) {
        NodeList nl = storedWsdl.getElementsByTagName("*");

        for (int i = 0; i < nl.getLength(); i++) {
            Element element = (Element) nl.item(i);

            if ("address".equals(element.getLocalName())) {
                assertEquals(endpointUrl, element.getAttribute("location"));
            }
        }
    }

    private ServerRetrieveServiceWsdlAssertion createServer(RetrieveServiceWsdlAssertion assertion) {
        ServerRetrieveServiceWsdlAssertion serverAssertion = new ServerRetrieveServiceWsdlAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                        .put("auditFactory", testAudit.factory())
                        .put("serviceCache", serviceCache)
                        .put("serviceDocumentManager", serviceDocumentManager)
                        .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext() {
        Message request = createRequest();
        Message response = createResponse();

        PolicyEnforcementContext context =
                PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        context.setService(wsdlQueryHandlerService);

        return context;
    }

    private Message createRequest() {
        MockHttpServletRequest hRequest = new MockHttpServletRequest();

        Message request = new Message();
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }

    private Message createResponse() {
        MockHttpServletResponse hResponse = new MockHttpServletResponse();

        Message response = new Message();
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hResponse));

        return response;
    }

    private String getTestDocumentAsString(String testDocument) throws IOException {
        return new String(IOUtils.slurpStream(getClass().getResourceAsStream(testDocument)));
    }
}
