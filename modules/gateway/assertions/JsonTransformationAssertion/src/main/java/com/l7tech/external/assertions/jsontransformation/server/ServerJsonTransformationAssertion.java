package com.l7tech.external.assertions.jsontransformation.server;


import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
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
import org.w3c.dom.Document;
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
            if (assertion.getTransformation().equals(JsonTransformationAssertion.Transformation.XML_to_JSON)) {
                String sourceString = getFirstPartString(sourceMessage);
                targetValue = doTransformation(sourceString, assertion.getTransformation(),
                        assertion.getConvention(), assertion.getRootTagString(), assertion.isPrettyPrint(), assertion.isArrayForm());
            } else {
                Map<String, Object> vars = context.getVariableMap(assertion.getVariablesUsed(), getAudit());
                String rootTag = ExpandVariables.process(assertion.getRootTagString(), vars, getAudit(), true);
                if(assertion.getConvention().equals(JsonTransformationAssertion.TransformationConvention.STANDARD)){
                    if(rootTag == null || rootTag.trim().isEmpty()){
                        logAndAudit( AssertionMessages.USERDETAIL_WARNING, "Root Tag is required.");
                        return AssertionStatus.FAILED;
                    }
                    if(!JsonTransformationAssertion.ROOT_TAG_VERIFIER.matcher(rootTag).matches()){
                        logAndAudit( AssertionMessages.USERDETAIL_WARNING, "Invalid root tag specified: " + rootTag );
                        return AssertionStatus.FAILED;
                    }
                }
                String source = getFirstPartString(sourceMessage);
                targetValue = doTransformation(source, assertion.getTransformation(), assertion.getConvention(),
                        rootTag, assertion.isPrettyPrint(), assertion.isArrayForm());

                Document document = XmlUtil.stringToDocument(targetValue);
                targetValue = assertion.isPrettyPrint() ? XmlUtil.nodeToFormattedString(document) : XmlUtil.nodeToString(document);
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
                                          String rootTag, boolean prettyPrint, boolean asArray) throws JSONException {
        JSONObject jsonObject;
    	String targetValue = "";
        String source = sourceString == null ? "" : sourceString.trim();
        //empty string - nothing to do
        if(source.isEmpty()){
            return "";
        }
    	if (transformation.equals(JsonTransformationAssertion.Transformation.XML_to_JSON)) {
            // Source is XML, so get a JSONObject from XML class
            if(convention.equals(JsonTransformationAssertion.TransformationConvention.STANDARD)){
                jsonObject = XML.toJSONObject(source);
                targetValue = prettyPrint ? jsonObject.toString(JsonStringIndent) : jsonObject.toString();
            }
            else if(convention.equals(JsonTransformationAssertion.TransformationConvention.JSONML)){
                if(asArray){
                    JSONArray jsonArray = JSONML.toJSONArray(source);
                    targetValue = prettyPrint ? jsonArray.toString(JsonStringIndent) : jsonArray.toString();
                }
                else {
                    jsonObject = JSONML.toJSONObject(source);
                    targetValue = prettyPrint ? jsonObject.toString(JsonStringIndent) : jsonObject.toString();
                }
            }
    	} else {
            if('{' == source.charAt(0)){
                jsonObject = new JSONObject(source);
                targetValue = convention.equals(JsonTransformationAssertion.TransformationConvention.STANDARD) ?
                        XML.toString(jsonObject, rootTag.trim().isEmpty()? null: rootTag) :
                        JSONML.toString(jsonObject);
            }
            else if('[' == source.charAt(0)){
                JSONArray jsonArray = new JSONArray(source);
                targetValue = convention.equals(JsonTransformationAssertion.TransformationConvention.STANDARD) ?
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
