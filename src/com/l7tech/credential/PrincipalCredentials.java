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
    public PrincipalCredentials( Principal principal, byte[] credential ) {
        _principal = principal;
        _credential = credential;
    }

    public Principal getPrincipal() {
        return _principal;
    }

    byte[] getCredential() {
        return _credential;
    }

    private final Principal _principal;
    private final byte[] _credential;
}
