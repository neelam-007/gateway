package com.l7tech.external.assertions.js.features.bindings.views;

import com.l7tech.external.assertions.js.features.bindings.JavaScriptMessage;

/**
 * Abstract implementation of different views of JavaScriptMessage
 */
@SuppressWarnings("unused")
public abstract class JavaScriptMessageView {

    protected final JavaScriptMessage javaScriptMessage;
    protected boolean closed;

    public JavaScriptMessageView(final JavaScriptMessage javaScriptMessage) {
        this.javaScriptMessage = javaScriptMessage;
    }

    /**
     * Ends the processing of the view. All updatable values like headers and statusCode are written to message.
     */
    public void end() {
        if (!closed) {
            flush();
            closed = true;
        }
    }

    /**
     * Flush all updatable values to the message. Implemented by the view.
     */
    protected abstract void flush();
}
