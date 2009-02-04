/*
 * Copyright (C) 2009 Layer 7 Technologies Inc.
 */
package com.l7tech.server.processcontroller.monitoring.notification;

import com.l7tech.common.http.*;
import com.l7tech.common.http.prov.jdk.UrlConnectionHttpClient;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.management.config.monitoring.AuthInfo;
import com.l7tech.server.management.config.monitoring.HttpNotificationRule;
import com.l7tech.server.management.config.monitoring.Trigger;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
class HttpNotifier extends Notifier<HttpNotificationRule> {
    private static final Logger logger = Logger.getLogger(HttpNotifier.class.getName());

    private final Auditor auditor = new LogOnlyAuditor(logger);
    private final GenericHttpClient httpClient;
    private final HttpMethod httpMethod;
    private final ContentTypeHeader contentType;
    private final List<Pair<String, String>> extraHeaders;
    private final AuthInfo authInfo;

    protected HttpNotifier(HttpNotificationRule rule) {
        super(rule);
        httpClient = new UrlConnectionHttpClient();
        httpMethod = this.rule.getMethod();
        authInfo = this.rule.getAuthInfo();
        final List<Pair<String, String>> heads = this.rule.getExtraHeaders();
        extraHeaders = heads == null ? new ArrayList<Pair<String, String>>() : heads;
        try {
            contentType = ContentTypeHeader.parseValue(this.rule.getContentType());
        } catch (IOException e) {
            throw new IllegalArgumentException("HTTP notifier: Bad content type: " + e);
        }
    }

    public void doNotification(Long timestamp, Object value, Trigger trigger) throws IOException {
        String bodyText = ExpandVariables.process(rule.getRequestBody(), getMonitoringVariables(trigger), auditor);

        GenericHttpRequestParams params = new GenericHttpRequestParams(new URL(rule.getUrl()));
        params.setContentType(contentType);
        for (Pair<String, String> header : extraHeaders)
            params.addExtraHeader(new GenericHttpHeader(header));

        if (authInfo != null) {
            params.setPasswordAuthentication(new PasswordAuthentication(authInfo.getUsername(), authInfo.getPassword()));
            params.setPreemptiveAuthentication(true);
        }

        GenericHttpRequest req = null;
        GenericHttpResponse resp = null;
        try {
            req = httpClient.createRequest(httpMethod, params);
            req.setInputStream(new ByteArrayInputStream(bodyText.getBytes("UTF-8")));

            resp = req.getResponse();
            if (resp.getStatus() != HttpConstants.STATUS_OK)
                throw new IOException("HTTP server responded with status " + resp.getStatus());

        } finally {
            ResourceUtils.closeQuietly(req);
            ResourceUtils.closeQuietly(resp);
        }
    }
}
