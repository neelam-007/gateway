/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.xml.SoapFaultDetail;

import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * @author alex
 */
public interface Response extends Message {
    /** WWW-Authenticate header */
    public static final String PARAM_HTTP_WWWAUTHENTICATE = PREFIX_HTTP_HEADER + ".WWW-Authenticate";
    public static final String PARAM_HTTP_STATUS = PREFIX_HTTP + ".status";
    public static final String PARAM_HTTP_POLICYURL         = PREFIX_HTTP_HEADER + "." + SecureSpanConstants.HttpHeaders.POLICYURL_HEADER;
    public static final String PARAM_HTTP_CERT_CHECK_PREFIX = PREFIX_HTTP_HEADER + "." + SecureSpanConstants.HttpHeaders.CERT_CHECK_PREFIX;
    public static final String PARAM_HTTP_CERT_STATUS =  PREFIX_HTTP_HEADER + "." + SecureSpanConstants.HttpHeaders.CERT_STATUS;

    /**
     * Adds an <code>AssertionResult</code> to this <code>Response</code>'s list of results.
     *
     * These are used to add additional detail beyond the AssertionStatus if necessary.
     *
     * @param result an <code>AssertionResult</code> object.
     */
    void addResult( AssertionResult result );

    /**
     * Specify the fault details for an error that occured in this request.
     */
    void setFaultDetail(SoapFaultDetail sfd);

    /**
     * Get the soap fault details if applicable.
     */
    SoapFaultDetail getFaultDetail();

    /**
     * Returns an Iterator containing all the <code>AssertionResult</code>s that have been attached to this <code>Response</code>.
     * @return
     */
    Iterator results();

    /**
     * Returns an Iterator containing any <code>AssertionResult</code> objects that have been attached to this <code>Response</code> that contain a given <code>AssertionStatus</code>.
     * @param status The <code>AssertionStatus</code> to search for
     * @return
     */
    Iterator resultsWithStatus( AssertionStatus status );

    /**
     * Returns an Iterator containing any <code>AssertionResult</code> objects that have been attached to this <code>Response</code> that contain one of the given <code>AssertionStatus</code>es.
     * @param statuses The <code>AssertionStatus</code>es to search for
     * @return
     */
    Iterator resultsWithStatus( AssertionStatus[] statuses );

    /** A flag indicating whether the response constitutes a failure due to authentication. */
    boolean isAuthenticationMissing();
    /** A flag indicating whether the response constitutes a failure due to authentication. */
    void setAuthenticationMissing( boolean authMissing );

    /** A flag indicating whether the response constitutes a failure due to a failure to follow a policy. */
    boolean isPolicyViolated();

    /** A flag indicating whether the response constitutes a failure due to a failure to follow a policy. */
    void setPolicyViolated( boolean policyViolated );
}
