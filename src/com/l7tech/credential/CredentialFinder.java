/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

import com.l7tech.message.Header;

import java.security.Principal;

/**
 * @author alex
 */
public interface CredentialFinder {
    PrincipalCredentials findCredentials( Header header ) throws CredentialFinderException;
}
