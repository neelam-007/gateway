package com.l7tech.message;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.util.Functions;
import com.l7tech.util.IteratorEnumeration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Stub implementation of HttpRequestKnob, for testing.
 * Feel free to add constructors and mutators to this as needed to provide more test services
 * for components that need an HttpRequestKnob.
 */
public class HttpRequestKnobStub extends HttpRequestKnobAdapter {
    private final GenericHttpRequestParams headerHolder = new GenericHttpRequestParams();
    private final String requestUri;
    private Map<String,String[]> params;

    public HttpRequestKnobStub() {
        this( null );
    }

    public HttpRequestKnobStub(List<HttpHeader> headers) {
        this( headers, null );
    }

    public HttpRequestKnobStub( final List<HttpHeader> headers,
                                final String requestUri ) {
        for (HttpHeader header : headers==null?Collections.<HttpHeader>emptyList():headers)
            headerHolder.addExtraHeader(header);
        this.requestUri = requestUri;
    }

    @Override
    public String getRequestUri() {
        return requestUri;
    }

    @Override
    public String[] getHeaderNames() {
        try {
            Collection<String> ret = Functions.map(headerHolder.getExtraHeaders(), Functions.<String, HttpHeader>getterTransform(GenericHttpHeader.class.getMethod("getName")));
            return ret.toArray(new String[ret.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] getHeaderValues(String name) {
        List<String> ret = new ArrayList<String>();
        for (HttpHeader header : headerHolder.getExtraHeaders()) {
            if (header.getName().equalsIgnoreCase(name))
                ret.add(header.getFullValue());
        }
        return ret.toArray(new String[ret.size()]);
    }

    @Override
    public String getHeaderSingleValue(String name) throws IOException {
        String[] ret = getHeaderValues(name);
        if (ret.length > 1) throw new IOException("More than one value for header " + name);
        return ret.length < 1 ? null : ret[0];
    }

    @Override
    public String getHeaderFirstValue( final String name ) {
        final String[] values = getHeaderValues( name );
        return values==null||values.length<1 ? null : values[0];
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

    @Override
    public String getParameter(String name) {
        prepareParams();
        String[] got = params.get(name);
        return got == null || got.length < 1 ? null : got[0];
    }

    @Override
    public Map getParameterMap() {
        prepareParams();
        TreeMap<String, String[]> ret = new TreeMap<String,String[]>(String.CASE_INSENSITIVE_ORDER);
        ret.putAll(params);
        return ret;
    }

    @Override
    public String[] getParameterValues(String s) {
        prepareParams();
        String[] ret = params.get(s);
        return ret == null ? new String[0] : ret;
    }

    @Override
    public Enumeration getParameterNames() {
        prepareParams();
        return new IteratorEnumeration<String>(params.keySet().iterator());
    }

    private void prepareParams() {
        if (params == null) {
            Map<String,String[]> map = new TreeMap<String,String[]>(String.CASE_INSENSITIVE_ORDER);
            String[] pairs = getQueryString().split("\\&");
            for (String pair : pairs) {
                String[] keyval = pair.split("\\=");
                if (keyval.length > 0) {
                    String name = keyval[0];
                    String value = keyval.length > 1 ? keyval[1] : null;
                    String[] oldValues = map.get(name);
                    if (oldValues == null)
                        oldValues = new String[0];
                    String[] newValues = new String[oldValues.length + 1];
                    System.arraycopy(oldValues, 0, newValues, 0, oldValues.length);
                    newValues[oldValues.length] = value;
                    map.put(name, newValues);
                }
            }

            // TODO include form body if present?
            params = map;
        }
    }

    @Override
    public String getQueryString() {
        try {
            return new URL(requestUri).getQuery();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
