package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;

import java.io.Serializable;

public class RestRequest implements Serializable {
    private String uri;
    private String queryString;
    private HttpMethod method;
    private String contentType;
    private String body;

    public RestRequest(String uri, String queryString, HttpMethod method, String contentType, String body) {
        this.uri = uri;
        this.queryString = queryString;
        this.method = method;
        this.contentType = contentType;
        this.body = body;
    }

    public String getUri() {
        return uri;
    }

    public String getQueryString() {
        return queryString;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getContentType() {
        return contentType;
    }

    public String getBody() {
        return body;
    }
}
