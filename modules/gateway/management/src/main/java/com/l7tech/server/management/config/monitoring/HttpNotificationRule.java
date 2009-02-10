/**
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.management.config.monitoring;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for an HTTP {@link NotificationRule}.
 */
public class HttpNotificationRule extends NotificationRule {
    private String url;
    private HttpMethod method;
    private String requestBody;
    private AuthInfo authInfo;
    private String contentType;
    private List<Pair<String, String>> extraHeaders = new ArrayList<Pair<String, String>>();

    public HttpNotificationRule(MonitoringConfiguration configuration) {
        super(configuration, Type.HTTP);
    }

    protected HttpNotificationRule() {
        super(Type.HTTP);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public AuthInfo getAuthInfo() {
        return authInfo;
    }

    public void setAuthInfo(AuthInfo authInfo) {
        this.authInfo = authInfo;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public List<Pair<String, String>> getExtraHeaders() {
        return extraHeaders;
    }

    public void setExtraHeaders(List<Pair<String, String>> extraHeaders) {
        this.extraHeaders = extraHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        HttpNotificationRule that = (HttpNotificationRule) o;

        if (authInfo != null ? !authInfo.equals(that.authInfo) : that.authInfo != null) return false;
        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
        if (extraHeaders != null ? !extraHeaders.equals(that.extraHeaders) : that.extraHeaders != null) return false;
        if (method != that.method) return false;
        if (requestBody != null ? !requestBody.equals(that.requestBody) : that.requestBody != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (requestBody != null ? requestBody.hashCode() : 0);
        result = 31 * result + (authInfo != null ? authInfo.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + (extraHeaders != null ? extraHeaders.hashCode() : 0);
        return result;
    }
}
