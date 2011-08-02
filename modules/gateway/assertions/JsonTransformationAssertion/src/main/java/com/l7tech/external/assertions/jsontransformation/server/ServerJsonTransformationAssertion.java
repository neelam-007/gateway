package com.l7tech.external.assertions.jsontransformation.server;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.springframework.context.ApplicationContext;

public class ServerJsonTransformationAssertion extends AbstractServerAssertion<JsonTransformationAssertion> {

    private final StashManagerFactory stashManagerFactory;
    private final static int JsonStringIndent = 4;

    public ServerJsonTransformationAssertion(JsonTransformationAssertion assertion,
                                             final ApplicationContext springContext ) throws PolicyAssertionException {
        super(assertion);
        stashManagerFactory = springContext.getBean("stashManagerFactory", StashManagerFactory.class);
    }


    protected ServerJsonTransformationAssertion( final JsonTransformationAssertion assertion,
                                         final StashManagerFactory stashManagerFactory){
        super( assertion );
        this.stashManagerFactory = stashManagerFactory;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException{
        Message sourceMessage;
        try {
            sourceMessage = context.getTargetMessage(assertion, false);
        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.MESSAGE_TARGET_ERROR, new String[]{assertion.getTargetName()}, ExceptionUtils.getDebugException(e) );
            return AssertionStatus.FAILED;
        }

        String targetValue;
        try {
            JSONObject jsonObject;

            if (assertion.getTransformation().equals(JsonTransformationAssertion.Transformation.XML_to_JSON)) {
                String sourceString =  getFirstPartString(sourceMessage);

                jsonObject = XML.toJSONObject(sourceString);
                targetValue = jsonObject.toString(JsonStringIndent);

            } else {

                Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
                String rootTag = ExpandVariables.process(assertion.getRootTagString(), vars, getAudit(), true);

                // try to use JSON map, less processing
                Object jsonObj =  sourceMessage.getJsonKnob().getJsonData().getJsonObject();
                if(jsonObj instanceof Map ){
                    Map<Object, Object>  data = (Map<Object, Object>)jsonObj;
                    jsonObject = new JSONObject(data);
                }else{
                    String sourceString =  getFirstPartString(sourceMessage);
                    jsonObject = new JSONObject(sourceString);
                }
                targetValue = XML.toString(jsonObject, rootTag.isEmpty()? null: rootTag);
            }

            setOutput(targetValue, context, assertion.getTransformation().equals(JsonTransformationAssertion.Transformation.XML_to_JSON));

        } catch (JSONException ex) {
            logAndAudit( AssertionMessages.JSON_TRANSFORMATION_FAILED, new String[]{}, ExceptionUtils.getDebugException(ex) );
            return AssertionStatus.FAILED;
        } catch (NoSuchVariableException ex) {
            logAndAudit( AssertionMessages.MESSAGE_TARGET_ERROR, new String[]{assertion.getTargetName(), ExceptionUtils.getMessage(ex)}, ExceptionUtils.getDebugException(ex) );
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException ex) {
            logAndAudit( AssertionMessages.NO_SUCH_PART, new String[]{assertion.getTargetName(), "1"}, ExceptionUtils.getDebugException(ex) );
            return AssertionStatus.FAILED;
        } catch (InvalidJsonException ex) {
            logAndAudit( AssertionMessages.JSON_INVALID_JSON, new String[]{assertion.getTargetName()}, ExceptionUtils.getDebugException(ex) );
            return AssertionStatus.FAILED;
        } catch (IOException ex){
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ex.getMessage()}, ExceptionUtils.getDebugException(ex) );
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    private String getFirstPartString(Message sourceMessage) throws NoSuchPartException, IOException {
        final PartInfo firstPart = sourceMessage.getMimeKnob().getFirstPart();
        final Charset encoding = firstPart.getContentType().getEncoding();
        return new String(IOUtils.slurpStream(firstPart.getInputStream(false)), encoding);
    }

    private void setOutput(String targetValue, PolicyEnforcementContext context, boolean isJson) throws IOException, NoSuchVariableException {
        Message target = context.getOrCreateTargetMessage(assertion.getDestinationMessageTarget(),false);
        target.initialize(stashManagerFactory.createStashManager(), isJson ? ContentTypeHeader.APPLICATION_JSON : ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(targetValue.getBytes()));

    }

    public static String doTransformation(String sourceString, JsonTransformationAssertion.Transformation transformation, String rootTag) throws JSONException {
        JSONObject jsonObject;
    	String targetValue;

    	if (transformation.equals(JsonTransformationAssertion.Transformation.XML_to_JSON)) {
            // Source is XML, so get a JSONObject from XML class
            jsonObject = XML.toJSONObject(sourceString);
            // Set target variable to JSON string with indentation
            targetValue = jsonObject.toString(JsonStringIndent);

    	} else {
            // Source is JSON, so construct a JSONObject
            jsonObject = new JSONObject(sourceString);
            // Set target variable to XML using XML class
            targetValue = XML.toString(jsonObject, rootTag.isEmpty()? null: rootTag);
    	}
        return targetValue;
    }
}
