/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.io.Serializable;

/**
 * @author alex
 */
public class RoutingAssertion extends Assertion implements Cloneable, Serializable {
    public static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 100; // Flagrantly in contravention of RFC2616!

    public RoutingAssertion() {
        this(null, null, null, null );
    }

    public RoutingAssertion(String protectedServiceUrl) {
        this(protectedServiceUrl, null, null, null );
    }


    public RoutingAssertion( String protectedServiceUrl, String login, String password, String realm ) {
        this( protectedServiceUrl, login, password, realm, -1 );
    }

    /**
     * Full constructor.
     *
     * @param protectedServiceUrl the service url
     * @param login protected service login
     * @param password protected service password
     * @param realm protected servcie realm
     */
    public RoutingAssertion( String protectedServiceUrl, String login, String password, String realm, int maxConnections ) {
        _protectedServiceUrl = protectedServiceUrl;
        _login = login;
        _password = password;
        _realm = realm;

        if ( maxConnections == -1 ) maxConnections = DEFAULT_MAX_CONNECTIONS_PER_HOST;
        _maxConnections = maxConnections;
    }

    public Object clone() throws CloneNotSupportedException {
        RoutingAssertion n = (RoutingAssertion)super.clone();
        return n;
    }

    public int getMaxConnections() {
        return _maxConnections;
    }

    public void setMaxConnections( int maxConnections ) {
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

    protected String _protectedServiceUrl;
    protected String _login;
    protected String _password;
    protected String _realm;
    protected String _userAgent;
    protected int _maxConnections;
}
