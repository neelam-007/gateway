/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

/**
 * Provides an HttpResponseKnob for a Message.
 */
public class HttpResponseFacet extends MessageFacet {
    private final HttpResponseKnob httpResponseKnob;

    /**
     * Create an HttpResponseFacet that will expose the specified HttpResponseKnob.
     *
     * @param message  the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     */
    public HttpResponseFacet(Message message, MessageFacet delegate, HttpResponseKnob httpResponseKnob) {
        super(message, delegate);
        if (httpResponseKnob == null) throw new NullPointerException();
        this.httpResponseKnob = httpResponseKnob;
    }

    public MessageKnob getKnob(Class c) {
        if (c == HttpResponseKnob.class)
            return httpResponseKnob;
        return super.getKnob(c);
    }
}
