package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;

import java.io.Serializable;
import java.util.Map;

public class RestRequest implements Serializable {
    private String uri;
    private String queryString;
    private HttpMethod method;
    private String contentType;
    private String body;
    private Map<String,String> headers;

    public RestRequest(String uri, String queryString, HttpMethod method, String contentType, String body, Map<String,String> headers) {
        this.uri = uri;
        this.queryString = queryString;
        this.method = method;
        this.contentType = contentType;
        this.body = body;
        this.headers = headers;
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

    public Map<String, String> getHeaders() {
        return headers;
    }
}
