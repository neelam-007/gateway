/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpDigest extends HttpCredentialSourceAssertion {
    public static final String SCHEME = "Digest";

    public static final String USERNAME = "username";
    public static final String RESPONSE = "response";
    public static final String REALM = "realm";
    public static final String NONCE = "nonce";
    public static final String CNONCE = "cnonce";
    public static final String QOP = "qop";
    public static final String QOP_AUTH = "auth";
    public static final String QOP_AUTH_INT = "auth-int";
    public static final String ALGORITHM = "algorithm";
    public static final String ALGORITHM_MD5 = "md5";
    public static final String NC = "nc";
    public static final String URI = "uri";

    public String scheme() {
        return SCHEME;
    }

    public int getMaxNonceCount() {
        return _maxNonceCount;
    }

    public void setMaxNonceCount(int maxNonceCount) {
        _maxNonceCount = maxNonceCount;
    }

    public int getNonceTimeout() {
        return _nonceTimeout;
    }

    public void setNonceTimeout( int nonceTimeout ) {
        _nonceTimeout = nonceTimeout;
    }

    protected int _nonceTimeout = 60 * 30 * 1000;
    protected int _maxNonceCount = 30;
}
