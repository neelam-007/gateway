package com.l7tech.external.assertions.js.features.bindings.views;

import com.l7tech.external.assertions.js.features.bindings.JavaScriptMessage;

/**
 * Abstract implementation providing the HTTP view of the HTTP message. Provides access all HTTP components of the HTTP
 * message.
 */
@SuppressWarnings("unused")
public abstract class HttpView extends HeadersView {

    private String contentType;

    public HttpView(final JavaScriptMessage javaScriptMessage) {
        super(javaScriptMessage);
        this.contentType = javaScriptMessage.getContentType();
    }

    /**
     * Gets the payload of the HTTP message.
     * @return
     */
    public Object getContent() {
        return javaScriptMessage.getContent();
    }

    /**
     * Sets the payload of the HTTP message.
     * @param content
     * @param contentType
     */
    public void setContent(final Object content, final String contentType) {
        if (!closed) {
            javaScriptMessage.setContent(content, contentType);
            this.contentType = javaScriptMessage.getContentType();
        }
    }

    /**
     * Gets the content type of the HTTP message.
     * @return
     */
    public String getContentType() {
        return contentType;
    }
}
