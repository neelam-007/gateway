/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

import java.security.Principal;

/**
 * Stores a reference to a Principal and its associated Credential (i.e. password).
 *
 * Immutable.
 * @author alex
 */
public class PrincipalCredentials {
    public PrincipalCredentials( Principal principal, byte[] credentials, String realm, CredentialFinder finder ) {
        _principal = principal;
        _credentials = credentials;
        _realm = realm;
        _finder = finder;
    }

    public Principal getPrincipal() {
        return _principal;
    }

    public byte[] getCredentials() {
        return _credentials;
    }

    public String getRealm() {
        return _realm;
    }

    public CredentialFinder getFinder() {
        return _finder;
    }

    public void setFinder( CredentialFinder finder ) {
        _finder = finder;
    }

    private final Principal _principal;
    private final byte[] _credentials;
    private final String _realm;
    private CredentialFinder _finder;
}
