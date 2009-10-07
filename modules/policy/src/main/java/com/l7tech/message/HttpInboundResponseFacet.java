package com.l7tech.message;

import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.HttpHeadersHaver;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Facet that implements HttpInboundResponseKnob and can be attached to a Message to record results
 * of outbound HTTP routing, specifically the raw HTTP status and headers returned with the response.
 * <p/>
 * The HttpResponseKnob is not suitable for this purpose as it contains only information relevant to
 * preparing the response that will eventually be sent to the client (and so only knows about headers
 * that are to be forwarded).
 */
public class HttpInboundResponseFacet implements HttpInboundResponseKnob, Closeable {
    private static final HttpHeader[] EMPTY_HTTP_HEADER_ARRAY = new HttpHeader[0];
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    HttpHeadersHaver headersSource;
    HttpHeader[] headers;

    private void initHeaders() {
        if (headers == null && headersSource != null) {
            headers = headersSource.getHeaders().toArray();
        }
    }

    @Override
    public void setHeaderSource(HttpHeadersHaver headerSource) {
        headers = null;
        headersSource = headerSource;
    }

    @Override
    public String[] getHeaderValues(String name) {
        initHeaders();
        if (headers == null)
            return EMPTY_STRING_ARRAY;

        List<String> ret = new ArrayList<String>();
        for (HttpHeader header : headers) {
            if (name.equalsIgnoreCase(header.getName())) {
                ret.add(header.getFullValue());
            }
        }

        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public boolean containsHeader(String name) {
        initHeaders();
        if (headers == null)
            return false;

        for (HttpHeader header : headers) {
            if (name.equalsIgnoreCase(header.getName()))
                return true;
        }

        return false;
    }

    @Override
    public HttpHeader[] getHeadersArray() {
        initHeaders();
        if (headers == null)
            return EMPTY_HTTP_HEADER_ARRAY;
        return headers;
    }

    @Override
    public void close() throws IOException {
        headersSource = null;
        headers = null;
    }
}
