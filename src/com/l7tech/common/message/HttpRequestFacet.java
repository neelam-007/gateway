/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

/**
 * Provides an HttpRequestKnob for a Message.
 */
public class HttpRequestFacet extends MessageFacet {
    private final HttpRequestKnob httpRequestKnob;

    /**
     * Create an HttpRequestFacet that will expose the specified HttpRequestKnob (which also exposes a TcpKnob).
     * 
     * @param message  the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     * @param httpRequestKnob the MessageKnob that provides HTTP-level information about this request Message.
     */
    public HttpRequestFacet(Message message, MessageFacet delegate, HttpRequestKnob httpRequestKnob) {
        super(message, delegate);
        if (httpRequestKnob == null) throw new NullPointerException();
        this.httpRequestKnob = httpRequestKnob;
    }

    public MessageKnob getKnob(Class c) {
        if (c == HttpRequestKnob.class || c == TcpKnob.class || c == HasSoapAction.class)
            return httpRequestKnob;
        if (c == HttpServletRequestKnob.class && httpRequestKnob instanceof HttpServletRequestKnob)
            return httpRequestKnob;
        return super.getKnob(c);
    }
}
