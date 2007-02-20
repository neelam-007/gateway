/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.ExpandVariables;
import com.l7tech.policy.variable.VariableMetadata;
import org.apache.commons.lang.StringUtils;

/**
 *
 * @author mike
 * @version 1.0
 */
public class HttpRoutingAssertion
        extends RoutingAssertion
        implements UsesVariables, SetsVariables
{
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = -1;
    public static final String VAR_ROUTING_LATENCY = "httpRouting.latency";
    public static final String VAR_SERVICE_URL = "service.url";
    public static final String PROP_SSL_SESSION_TIMEOUT =
            HttpRoutingAssertion.class.getName() + ".sslSessionTimeoutSeconds";
    public static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;

    protected String protectedServiceUrl;
    protected String login;
    protected String password;
    protected String realm;
    protected String ntlmHost;
    protected String userAgent;
    protected int maxConnections;
    protected boolean passthroughHttpAuthentication;
    protected String[] customIpAddresses = null;
    protected String failoverStrategyName = "ordered";
    private boolean taiCredentialChaining = false;
    protected Integer connectionTimeout;
    protected Integer timeout;
    protected HttpPassthroughRuleSet responseHeaderRules = new HttpPassthroughRuleSet(false,
                                                            new HttpPassthroughRule[]{
                                                             new HttpPassthroughRule("Set-Cookie", false, null)});
    protected HttpPassthroughRuleSet requestHeaderRules = new HttpPassthroughRuleSet(false,
                                                           new HttpPassthroughRule[]{
                                                             new HttpPassthroughRule("Cookie", false, null),
                                                             new HttpPassthroughRule("SOAPAction", false, null)});
    protected HttpPassthroughRuleSet requestParamRules = new HttpPassthroughRuleSet(true, new HttpPassthroughRule[]{});
    protected boolean followRedirects = false;
    protected boolean failOnErrorStatus = true;

    public HttpRoutingAssertion() {
        this(null, null, null, null);
    }

    public HttpRoutingAssertion(String protectedServiceUrl) {
        this(protectedServiceUrl, null, null, null);
    }


    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm) {
        this(protectedServiceUrl, login, password, realm, DEFAULT_MAX_CONNECTIONS_PER_HOST);
    }

    /**
     * Constructor.
     *
     * @param protectedServiceUrl the service url
     * @param login               protected service login
     * @param password            protected service password
     * @param realm               protected servcie realm
     */
    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm, int maxConnections) {
        this(protectedServiceUrl, login, password, realm, maxConnections, true);
    }

    /**
     * Full constructor.
     *
     * @param protectedServiceUrl the service url
     * @param login               protected service login
     * @param password            protected service password
     * @param realm               protected servcie realm
     * @param copyCookies         true to copy cookies to the service request
     */
    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm, int maxConnections, boolean copyCookies) {
        this.protectedServiceUrl = protectedServiceUrl;
        this.login = login;
        this.password = password;
        this.realm = realm;
        this.passthroughHttpAuthentication = false;
        this.maxConnections = maxConnections;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public boolean isFailOnErrorStatus() {
        return failOnErrorStatus;
    }

    public void setFailOnErrorStatus(boolean failOnErrorStatus) {
        this.failOnErrorStatus = failOnErrorStatus;
    }

    public HttpPassthroughRuleSet getResponseHeaderRules() {
        return responseHeaderRules;
    }

    public void setResponseHeaderRules(HttpPassthroughRuleSet responseHeaderRules) {
        this.responseHeaderRules = responseHeaderRules;
    }

    public HttpPassthroughRuleSet getRequestHeaderRules() {
        return requestHeaderRules;
    }

    public void setRequestHeaderRules(HttpPassthroughRuleSet requestHeaderRules) {
        this.requestHeaderRules = requestHeaderRules;
    }

    public HttpPassthroughRuleSet getRequestParamRules() {
        return requestParamRules;
    }

    public void setRequestParamRules(HttpPassthroughRuleSet requestParamRules) {
        this.requestParamRules = requestParamRules;
    }


    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public boolean isPassthroughHttpAuthentication() {
        return passthroughHttpAuthentication;
    }

    public void setPassthroughHttpAuthentication(boolean passthroughHttpAuthentication) {
        this.passthroughHttpAuthentication = passthroughHttpAuthentication;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * This is really the NTLM domain--the client never actually sends the realm
     */
    public String getRealm() {
        return realm;
    }

    /**
     * This is really the NTLM domain--the client never actually sends the realm
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getProtectedServiceUrl() {
        return protectedServiceUrl;
    }

    public void setProtectedServiceUrl(String protectedServiceUrl) {
        this.protectedServiceUrl = protectedServiceUrl;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String toString() {
        return super.toString() + " url=" + getProtectedServiceUrl() + " login=" + getLogin() + " realm=" + getRealm();
    }

    /** @return the custom IP addresses to use as an array of String, or null if no custom IP address list is configured. */
    public String[] getCustomIpAddresses() {
        return customIpAddresses;
    }

    /** @param customIpAddresses custom addresses to use, or null if no custom addresses should be used. */
    public void setCustomIpAddresses(String[] customIpAddresses) {
        this.customIpAddresses = customIpAddresses;
    }

    /**
     * @return the name of the FailoverStrategy to use with the CustomIpAddresses.
     * @see com.l7tech.common.io.failover.FailoverStrategyFactory
     */
    public String getFailoverStrategyName() {
        return failoverStrategyName;
    }

    /**
     * @param failoverStrategyName the name of the FailoverStrategy to use with the CustomIpAddresses.
     * @see com.l7tech.common.io.failover.FailoverStrategyFactory
     */
    public void setFailoverStrategyName(String failoverStrategyName) {
        this.failoverStrategyName = failoverStrategyName;
    }

    /** Subclasses can choose to offer this functionality by adding a public method that chains to this one. */
    protected void copyFrom(HttpRoutingAssertion source) {
        super.copyFrom(source);
        this.setCustomIpAddresses(source.getCustomIpAddresses());
        this.setFailoverStrategyName(source.getFailoverStrategyName());
        this.setLogin(source.getLogin());
        this.setMaxConnections(source.getMaxConnections());
        this.setPassword(source.getPassword());
        this.setProtectedServiceUrl(source.getProtectedServiceUrl());
        this.setPassthroughHttpAuthentication(source.isPassthroughHttpAuthentication());
        this.setRealm(source.getRealm());
        this.setUserAgent(source.getUserAgent());
        this.setTaiCredentialChaining(source.isTaiCredentialChaining());
        this.setNtlmHost(source.getNtlmHost());
        this.setConnectionTimeout(source.getConnectionTimeout());
        this.setTimeout(source.getTimeout());
        this.setResponseHeaderRules(source.getResponseHeaderRules());
    }

    public String[] getVariablesUsed() {
        StringBuffer tmp = new StringBuffer();
        if (!StringUtils.isEmpty(login)) tmp.append(login);
        if (!StringUtils.isEmpty(password)) tmp.append(password);
        if (!StringUtils.isEmpty(protectedServiceUrl)) tmp.append(protectedServiceUrl);
        if (!StringUtils.isEmpty(ntlmHost)) tmp.append(ntlmHost);
        if (!StringUtils.isEmpty(realm)) tmp.append(realm);

        HttpPassthroughRuleSet[] ruleset = {responseHeaderRules, requestHeaderRules, requestParamRules};
        for (int i = 0; i < ruleset.length; i++) {
            HttpPassthroughRuleSet rules = ruleset[i];
            if (!rules.isForwardAll()) {
                for (int j = 0; j < rules.getRules().length; j++) {
                    HttpPassthroughRule rule = rules.getRules()[j];
                    if (!StringUtils.isEmpty(rule.getCustomizeValue())) tmp.append(rule.getCustomizeValue());
                }
            }
        }
        return ExpandVariables.getReferencedNames(tmp.toString());
    }

    public VariableMetadata[] getVariablesSet() {
        return new VariableMetadata[] {
            new VariableMetadata(VAR_ROUTING_LATENCY, false, false, VAR_ROUTING_LATENCY, false),
            new VariableMetadata(VAR_SERVICE_URL, false, false, VAR_SERVICE_URL, false)
        };
    }

    public boolean isTaiCredentialChaining() {
        return taiCredentialChaining;
    }

    public void setTaiCredentialChaining(boolean taiCredentialChaining) {
        this.taiCredentialChaining = taiCredentialChaining;
    }

    public String getNtlmHost() {
        return ntlmHost;
    }

    public void setNtlmHost(String ntlmHost) {
        this.ntlmHost = ntlmHost;
    }
}
