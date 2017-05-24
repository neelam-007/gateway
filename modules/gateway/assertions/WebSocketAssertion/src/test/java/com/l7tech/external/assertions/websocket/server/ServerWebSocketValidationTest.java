package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.websocket.WebSocketValidationAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * User: nilic
 * Date: 7/13/12
 * Time: 1:40 PM
 */
public class ServerWebSocketValidationTest {
    
    private static ApplicationContext applicationContext;
    private PolicyEnforcementContext pec;
    private WebSocketValidationAssertion wsa;

    String xmlStr = "<request><websocket><id>12345678:1:chat</id><clientId></clientId><type>TEXT</type><origin>http://example.com</origin><protocol>chat</protocol><offset>BLA</offset>" +
            "<length>516</length><data>something</data></websocket></request>";

    String emptyXmlStr = "<request><websocket><id></id><clientId></clientId><type></type><origin></origin><protocol></protocol><offset></offset>" +
            "<length></length><data></data></websocket></request>";

    String badXmlStr = "<request>Hello</request>";

    @Before
    public void setUp(){
        if(applicationContext == null){
            applicationContext = ApplicationContexts.getTestApplicationContext();
            Assert.assertNotNull("Fail - unable to get application context instance", applicationContext);
        }

        //get the Policy Enforcement Context
        pec = getBasicPEC();
        //get the assertion
        wsa = new WebSocketValidationAssertion();
    }

    @Test
    //testing message as request
    public void testWebSocketValidationRequestValues() throws Exception {

        wsa.setTarget(TargetMessageType.REQUEST);

        getOrCreateTargetXMLMessage(pec, xmlStr, TargetMessageType.REQUEST);

        ServerWebSocketValidationAssertion swsv = buildServerAssertion(wsa);
        AssertionStatus result = swsv.checkRequest(pec);

        Assert.assertEquals(result, AssertionStatus.NONE);

        validateContextVariable(pec);
    }

    @Test
    //testing message with message target set to other variables
    public void testWebSocketValidationMessageVariableValues() throws Exception {

        //set the assertion
        wsa.setOtherTargetMessageVariable("message");
        wsa.setTarget(TargetMessageType.OTHER);

        getOrCreateTargetXMLMessageContextVariable(pec, xmlStr, "message");

        ServerWebSocketValidationAssertion swsv = buildServerAssertion(wsa);

        AssertionStatus result = swsv.checkRequest(pec);

        Assert.assertEquals(result, AssertionStatus.NONE);

        validateContextVariable(pec);
    }

    @Test
    //testing message with message target set to other variables and context variable as String
    public void testWebSocketValidationContextVariableValues() throws Exception {

        //set the assertion to accept OTHER target message
        wsa.setOtherTargetMessageVariable("message");
        wsa.setTarget(TargetMessageType.OTHER);

        //set context variable in the policy context
        pec.setVariable("message", xmlStr);

        ServerWebSocketValidationAssertion swsv = buildServerAssertion(wsa);

        AssertionStatus result = swsv.checkRequest(pec);

        Assert.assertEquals(result, AssertionStatus.NONE);

        validateContextVariable(pec);
    }

     @Test
    //testing message as response passed xml message
    public void testWebSocketValidationResponseValues() throws Exception {

        wsa.setTarget(TargetMessageType.RESPONSE);

        getOrCreateTargetXMLMessage(pec, xmlStr, TargetMessageType.RESPONSE);

        ServerWebSocketValidationAssertion swsv = buildServerAssertion(wsa);
        AssertionStatus result = swsv.checkRequest(pec);

        Assert.assertEquals(result, AssertionStatus.NONE);

         validateContextVariable(pec);
    }

    @Test
    //testing message as response passed xml message
    public void testWebSocketValidationResponseValuesFailed() throws Exception {

        wsa.setTarget(TargetMessageType.RESPONSE);

        getOrCreateTargetXMLMessage(pec, badXmlStr, TargetMessageType.RESPONSE);

        ServerWebSocketValidationAssertion swsv = buildServerAssertion(wsa);
        AssertionStatus result = swsv.checkRequest(pec);

        Assert.assertEquals(result, AssertionStatus.FAILED);
    }

    @Test
    //testing message when contain empty xml String
    public void testWebSocketValidationEmptyValues() throws Exception {

        wsa.setTarget(TargetMessageType.RESPONSE);

        getOrCreateTargetXMLMessage(pec, emptyXmlStr, TargetMessageType.RESPONSE);

        ServerWebSocketValidationAssertion swsv = buildServerAssertion(wsa);
        AssertionStatus result = swsv.checkRequest(pec);

        Assert.assertEquals(result, AssertionStatus.NONE);

        validateEmptyContextVariable(pec);
    }

    private ServerWebSocketValidationAssertion buildServerAssertion(WebSocketValidationAssertion wsa) throws PolicyAssertionException {
        //call server side of the assertion
        return new ServerWebSocketValidationAssertion(wsa, applicationContext);
    }

    private void validateContextVariable(PolicyEnforcementContext pec)throws NoSuchVariableException{

        String webSocketProtocol = (String)pec.getVariable("websocket.protocol");
        Assert.assertNotNull(webSocketProtocol);
        String webSocketOrigin = (String) pec.getVariable("websocket.origin");
        Assert.assertNotNull(webSocketOrigin);
        String webSocketData = (String) pec.getVariable("webSocket.data");
        Assert.assertNotNull(webSocketData);
    }

    private void validateEmptyContextVariable(PolicyEnforcementContext pec)throws NoSuchVariableException {

        String webSocketProtocol = (String)pec.getVariable("websocket.protocol");
        Assert.assertEquals(webSocketProtocol, "");
        String webSocketOrigin = (String) pec.getVariable("websocket.origin");
        Assert.assertEquals(webSocketOrigin, "");
        String webSocketData = (String) pec.getVariable("webSocket.data");
        Assert.assertEquals(webSocketData, "");
    }


    /**
     * Create a simple PEC using XmlFacet request/response
     *
     * @return PolicyEnforcementContext a test PEC
     */
    public static PolicyEnforcementContext getBasicPEC() {
        return createPEContext("<request/>", "<response/>");
    }


    /**
     * Create or get xml target message, based on the different Target Message Type (request, response)
     * @param pec   PolicyEnforcementContext
     * @param xmlStr  String format of xml file
     * @param messageType TargetMessageType
     * @throws com.l7tech.policy.variable.NoSuchVariableException
     * @throws java.io.IOException
     */
    public static void getOrCreateTargetXMLMessage(PolicyEnforcementContext pec, String xmlStr, TargetMessageType messageType) throws NoSuchVariableException, IOException {
        Message xmlMessage = pec.getOrCreateTargetMessage(new MessageTargetableSupport(messageType), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(xmlStr.getBytes()));
    }


    /**
     * Create or get xml target message, based on the other context variable
     * @param pec PolicyEnforcementContext
     * @param xmlStr String format of xml file passed to the message
     * @param variableName Variable name of Other variable as TargetMessageType
     * @throws NoSuchVariableException
     * @throws IOException
     */
    public static void getOrCreateTargetXMLMessageContextVariable(PolicyEnforcementContext pec, String xmlStr, String variableName) throws NoSuchVariableException, IOException{
        Message xmlMessage = pec.getOrCreateTargetMessage(new MessageTargetableSupport(variableName), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(xmlStr.getBytes()));
    }

    private static PolicyEnforcementContext createPEContext(String req, String res) {
        Message request = new Message();
        request.initialize( XmlUtil.stringAsDocument( req ));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );
    }


}
