/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

import com.l7tech.identity.User;

/**
 * Stores a reference to a User and its associated credentials (i.e. password).
 *
 * Immutable.
 *
 * @author alex
 */
public class PrincipalCredentials {
    public PrincipalCredentials( User user, byte[] credentials, CredentialFormat format, String realm  ) {
        _user = user;
        _credentials = credentials;
        _realm = realm;
        _format = format;
    }

    public PrincipalCredentials( User user, byte[] credentials, CredentialFormat format ) {
        this( user, credentials, format, null );
    }

    public PrincipalCredentials( User user, byte[] credentials ) {
        this( user, credentials, CredentialFormat.CLEARTEXT );
    }

    public User getUser() {
        return _user;
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

    private final User _user;
    private final byte[] _credentials;
    private final String _realm;
    private CredentialFormat _format;
}
