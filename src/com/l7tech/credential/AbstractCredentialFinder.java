/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential;

import com.l7tech.message.Request;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class AbstractCredentialFinder implements CredentialFinder {
    public PrincipalCredentials findCredentials( Request request ) throws CredentialFinderException {
        PrincipalCredentials pc = doFindCredentials( request );
        pc.setFinder(this);
        return pc;
    }

    protected abstract PrincipalCredentials doFindCredentials( Request request ) throws CredentialFinderException;
}
