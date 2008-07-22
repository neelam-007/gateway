/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.http;

import com.l7tech.common.mime.ContentTypeHeader;

/**
 * Bean that holds information about an HTTP response.
 */
public class GenericHttpResponseParamsImpl implements GenericHttpResponseParams {
    private int status;
    private HttpHeaders headers;
    private ContentTypeHeader contentType;
    private Long contentLength;

    public GenericHttpResponseParamsImpl() {
    }

    public GenericHttpResponseParamsImpl(GenericHttpResponseParams source) {
        this.status = source.getStatus();
        this.headers = source.getHeaders();
        this.contentType = source.getContentType();
        this.contentLength = source.getContentLength();
    }

    public GenericHttpResponseParamsImpl(int status, HttpHeaders headers, ContentTypeHeader contentType, Long contentLength) {
        this.status = status;
        this.headers = headers;
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public ContentTypeHeader getContentType() {
        return contentType;
    }

    public void setContentType(ContentTypeHeader contentType) {
        this.contentType = contentType;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public void setContentLength(Long contentLength) {
        this.contentLength = contentLength;
    }
}
