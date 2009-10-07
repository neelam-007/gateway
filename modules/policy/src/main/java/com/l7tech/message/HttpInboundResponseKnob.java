package com.l7tech.message;

import com.l7tech.common.http.HttpHeadersHaver;
import com.l7tech.common.http.HttpHeader;

/**
 * Knob that records transport-level information about a Message that was populated from an HTTP response.
 */
public interface HttpInboundResponseKnob extends MessageKnob, HasHeaders {
    /**
     * Provide a source of headers that will be read lazily if headers are needed.  Any previous headers
     * or header source will be forgotten.
     * <p/>
     * Caller is responsible for ensuring that the header source will remain valid for at least
     * as long as this message knob remains open.
     * <p/>
     * This knob will not take ownership of the header source -- it will simply assume it can use it
     * for as long as it has access to it.
     */
    void setHeaderSource(HttpHeadersHaver headerSource);

    /**
     * Returns true if the response contained at least one value for the header with the specified name.
     * @param name The name of the header to test
     * @return true if the response contains at least one value for the specified header, otherwise false.
     */
    boolean containsHeader(String name);

    /**
     * Get the raw array of headers.
     * @return the raw header array.  May be empty but never null.  Please do not modify it!
     */
    HttpHeader[] getHeadersArray();
}
