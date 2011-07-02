package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.wsaddressing.AddWsAddressingAssertion;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Collections;

/**
 * @author darmstrong
 */
public class ServerAddWsAddressingAssertionTest {

    @Test
    public void testMessageProperties_NoVariables() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();
        final String wsaAction = "http://warehouse.acme.com/ws/listProducts";
        assertion.setAction(wsaAction);
        final String namespace = AddWsAddressingAssertion.DEFAULT_NAMESPACE;
        assertion.setWsaNamespaceUri(namespace);
        final String msgId = SoapUtil.generateUniqueUri("MessageId-", true);
        assertion.setMessageId(msgId);
        final String destination = "http://hugh/ACMEWarehouseWS/Service1.asmx";
        assertion.setDestination(destination);

        final String ssgHost = "http://ssghost.com";
        assertion.setSourceEndpoint(ssgHost + "/from");
        assertion.setReplyEndpoint(ssgHost + "/reply");
        assertion.setFaultEndpoint(ssgHost + "/fault");
        final String relatesToMsgsId = SoapUtil.generateUniqueUri("MessageId", true);
        assertion.setRelatesToMessageId(relatesToMsgsId);

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(soapMsg, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        //validate request
        final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
        final Element headerEl = SoapUtil.getHeaderElement(requestDoc);
        System.out.println(XmlUtil.nodeToFormattedString(headerEl));

        final Element actionEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, assertion.getWsaNamespaceUri(), SoapConstants.WSA_MSG_PROP_ACTION);
        Assert.assertNotNull("Action should have been found", actionEl);
        final String actionId = actionEl.getAttributeNS(SoapConstants.WSU_NAMESPACE, "Id");
        Assert.assertNotSame("Id should have been added.", "", actionId.trim());

        final Element msgIdEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_MESSAGE_ID);
        Assert.assertNotNull("MessageId should have been found", msgIdEl);
        validateIdAttribute(msgIdEl);
        validateNamespace(msgIdEl, namespace);
        Assert.assertEquals("Incorrect element value", msgId, msgIdEl.getTextContent());

        final Element destEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_DESTINATION);
        Assert.assertNotNull("Destination should have been found", destEl);
        validateIdAttribute(destEl);
        validateNamespace(destEl, namespace);
        Assert.assertEquals("Incorrect element value", destination, destEl.getTextContent());

        final Element sourceEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT);
        Assert.assertNotNull("From should have been found", sourceEl);
        validateIdAttribute(sourceEl);
        validateNamespace(sourceEl, namespace);
        Assert.assertEquals("Incorrect element value", ssgHost + "/from", sourceEl.getTextContent());

        final Element replyToEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_REPLY_TO);
        Assert.assertNotNull("ReplyTo should have been found", replyToEl);
        validateIdAttribute(replyToEl);
        validateNamespace(replyToEl, namespace);
        Assert.assertEquals("Incorrect element value", ssgHost + "/reply", replyToEl.getTextContent());

        final Element faultToEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_FAULT_TO);
        Assert.assertNotNull("FaultTo should have been found", faultToEl);
        validateIdAttribute(faultToEl);
        validateNamespace(faultToEl, namespace);
        Assert.assertEquals("Incorrect element value", ssgHost + "/fault", faultToEl.getTextContent());

        final Element relatesToMsgEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_RELATES_TO);
        Assert.assertNotNull("RelatesTo MsgId should have been found", relatesToMsgEl);
        validateIdAttribute(relatesToMsgEl);
        final String relatesToAttributeValue =
                relatesToMsgEl.getAttributeNS(SoapConstants.WSA_NAMESPACE_10, SoapConstants.WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE);
        Assert.assertTrue("Relates to attribute should have been added.", !relatesToAttributeValue.trim().isEmpty());
        Assert.assertEquals("Incorrect relationship type value found", SoapConstants.WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE,
                relatesToAttributeValue);

        validateNamespace(relatesToMsgEl, namespace);
        Assert.assertEquals("Incorrect element value", relatesToMsgsId, relatesToMsgEl.getTextContent());

        //validate context variables
        testContextVariables(context, assertion, wsaAction, true, msgId);
    }

    /**
     * Tests that a SOAP message with no header does not cause the assertion to fail. The header should be created
     * automatically if it is not present.
     * 
     * @throws Exception
     */
    @BugNumber(9268)
    @Test
    public void testNoSoapHeaderPresent() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();
        assertion.setAction("http://warehouse.acme.com/ws/listProducts");
        assertion.setWsaNamespaceUri(SoapConstants.WSA_NAMESPACE);
        assertion.setMessageId(SoapUtil.generateUniqueUri("MessageId-", true));
        assertion.setDestination("http://hugh/ACMEWarehouseWS/Service1.asmx");

        final String ssgHost = "http://ssghost.com";
        assertion.setSourceEndpoint(ssgHost);
        assertion.setReplyEndpoint(ssgHost);
        assertion.setFaultEndpoint(ssgHost);
        final String relatesToMsgsId = SoapUtil.generateUniqueUri("MessageId", true);
        assertion.setRelatesToMessageId(relatesToMsgsId);

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(warehouseResponse, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);
    }

    /**
     * If the Action property is configured to be AddWsAddressingAssertion.ACTION_FROM_TARGET_MESSAGE then it should
     * obtain the value of the SOAPAction from the associated target message when it is available. Also tests
     * that this value is set in the correct context variable.
     * Also tests a custom variable prefix.
     * 
     * @throws Exception
     */
    @Test
    public void testActionFromTargetMessage() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();
        assertion.setAction(AddWsAddressingAssertion.ACTION_FROM_TARGET_MESSAGE);
        assertion.setWsaNamespaceUri(SoapConstants.WSA_NAMESPACE);
        assertion.setVariablePrefix("testprefix");
        assertion.setDestination("http://hugh/ACMEWarehouseWS/Service1.asmx");

        final String ssgHost = "http://ssghost.com";
        assertion.setSourceEndpoint(ssgHost);
        assertion.setReplyEndpoint(ssgHost);
        assertion.setFaultEndpoint(ssgHost);
        final String relatesToMsgsId = SoapUtil.generateUniqueUri("MessageId", true);
        assertion.setRelatesToMessageId(relatesToMsgsId);

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final String soapAction = "http://warehouse.acme.com/ws/listProducts";
        final PolicyEnforcementContext context = getContext(warehouseResponse, soapAction);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        //validate request
        final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
        final Element headerEl = SoapUtil.getHeaderElement(requestDoc);
        System.out.println(XmlUtil.nodeToFormattedString(headerEl));

        final Element actionEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, assertion.getWsaNamespaceUri(), SoapConstants.WSA_MSG_PROP_ACTION);
        Assert.assertNotNull("Action should have been found", actionEl);

        Assert.assertEquals("SOAPAction should have been added as the value of the Action element",
                soapAction, actionEl.getTextContent());

        //validate context variables
        testContextVariables(context, assertion, soapAction, false, null);
    }

    /**
     * If there is no SOAPAction associated with the target message, and the assertion's action is set to
     * AddWsAddressingAssertion.ACTION_FROM_TARGET_MESSAGE then the assertion should fail.
     *
     * @throws Exception
     */
    @BugNumber(9270)
    @Test
    public void testActionFromTargetMessage_NotFound() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();
        assertion.setAction(AddWsAddressingAssertion.MESSAGE_ID_AUTOMATIC);
        assertion.setWsaNamespaceUri(SoapConstants.WSA_NAMESPACE);
        assertion.setMessageId(SoapUtil.generateUniqueUri("MessageId-", true));
        assertion.setDestination("http://hugh/ACMEWarehouseWS/Service1.asmx");

        final String ssgHost = "http://ssghost.com";
        assertion.setSourceEndpoint(ssgHost);
        assertion.setReplyEndpoint(ssgHost);
        assertion.setFaultEndpoint(ssgHost);
        final String relatesToMsgsId = SoapUtil.generateUniqueUri("MessageId", true);
        assertion.setRelatesToMessageId(relatesToMsgsId);

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(warehouseResponse, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }

    @BugNumber(9282)
    @Test
    public void testMessageProperties_Variables() throws Exception{
        //run test for each namespace, in case namespace ever introduces a change
        for (String namespace : AddWsAddressingAssertion.WSA_NAMESPACES) {
            
            final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();
            final String actionVar = "action";
            final String namespaceVar = "namespace";
            final String msgIdVar = "msgId";
            final String destVar = "destination";
            final String fromVar = "from";
            final String replyVar = "reply";
            final String faultVar = "fault";
            final String relatesMsgIdVar = "relatesMsgId";

            final String action = "http://warehouse.acme.com/ws/listProducts";
            final String msgId = SoapUtil.generateUniqueUri("MessageId-", true);
            final String destination = "http://hugh/ACMEWarehouseWS/Service1.asmx";
            final String ssgHost = "http://ssghost.com";
            final String relatesToMsgsId = SoapUtil.generateUniqueUri("MessageId", true);

            assertion.setAction("${" + actionVar + "}");
            assertion.setWsaNamespaceUri("${" + namespaceVar + "}");
            assertion.setMessageId("${" + msgIdVar + "}");
            assertion.setDestination("${" + destVar + "}");
            assertion.setSourceEndpoint("${" + fromVar + "}");
            assertion.setReplyEndpoint("${" + replyVar + "}");
            assertion.setFaultEndpoint("${" + faultVar + "}");
            assertion.setRelatesToMessageId("${" + relatesMsgIdVar + "}");

            final ServerAddWsAddressingAssertion serverAssertion =
                    new ServerAddWsAddressingAssertion(assertion);

            final String soapAction = "http://warehouse.acme.com/ws/listProducts";
            final PolicyEnforcementContext context = getContext(soapMsg, soapAction);
            context.setVariable(actionVar, action);
            context.setVariable(namespaceVar, namespace);
            context.setVariable(msgIdVar, msgId);
            context.setVariable(destVar, destination);
            context.setVariable(fromVar, ssgHost + "/from");
            context.setVariable(replyVar, ssgHost + "/reply");
            context.setVariable(faultVar, ssgHost + "/fault");
            context.setVariable(relatesMsgIdVar, relatesToMsgsId);

            final AssertionStatus status = serverAssertion.checkRequest(context);
            Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

            final Message request = context.getRequest();

            //validate request
            final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
            final Element headerEl = SoapUtil.getHeaderElement(requestDoc);
            System.out.println(XmlUtil.nodeToFormattedString(headerEl));

            final Element actionEl =
                    XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_ACTION);
            Assert.assertNotNull("Action should have been found", actionEl);
            validateIdAttribute(actionEl);
            validateNamespace(actionEl, namespace);
            Assert.assertEquals("Incorrect element value", action, actionEl.getTextContent());

            final Element msgIdEl =
                    XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_MESSAGE_ID);
            Assert.assertNotNull("MessageId should have been found", msgIdEl);
            validateIdAttribute(msgIdEl);
            validateNamespace(msgIdEl, namespace);
            Assert.assertEquals("Incorrect element value", msgId, msgIdEl.getTextContent());

            final Element destEl =
                    XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_DESTINATION);
            Assert.assertNotNull("Destination should have been found", destEl);
            validateIdAttribute(destEl);
            validateNamespace(destEl, namespace);
            Assert.assertEquals("Incorrect element value", destination, destEl.getTextContent());

            final Element sourceEl =
                    XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_SOURCE_ENDPOINT);
            Assert.assertNotNull("From should have been found", sourceEl);
            validateIdAttribute(sourceEl);
            validateNamespace(sourceEl, namespace);
            Assert.assertEquals("Incorrect element value", ssgHost + "/from", sourceEl.getTextContent());

            final Element replyToEl =
                    XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_REPLY_TO);
            Assert.assertNotNull("ReplyTo should have been found", replyToEl);
            validateIdAttribute(replyToEl);
            validateNamespace(replyToEl, namespace);
            Assert.assertEquals("Incorrect element value", ssgHost + "/reply", replyToEl.getTextContent());

            final Element faultToEl =
                    XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_FAULT_TO);
            Assert.assertNotNull("FaultTo should have been found", faultToEl);
            validateIdAttribute(faultToEl);
            validateNamespace(faultToEl, namespace);
            Assert.assertEquals("Incorrect element value", ssgHost + "/fault", faultToEl.getTextContent());

            final Element relatesToMsgEl =
                    XmlUtil.findExactlyOneChildElementByName(headerEl, namespace, SoapConstants.WSA_MSG_PROP_RELATES_TO);
            Assert.assertNotNull("RelatesTo MsgId should have been found", relatesToMsgEl);
            validateIdAttribute(relatesToMsgEl);
            final String relatesToAttributeValue =
                    relatesToMsgEl.getAttributeNS(namespace, SoapConstants.WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE);
            Assert.assertTrue("Relates to attribute should have been added.", !relatesToAttributeValue.trim().isEmpty());
            Assert.assertEquals("Incorrect relationship type value found", SoapConstants.WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE,
                    relatesToAttributeValue);

            validateNamespace(relatesToMsgEl, namespace);
            Assert.assertEquals("Incorrect element value", relatesToMsgsId, relatesToMsgEl.getTextContent());

            //validate context variables
            testContextVariables(context, assertion, soapAction, true, msgId);
        }
    }

    @BugNumber(9284)
    @Test
    public void testMessageProperties_VariablesDefaultNamespace() throws Exception {
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        final String action = "http://warehouse.acme.com/ws/listProducts";
        final String namespace = SoapConstants.WSA_NAMESPACE_10;

        assertion.setAction(action);
        assertion.setWsaNamespaceUri("${doesnotexist}");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(soapMsg, null);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }

    @Test
    public void testNotSoap() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        final String action = "http://warehouse.acme.com/ws/listProducts";
        assertion.setAction(action);
        assertion.setWsaNamespaceUri(SoapConstants.WSA_NAMESPACE_10);
        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext("<notsoap>nosoaphere</notsoap>", null);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NOT_APPLICABLE", AssertionStatus.NOT_APPLICABLE, status);
    }

    /**
     * Tests that the Action value is extracted correct from the Input element in the WSDL. Also tests that this
     * value is set in the correct context variable.
     * @throws Exception
     */
    @Test
    public void testActionExplicitFromWsdl_Input() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_INPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(wcfWsdl);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        //validate request
        final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
        final Element headerEl = SoapUtil.getHeaderElement(requestDoc);
        System.out.println(XmlUtil.nodeToFormattedString(headerEl));

        //RateRestaurantRequestActionName
        final Element actionEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE, SoapConstants.WSA_MSG_PROP_ACTION);
        Assert.assertNotNull("Action should have been found", actionEl);
        validateNamespace(actionEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE);
        final String expectedAction = "RateRestaurantRequestActionName";
        Assert.assertEquals("Incorrect element value", expectedAction, actionEl.getTextContent());

        //validate context variables
        testContextVariables(context, assertion, expectedAction, false, null);
    }

    @Test
    public void testActionExplicitFromWsdl_Input_FallBackOnSoapAction() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_INPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(wcfWsdl_WsaActionRemoved);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        //validate request
        final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
        final Element headerEl = SoapUtil.getHeaderElement(requestDoc);
        System.out.println(XmlUtil.nodeToFormattedString(headerEl));

        //RateRestaurantRequestActionName
        final Element actionEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE, SoapConstants.WSA_MSG_PROP_ACTION);
        Assert.assertNotNull("Action should have been found", actionEl);
        validateNamespace(actionEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE);
        final String expectedAction = "RateRestaurantRequest";
        Assert.assertEquals("Incorrect element value", expectedAction, actionEl.getTextContent());

        //validate context variables
        testContextVariables(context, assertion, expectedAction, false, null);
    }

    /**
     * Tests that the assertion fails when no soap action is available from the WSDL, and the assertion is configured
     * to obtain the Action property from the WSDL.
     * @throws Exception
     */
    @Test
    public void testActionExplicitFromWsdl_FallBackOnSoapAction_NoneFound() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_INPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(wcfWsdl_AllActionsRemoved);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }
    
    /**
     * Tests that the Action value is extracted correct from the Output element in the WSDL. Also tests that this
     * value is set in the correct context variable.
     * @throws Exception
     */
    @Test
    public void testActionExplicitFromWsdl_Output() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_OUTPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(wcfWsdl);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        //validate request
        final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
        final Element headerEl = SoapUtil.getHeaderElement(requestDoc);
        System.out.println(XmlUtil.nodeToFormattedString(headerEl));

        //RateRestaurantRequestActionName
        final Element actionEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE, SoapConstants.WSA_MSG_PROP_ACTION);
        Assert.assertNotNull("Action should have been found", actionEl);
        validateNamespace(actionEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE);
        final String expectedAction = "RateRestaurantResponse";
        Assert.assertEquals("Incorrect element value", expectedAction, actionEl.getTextContent());

        //validate context variables
        testContextVariables(context, assertion, expectedAction, false, null);
    }

    /**
     * Tests that there is no fall back on the soap:operation soapAction attribute when explicit association from the
     * WSDL Output is configured.
     * 
     * @throws Exception
     */
    @Test
    public void testActionExplicitFromWsdl_Output_NoFallBackOnSoapAction() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_OUTPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(wcfWsdl_WsaActionRemoved);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }

    @Test
    public void testInvalidActionResolved() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction("invalid action");
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }

    @Test
    public void testMessageId_Auto() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction("http://action");
        assertion.setMessageId(AddWsAddressingAssertion.MESSAGE_ID_AUTOMATIC);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        testContextVariables(context, assertion, "http://action", true, null);
    }

    @Test
    public void testMessageId_Explicit() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction("http://action");
        final String msgId = SoapUtil.generateUniqueUri("test", true);
        assertion.setMessageId(msgId);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        testContextVariables(context, assertion, "http://action", true, msgId);
    }

    /**
     * The message id variable should nto be set. Looking it up should cause the exception to be thrown.
     * @throws Exception
     */
    @Test (expected = NoSuchVariableException.class)
    public void testMessageId_Default_Empty() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction("http://action");
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        context.getVariable(assertion.getVariablePrefix() + "." + AddWsAddressingAssertion.SUFFIX_MESSAGE_ID);
    }

    @Test
    public void testInvalidMsgIdResolved() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction("http://action");
        assertion.setMessageId("invalid action");
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }

    /**
     * Tests the documented behaviour that invalid non required properties are just ignored at runtime. Checking
     * the output of this test shoudl verify that the correct INFO statements are logged.
     * @throws Exception
     */
    @Test
    public void testInvalidNonRequiredPropertiesAreIgnored() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction("http://action");
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final String invalidUri = "invalid uri";
        assertion.setSourceEndpoint(invalidUri);
        assertion.setDestination(invalidUri);
        assertion.setReplyEndpoint(invalidUri);
        assertion.setFaultEndpoint(invalidUri);
        assertion.setRelatesToMessageId(invalidUri);
        
        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(restaurantMsg, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);
    }

    /**
     * Test that for a wsdl which defines a namespace prefix of 'http' and there is an wsdl:Input element with
     * an extension WS-Addressing Action attribute containing a URL, that the WSDL does not incorrectly interpret
     * this as a QName.
     *
     * @throws Exception
     */
    @BugNumber(9646)
    @Test
    public void testActionExplicitFromWsdl_ExtensionAttributeCorrectlyHandled_Input() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_INPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(fauldLabMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(fraudLabsWsdl);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        final String expectedAction = "http://ws.fraudlabs.com/MailBoxValidator";
        validateActionElement(request, expectedAction);
        //validate context variables
        testContextVariables(context, assertion, expectedAction, false, null);
    }

    /**
     * Test that for a wsdl which defines a namespace prefix of 'http' and there is an wsdl:Output element with
     * an extension WS-Addressing Action attribute containing a URL, that the WSDL does not incorrectly interpret
     * this as a QName.
     *
     * @throws Exception
     */
    @BugNumber(9646)
    @Test
    public void testActionExplicitFromWsdl_ExtensionAttributeCorrectlyHandled_Output() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_OUTPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(fauldLabMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(fraudLabsWsdl);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        final String expectedAction = "http://ws.fraudlabs.com/MailBoxValidatorResponse";
        validateActionElement(request, expectedAction);
        //validate context variables
        testContextVariables(context, assertion, expectedAction, false, null);
    }

    /**
     * Adding test coverage for the case when a wsaw:Action attribute is defined, and the value of the wswa namespace
     * is not the default value. Testing the logic to fallback and try the other known ws addressing wsdl namespaces.
     *
     * @throws Exception
     */
    @Test
    public void testActionExplicitFromWsdl_ExtensionAttributeCorrectlyHandled_NonDefaultNamespace() throws Exception{
        
        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();

        assertion.setAction(AddWsAddressingAssertion.ACTION_EXPLICIT_FROM_WSDL_INPUT);
        assertion.setWsaNamespaceUri("http://www.w3.org/2005/08/addressing");

        final ServerAddWsAddressingAssertion serverAssertion =
                new ServerAddWsAddressingAssertion(assertion);

        final PolicyEnforcementContext context = getContext(fauldLabMsg, null);
        PublishedService srvc = new PublishedService();
        srvc.setWsdlXml(fraudLabsWsdlWsAddressingNonDefaultNamespace);
        ServiceDocumentWsdlStrategy strategy = new ServiceDocumentWsdlStrategy(Collections.<ServiceDocument>emptySet());
        srvc.parseWsdlStrategy(strategy);
        context.setService(srvc);

        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);

        final Message request = context.getRequest();

        final String expectedAction = "http://ws.fraudlabs.com/MailBoxValidator";
        validateActionElement(request, expectedAction);
        //validate context variables
        testContextVariables(context, assertion, expectedAction, false, null);
    }

    // - PRIVATE

    private void validateActionElement(Message request, String expectedAction) throws Exception{
        final Document requestDoc = request.getXmlKnob().getDocumentReadOnly();
        final Element headerEl = SoapUtil.getHeaderElement(requestDoc);
        System.out.println(XmlUtil.nodeToFormattedString(headerEl));

        final Element actionEl =
                XmlUtil.findExactlyOneChildElementByName(headerEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE, SoapConstants.WSA_MSG_PROP_ACTION);
        Assert.assertNotNull("Action should have been found", actionEl);
        validateNamespace(actionEl, AddWsAddressingAssertion.DEFAULT_NAMESPACE);
        Assert.assertEquals("Incorrect element value", expectedAction, actionEl.getTextContent());
    }
    
    private PolicyEnforcementContext getContext(String messageContent, final String soapAction) throws IOException, SAXException {

        Message request = new Message(XmlUtil.parse(messageContent));
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod(HttpMethod.POST.toString());
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest){
            @Override
            public String getSoapAction() throws IOException {
                if(soapAction != null){
                    return soapAction;
                }
                return super.getSoapAction();
            }
        });
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        return policyEnforcementContext;

    }

    /**
     *
     * @param context
     * @param assertion
     * @param expectedAction
     * @param expectMessageIdVar
     *@param expectedMsgId if null it means a message id should have been automatically created.  @throws Exception
     */
    private void testContextVariables(final PolicyEnforcementContext context,
                                      final AddWsAddressingAssertion assertion,
                                      final String expectedAction,
                                      final boolean expectMessageIdVar, 
                                      final String expectedMsgId) throws Exception{
        final Object actionVar = context.getVariable(assertion.getVariablePrefix() + "." + AddWsAddressingAssertion.SUFFIX_ACTION);
        Assert.assertNotNull("Variable " + assertion.getVariablePrefix() + "." +
                AddWsAddressingAssertion.SUFFIX_ACTION + " should be found", actionVar);

        Assert.assertEquals("Incorrect context variable value found", expectedAction, actionVar);

        if(expectMessageIdVar){
            final Object msgIdVar = context.getVariable(assertion.getVariablePrefix() + "." + AddWsAddressingAssertion.SUFFIX_MESSAGE_ID);
            Assert.assertNotNull("Variable " + assertion.getVariablePrefix() + "." +
                    AddWsAddressingAssertion.SUFFIX_MESSAGE_ID + " should be found", msgIdVar);

            if(expectedMsgId != null){
                Assert.assertEquals("Incorrect context variable value found", expectedMsgId, msgIdVar);
            }
        }
    }
    
    private void validateNamespace(Element element, String namespace){
        final String foundNs = element.getNamespaceURI();
        Assert.assertEquals("Incorrect namespace found", namespace, foundNs);
    }

    private void validateIdAttribute(Element element) {
        final String actionId = element.getAttributeNS(SoapConstants.WSU_NAMESPACE, "Id");
        Assert.assertTrue("Id attribute should have been added.", !actionId.trim().isEmpty());
    }
    
    private static final String soapMsg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <s:Header/>\n" +
            "    <s:Body>\n" +
            "        <tns:listProducts xmlns:tns=\"http://warehouse.acme.com/ws\">\n" +
            "            <tns:delay>10</tns:delay>\n" +
            "        </tns:listProducts>\n" +
            "    </s:Body>\n" +
            "</s:Envelope>";

    private static final String restaurantMsg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<s:Envelope xmlns:s=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <s:Header/>\n" +
            "    <s:Body>\n" +
            "        <tns:RateRestaurant xmlns:tns=\"http://www.thinktecture.com/services/restaurant/2006/12\">\n" +
            "            <tns:message>RateRestaurantMessage</tns:message>\n" +
            "        </tns:RateRestaurant>\n" +
            "    </s:Body>\n" +
            "</s:Envelope>";
    
    private static final String warehouseResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <soap:Body>\n" +
            "        <listProductsResponse xmlns=\"http://warehouse.acme.com/ws\">\n" +
            "            <listProductsResult>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>White board</productName>\n" +
            "                    <productId>111111113</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>Wall Clock</productName>\n" +
            "                    <productId>111111112</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>PhoneBook</productName>\n" +
            "                    <productId>111111111</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>Civic</productName>\n" +
            "                    <productId>111111119</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>BMW</productName>\n" +
            "                    <productId>111111118</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>Camcorder</productName>\n" +
            "                    <productId>111111117</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>Plasma TV</productName>\n" +
            "                    <productId>111111116</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>Speakers</productName>\n" +
            "                    <productId>111111115</productId>\n" +
            "                </ProductListHeader>\n" +
            "                <ProductListHeader>\n" +
            "                    <productName>Digital camera</productName>\n" +
            "                    <productId>111111114</productId>\n" +
            "                </ProductListHeader>\n" +
            "            </listProductsResult>\n" +
            "        </listProductsResponse>\n" +
            "    </soap:Body>\n" +
            "</soap:Envelope>";

    private static final String wcfWsdl_WsaActionRemoved = "<wsdl:definitions name=\"RestaurantService\" targetNamespace=\"http://www.thinktecture.com/services/restaurant/2006/12\" xmlns:msc=\"http://schemas.microsoft.com/ws/2005/12/wsdl/contract\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tns=\"http://www.thinktecture.com/services/restaurant/2006/12\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsa10=\"http://www.w3.org/2005/08/addressing\" xmlns:wsam=\"http://www.w3.org/2007/05/addressing/metadata\" xmlns:wsap=\"http://schemas.xmlsoap.org/ws/2004/08/addressing/policy\" xmlns:wsaw=\"http://www.w3.org/2006/05/addressing/wsdl\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:wsx=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <wsp:Policy wsu:Id=\"WSHttpBinding_RestaurantService_policy\">\n" +
            "        <wsp:ExactlyOne>\n" +
            "            <wsp:All>\n" +
            "                <wsaw:UsingAddressing/>\n" +
            "            </wsp:All>\n" +
            "        </wsp:ExactlyOne>\n" +
            "    </wsp:Policy>\n" +
            "    <wsdl:types>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.thinktecture.com/services/restaurant/2006/12\">\n" +
            "            <xsd:element name=\"RateRestaurant\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"message\" nillable=\"true\" type=\"q1:RateRestaurantMessage\" xmlns:q1=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"RateRestaurantResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence/>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"ListRestaurants\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"message\" nillable=\"true\" type=\"q2:GetRestaurantsRequest\" xmlns:q2=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"ListRestaurantsResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"ListRestaurantsResult\" nillable=\"true\" type=\"q3:GetRestaurantsResponse\" xmlns:q3=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"GetMenu\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence/>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"GetMenuResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"GetMenuResult\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "        </xsd:schema>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\" xmlns:tns=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\">\n" +
            "            <xsd:complexType name=\"RateRestaurantMessage\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurantID\" type=\"xsd:int\"/>\n" +
            "                    <xsd:element name=\"rate\" type=\"q4:RatingInfo\" xmlns:q4=\"urn:thinktecture-com:demos:restaurantservice:data:v1\"/>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RateRestaurantMessage\" nillable=\"true\" type=\"tns:RateRestaurantMessage\"/>\n" +
            "            <xsd:complexType name=\"GetRestaurantsRequest\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"zip\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"GetRestaurantsRequest\" nillable=\"true\" type=\"tns:GetRestaurantsRequest\"/>\n" +
            "            <xsd:complexType name=\"GetRestaurantsResponse\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurants\" nillable=\"true\" type=\"q5:RestaurantsList\" xmlns:q5=\"urn:thinktecture-com:demos:restaurantservice:data:v1\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"GetRestaurantsResponse\" nillable=\"true\" type=\"tns:GetRestaurantsResponse\"/>\n" +
            "        </xsd:schema>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"urn:thinktecture-com:demos:restaurantservice:data:v1\" xmlns:tns=\"urn:thinktecture-com:demos:restaurantservice:data:v1\">\n" +
            "            <xsd:simpleType name=\"RatingInfo\">\n" +
            "                <xsd:restriction base=\"xsd:string\">\n" +
            "                    <xsd:enumeration value=\"poor\"/>\n" +
            "                    <xsd:enumeration value=\"good\"/>\n" +
            "                    <xsd:enumeration value=\"veryGood\"/>\n" +
            "                    <xsd:enumeration value=\"excellent\"/>\n" +
            "                </xsd:restriction>\n" +
            "            </xsd:simpleType>\n" +
            "            <xsd:element name=\"RatingInfo\" nillable=\"true\" type=\"tns:RatingInfo\"/>\n" +
            "            <xsd:complexType name=\"RestaurantsList\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"restaurant\" nillable=\"true\" type=\"tns:RestaurantInfo\"/>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RestaurantsList\" nillable=\"true\" type=\"tns:RestaurantsList\"/>\n" +
            "            <xsd:complexType name=\"RestaurantInfo\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurantID\" type=\"xsd:int\"/>\n" +
            "                    <xsd:element name=\"name\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"address\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"city\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"state\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"zip\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"openFrom\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"openTo\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RestaurantInfo\" nillable=\"true\" type=\"tns:RestaurantInfo\"/>\n" +
            "        </xsd:schema>\n" +
            "        <xs:schema attributeFormDefault=\"qualified\" elementFormDefault=\"qualified\" targetNamespace=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:tns=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "            <xs:element name=\"anyType\" nillable=\"true\" type=\"xs:anyType\"/>\n" +
            "            <xs:element name=\"anyURI\" nillable=\"true\" type=\"xs:anyURI\"/>\n" +
            "            <xs:element name=\"base64Binary\" nillable=\"true\" type=\"xs:base64Binary\"/>\n" +
            "            <xs:element name=\"boolean\" nillable=\"true\" type=\"xs:boolean\"/>\n" +
            "            <xs:element name=\"byte\" nillable=\"true\" type=\"xs:byte\"/>\n" +
            "            <xs:element name=\"dateTime\" nillable=\"true\" type=\"xs:dateTime\"/>\n" +
            "            <xs:element name=\"decimal\" nillable=\"true\" type=\"xs:decimal\"/>\n" +
            "            <xs:element name=\"double\" nillable=\"true\" type=\"xs:double\"/>\n" +
            "            <xs:element name=\"float\" nillable=\"true\" type=\"xs:float\"/>\n" +
            "            <xs:element name=\"int\" nillable=\"true\" type=\"xs:int\"/>\n" +
            "            <xs:element name=\"long\" nillable=\"true\" type=\"xs:long\"/>\n" +
            "            <xs:element name=\"QName\" nillable=\"true\" type=\"xs:QName\"/>\n" +
            "            <xs:element name=\"short\" nillable=\"true\" type=\"xs:short\"/>\n" +
            "            <xs:element name=\"string\" nillable=\"true\" type=\"xs:string\"/>\n" +
            "            <xs:element name=\"unsignedByte\" nillable=\"true\" type=\"xs:unsignedByte\"/>\n" +
            "            <xs:element name=\"unsignedInt\" nillable=\"true\" type=\"xs:unsignedInt\"/>\n" +
            "            <xs:element name=\"unsignedLong\" nillable=\"true\" type=\"xs:unsignedLong\"/>\n" +
            "            <xs:element name=\"unsignedShort\" nillable=\"true\" type=\"xs:unsignedShort\"/>\n" +
            "            <xs:element name=\"char\" nillable=\"true\" type=\"tns:char\"/>\n" +
            "            <xs:simpleType name=\"char\">\n" +
            "                <xs:restriction base=\"xs:int\"/>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:element name=\"duration\" nillable=\"true\" type=\"tns:duration\"/>\n" +
            "            <xs:simpleType name=\"duration\">\n" +
            "                <xs:restriction base=\"xs:duration\">\n" +
            "                    <xs:pattern value=\"\\-?P(\\d*D)?(T(\\d*H)?(\\d*M)?(\\d*(\\.\\d*)?S)?)?\"/>\n" +
            "                    <xs:minInclusive value=\"-P10675199DT2H48M5.4775808S\"/>\n" +
            "                    <xs:maxInclusive value=\"P10675199DT2H48M5.4775807S\"/>\n" +
            "                </xs:restriction>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:element name=\"guid\" nillable=\"true\" type=\"tns:guid\"/>\n" +
            "            <xs:simpleType name=\"guid\">\n" +
            "                <xs:restriction base=\"xs:string\">\n" +
            "                    <xs:pattern value=\"[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}\"/>\n" +
            "                </xs:restriction>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:attribute name=\"FactoryType\" type=\"xs:QName\"/>\n" +
            "            <xs:attribute name=\"Id\" type=\"xs:ID\"/>\n" +
            "            <xs:attribute name=\"Ref\" type=\"xs:IDREF\"/>\n" +
            "        </xs:schema>\n" +
            "    </wsdl:types>\n" +
            "    <wsdl:message name=\"RestaurantService_RateRestaurant_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:RateRestaurant\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_RateRestaurant_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:RateRestaurantResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_ListRestaurants_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:ListRestaurants\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_ListRestaurants_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:ListRestaurantsResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_GetMenu_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:GetMenu\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_GetMenu_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:GetMenuResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:portType name=\"RestaurantService\">\n" +
            "        <wsdl:operation name=\"RateRestaurant\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_RateRestaurant_InputMessage\" />\n" +
            "            <wsdl:output message=\"tns:RestaurantService_RateRestaurant_OutputMessage\" />\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"ListRestaurants\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_ListRestaurants_InputMessage\" />\n" +
            "            <wsdl:output message=\"tns:RestaurantService_ListRestaurants_OutputMessage\" />\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetMenu\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_GetMenu_InputMessage\" />\n" +
            "            <wsdl:output message=\"tns:RestaurantService_GetMenu_OutputMessage\" />\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:binding name=\"WSHttpBinding_RestaurantService\" type=\"tns:RestaurantService\">\n" +
            "        <wsp:PolicyReference URI=\"#WSHttpBinding_RestaurantService_policy\"/>\n" +
            "        <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <wsdl:operation name=\"RateRestaurant\">\n" +
            "            <soap12:operation soapAction=\"RateRestaurantRequest\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"ListRestaurants\">\n" +
            "            <soap12:operation soapAction=\"ListRestaurantsRequest\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetMenu\">\n" +
            "            <soap12:operation soapAction=\"GetMenuRequest\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:service name=\"RestaurantService\">\n" +
            "        <wsdl:port binding=\"tns:WSHttpBinding_RestaurantService\" name=\"WSHttpBinding_RestaurantService\">\n" +
            "            <soap12:address location=\"http://hugh.qawin2003.com/FlatWsdl12/Service.svc/RS\"/>\n" +
            "            <wsa10:EndpointReference>\n" +
            "                <wsa10:Address>http://hugh.qawin2003.com/FlatWsdl12/Service.svc/RS</wsa10:Address>\n" +
            "            </wsa10:EndpointReference>\n" +
            "        </wsdl:port>\n" +
            "    </wsdl:service>\n" +
            "</wsdl:definitions>";

    private static final String wcfWsdl_AllActionsRemoved = "<wsdl:definitions name=\"RestaurantService\" targetNamespace=\"http://www.thinktecture.com/services/restaurant/2006/12\" xmlns:msc=\"http://schemas.microsoft.com/ws/2005/12/wsdl/contract\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tns=\"http://www.thinktecture.com/services/restaurant/2006/12\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsa10=\"http://www.w3.org/2005/08/addressing\" xmlns:wsam=\"http://www.w3.org/2007/05/addressing/metadata\" xmlns:wsap=\"http://schemas.xmlsoap.org/ws/2004/08/addressing/policy\" xmlns:wsaw=\"http://www.w3.org/2006/05/addressing/wsdl\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:wsx=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <wsp:Policy wsu:Id=\"WSHttpBinding_RestaurantService_policy\">\n" +
            "        <wsp:ExactlyOne>\n" +
            "            <wsp:All>\n" +
            "                <wsaw:UsingAddressing/>\n" +
            "            </wsp:All>\n" +
            "        </wsp:ExactlyOne>\n" +
            "    </wsp:Policy>\n" +
            "    <wsdl:types>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.thinktecture.com/services/restaurant/2006/12\">\n" +
            "            <xsd:element name=\"RateRestaurant\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"message\" nillable=\"true\" type=\"q1:RateRestaurantMessage\" xmlns:q1=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"RateRestaurantResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence/>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"ListRestaurants\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"message\" nillable=\"true\" type=\"q2:GetRestaurantsRequest\" xmlns:q2=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"ListRestaurantsResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"ListRestaurantsResult\" nillable=\"true\" type=\"q3:GetRestaurantsResponse\" xmlns:q3=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"GetMenu\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence/>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"GetMenuResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"GetMenuResult\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "        </xsd:schema>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\" xmlns:tns=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\">\n" +
            "            <xsd:complexType name=\"RateRestaurantMessage\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurantID\" type=\"xsd:int\"/>\n" +
            "                    <xsd:element name=\"rate\" type=\"q4:RatingInfo\" xmlns:q4=\"urn:thinktecture-com:demos:restaurantservice:data:v1\"/>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RateRestaurantMessage\" nillable=\"true\" type=\"tns:RateRestaurantMessage\"/>\n" +
            "            <xsd:complexType name=\"GetRestaurantsRequest\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"zip\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"GetRestaurantsRequest\" nillable=\"true\" type=\"tns:GetRestaurantsRequest\"/>\n" +
            "            <xsd:complexType name=\"GetRestaurantsResponse\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurants\" nillable=\"true\" type=\"q5:RestaurantsList\" xmlns:q5=\"urn:thinktecture-com:demos:restaurantservice:data:v1\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"GetRestaurantsResponse\" nillable=\"true\" type=\"tns:GetRestaurantsResponse\"/>\n" +
            "        </xsd:schema>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"urn:thinktecture-com:demos:restaurantservice:data:v1\" xmlns:tns=\"urn:thinktecture-com:demos:restaurantservice:data:v1\">\n" +
            "            <xsd:simpleType name=\"RatingInfo\">\n" +
            "                <xsd:restriction base=\"xsd:string\">\n" +
            "                    <xsd:enumeration value=\"poor\"/>\n" +
            "                    <xsd:enumeration value=\"good\"/>\n" +
            "                    <xsd:enumeration value=\"veryGood\"/>\n" +
            "                    <xsd:enumeration value=\"excellent\"/>\n" +
            "                </xsd:restriction>\n" +
            "            </xsd:simpleType>\n" +
            "            <xsd:element name=\"RatingInfo\" nillable=\"true\" type=\"tns:RatingInfo\"/>\n" +
            "            <xsd:complexType name=\"RestaurantsList\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"restaurant\" nillable=\"true\" type=\"tns:RestaurantInfo\"/>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RestaurantsList\" nillable=\"true\" type=\"tns:RestaurantsList\"/>\n" +
            "            <xsd:complexType name=\"RestaurantInfo\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurantID\" type=\"xsd:int\"/>\n" +
            "                    <xsd:element name=\"name\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"address\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"city\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"state\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"zip\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"openFrom\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"openTo\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RestaurantInfo\" nillable=\"true\" type=\"tns:RestaurantInfo\"/>\n" +
            "        </xsd:schema>\n" +
            "        <xs:schema attributeFormDefault=\"qualified\" elementFormDefault=\"qualified\" targetNamespace=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:tns=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "            <xs:element name=\"anyType\" nillable=\"true\" type=\"xs:anyType\"/>\n" +
            "            <xs:element name=\"anyURI\" nillable=\"true\" type=\"xs:anyURI\"/>\n" +
            "            <xs:element name=\"base64Binary\" nillable=\"true\" type=\"xs:base64Binary\"/>\n" +
            "            <xs:element name=\"boolean\" nillable=\"true\" type=\"xs:boolean\"/>\n" +
            "            <xs:element name=\"byte\" nillable=\"true\" type=\"xs:byte\"/>\n" +
            "            <xs:element name=\"dateTime\" nillable=\"true\" type=\"xs:dateTime\"/>\n" +
            "            <xs:element name=\"decimal\" nillable=\"true\" type=\"xs:decimal\"/>\n" +
            "            <xs:element name=\"double\" nillable=\"true\" type=\"xs:double\"/>\n" +
            "            <xs:element name=\"float\" nillable=\"true\" type=\"xs:float\"/>\n" +
            "            <xs:element name=\"int\" nillable=\"true\" type=\"xs:int\"/>\n" +
            "            <xs:element name=\"long\" nillable=\"true\" type=\"xs:long\"/>\n" +
            "            <xs:element name=\"QName\" nillable=\"true\" type=\"xs:QName\"/>\n" +
            "            <xs:element name=\"short\" nillable=\"true\" type=\"xs:short\"/>\n" +
            "            <xs:element name=\"string\" nillable=\"true\" type=\"xs:string\"/>\n" +
            "            <xs:element name=\"unsignedByte\" nillable=\"true\" type=\"xs:unsignedByte\"/>\n" +
            "            <xs:element name=\"unsignedInt\" nillable=\"true\" type=\"xs:unsignedInt\"/>\n" +
            "            <xs:element name=\"unsignedLong\" nillable=\"true\" type=\"xs:unsignedLong\"/>\n" +
            "            <xs:element name=\"unsignedShort\" nillable=\"true\" type=\"xs:unsignedShort\"/>\n" +
            "            <xs:element name=\"char\" nillable=\"true\" type=\"tns:char\"/>\n" +
            "            <xs:simpleType name=\"char\">\n" +
            "                <xs:restriction base=\"xs:int\"/>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:element name=\"duration\" nillable=\"true\" type=\"tns:duration\"/>\n" +
            "            <xs:simpleType name=\"duration\">\n" +
            "                <xs:restriction base=\"xs:duration\">\n" +
            "                    <xs:pattern value=\"\\-?P(\\d*D)?(T(\\d*H)?(\\d*M)?(\\d*(\\.\\d*)?S)?)?\"/>\n" +
            "                    <xs:minInclusive value=\"-P10675199DT2H48M5.4775808S\"/>\n" +
            "                    <xs:maxInclusive value=\"P10675199DT2H48M5.4775807S\"/>\n" +
            "                </xs:restriction>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:element name=\"guid\" nillable=\"true\" type=\"tns:guid\"/>\n" +
            "            <xs:simpleType name=\"guid\">\n" +
            "                <xs:restriction base=\"xs:string\">\n" +
            "                    <xs:pattern value=\"[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}\"/>\n" +
            "                </xs:restriction>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:attribute name=\"FactoryType\" type=\"xs:QName\"/>\n" +
            "            <xs:attribute name=\"Id\" type=\"xs:ID\"/>\n" +
            "            <xs:attribute name=\"Ref\" type=\"xs:IDREF\"/>\n" +
            "        </xs:schema>\n" +
            "    </wsdl:types>\n" +
            "    <wsdl:message name=\"RestaurantService_RateRestaurant_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:RateRestaurant\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_RateRestaurant_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:RateRestaurantResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_ListRestaurants_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:ListRestaurants\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_ListRestaurants_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:ListRestaurantsResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_GetMenu_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:GetMenu\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_GetMenu_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:GetMenuResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:portType name=\"RestaurantService\">\n" +
            "        <wsdl:operation name=\"RateRestaurant\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_RateRestaurant_InputMessage\" />\n" +
            "            <wsdl:output message=\"tns:RestaurantService_RateRestaurant_OutputMessage\" />\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"ListRestaurants\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_ListRestaurants_InputMessage\" />\n" +
            "            <wsdl:output message=\"tns:RestaurantService_ListRestaurants_OutputMessage\" />\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetMenu\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_GetMenu_InputMessage\" />\n" +
            "            <wsdl:output message=\"tns:RestaurantService_GetMenu_OutputMessage\" />\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:binding name=\"WSHttpBinding_RestaurantService\" type=\"tns:RestaurantService\">\n" +
            "        <wsp:PolicyReference URI=\"#WSHttpBinding_RestaurantService_policy\"/>\n" +
            "        <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <wsdl:operation name=\"RateRestaurant\">\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"ListRestaurants\">\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetMenu\">\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:service name=\"RestaurantService\">\n" +
            "        <wsdl:port binding=\"tns:WSHttpBinding_RestaurantService\" name=\"WSHttpBinding_RestaurantService\">\n" +
            "            <soap12:address location=\"http://hugh.qawin2003.com/FlatWsdl12/Service.svc/RS\"/>\n" +
            "            <wsa10:EndpointReference>\n" +
            "                <wsa10:Address>http://hugh.qawin2003.com/FlatWsdl12/Service.svc/RS</wsa10:Address>\n" +
            "            </wsa10:EndpointReference>\n" +
            "        </wsdl:port>\n" +
            "    </wsdl:service>\n" +
            "</wsdl:definitions>";

    private static final String wcfWsdl = "<wsdl:definitions name=\"RestaurantService\" targetNamespace=\"http://www.thinktecture.com/services/restaurant/2006/12\" xmlns:msc=\"http://schemas.microsoft.com/ws/2005/12/wsdl/contract\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tns=\"http://www.thinktecture.com/services/restaurant/2006/12\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wsa10=\"http://www.w3.org/2005/08/addressing\" xmlns:wsam=\"http://www.w3.org/2007/05/addressing/metadata\" xmlns:wsap=\"http://schemas.xmlsoap.org/ws/2004/08/addressing/policy\" xmlns:wsaw=\"http://www.w3.org/2006/05/addressing/wsdl\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:wsx=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <wsp:Policy wsu:Id=\"WSHttpBinding_RestaurantService_policy\">\n" +
            "        <wsp:ExactlyOne>\n" +
            "            <wsp:All>\n" +
            "                <wsaw:UsingAddressing/>\n" +
            "            </wsp:All>\n" +
            "        </wsp:ExactlyOne>\n" +
            "    </wsp:Policy>\n" +
            "    <wsdl:types>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.thinktecture.com/services/restaurant/2006/12\">\n" +
            "            <xsd:element name=\"RateRestaurant\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"message\" nillable=\"true\" type=\"q1:RateRestaurantMessage\" xmlns:q1=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"RateRestaurantResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence/>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"ListRestaurants\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"message\" nillable=\"true\" type=\"q2:GetRestaurantsRequest\" xmlns:q2=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"ListRestaurantsResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"ListRestaurantsResult\" nillable=\"true\" type=\"q3:GetRestaurantsResponse\" xmlns:q3=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"GetMenu\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence/>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "            <xsd:element name=\"GetMenuResponse\">\n" +
            "                <xsd:complexType>\n" +
            "                    <xsd:sequence>\n" +
            "                        <xsd:element minOccurs=\"0\" name=\"GetMenuResult\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "                    </xsd:sequence>\n" +
            "                </xsd:complexType>\n" +
            "            </xsd:element>\n" +
            "        </xsd:schema>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\" xmlns:tns=\"urn:thinktecture-com:demos:restaurantservice:messages:v1\">\n" +
            "            <xsd:complexType name=\"RateRestaurantMessage\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurantID\" type=\"xsd:int\"/>\n" +
            "                    <xsd:element name=\"rate\" type=\"q4:RatingInfo\" xmlns:q4=\"urn:thinktecture-com:demos:restaurantservice:data:v1\"/>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RateRestaurantMessage\" nillable=\"true\" type=\"tns:RateRestaurantMessage\"/>\n" +
            "            <xsd:complexType name=\"GetRestaurantsRequest\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"zip\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"GetRestaurantsRequest\" nillable=\"true\" type=\"tns:GetRestaurantsRequest\"/>\n" +
            "            <xsd:complexType name=\"GetRestaurantsResponse\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurants\" nillable=\"true\" type=\"q5:RestaurantsList\" xmlns:q5=\"urn:thinktecture-com:demos:restaurantservice:data:v1\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"GetRestaurantsResponse\" nillable=\"true\" type=\"tns:GetRestaurantsResponse\"/>\n" +
            "        </xsd:schema>\n" +
            "        <xsd:schema elementFormDefault=\"qualified\" targetNamespace=\"urn:thinktecture-com:demos:restaurantservice:data:v1\" xmlns:tns=\"urn:thinktecture-com:demos:restaurantservice:data:v1\">\n" +
            "            <xsd:simpleType name=\"RatingInfo\">\n" +
            "                <xsd:restriction base=\"xsd:string\">\n" +
            "                    <xsd:enumeration value=\"poor\"/>\n" +
            "                    <xsd:enumeration value=\"good\"/>\n" +
            "                    <xsd:enumeration value=\"veryGood\"/>\n" +
            "                    <xsd:enumeration value=\"excellent\"/>\n" +
            "                </xsd:restriction>\n" +
            "            </xsd:simpleType>\n" +
            "            <xsd:element name=\"RatingInfo\" nillable=\"true\" type=\"tns:RatingInfo\"/>\n" +
            "            <xsd:complexType name=\"RestaurantsList\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"restaurant\" nillable=\"true\" type=\"tns:RestaurantInfo\"/>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RestaurantsList\" nillable=\"true\" type=\"tns:RestaurantsList\"/>\n" +
            "            <xsd:complexType name=\"RestaurantInfo\">\n" +
            "                <xsd:sequence>\n" +
            "                    <xsd:element name=\"restaurantID\" type=\"xsd:int\"/>\n" +
            "                    <xsd:element name=\"name\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"address\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"city\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"state\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"zip\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"openFrom\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                    <xsd:element name=\"openTo\" nillable=\"true\" type=\"xsd:string\">\n" +
            "                        <xsd:annotation>\n" +
            "                            <xsd:appinfo>\n" +
            "                                <DefaultValue EmitDefaultValue=\"false\" xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\"/>\n" +
            "                            </xsd:appinfo>\n" +
            "                        </xsd:annotation>\n" +
            "                    </xsd:element>\n" +
            "                </xsd:sequence>\n" +
            "            </xsd:complexType>\n" +
            "            <xsd:element name=\"RestaurantInfo\" nillable=\"true\" type=\"tns:RestaurantInfo\"/>\n" +
            "        </xsd:schema>\n" +
            "        <xs:schema attributeFormDefault=\"qualified\" elementFormDefault=\"qualified\" targetNamespace=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:tns=\"http://schemas.microsoft.com/2003/10/Serialization/\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "            <xs:element name=\"anyType\" nillable=\"true\" type=\"xs:anyType\"/>\n" +
            "            <xs:element name=\"anyURI\" nillable=\"true\" type=\"xs:anyURI\"/>\n" +
            "            <xs:element name=\"base64Binary\" nillable=\"true\" type=\"xs:base64Binary\"/>\n" +
            "            <xs:element name=\"boolean\" nillable=\"true\" type=\"xs:boolean\"/>\n" +
            "            <xs:element name=\"byte\" nillable=\"true\" type=\"xs:byte\"/>\n" +
            "            <xs:element name=\"dateTime\" nillable=\"true\" type=\"xs:dateTime\"/>\n" +
            "            <xs:element name=\"decimal\" nillable=\"true\" type=\"xs:decimal\"/>\n" +
            "            <xs:element name=\"double\" nillable=\"true\" type=\"xs:double\"/>\n" +
            "            <xs:element name=\"float\" nillable=\"true\" type=\"xs:float\"/>\n" +
            "            <xs:element name=\"int\" nillable=\"true\" type=\"xs:int\"/>\n" +
            "            <xs:element name=\"long\" nillable=\"true\" type=\"xs:long\"/>\n" +
            "            <xs:element name=\"QName\" nillable=\"true\" type=\"xs:QName\"/>\n" +
            "            <xs:element name=\"short\" nillable=\"true\" type=\"xs:short\"/>\n" +
            "            <xs:element name=\"string\" nillable=\"true\" type=\"xs:string\"/>\n" +
            "            <xs:element name=\"unsignedByte\" nillable=\"true\" type=\"xs:unsignedByte\"/>\n" +
            "            <xs:element name=\"unsignedInt\" nillable=\"true\" type=\"xs:unsignedInt\"/>\n" +
            "            <xs:element name=\"unsignedLong\" nillable=\"true\" type=\"xs:unsignedLong\"/>\n" +
            "            <xs:element name=\"unsignedShort\" nillable=\"true\" type=\"xs:unsignedShort\"/>\n" +
            "            <xs:element name=\"char\" nillable=\"true\" type=\"tns:char\"/>\n" +
            "            <xs:simpleType name=\"char\">\n" +
            "                <xs:restriction base=\"xs:int\"/>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:element name=\"duration\" nillable=\"true\" type=\"tns:duration\"/>\n" +
            "            <xs:simpleType name=\"duration\">\n" +
            "                <xs:restriction base=\"xs:duration\">\n" +
            "                    <xs:pattern value=\"\\-?P(\\d*D)?(T(\\d*H)?(\\d*M)?(\\d*(\\.\\d*)?S)?)?\"/>\n" +
            "                    <xs:minInclusive value=\"-P10675199DT2H48M5.4775808S\"/>\n" +
            "                    <xs:maxInclusive value=\"P10675199DT2H48M5.4775807S\"/>\n" +
            "                </xs:restriction>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:element name=\"guid\" nillable=\"true\" type=\"tns:guid\"/>\n" +
            "            <xs:simpleType name=\"guid\">\n" +
            "                <xs:restriction base=\"xs:string\">\n" +
            "                    <xs:pattern value=\"[\\da-fA-F]{8}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{4}-[\\da-fA-F]{12}\"/>\n" +
            "                </xs:restriction>\n" +
            "            </xs:simpleType>\n" +
            "            <xs:attribute name=\"FactoryType\" type=\"xs:QName\"/>\n" +
            "            <xs:attribute name=\"Id\" type=\"xs:ID\"/>\n" +
            "            <xs:attribute name=\"Ref\" type=\"xs:IDREF\"/>\n" +
            "        </xs:schema>\n" +
            "    </wsdl:types>\n" +
            "    <wsdl:message name=\"RestaurantService_RateRestaurant_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:RateRestaurant\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_RateRestaurant_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:RateRestaurantResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_ListRestaurants_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:ListRestaurants\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_ListRestaurants_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:ListRestaurantsResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_GetMenu_InputMessage\">\n" +
            "        <wsdl:part element=\"tns:GetMenu\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"RestaurantService_GetMenu_OutputMessage\">\n" +
            "        <wsdl:part element=\"tns:GetMenuResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:portType name=\"RestaurantService\">\n" +
            "        <wsdl:operation name=\"RateRestaurant\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_RateRestaurant_InputMessage\" wsaw:Action=\"RateRestaurantRequestActionName\"/>\n" +
            "            <wsdl:output message=\"tns:RestaurantService_RateRestaurant_OutputMessage\" wsaw:Action=\"RateRestaurantResponse\"/>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"ListRestaurants\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_ListRestaurants_InputMessage\" wsaw:Action=\"ListRestaurantsRequest\"/>\n" +
            "            <wsdl:output message=\"tns:RestaurantService_ListRestaurants_OutputMessage\" wsaw:Action=\"ListRestaurantsResponse\"/>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetMenu\">\n" +
            "            <wsdl:input message=\"tns:RestaurantService_GetMenu_InputMessage\" wsaw:Action=\"GetMenuRequest\"/>\n" +
            "            <wsdl:output message=\"tns:RestaurantService_GetMenu_OutputMessage\" wsaw:Action=\"GetMenuResponse\"/>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:binding name=\"WSHttpBinding_RestaurantService\" type=\"tns:RestaurantService\">\n" +
            "        <wsp:PolicyReference URI=\"#WSHttpBinding_RestaurantService_policy\"/>\n" +
            "        <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <wsdl:operation name=\"RateRestaurant\">\n" +
            "            <soap12:operation soapAction=\"RateRestaurantRequest\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"ListRestaurants\">\n" +
            "            <soap12:operation soapAction=\"ListRestaurantsRequest\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "        <wsdl:operation name=\"GetMenu\">\n" +
            "            <soap12:operation soapAction=\"GetMenuRequest\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:service name=\"RestaurantService\">\n" +
            "        <wsdl:port binding=\"tns:WSHttpBinding_RestaurantService\" name=\"WSHttpBinding_RestaurantService\">\n" +
            "            <soap12:address location=\"http://hugh.qawin2003.com/FlatWsdl12/Service.svc/RS\"/>\n" +
            "            <wsa10:EndpointReference>\n" +
            "                <wsa10:Address>http://hugh.qawin2003.com/FlatWsdl12/Service.svc/RS</wsa10:Address>\n" +
            "            </wsa10:EndpointReference>\n" +
            "        </wsdl:port>\n" +
            "    </wsdl:service>\n" +
            "</wsdl:definitions>";

    private String fauldLabMsg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "    <s:Header/>\n" +
            "    <s:Body>\n" +
            "        <tns:MailBoxValidator xmlns:tns=\"http://ws.fraudlabs.com/\">\n" +
            "            <tns:EMAIL>string</tns:EMAIL>\n" +
            "            <tns:LICENSE>string</tns:LICENSE>\n" +
            "        </tns:MailBoxValidator>\n" +
            "    </s:Body>\n" +
            "</s:Envelope>";

    private String fraudLabsWsdl = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<wsdl:definitions xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" " +
            "xmlns:wsaw=\"http://www.w3.org/2006/05/addressing/wsdl\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
            "xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:tns=\"http://ws.fraudlabs.com/\"" +
            " xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" " +
            "targetNamespace=\"http://ws.fraudlabs.com/\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "  <wsdl:types>\n" +
            "    <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://ws.fraudlabs.com/\">\n" +
            "      <s:element name=\"MailBoxValidator\">\n" +
            "        <s:complexType>\n" +
            "          <s:sequence>\n" +
            "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"EMAIL\" type=\"s:string\" />\n" +
            "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"LICENSE\" type=\"s:string\" />\n" +
            "          </s:sequence>\n" +
            "        </s:complexType>\n" +
            "      </s:element>\n" +
            "      <s:element name=\"MailBoxValidatorResponse\">\n" +
            "        <s:complexType>\n" +
            "          <s:sequence>\n" +
            "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"MailBoxValidatorResult\" type=\"tns:MAILBOX_VALIDATOR\" />\n" +
            "          </s:sequence>\n" +
            "        </s:complexType>\n" +
            "      </s:element>\n" +
            "      <s:complexType name=\"MAILBOX_VALIDATOR\">\n" +
            "        <s:sequence>\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_SYNTAX\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_DOMAIN\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_SMTP\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_LEVEL\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"CREDITSAVAILABLE\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"MESSAGE\" type=\"s:string\" />\n" +
            "        </s:sequence>\n" +
            "      </s:complexType>\n" +
            "      <s:element name=\"MAILBOX_VALIDATOR\" nillable=\"true\" type=\"tns:MAILBOX_VALIDATOR\" />\n" +
            "    </s:schema>\n" +
            "  </wsdl:types>\n" +
            "  <wsdl:message name=\"MailBoxValidatorSoapIn\">\n" +
            "    <wsdl:part name=\"parameters\" element=\"tns:MailBoxValidator\" />\n" +
            "  </wsdl:message>\n" +
            "  <wsdl:message name=\"MailBoxValidatorSoapOut\">\n" +
            "    <wsdl:part name=\"parameters\" element=\"tns:MailBoxValidatorResponse\" />\n" +
            "  </wsdl:message>\n" +
            "  <wsdl:portType name=\"mailboxvalidatorSoap\">\n" +
            "    <wsdl:operation name=\"MailBoxValidator\">\n" +
            "      <wsdl:input message=\"tns:MailBoxValidatorSoapIn\" wsaw:Action=\"http://ws.fraudlabs.com/MailBoxValidator\"/>\n" +
            "      <wsdl:output message=\"tns:MailBoxValidatorSoapOut\" wsaw:Action=\"http://ws.fraudlabs.com/MailBoxValidatorResponse\"/>\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:portType>\n" +
            "  <wsdl:binding name=\"mailboxvalidatorSoap\" type=\"tns:mailboxvalidatorSoap\">\n" +
            "    <soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\" />\n" +
            "    <wsdl:operation name=\"MailBoxValidator\">\n" +
            "      <soap:operation soapAction=\"http://ws.fraudlabs.com/MailBoxValidator\" style=\"document\" />\n" +
            "      <wsdl:input>\n" +
            "        <soap:body use=\"literal\" />\n" +
            "      </wsdl:input>\n" +
            "      <wsdl:output>\n" +
            "        <soap:body use=\"literal\" />\n" +
            "      </wsdl:output>\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:binding>\n" +
            "  <wsdl:service name=\"mailboxvalidator\">\n" +
            "    <wsdl:port name=\"mailboxvalidatorSoap\" binding=\"tns:mailboxvalidatorSoap\">\n" +
            "      <soap:address location=\"http://ws2.fraudlabs.com/mailboxvalidator.asmx\" />\n" +
            "    </wsdl:port>\n" +
            "  </wsdl:service>\n" +
            "</wsdl:definitions>";

    private String fraudLabsWsdlWsAddressingNonDefaultNamespace = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<wsdl:definitions xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" " +
            "xmlns:wsaw=\"http://www.w3.org/2006/02/addressing/wsdl\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" " +
            "xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:tns=\"http://ws.fraudlabs.com/\"" +
            " xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" " +
            "targetNamespace=\"http://ws.fraudlabs.com/\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "  <wsdl:types>\n" +
            "    <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://ws.fraudlabs.com/\">\n" +
            "      <s:element name=\"MailBoxValidator\">\n" +
            "        <s:complexType>\n" +
            "          <s:sequence>\n" +
            "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"EMAIL\" type=\"s:string\" />\n" +
            "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"LICENSE\" type=\"s:string\" />\n" +
            "          </s:sequence>\n" +
            "        </s:complexType>\n" +
            "      </s:element>\n" +
            "      <s:element name=\"MailBoxValidatorResponse\">\n" +
            "        <s:complexType>\n" +
            "          <s:sequence>\n" +
            "            <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"MailBoxValidatorResult\" type=\"tns:MAILBOX_VALIDATOR\" />\n" +
            "          </s:sequence>\n" +
            "        </s:complexType>\n" +
            "      </s:element>\n" +
            "      <s:complexType name=\"MAILBOX_VALIDATOR\">\n" +
            "        <s:sequence>\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_SYNTAX\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_DOMAIN\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_SMTP\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"IS_LEVEL\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"CREDITSAVAILABLE\" type=\"s:string\" />\n" +
            "          <s:element minOccurs=\"0\" maxOccurs=\"1\" name=\"MESSAGE\" type=\"s:string\" />\n" +
            "        </s:sequence>\n" +
            "      </s:complexType>\n" +
            "      <s:element name=\"MAILBOX_VALIDATOR\" nillable=\"true\" type=\"tns:MAILBOX_VALIDATOR\" />\n" +
            "    </s:schema>\n" +
            "  </wsdl:types>\n" +
            "  <wsdl:message name=\"MailBoxValidatorSoapIn\">\n" +
            "    <wsdl:part name=\"parameters\" element=\"tns:MailBoxValidator\" />\n" +
            "  </wsdl:message>\n" +
            "  <wsdl:message name=\"MailBoxValidatorSoapOut\">\n" +
            "    <wsdl:part name=\"parameters\" element=\"tns:MailBoxValidatorResponse\" />\n" +
            "  </wsdl:message>\n" +
            "  <wsdl:portType name=\"mailboxvalidatorSoap\">\n" +
            "    <wsdl:operation name=\"MailBoxValidator\">\n" +
            "      <wsdl:input message=\"tns:MailBoxValidatorSoapIn\" wsaw:Action=\"http://ws.fraudlabs.com/MailBoxValidator\"/>\n" +
            "      <wsdl:output message=\"tns:MailBoxValidatorSoapOut\" wsaw:Action=\"http://ws.fraudlabs.com/MailBoxValidatorResponse\"/>\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:portType>\n" +
            "  <wsdl:binding name=\"mailboxvalidatorSoap\" type=\"tns:mailboxvalidatorSoap\">\n" +
            "    <soap:binding transport=\"http://schemas.xmlsoap.org/soap/http\" />\n" +
            "    <wsdl:operation name=\"MailBoxValidator\">\n" +
            "      <soap:operation soapAction=\"http://ws.fraudlabs.com/MailBoxValidator\" style=\"document\" />\n" +
            "      <wsdl:input>\n" +
            "        <soap:body use=\"literal\" />\n" +
            "      </wsdl:input>\n" +
            "      <wsdl:output>\n" +
            "        <soap:body use=\"literal\" />\n" +
            "      </wsdl:output>\n" +
            "    </wsdl:operation>\n" +
            "  </wsdl:binding>\n" +
            "  <wsdl:service name=\"mailboxvalidator\">\n" +
            "    <wsdl:port name=\"mailboxvalidatorSoap\" binding=\"tns:mailboxvalidatorSoap\">\n" +
            "      <soap:address location=\"http://ws2.fraudlabs.com/mailboxvalidator.asmx\" />\n" +
            "    </wsdl:port>\n" +
            "  </wsdl:service>\n" +
            "</wsdl:definitions>";

}
