/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.wss;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.message.Request;

/**
 * @author alex
 */
public class WssBasicCredentialFinder extends WssCredentialFinder {
    public PrincipalCredentials doFindCredentials( Request request ) throws CredentialFinderException {
        return null;
    }
}
