/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.credential.PrincipalCredentials;

import java.io.InputStream;
import java.util.Set;

/**
 * @author alex
 */
public interface Request extends Message {
    InputStream getRequestStream();
    PrincipalCredentials getPrincipalCredentials();
    void setPrincipalCredentials( PrincipalCredentials pc );
    boolean isAuthenticated();
    void setAuthenticated( boolean authenticated );
}
