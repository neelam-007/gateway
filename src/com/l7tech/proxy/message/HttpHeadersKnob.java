/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.message;

import com.l7tech.common.message.MessageKnob;
import com.l7tech.proxy.datamodel.HttpHeaders;

/**
 * A MessageKnob that provides possibly-lazy access to a set of HttpHeaders.
 */
public class HttpHeadersKnob implements MessageKnob {
    private final HttpHeaders headers;

    /**
     * Make an HttpHeadersKnob that will publish the specified HTTP headers.
     *
     * @param headers the HTTP headers to publish.  Must not be null.
     */
    public HttpHeadersKnob(HttpHeaders headers) {
        if (headers == null) throw new NullPointerException();
        this.headers = headers;
    }

    /**
     * Get the HTTP headers published by this knob.
     *
     * @return the HTTP headers.  Never null.
     */
    public HttpHeaders getHeaders() {
        return headers;
    }
}
