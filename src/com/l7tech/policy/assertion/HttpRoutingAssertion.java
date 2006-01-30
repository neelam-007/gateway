/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.variable.ExpandVariables;

/**
 *
 * @author mike
 * @version 1.0
 */
public class HttpRoutingAssertion
        extends RoutingAssertion
        implements UsesVariables, SetsVariables
{
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 100; // Flagrantly in contravention of RFC2616!
    public static final String ROUTING_LATENCY = "httpRouting.latency";

    public HttpRoutingAssertion() {
        this(null, null, null, null);
    }

    public HttpRoutingAssertion(String protectedServiceUrl) {
        this(protectedServiceUrl, null, null, null);
    }


    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm) {
        this(protectedServiceUrl, login, password, realm, -1);
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
        _protectedServiceUrl = protectedServiceUrl;
        _login = login;
        _password = password;
        _realm = realm;

        if (maxConnections == -1) maxConnections = DEFAULT_MAX_CONNECTIONS_PER_HOST;
        _maxConnections = maxConnections;
        _copyCookies = copyCookies;
    }

    public Object clone() throws CloneNotSupportedException {
        RoutingAssertion n = (RoutingAssertion)super.clone();
        return n;
    }

    public int getMaxConnections() {
        return _maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        _maxConnections = maxConnections;
    }

    public boolean isCopyCookies() {
        return _copyCookies;
    }

    public void setCopyCookies(boolean copyCookies) {
        this._copyCookies = copyCookies;
    }

    public String getLogin() {
        return _login;
    }

    public void setLogin(String login) {
        _login = login;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = password;
    }

    public String getRealm() {
        return _realm;
    }

    public void setRealm(String realm) {
        _realm = realm;
    }

    public String getProtectedServiceUrl() {
        return _protectedServiceUrl;
    }

    public void setProtectedServiceUrl(String protectedServiceUrl) {
        this._protectedServiceUrl = protectedServiceUrl;
    }

    public String getUserAgent() {
        return _userAgent;
    }

    public void setUserAgent(String userAgent) {
        _userAgent = userAgent;
    }

    public String toString() {
        return super.toString() + " url=" + getProtectedServiceUrl() + " login=" + getLogin() + " realm=" + getRealm();
    }

    /** @return the custom IP addresses to use as an array of String, or null if no custom IP address list is configured. */
    public String[] getCustomIpAddresses() {
        return _customIpAddresses;
    }

    /** @param customIpAddresses custom addresses to use, or null if no custom addresses should be used. */
    public void setCustomIpAddresses(String[] customIpAddresses) {
        _customIpAddresses = customIpAddresses;
    }

    /**
     * @return the name of the FailoverStrategy to use with the CustomIpAddresses.
     * @see com.l7tech.common.io.failover.FailoverStrategyFactory
     */
    public String getFailoverStrategyName() {
        return _failoverStrategyName;
    }

    /**
     * @param failoverStrategyName the name of the FailoverStrategy to use with the CustomIpAddresses.
     * @see com.l7tech.common.io.failover.FailoverStrategyFactory
     */
    public void setFailoverStrategyName(String failoverStrategyName) {
        this._failoverStrategyName = failoverStrategyName;
    }

    /** Subclasses can choose to offer this functionality by adding a public method that chains to this one. */
    protected void copyFrom(HttpRoutingAssertion source) {
        super.copyFrom(source);
        this.setCustomIpAddresses((String[])source.getCustomIpAddresses());
        this.setFailoverStrategyName(source.getFailoverStrategyName());
        this.setLogin(source.getLogin());
        this.setMaxConnections(source.getMaxConnections());
        this.setCopyCookies(source.isCopyCookies());
        this.setPassword(source.getPassword());
        this.setProtectedServiceUrl(source.getProtectedServiceUrl());
        this.setRealm(source.getRealm());
        this.setUserAgent(source.getUserAgent());
    }

    protected String _protectedServiceUrl;
    protected String _login;
    protected String _password;
    protected String _realm;
    protected String _userAgent;
    protected int _maxConnections;
    protected boolean _copyCookies;
    protected String[] _customIpAddresses = null;
    protected String _failoverStrategyName = "ordered";

    public String[] getVariablesUsed() {
        return ExpandVariables.getReferencedNames(_login + _password + _protectedServiceUrl);
    }

    public String[] getVariablesSet() {
        return new String[] { ROUTING_LATENCY };
    }
}
