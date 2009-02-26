/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.management.api.monitoring.NotificationAttempt;
import com.l7tech.server.management.config.monitoring.AuthInfo;
import com.l7tech.server.management.config.monitoring.Header;
import com.l7tech.server.management.config.monitoring.HttpNotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.processcontroller.monitoring.InOut;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

class HttpNotifier extends Notifier<HttpNotificationRule> {
    private static final Logger logger = Logger.getLogger(HttpNotifier.class.getName());

    private final Auditor auditor = new LogOnlyAuditor(logger);
    private final GenericHttpClient httpClient;
    private final HttpMethod httpMethod;
    private final ContentTypeHeader contentType;
    private final List<Header> extraHeaders;
    private final AuthInfo authInfo;

    public HttpNotifier(HttpNotificationRule rule) {
        super(rule);
        httpClient = new UrlConnectionHttpClient();
        httpMethod = this.rule.getMethod();
        authInfo = this.rule.getAuthInfo();
        final List<Header> heads = this.rule.getHeaders();
        extraHeaders = heads == null ? new ArrayList<Header>() : heads;
        if (httpMethod.needsRequestBody()) {
            try {
                final String ctval = this.rule.getContentType();
                contentType = ctval == null ? ContentTypeHeader.XML_DEFAULT : ContentTypeHeader.parseValue(ctval);
            } catch (IOException e) {
                throw new IllegalArgumentException("HTTP notifier: Bad content type: " + e);
            }
        } else {
            contentType = null;
        }
    }

    public NotificationAttempt.StatusType doNotification(Long timestamp, InOut inOut, Object value, Trigger trigger) throws IOException {
        String bodyText = rule.getRequestBody() == null ? null : ExpandVariables.process(rule.getRequestBody(), getMonitoringVariables(trigger, inOut, value), auditor);

        GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(rule.getUrl()));
        params.setContentType(contentType);
        for (Header header : extraHeaders)
            params.addExtraHeader(new GenericHttpHeader(header));

        if (authInfo != null) {
            params.setPasswordAuthentication(new PasswordAuthentication(authInfo.getUsername(), authInfo.getPassword()));
            params.setPreemptiveAuthentication(true);
        }

        GenericHttpRequest req = null;
        GenericHttpResponse resp = null;
        try {
            req = httpClient.createRequest(httpMethod, params);
            if (bodyText != null) req.setInputStream(new ByteArrayInputStream(bodyText.getBytes("UTF-8")));

            resp = req.getResponse();
            if (resp.getStatus() != HttpConstants.STATUS_OK)
                throw new IOException("HTTP server responded with status " + resp.getStatus());
            
            return NotificationAttempt.StatusType.ACKNOWLEDGED;
        } finally {
            ResourceUtils.closeQuietly(req);
            ResourceUtils.closeQuietly(resp);
        }
    }
}
