/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

/**
 *
 * @author mike
 * @version 1.0
 */
public class HttpRoutingAssertion extends RoutingAssertion {
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 100; // Flagrantly in contravention of RFC2616!

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
     * Full constructor.
     *
     * @param protectedServiceUrl the service url
     * @param login               protected service login
     * @param password            protected service password
     * @param realm               protected servcie realm
     */
    public HttpRoutingAssertion(String protectedServiceUrl, String login, String password, String realm, int maxConnections) {
        _protectedServiceUrl = protectedServiceUrl;
        _login = login;
        _password = password;
        _realm = realm;

        if (maxConnections == -1) maxConnections = DEFAULT_MAX_CONNECTIONS_PER_HOST;
        _maxConnections = maxConnections;
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

    protected String _protectedServiceUrl;
    protected String _login;
    protected String _password;
    protected String _realm;
    protected String _userAgent;
    protected int _maxConnections;
    protected String[] _customIpAddresses = null;
    protected String _failoverStrategyName = "sticky";
}
