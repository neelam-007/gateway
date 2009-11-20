/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
  * User: darmstrong
  * Date: Jun 9, 2009
  * Time: 1:30:30 PM
  */
package com.l7tech.external.assertions.xacmlpdp.server;

import org.junit.Test;
 import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.springframework.mock.web.MockServletContext;
 import org.springframework.mock.web.MockHttpServletRequest;
 import org.springframework.mock.web.MockHttpServletResponse;
 import org.xml.sax.SAXException;
 import com.l7tech.server.ApplicationContexts;
 import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.external.assertions.xacmlpdp.XacmlPdpAssertion;
 import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
 import com.l7tech.common.io.XmlUtil;
 import com.l7tech.message.HttpServletRequestKnob;
 import com.l7tech.message.Message;
 import com.l7tech.message.HttpServletResponseKnob;
 import com.l7tech.policy.assertion.PolicyAssertionException;
 import com.l7tech.policy.assertion.AssertionStatus;
 import com.l7tech.policy.StaticResourceInfo;

 import java.io.IOException;

/**
 * These tests aims to ensure that whether the incoming XACML request is retrieved via the request, response or
  * context variable, and whether the reqeust is encapsulated in soap or not, that all of these vectors have test
  * coverage
  */
 public class ServerXacmlPdpAssertionTest {

     //------------Test xacml requests present in the reqeust message

     @Test
     public void testCheckRequest_Pre_2_0_Soap_RequestInRequest_Valid() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_SOAP_XML, true, null);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

//     @Test
//     public void testCheckRequest_TestPolicyIncludes() throws Exception {
//              XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
//              StaticResourceInfo sri = new StaticResourceInfo(POLICY_SET_POLICY);
//              xacmlPdpAssertion.setResourceInfo(sri);
//
//              xacmlPdpAssertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);
//
//              ServerXacmlPdpAssertion serverPdpAssertion =
//                                    new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());
//
//              PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_SOAP_XML, true, null);
//
//              AssertionStatus status = serverPdpAssertion.checkRequest(context);
//              Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);
//
//              //check decision from PDP for permit
//              Message response = context.getResponse();
//              String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
//              Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
//          }

     @Test
     public void testCheckRequest_Pre_2_0_NoSoap_RequestInRequest_Valid() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_NOSOAP_XML, true, null);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

     //------------Test xacml requests present in the response message

     @Test
     public void testCheckRequest_Pre_2_0_Soap_RequestInResponse_Valid() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);
         xacmlPdpAssertion.setInputMessageSource(XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_SOAP_XML, false, null);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

     @Test
     public void testCheckRequest_Pre_2_0_NoSoap_RequestInResponse_Valid() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setInputMessageSource(XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_NOSOAP_XML, false, null);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

     //------------Test xacml requests present in a context variable

     @Test
     public void testCheckRequest_Pre_2_0_Soap_RequestInContextVariable_Valid() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);
         xacmlPdpAssertion.setInputMessageSource(XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE);
         String messageVariableName = "CONTEXT_VARIABLE_NAME";
         xacmlPdpAssertion.setInputMessageVariableName(messageVariableName);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_SOAP_XML, false, messageVariableName);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

     @Test
     public void testCheckRequest_Pre_2_0_NoSoap_RequestInContextVariable_Valid() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setInputMessageSource(XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE);
         String messageVariableName = "CONTEXT_VARIABLE_NAME";
         xacmlPdpAssertion.setInputMessageVariableName(messageVariableName);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_NOSOAP_XML, false, messageVariableName);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

     //------------Test 2.0 requests, use the version identifier, then use a request which only 2.0 will allow

     /**
      * Note: it's possible this test should not succeed. Were sending a request saying its 2.0 to a PDP which
      * is using an implementation from Sun which does not claim XACML 2.0 support
      * @throws Exception
      */
     @Test
     public void testCheckRequest_2_0() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setInputMessageSource(XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_2_0_NOSOAP_XML, true, null);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

     @Test
     public void testCheckRequest_Valid_2_0_Only() throws Exception {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_ANY_RESOURCE_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setInputMessageSource(XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext(XACML_REQUEST_2_0_ONLY_NOSOAP_XML, true, null);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

         //check decision from PDP for permit
         Message response = context.getResponse();
         String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
         Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
     }

     //------------Test invalid xacml requests
     @Test
     public void testCheckRequest_InvalidRequestXacml() throws PolicyAssertionException, SAXException, IOException {
         XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
         StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
         xacmlPdpAssertion.setResourceInfo(sri);

         xacmlPdpAssertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);

         ServerXacmlPdpAssertion serverPdpAssertion =
                 new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

         PolicyEnforcementContext context = getContext("<invalidxacmlrequestxml />", true, null);

         AssertionStatus status = serverPdpAssertion.checkRequest(context);
         Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.FALSIFIED, status);
     }

    //-------------Test with variable interpolation
    @Test
    public void testCheckRequest_PolicyFromContextVariable() throws Exception {
        XacmlPdpAssertion xacmlPdpAssertion = new XacmlPdpAssertion();
        StaticResourceInfo sri = new StaticResourceInfo("${pdpPolicyXml}");
        xacmlPdpAssertion.setResourceInfo(sri);

        xacmlPdpAssertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);
        xacmlPdpAssertion.setInputMessageSource(XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE);
        String messageVariableName = "CONTEXT_VARIABLE_NAME";
        xacmlPdpAssertion.setInputMessageVariableName(messageVariableName);

        assertEquals(2, xacmlPdpAssertion.getVariablesUsed().length);
        assertEquals("pdpPolicyXml", xacmlPdpAssertion.getVariablesUsed()[0]);

        ServerXacmlPdpAssertion serverPdpAssertion =
                new ServerXacmlPdpAssertion(xacmlPdpAssertion, ApplicationContexts.getTestApplicationContext());

        PolicyEnforcementContext context = getContext(XACML_REQUEST_1_0_AND_1_1_SOAP_XML, false, messageVariableName);
        context.setVariable("pdpPolicyXml", PDP_POLICY_XML);

        AssertionStatus status = serverPdpAssertion.checkRequest(context);
        Assert.assertEquals("checkRequest returned invalid AssertionStatus",  AssertionStatus.NONE, status);

        //check decision from PDP for permit
        Message response = context.getResponse();
        String responseXml = XmlUtil.elementToXml(response.getXmlKnob().getDocumentReadOnly().getDocumentElement());
        Assert.assertEquals("Invalid response received from PDP", PERMIT_DECISION, fixLines(responseXml).trim());
    }


     /**
      * contextVariableName takes precedence over useRequest. When not null the xacmlRequestXml is placed in a variable
      */
     private PolicyEnforcementContext getContext(String xacmlRequestXml, boolean useRequest, String contextVariableName)
             throws SAXException {
         Message request = new Message();
         Message response = new Message();

         MockServletContext servletContext = new MockServletContext();
         MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
         MockHttpServletResponse hresponse = new MockHttpServletResponse();

         PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

         if(contextVariableName != null && !contextVariableName.isEmpty()){
             policyEnforcementContext.setVariable(contextVariableName,
                     new Message(XmlUtil.stringToDocument(xacmlRequestXml)));
         }else{
             if(useRequest){
                 request.initialize( XmlUtil.stringToDocument(xacmlRequestXml));
             }else{
                 response.initialize(XmlUtil.stringToDocument(xacmlRequestXml));
             }
         }
         request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
         response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

         return policyEnforcementContext;
     }

     private String fixLines(String input) {
         return input.replaceAll("\\r\\n", "\n").replaceAll("\\n\\r", "\n").replaceAll("\\r", "\n");
     }

     private final static String XACML_REQUEST_1_0_AND_1_1_SOAP_XML = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
             "    <soapenv:Header/>\n" +
             "    <soapenv:Body>\n" +
             "        <Request xmlns=\"urn:oasis:names:tc:xacml:1.0:context\">\n" +
             "            <Subject>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\" DataType=\"urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name\">\n" +
             "                    <AttributeValue>seth@users.example.com</AttributeValue>\n" +
             "                </Attribute>\n" +
             "                <Attribute AttributeId=\"group\"\n" +
             "                    DataType=\"http://www.w3.org/2001/XMLSchema#string\" Issuer=\"admin@users.example.com\">\n" +
             "                    <AttributeValue>developers</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Subject>\n" +
             "            <Resource>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">\n" +
             "                    <AttributeValue>http://server.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Resource>\n" +
             "            <Action>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" DataType=\"http://www.w3.org/2001/XMLSchema#string\">\n" +
             "                    <AttributeValue>read</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Action>\n" +
             "            <Environment/>\n" +
             "        </Request>\n" +
             "    </soapenv:Body>\n" +
             "</soapenv:Envelope>";

     private final static String XACML_REQUEST_1_0_AND_1_1_NOSOAP_XML = "<Request xmlns=\"urn:oasis:names:tc:xacml:1.0:context\">\n" +
             "            <Subject>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\" DataType=\"urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name\">\n" +
             "                    <AttributeValue>seth@users.example.com</AttributeValue>\n" +
             "                </Attribute>\n" +
             "                <Attribute AttributeId=\"group\"\n" +
             "                    DataType=\"http://www.w3.org/2001/XMLSchema#string\" Issuer=\"admin@users.example.com\">\n" +
             "                    <AttributeValue>developers</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Subject>\n" +
             "            <Resource>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">\n" +
             "                    <AttributeValue>http://server.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Resource>\n" +
             "            <Action>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" DataType=\"http://www.w3.org/2001/XMLSchema#string\">\n" +
             "                    <AttributeValue>read</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Action>\n" +
             "            <Environment/>\n" +
             "        </Request>\n" ;

     private final static String XACML_REQUEST_2_0_NOSOAP_XML = "<Request xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">\n" +
             "            <Subject>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\" DataType=\"urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name\">\n" +
             "                    <AttributeValue>seth@users.example.com</AttributeValue>\n" +
             "                </Attribute>\n" +
             "                <Attribute AttributeId=\"group\"\n" +
             "                    DataType=\"http://www.w3.org/2001/XMLSchema#string\" Issuer=\"admin@users.example.com\">\n" +
             "                    <AttributeValue>developers</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Subject>\n" +
             "            <Resource>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">\n" +
             "                    <AttributeValue>http://server.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Resource>\n" +
             "            <Action>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" DataType=\"http://www.w3.org/2001/XMLSchema#string\">\n" +
             "                    <AttributeValue>read</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Action>\n" +
             "            <Environment/>\n" +
             "        </Request>\n" ;

     private final static String XACML_REQUEST_2_0_ONLY_NOSOAP_XML = "<Request xmlns=\"urn:oasis:names:tc:xacml:2.0:context:schema:os\">\n" +
             "            <Subject>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\" DataType=\"urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name\">\n" +
             "                    <AttributeValue>seth@users.example.com</AttributeValue>\n" +
             "                </Attribute>\n" +
             "                <Attribute AttributeId=\"group\"\n" +
             "                    DataType=\"http://www.w3.org/2001/XMLSchema#string\" Issuer=\"admin@users.example.com\">\n" +
             "                    <AttributeValue>developers</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Subject>\n" +
             "            <Resource>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">\n" +
             "                    <AttributeValue>http://www.google.com</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Resource>\n" +
             "            <Resource>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\" DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">\n" +
             "                    <AttributeValue>http://server.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Resource>\n" +
             "            <Action>\n" +
             "                <Attribute\n" +
             "                    AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\" DataType=\"http://www.w3.org/2001/XMLSchema#string\">\n" +
             "                    <AttributeValue>read</AttributeValue>\n" +
             "                </Attribute>\n" +
             "            </Action>\n" +
             "            <Environment/>\n" +
             "        </Request>\n" ;

     private final static String PDP_POLICY_XML = "<Policy PolicyId=\"ExamplePolicy\"\n" +
             "          RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides\">\n" +
             "    <Target>\n" +
             "      <Subjects>\n" +
             "        <AnySubject/>\n" +
             "      </Subjects>\n" +
             "      <Resources>\n" +
             "        <Resource>\n" +
             "          <ResourceMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:anyURI-equal\">\n" +
             "            <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">http://server.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
             "            <ResourceAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\"\n" +
             "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\"/>\n" +
             "          </ResourceMatch>\n" +
             "        </Resource>\n" +
             "      </Resources>\n" +
             "      <Actions>\n" +
             "        <AnyAction/>\n" +
             "      </Actions>\n" +
             "    </Target>\n" +
             "    <Rule RuleId=\"ReadRule\" Effect=\"Permit\">\n" +
             "      <Target>\n" +
             "        <Subjects>\n" +
             "          <AnySubject/>\n" +
             "        </Subjects>\n" +
             "        <Resources>\n" +
             "          <AnyResource/>\n" +
             "        </Resources>\n" +
             "        <Actions>\n" +
             "          <Action>\n" +
             "            <ActionMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
             "              <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</AttributeValue>\n" +
             "              <ActionAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
             "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\"/>\n" +
             "            </ActionMatch>\n" +
             "          </Action>\n" +
             "        </Actions>\n" +
             "      </Target>\n" +
             "      <Condition FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
             "        <Apply FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only\">\n" +
             "          <SubjectAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
             "                                      AttributeId=\"group\"/>\n" +
             "        </Apply>\n" +
             "        <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">developers</AttributeValue>\n" +
             "      </Condition>\n" +
             "    </Rule>\n" +
             "  </Policy>";

     private final static String PDP_ANY_RESOURCE_POLICY_XML = "<Policy PolicyId=\"ExamplePolicy\"\n" +
             "          RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides\">\n" +
             "    <Target>\n" +
             "      <Subjects>\n" +
             "        <AnySubject/>\n" +
             "      </Subjects>\n" +
             "      <Resources>\n" +
             "        <Resource>\n" +
             "        <AnyResource/>\n" +
             "        </Resource>\n" +
             "      </Resources>\n" +
             "      <Actions>\n" +
             "        <AnyAction/>\n" +
             "      </Actions>\n" +
             "    </Target>\n" +
             "    <Rule RuleId=\"ReadRule\" Effect=\"Permit\">\n" +
             "      <Target>\n" +
             "        <Subjects>\n" +
             "          <AnySubject/>\n" +
             "        </Subjects>\n" +
             "        <Resources>\n" +
             "          <AnyResource/>\n" +
             "        </Resources>\n" +
             "        <Actions>\n" +
             "          <Action>\n" +
             "            <ActionMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
             "              <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</AttributeValue>\n" +
             "              <ActionAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
             "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\"/>\n" +
             "            </ActionMatch>\n" +
             "          </Action>\n" +
             "        </Actions>\n" +
             "      </Target>\n" +
             "      <Condition FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
             "        <Apply FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only\">\n" +
             "          <SubjectAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
             "                                      AttributeId=\"group\"/>\n" +
             "        </Apply>\n" +
             "        <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">developers</AttributeValue>\n" +
             "      </Condition>\n" +
             "    </Rule>\n" +
             "  </Policy>";

     private final static String PERMIT_DECISION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
             "<Response>\n" +
             "    <Result ResourceID=\"http://server.example.com/code/docs/developer-guide.html\">\n" +
             "        <Decision>Permit</Decision>\n" +
             "        <Status>\n" +
             "            <StatusCode Value=\"urn:oasis:names:tc:xacml:1.0:status:ok\"/>\n" +
             "        </Status>\n" +
             "    </Result>\n" +
             "</Response>";

     private final static String NOT_APPLICABLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
             "<Response>\n" +
             "    <Result ResourceID=\"http://www.google.com\">\n" +
             "        <Decision>NotApplicable</Decision>\n" +
             "        <Status>\n" +
             "            <StatusCode Value=\"urn:oasis:names:tc:xacml:1.0:status:ok\"/>\n" +
             "        </Status>\n" +
             "    </Result>\n" +
             "</Response>";

     private final static String POLICY_SET_POLICY = "<PolicySet>\n" +
             "<Target>\n" +
             "      <Subjects>\n" +
             "        <AnySubject/>\n" +
             "      </Subjects>\n" +
             "      <Resources>\n" +
             "        <Resource>\n" +
             "          <ResourceMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:anyURI-equal\">\n" +
             "            <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">http://server.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
             "            <ResourceAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\"\n" +
             "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\"/>\n" +
             "          </ResourceMatch>\n" +
             "        </Resource>\n" +
             "      </Resources>\n" +
             "      <Actions>\n" +
             "        <AnyAction/>\n" +
             "      </Actions>\n" +
             "</Target>\n" +
             "<Policy PolicyId=\"ExamplePolicy\"\n" +
             "          RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides\">\n" +
             "    <Target>\n" +
             "      <Subjects>\n" +
             "        <AnySubject/>\n" +
             "      </Subjects>\n" +
             "      <Resources>\n" +
             "        <Resource>\n" +
             "          <ResourceMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:anyURI-equal\">\n" +
             "            <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">http://1ssserver.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
             "            <ResourceAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\"\n" +
             "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\"/>\n" +
             "          </ResourceMatch>\n" +
             "        </Resource>\n" +
             "      </Resources>\n" +
             "      <Actions>\n" +
             "        <AnyAction/>\n" +
             "      </Actions>\n" +
             "    </Target>\n" +
             "    <Rule RuleId=\"ReadRule\" Effect=\"Permit\">\n" +
             "      <Target>\n" +
             "        <Subjects>\n" +
             "          <AnySubject/>\n" +
             "        </Subjects>\n" +
             "        <Resources>\n" +
             "          <AnyResource/>\n" +
             "        </Resources>\n" +
             "        <Actions>\n" +
             "          <Action>\n" +
             "            <ActionMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
             "              <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</AttributeValue>\n" +
             "              <ActionAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
             "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\"/>\n" +
             "            </ActionMatch>\n" +
             "          </Action>\n" +
             "        </Actions>\n" +
             "      </Target>\n" +
             "      <Condition FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
             "        <Apply FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only\">\n" +
             "          <SubjectAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
             "                                      AttributeId=\"group\"/>\n" +
             "        </Apply>\n" +
             "        <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">developers</AttributeValue>\n" +
             "      </Condition>\n" +
             "    </Rule>\n" +
             "  </Policy>\n" +
             "</PolicySet>";
 }
