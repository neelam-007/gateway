package com.l7tech.external.assertions.websocket.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.websocket.WebSocketValidationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.communityschemas.SchemaValidationErrorHandler;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This assertion is used to validate xml WebSocket message. It can be used only in conjuction with WebSocket Assertion
 * Created with IntelliJ IDEA.
 * User: nilic
 * Date: 7/3/12
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerWebSocketValidationAssertion extends AbstractServerAssertion<WebSocketValidationAssertion>{

    private final WebSocketValidationAssertion assertion;
    private final StashManagerFactory stashManagerFactory;
    final String[] nodeNames = {WebSocketValidationAssertion.WebSocketSuffix.VARIABLE_SUFFIX_ORIGIN.getWebSocketSuffix(),
                                WebSocketValidationAssertion.WebSocketSuffix.VARIABLE_SUFFIX_PROTOCOL.getWebSocketSuffix(),
                                WebSocketValidationAssertion.WebSocketSuffix.VARIABLE_SUFFIX_DATA.getWebSocketSuffix()};

    public ServerWebSocketValidationAssertion(WebSocketValidationAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);
        this.assertion = assertion;
        this.stashManagerFactory = context.getBean("stashManagerFactory", StashManagerFactory.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        initContextVariables(context);
        //Target Message could be Message or String
        Message msg = null;

        try {
            //check first is the assertion message request, response, or other?
            if(assertion.getTarget().equals(TargetMessageType.OTHER) &&
              (context.getVariable(assertion.getOtherTargetMessageVariable()) instanceof String)){
                    //conver to Document and continue
                    msg = new Message(XmlUtil.parse((String)context.getVariable(assertion.getOtherTargetMessageVariable())));
            }
            else if ((assertion.getTarget().equals(TargetMessageType.REQUEST)||
                      assertion.getTarget().equals(TargetMessageType.RESPONSE)||
                      assertion.getTarget().equals(TargetMessageType.OTHER)) &&
                      (context.getTargetMessage(assertion, false) instanceof Message)){
                msg = context.getTargetMessage(assertion, false);
            }

            if (!msg.isXml()) {
                logAndAudit(AssertionMessages.MESSAGE_BAD_XML);
                return AssertionStatus.FAILED;
            }

            //if message is empty throw another warning
            if (msg.getXmlKnob().getInputSource(false).getByteStream().available() == 0){
                logAndAudit(AssertionMessages.USERDETAIL_WARNING, "Message is empty");
                return AssertionStatus.FAILED;
            }

            return validateMessage(msg, context);
        }
        catch (SAXException e) {
            logAndAudit(AssertionMessages.OVERSIZEDTEXT_NOT_XML, printBadXMLMessage(e));
            return AssertionStatus.FAILED;
        }
        catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, new String[]{assertion.getTargetName()}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }
    }

    /**
     * This function is used to validate WebSocket message against the XML Schema
     * @param msg Message
     * @param context PolicyEnforcementContext
     * @return AssertionStatus
     * @throws IOException
     */
    private AssertionStatus validateMessage(Message msg, PolicyEnforcementContext context) throws IOException {

        try {
            SchemaValidationErrorHandler errorHandler = new SchemaValidationErrorHandler();

            //get a schema factory
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            //create schema from the schema file
            Schema schema = schemaFactory.newSchema(new StreamSource(ReadWebSocketXsdSchema.getInstance().getInputStreamXMLSchema()));
            //define validator from schema and set the error handler to catch any errors
            Validator validator = schema.newValidator();
            validator.reset();
            validator.setErrorHandler(errorHandler);    //error handler to catch errors
            validator.validate(new StreamSource(msg.getXmlKnob().getInputSource(false).getByteStream()));

            if(!errorHandler.recordedErrors().isEmpty()){
                ArrayList<SAXParseException> se = new ArrayList<SAXParseException>(errorHandler.recordedErrors());
                logAndAudit(AssertionMessages.SCHEMA_VALIDATION_FAILED, se.get(0).getMessage());
                return AssertionStatus.FAILED;
            }

            updateContextVariable(msg, context);
            assignMessage(context, msg);

            return AssertionStatus.NONE;
        }
        catch(SAXException e){
            logAndAudit(AssertionMessages.OVERSIZEDTEXT_NOT_XML, printBadXMLMessage(e));
            return AssertionStatus.FAILED;
        }
    }

    //initialize context variables
    private void initContextVariables(PolicyEnforcementContext context) {

        for (String nodeName:nodeNames) {
            context.setVariable(assertion.setVariableName(nodeName), "");
        }
    }

    //assign message to the response body, use response message from the context
    private void assignMessage(PolicyEnforcementContext context, Message msg) throws SAXException, IOException {

        context.getResponse().initialize(stashManagerFactory.createStashManager(),
                ContentTypeHeader.XML_DEFAULT,
                msg.getXmlKnob().getInputSource(false).getByteStream());

    }

    //update context variables in the Policy context with message content
    private void updateContextVariable(Message msg, PolicyEnforcementContext context) throws SAXException, IOException{

        for (String nodeName:nodeNames) {
            NodeList nodes = msg.getXmlKnob().getDocumentReadOnly().getElementsByTagName(nodeName);

            for(int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeName().equals(nodeName) && node.getFirstChild() != null) {
                    context.setVariable(assertion.setVariableName(nodeName), node.getFirstChild().getNodeValue());
                }
            }
        }
    }

    //customize the message in the LogAndAudit, when Message is not in the valid xml format
    private final String printBadXMLMessage(Exception e){

        StringBuffer sb = new StringBuffer();
        sb.append(e.getMessage());
        sb.append("Message is not valid WebSocket message. Message");

        return sb.toString();
    }
}
