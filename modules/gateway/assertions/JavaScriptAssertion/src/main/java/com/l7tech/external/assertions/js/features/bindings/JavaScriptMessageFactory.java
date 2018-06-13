package com.l7tech.external.assertions.js.features.bindings;

import com.l7tech.external.assertions.js.features.bindings.views.*;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.util.ExceptionUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the Message types and Views implementation to the JavaScript.
 */
public final class JavaScriptMessageFactory {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptMessageFactory.class.getName());
    private static final String JAVA_SCRIPT_MESSAGE_VIEW_HTTP = "http";
    private static final String JAVA_SCRIPT_MESSAGE_VIEW_HEADERS = "headers";
    private static final String JAVA_SCRIPT_MESSAGE_VIEW_HTTP_REQUEST = "request:http";
    private static final String JAVA_SCRIPT_MESSAGE_VIEW_HTTP_RESPONSE = "response:http";
    private static final String JAVA_SCRIPT_MESSAGE_VIEW_URL = "url";
    private static final String JAVA_SCRIPT_MESSAGE_VIEW_REQUEST_URL = "request:url";

    private Map<String, Function<JavaScriptMessage, JavaScriptMessageView>> viewFactoryMap = new HashMap<>();

    private JavaScriptMessageFactory() {
        viewFactoryMap.put(JAVA_SCRIPT_MESSAGE_VIEW_HTTP_REQUEST, message -> new HttpRequestView((JavaScriptHttpRequestMessage) message));
        viewFactoryMap.put(JAVA_SCRIPT_MESSAGE_VIEW_HTTP_RESPONSE, message -> new HttpResponseView((JavaScriptHttpResponseMessage) message));
        viewFactoryMap.put(JAVA_SCRIPT_MESSAGE_VIEW_HEADERS, message -> new HeadersView(message));
        viewFactoryMap.put(JAVA_SCRIPT_MESSAGE_VIEW_REQUEST_URL, message -> new UrlView((JavaScriptHttpRequestMessage) message));
    }

    private static class JavaScriptMessageFactoryLazyInitializer {
        private static final JavaScriptMessageFactory INSTANCE = new JavaScriptMessageFactory();
    }

    public static JavaScriptMessageFactory getInstance() {
        return JavaScriptMessageFactoryLazyInitializer.INSTANCE;
    }

    private JavaScriptMessageView getMessageView(final String view, final JavaScriptMessage javaScriptMessage) throws NoSuchVariableException {
        try {
            return viewFactoryMap.get(view).apply(javaScriptMessage);
        } catch (NullPointerException | ClassCastException e) {
            LOGGER.log(Level.WARNING, () -> String.format("Failed to create message view (%s): %s", view, ExceptionUtils.getMessage(e)));
            throw new NoSuchVariableException(view, "This " + view + " is not supported");
        }
    }

    private String getResolvedView(final Message message, final String view) {
        if (JAVA_SCRIPT_MESSAGE_VIEW_HTTP.equalsIgnoreCase(view)) {
            if (message.isHttpRequest()) {
                return JAVA_SCRIPT_MESSAGE_VIEW_HTTP_REQUEST;
            } else if (message.isHttpResponse()) {
                return JAVA_SCRIPT_MESSAGE_VIEW_HTTP_RESPONSE;
            }
        } else if (message.isHttpRequest() && JAVA_SCRIPT_MESSAGE_VIEW_URL.equalsIgnoreCase(view)) {
            return JAVA_SCRIPT_MESSAGE_VIEW_REQUEST_URL;
        }
        return view;
    }

    public JavaScriptMessageView get(final Message message, final String view, final ScriptObjectMirror scriptObjectMirror) throws NoSuchVariableException {
        return getMessageView(getResolvedView(message, view), get(message, scriptObjectMirror));
    }

    public JavaScriptMessage get(final Message message, final ScriptObjectMirror scriptObjectMirror) {
        if (message.isHttpRequest()) {
            return new JavaScriptHttpRequestMessageImpl(message, scriptObjectMirror);
        } else if (message.isHttpResponse()) {
            return new JavaScriptHttpResponseMessageImpl(message, scriptObjectMirror);
        }
        return new JavaScriptMessageImpl(message, scriptObjectMirror);
    }
}


