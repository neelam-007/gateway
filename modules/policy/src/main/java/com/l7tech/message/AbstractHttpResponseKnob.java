/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.message;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * An abstract skeleton of an HttpResponseKnob implementation.
 */
public abstract class AbstractHttpResponseKnob implements HttpResponseKnob {
    protected final OutboundHeaderSupport headerSupport = new OutboundHeaderSupport();
    protected final List<String> challengesToSend = new ArrayList<String>();
    protected int statusToSet;

    @Override
    public void setDateHeader( final String name, final long date ) {
        headerSupport.setDateHeader( name, date );
    }

    @Override
    public void addDateHeader( final String name, final long date ) {
        headerSupport.addDateHeader( name, date );
    }

    @Override
    public void setHeader( final String name, final String value ) {
        headerSupport.setHeader( name, value );
    }

    @Override
    public void addHeader( final String name, final String value ) {
        headerSupport.addHeader( name, value );
    }

    @Override
    public String[] getHeaderValues( final String name ) {
        return headerSupport.getHeaderValues( name );
    }

    @Override
    public String[] getHeaderNames() {
        return headerSupport.getHeaderNames( );
    }

    @Override
    public boolean containsHeader( final String name ) {
        return headerSupport.containsHeader( name );
    }

    @Override
    public void removeHeader( final String name ) {
        headerSupport.removeHeader( name );
    }

    @Override
    public void removeHeader(final String name, final Object value) {
        headerSupport.removeHeader( name, value );
    }

    @Override
    public void clearHeaders() {
        headerSupport.clearHeaders();
    }

    @Override
    public void writeHeaders( final GenericHttpRequestParams target ) {
        headerSupport.writeHeaders( target );
    }

    @Override
    public void writeHeaders(GenericHttpRequestParams target, String headerName) {
        headerSupport.writeHeaders(target, headerName);
    }

    @Override
    public void addChallenge(String value) {
        challengesToSend.add(value);
    }

    @Override
    public void setStatus(int code) {
        statusToSet = code;
    }

    @Override
    public int getStatus() {
        return statusToSet;
    }

    protected List<Pair<String, Object>> getHeadersToSend() {
        return headerSupport.headersToSend;
    }
}
