/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.wss;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.message.Request;

/**
 * @author alex
 */
public class WssClientCertCredentialFinder extends WssCredentialFinder {
    public PrincipalCredentials findCredentials( Request request ) throws CredentialFinderException {
        return null;
    }
}
