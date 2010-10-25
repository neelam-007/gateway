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

        final PolicyEnforcementContext context = getContext(soapMsg);
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

    @BugNumber(9264)
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

        final PolicyEnforcementContext context = getContext(warehouseResponse);
        final AssertionStatus status = serverAssertion.checkRequest(context);
        Assert.assertEquals("Status should be NONE", AssertionStatus.NONE, status);
    }

    @Ignore
    @Test
    public void testMessageProperties_NoVariables_WithRelatesTo() throws Exception{
        Assert.fail("Implement me");
    }

    @Ignore
    @Test
    public void testMessageProperties_Variables() throws Exception{
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

    private PolicyEnforcementContext getContext(String messageContent) throws IOException, SAXException {

        Message request = new Message(XmlUtil.parse(messageContent));
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod(HttpMethod.POST.toString());
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
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
