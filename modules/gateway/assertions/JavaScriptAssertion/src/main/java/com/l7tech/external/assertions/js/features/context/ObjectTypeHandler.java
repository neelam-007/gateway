package com.l7tech.external.assertions.js.features.context;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ExceptionUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler to write JavaScript object to the context. It converts JavaScript object to JSON and then create a Message
 * type variable with content type application/json and sets it to the context.
 */
public class ObjectTypeHandler implements ContextDataTypeHandler {

    private static final Logger LOGGER = Logger.getLogger(ObjectTypeHandler.class.getName());

    @Override
    public void set(final PolicyEnforcementContext context, final ScriptObjectMirror jsonScriptObjectMirror,
                    final String name, final Object value) {
        try {
            final String strObj = (String) jsonScriptObjectMirror.callMember("stringify", value);

            // create message type object
            // this is needed so that the context variable can be used in other assertions in a policy
            final ContentTypeHeader contentType = ContentTypeHeader.APPLICATION_JSON;
            final Message message = context.getOrCreateTargetMessage(new MessageTargetableSupport(name), false);
            message.initialize(contentType, strObj.getBytes(contentType.getEncoding()));
        } catch (NoSuchVariableException | IOException e) {
            LOGGER.log(Level.WARNING, "Exception during context variable update.", ExceptionUtils.getDebugException(e));
        }
    }
}
