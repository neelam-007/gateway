/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

import com.l7tech.message.Message;
import com.l7tech.message.Request;

/**
 * @author alex
 */
public interface CredentialFinder {
    PrincipalCredentials findCredentials( Request request ) throws CredentialFinderException;
}
