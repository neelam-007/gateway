/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.credential.PrincipalCredentials;

import java.io.InputStream;

/**
 * @author alex
 */
public interface Request extends Message {
    static final String PREFIX = "com.l7tech.message.request";

    public static final String PARAM_URN        = PREFIX + ".urn";
    public static final String PARAM_SOAPACTION = PREFIX + ".soapaction";
    public static final String PARAM_REMOTEADDR = PREFIX + ".remoteaddr";

    InputStream getRequestStream();
    PrincipalCredentials getPrincipalCredentials();
    void setPrincipalCredentials( PrincipalCredentials pc );
    boolean isAuthenticated();
    void setAuthenticated( boolean authenticated );
}
