package com.l7tech.message;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.util.Functions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stub implementation of HttpRequestKnob, for testing.
 * Feel free to add constructors and mutators to this as needed to provide more test services
 * for components that need an HttpRequestKnob.
 */
public class HttpRequestKnobStub extends HttpRequestKnobAdapter {
    private final GenericHttpRequestParams headerHolder = new GenericHttpRequestParams();

    public HttpRequestKnobStub() {
    }

    public HttpRequestKnobStub(List<HttpHeader> headers) {
        for (HttpHeader header : headers)
            headerHolder.addExtraHeader(header);
    }

    public String[] getHeaderNames() {
        try {
            Collection<String> ret = Functions.map(headerHolder.getExtraHeaders(), Functions.<String, HttpHeader>getterTransform(GenericHttpHeader.class.getMethod("getName")));
            return ret.toArray(new String[ret.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getHeaderValues(String name) {
        List<String> ret = new ArrayList<String>();
        for (HttpHeader header : headerHolder.getExtraHeaders()) {
            if (header.getName().equalsIgnoreCase(name))
                ret.add(header.getFullValue());
        }
        return ret.toArray(new String[ret.size()]);
    }

    public String getHeaderSingleValue(String name) throws IOException {
        String[] ret = getHeaderValues(name);
        if (ret.length > 1) throw new IOException("More than one value for header " + name);
        return ret.length < 1 ? null : ret[0];
    }

    public void replaceHeader(String name, String value) {
        replaceHeader(new GenericHttpHeader(name, value));
    }

    public void replaceHeader(HttpHeader header) {
        headerHolder.replaceExtraHeader(header);
    }

    public void addHeader(String name, String value) {
        addHeader(new GenericHttpHeader(name, value));
    }

    public void addHeader(HttpHeader header) {
        headerHolder.addExtraHeader(header);
    }

    public boolean removeHeader(String name) {
        return headerHolder.removeExtraHeader(name);
    }
}
