package com.l7tech.external.assertions.jsonwebtoken.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class AssertionTestUtil {

    /**
     * Create a simple PEC using XmlFacet request/response
     *
     * @return PolicyEnforcementContext a test PEC
     */
    public static PolicyEnforcementContext getBasicPEC() {
        return createPEContext("<request/>", "<response/>");
    }


    /**
     * Load the Test ApplicationContext with an extended context definition
     * @return ApplicationContext
     */
    public static ApplicationContext getTestApplicationContext() {
        return loadApplicationContext("");
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

    private static ApplicationContext loadApplicationContext(String extendedContextDefinitionPath) {
        if (!"".equals(extendedContextDefinitionPath)) {
            return new ClassPathXmlApplicationContext( ApplicationContexts.DEFAULT_TEST_BEAN_DEFINITIONS, extendedContextDefinitionPath);
        } else {
            return new ClassPathXmlApplicationContext(new String[]{ApplicationContexts.DEFAULT_TEST_BEAN_DEFINITIONS});
        }
    }

    private static PolicyEnforcementContext createPEContext(String req, String res) {
        Message request = new Message();
        request.initialize( XmlUtil.stringAsDocument( req ));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext( request, response );
    }

}