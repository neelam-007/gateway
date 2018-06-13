package com.l7tech.external.assertions.js.features.bindings;

import com.google.common.base.Splitter;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.util.ExceptionUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JavaScriptHttpRequestMessage implementation to support HTTP type of messages
 */
public class JavaScriptHttpRequestMessageImpl extends JavaScriptMessageImpl implements JavaScriptHttpRequestMessage {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptHttpRequestMessageImpl.class.getName());

    private final String httpVersion;
    private final String httpMethod;
    private final Object jsonParameterObject;
    private final String fullUrl;

    public JavaScriptHttpRequestMessageImpl(final Message message, final ScriptObjectMirror scriptObjectMirror) {
        super(message, scriptObjectMirror);
        httpVersion = parseHttpVersion(message);
        httpMethod = message.getHttpRequestKnob().getMethodAsString();
        fullUrl = getFullUrl();
        jsonParameterObject = toJavaScriptObject(message);
    }

    @Override
    public String getHttpVersion() {
        return httpVersion;
    }

    @Override
    public String getMethod() {
        return httpMethod;
    }

    @Override
    public String getUrl() {
        return fullUrl;
    }

    @Override
    public Object getParameters() {
        return jsonParameterObject;
    }

    private String parseHttpVersion(final Message message) {
        final String protocol = message.getKnob(HttpServletRequestKnob.class).getHttpServletRequest().getProtocol();
        return Splitter.on("/").trimResults().splitToList(protocol).get(1);
    }

    private String getFullUrl() {
        final StringBuilder requestUrl = new StringBuilder(message.getHttpRequestKnob().getRequestUrl());
        final String queryString = message.getHttpRequestKnob().getQueryString();
        if (StringUtils.isNotBlank(queryString)) {
            requestUrl.append("?").append(queryString);
        }
        return requestUrl.toString();
    }

    private Object toJavaScriptObject(final Message message) {
        try {
            return toJavaScriptObject(message.getHttpRequestKnob().getParameterMap());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Exception during getting parameters list.", ExceptionUtils.getDebugException(e));
        }
        return null;
    }
}
