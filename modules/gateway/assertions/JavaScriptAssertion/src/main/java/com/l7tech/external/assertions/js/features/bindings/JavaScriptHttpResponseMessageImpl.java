package com.l7tech.external.assertions.js.features.bindings;

import com.l7tech.message.Message;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.logging.Logger;

/**
 * JavaScriptHttpRequestMessage implementation to support HTTP type of messages
 */
public class JavaScriptHttpResponseMessageImpl extends JavaScriptMessageImpl implements JavaScriptHttpResponseMessage {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptHttpResponseMessageImpl.class.getName());

    public JavaScriptHttpResponseMessageImpl(final Message message, final ScriptObjectMirror scriptObjectMirror) {
        super(message, scriptObjectMirror);
    }

    @Override
    public int getStatusCode() {
        return message.getHttpResponseKnob().getStatus();
    }

    @Override
    public void setContent(Object content, String contentType, int statusCode) {
        message.getHttpResponseKnob().setStatus(statusCode);
        setContent(content, contentType);
    }
}
