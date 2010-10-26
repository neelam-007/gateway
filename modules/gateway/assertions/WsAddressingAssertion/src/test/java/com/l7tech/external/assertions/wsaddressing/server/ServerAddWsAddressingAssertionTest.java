/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.external.assertions.wsaddressing.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.wsaddressing.AddWsAddressingAssertion;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

public class ServerAddWsAddressingAssertionTest {

    @Test
    public void testMessageProperties_NoVariables_NoRelatesTo() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

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
                new ServerAddWsAddressingAssertion(assertion, appContext);

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
    }

    @BugNumber(9268)
    @Test
    public void testNoSoapHeaderPresent() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

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
                new ServerAddWsAddressingAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext(warehouseResponse, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);
    }

    /**
     * If there is no SOAPAction associated with the target message, and the assertion's action is set to <<auto>>
     * then the assertion should fail.
     * 
     * @throws Exception
     */
    @BugNumber(9270)
    @Test
    public void testAutoSoapActionHandledCorrectly() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();
        assertion.setAction(AddWsAddressingAssertion.ACTION_AUTOMATIC);
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
                new ServerAddWsAddressingAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext(warehouseResponse, null);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }

    /**
     * If the target message has a soap action and the assertion is configured with a non auto action, then at runtime
     * they must match.
     * @throws Exception
     */
    @Test
    public void testAutoSoapActionMismatch() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

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
                new ServerAddWsAddressingAssertion(assertion, appContext);

        final PolicyEnforcementContext context = getContext(warehouseResponse, "http://warehouse.acme.com/ws/listProducts_WONTMATCH");
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be FAILED", AssertionStatus.FAILED, status);
    }

    /**
     * If the Action property is configured to be <<auto>> then it should obtain the value of the SOAPAction from the
     * associated target message when it is available.
     * 
     * @throws Exception
     */
    @Test
    public void testAutoSoapActionUsedCorrectly() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

        final AddWsAddressingAssertion assertion = new AddWsAddressingAssertion();
        assertion.setAction(AddWsAddressingAssertion.ACTION_AUTOMATIC);
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
                new ServerAddWsAddressingAssertion(assertion, appContext);

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
    }

    @Test
    public void testMessageProperties_Variables() throws Exception{
        final ApplicationContext appContext = ApplicationContexts.getTestApplicationContext();

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
        final String namespace = SoapConstants.WSA_NAMESPACE_10;
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
                new ServerAddWsAddressingAssertion(assertion, appContext);

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
                relatesToMsgEl.getAttributeNS(SoapConstants.WSA_NAMESPACE_10, SoapConstants.WSA_MSG_PROP_RELATES_TO_RELATIONSHIP_TYPE);
        Assert.assertTrue("Relates to attribute should have been added.", !relatesToAttributeValue.trim().isEmpty());
        Assert.assertEquals("Incorrect relationship type value found", SoapConstants.WSA_MSG_PROP_RELATIONSHIP_REPLY_NAMESPACE,
                relatesToAttributeValue);

        validateNamespace(relatesToMsgEl, namespace);
        Assert.assertEquals("Incorrect element value", relatesToMsgsId, relatesToMsgEl.getTextContent());

    }

    private void validateNamespace(Element element, String namespace){
        final String foundNs = element.getNamespaceURI();
        Assert.assertEquals("Incorrect namespace found", namespace, foundNs);
    }

    private void validateIdAttribute(Element element) {
        final String actionId = element.getAttributeNS(SoapConstants.WSU_NAMESPACE, "Id");
        Assert.assertTrue("Id attribute should have been added.", !actionId.trim().isEmpty());
    }

    @Ignore
    @Test
    public void testMessageProperties_NoVariables_WithRelatesTo() throws Exception{
        Assert.fail("Implement me");
    }

    @Ignore
    @Test
    public void testNotSoap() throws Exception{
        Assert.fail("Implement me");
    }

    @Ignore
    @Test
    public void testUnknownVariableReferenced() throws Exception{
        Assert.fail("Implement me");
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
}
