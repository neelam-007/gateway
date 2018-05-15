package com.l7tech.external.assertions.js.features;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Context implementation to expose the PolicyEnforcementContext to Javascript.
 */
public class JavaScriptPolicyEnforcementContextImpl implements JavaScriptPolicyEnforcementContext {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptPolicyEnforcementContextImpl.class.getName());

    private PolicyEnforcementContext policyContext;
    private ScriptObjectMirror jsonScriptObjectMirror;

    public JavaScriptPolicyEnforcementContextImpl(final PolicyEnforcementContext ctx, final ScriptObjectMirror
            jsonScriptObjectMirror) {
        this.policyContext = ctx;
        this.jsonScriptObjectMirror = jsonScriptObjectMirror;
    }

    @Override
    public Object getVariable(String name) throws NoSuchVariableException {
        final Object obj = policyContext.getVariable(name);

        try {
            if (obj instanceof Message) {
                final Message msg = (Message) obj;
                if (msg.isJson()) {
                    // convert to json object type object
                    final String jsonStr = msg.getJsonKnob().getJsonData().getJsonData();
                    return jsonScriptObjectMirror.callMember("parse", jsonStr);
                } else {
                    return JavaScriptUtil.getStringFromMsg(msg);
                }
            } else {
                return obj;
            }
        } catch (Exception e) {
            LOGGER.info("Inside getVariable - Exception block : " + ExceptionUtils.getMessage(e));
            return null;
        }
    }

    @Override
    public void setVariable(String name, Object scriptObj) throws NoSuchVariableException, IOException {

        if (scriptObj instanceof ScriptObjectMirror) {
            final ScriptObjectMirror som = (ScriptObjectMirror) scriptObj;

            if("Date".equalsIgnoreCase(som.getClassName())){
                final Double mili = (Double)som.callMember("getTime");
                final Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(Math.round(mili));
                final Date date = calendar.getTime();
                policyContext.setVariable(name, date);
            } else {
                final String strObj = (String) jsonScriptObjectMirror.callMember("stringify", scriptObj);

                // create message type object
                // this is needed so that the context variable can be used in other assertions in a policy
                final ContentTypeHeader contentType = ContentTypeHeader.APPLICATION_JSON;
                final Message message = policyContext.getOrCreateTargetMessage(new MessageTargetableSupport(name), false);
                message.initialize(contentType, strObj.getBytes(contentType.getEncoding()));

                policyContext.setVariable(name, message);
            }
        } else {
            policyContext.setVariable(name, scriptObj);
        }
    }
}