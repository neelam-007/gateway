/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential;

import java.security.cert.X509Certificate;

/**
 * Stores a reference to a User and its associated credentials (i.e. password).
 *
 * Immutable.
 *
 * @author alex
 */
public class LoginCredentials {
    public LoginCredentials( String login, byte[] credentials, CredentialFormat format, String realm, Object payload ) {
        _login = login;
        _credentials = credentials;
        _realm = realm;
        _format = format;
        _payload = payload;
        if (format.isClientCert() && !(payload instanceof X509Certificate))
            throw new IllegalArgumentException("Must provide a certificate when creating client cert credentials");
    }

    public LoginCredentials( String login, byte[] credentials, CredentialFormat format, String realm ) {
        this( login, credentials, format, realm, null );
    }

    public LoginCredentials( String login, byte[] credentials, CredentialFormat format ) {
        this( login, credentials, format, null, null );
    }

    public LoginCredentials( String login, byte[] credentials ) {
        this( login, credentials, CredentialFormat.CLEARTEXT, null, null );
    }

    public String getLogin() {
        return _login;
    }

    public byte[] getCredentials() {
        return _credentials;
    }

    /**
     * Could be null.
     * @return
     */
    public String getRealm() {
        return _realm;
    }

    public CredentialFormat getFormat() {
        return _format;
    }

    /**
     * Could be null.
     * @return
     */
    public Object getPayload() {
        return _payload;
    }

    public String toString() {
        return getClass().getName() + "\n\t" +
                "format: " + _format.toString() + "\n\t" +
                "login: " + _login;
    }

    private final String _login;
    private final byte[] _credentials;
    private final String _realm;
    private CredentialFormat _format;
    private Object _payload;
}
