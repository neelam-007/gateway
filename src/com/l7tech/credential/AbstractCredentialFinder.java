/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

import com.l7tech.message.Request;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AbstractCredentialFinder implements CredentialFinder {
    public PrincipalCredentials findCredentials( Request request ) throws IOException, CredentialFinderException {
        PrincipalCredentials pc = doFindCredentials( request );
        if ( pc != null ) pc.setFinder(this);
        return pc;
    }

    protected abstract PrincipalCredentials doFindCredentials( Request request ) throws IOException, CredentialFinderException;
}
