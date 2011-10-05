package com.l7tech.external.assertions.jsontransformation.server;


import com.l7tech.common.io.XmlUtil;
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
import org.json.*;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

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
                String sourceString = getFirstPartString(sourceMessage);
                targetValue = doTransformation(sourceString, assertion.getTransformation(),
                        assertion.getConvention(), assertion.getRootTagString());
            } else {

                Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
                String rootTag = ExpandVariables.process(assertion.getRootTagString(), vars, getAudit(), true);

                // try to use JSON map, less processing
                Object jsonObj = sourceMessage.getJsonKnob().getJsonData().getJsonObject();
                if (jsonObj instanceof Map) {
                    Map<Object, Object> data = (Map<Object, Object>) jsonObj;
                    jsonObject = new JSONObject(data);
                    targetValue = assertion.getConvention() == JsonTransformationAssertion.TransformationConvention.STANDARD ?
                            XML.toString(jsonObject, rootTag.trim().isEmpty() ? null : rootTag) :
                            JSONML.toString(jsonObject);
                } else {
                    String source = getFirstPartString(sourceMessage);
                    targetValue = doTransformation(source, assertion.getTransformation(),
                            assertion.getConvention(), assertion.getRootTagString());
                }
                targetValue = XmlUtil.nodeToFormattedString(XmlUtil.stringToDocument(targetValue));
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
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ex.getMessage()}, ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        } catch (SAXException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Converted XML is invalid.", e.getMessage()}, ExceptionUtils.getDebugException(e));
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

    public static String doTransformation(String sourceString, JsonTransformationAssertion.Transformation transformation,
                                          JsonTransformationAssertion.TransformationConvention convention,
                                          String rootTag) throws JSONException {
        JSONObject jsonObject;
    	String targetValue = "";
        String source = sourceString == null ? "" : sourceString.trim();
        //empty string - nothing to do
        if(source.isEmpty()){
            return "";
        }
    	if (transformation.equals(JsonTransformationAssertion.Transformation.XML_to_JSON)) {
            // Source is XML, so get a JSONObject from XML class
            jsonObject = convention == JsonTransformationAssertion.TransformationConvention.STANDARD ?
                    XML.toJSONObject(source) :
                    JSONML.toJSONObject(source);
            // Set target variable to JSON string with indentation
            targetValue = jsonObject.toString(JsonStringIndent);

    	} else {
            if('{' == source.charAt(0)){
                jsonObject = new JSONObject(source);
                targetValue = convention == JsonTransformationAssertion.TransformationConvention.STANDARD ?
                        XML.toString(jsonObject, rootTag.trim().isEmpty()? null: rootTag) :
                        JSONML.toString(jsonObject);
            }
            else if('[' == source.charAt(0)){
                JSONArray jsonArray = new JSONArray(source);
                targetValue = convention == JsonTransformationAssertion.TransformationConvention.STANDARD ?
                        XML.toString(jsonArray, rootTag.trim().isEmpty() ? null: rootTag) :
                        JSONML.toString(jsonArray);
            }
            else {
                throw new JSONException("Source is not a valid JSON string.");
            }
    	}
        return targetValue;
    }
}
