/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

/**
 * Gathers HTTP Digest Authentication info from the request.  Implementations are
 * responsible for filling in the correct values in the <code>Authorization</code> header.
 *
 * @author alex
 * @version $Revision$
 */
public class HttpDigest extends HttpCredentialSourceAssertion {
    // Some handy constants for the Authorization and WWW-Authenticate headers
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_RESPONSE = "response";
    public static final String PARAM_NONCE = "nonce";
    public static final String PARAM_CNONCE = "cnonce";
    public static final String PARAM_QOP = "qop";
    public static final String PARAM_ALGORITHM = "algorithm";
    public static final String PARAM_NC = "nc";
    public static final String PARAM_URI = "uri";

    public static final String SCHEME = "Digest";

    // Some values that are commonly found in Authorization and WWW-Authenticate headers
    public static final String QOP_AUTH = "auth";
    public static final String QOP_AUTH_INT = "auth-int";
    public static final String ALGORITHM_MD5 = "md5";

    public String scheme() {
        return SCHEME;
    }

    /**
     * The maximum number of times (default 30) that a nonce can be used.
     * @return
     */
    public int getMaxNonceCount() {
        return _maxNonceCount;
    }

    /**
     * The maximum number of times (default 30) that a nonce can be used.
     * @param maxNonceCount
     */
    public void setMaxNonceCount(int maxNonceCount) {
        _maxNonceCount = maxNonceCount;
    }

    /**
     * The maximum interval (in milliseconds, default 30 minutes) during which a particular nonce is
     * valid, irrespective of how many times it has been used.
     * @return
     */
    public int getNonceTimeout() {
        return _nonceTimeout;
    }

    /**
     * The maximum interval (in milliseconds, default 30 minutes) during which a particular nonce is
     * valid, irrespective of how many times it has been used.
     * @param nonceTimeout
     */
    public void setNonceTimeout( int nonceTimeout ) {
        _nonceTimeout = nonceTimeout;
    }

    protected int _nonceTimeout = 60 * 30 * 1000;
    protected int _maxNonceCount = 30;
}
