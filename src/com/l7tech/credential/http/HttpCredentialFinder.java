/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.http;

import com.l7tech.credential.*;
import com.l7tech.logging.LogManager;

import java.util.logging.Level;

/**
 * @author alex
 */
public abstract class HttpCredentialFinder implements CredentialFinder {
    public void throwError( String err ) throws CredentialFinderException {
        LogManager.getInstance().getSystemLogger().log(Level.SEVERE, err);
        throw new CredentialFinderException( err );
    }
}
