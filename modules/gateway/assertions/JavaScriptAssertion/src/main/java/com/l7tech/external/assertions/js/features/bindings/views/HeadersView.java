package com.l7tech.external.assertions.js.features.bindings.views;

import com.l7tech.external.assertions.js.features.bindings.JavaScriptMessage;

/**
 * Provides the headers view of the message. Can be accessed using context.getVariable('<MESSAGE_VAR>:header');
 */
@SuppressWarnings("unused")
public class HeadersView extends JavaScriptMessageView {

    private Object headers;

    public HeadersView(final JavaScriptMessage javaScriptMessage) {
        super(javaScriptMessage);
        this.headers = javaScriptMessage.getHeaders();
    }

    /**
     * Gets the headers of the message.
     * @return headers
     */
    public Object getHeaders() {
        return headers;
    }

    /**
     * Sets the headers of the message.
     * @param headers
     */
    public void setHeaders(final Object headers) {
        this.headers = headers;
    }

    /**
     * Flush all the headers to the message when <VARIABLE>.end() is called.
     */
    @Override
    protected void flush() {
        javaScriptMessage.setHeaders(headers);
    }
}
