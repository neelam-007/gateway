/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;

import javax.xml.soap.SOAPFault;
import java.io.*;
import java.util.Iterator;

/**
 * @author alex
 */
public interface Response extends Message {
    void setProtectedResponseStream( InputStream stream );
    InputStream getProtectedResponseStream();

    void addResult( AssertionResult result );
    Iterator results();
    Iterator resultsWithStatus( AssertionStatus status );
    Iterator resultsWithStatus( AssertionStatus[] statuses );

    boolean isAuthenticationMissing();
    void setAuthenticationMissing( boolean authMissing );
}
