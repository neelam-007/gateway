/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
/**
 * Gathers HTTP Digest Authentication info from the request.  Implementations are
 * responsible for filling in the correct values in the <code>Authorization</code> header.
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
    public static final String PARAM_OPAQUE = "opaque";
    public static final String PARAM_METHOD = "method";

    public static final String SCHEME = "Digest";

    /** The hard-coded HTTP Digest realm.  NOTE: If you change this, it will break A LOT of stuff! */
    public static final String REALM = "L7SSGDigestRealm"; // TODO: Make this configurable on a system-wide and eventually per-identity-provider basis

    // Some values that are commonly found in Authorization and WWW-Authenticate headers
    public static final String QOP_AUTH = "auth";
    public static final String QOP_AUTH_INT = "auth-int";
    public static final String ALGORITHM_MD5 = "md5";

    /**
     * Default maximum age of a nonce, 30 minutes.
     */
    public static final int DEFAULT_NONCE_TIMEOUT = 30 * 60 * 1000;

    /**
     * Default maximum number of uses of a nonce, 30.
     */
    public static final int DEFAULT_NONCE_MAXUSES = 30;


    /**
     * The maximum number of times (default 30) that a nonce can be used.
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

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();
        meta.put(AssertionMetadata.PALETTE_FOLDERS, new String[]{"accessControl"});

        meta.put(AssertionMetadata.SHORT_NAME, "Require HTTP Digest Credentials");
        meta.put(AssertionMetadata.LONG_NAME, "The requestor must provide credentials using the HTTP DIGEST authentication method.");
        meta.put(AssertionMetadata.PALETTE_NODE_ICON, "com/l7tech/console/resources/authentication.gif");
        meta.put(AssertionMetadata.PALETTE_NODE_CLIENT_ICON, "com/l7tech/proxy/resources/tree/authentication.gif");

        meta.putNull(AssertionMetadata.PROPERTIES_ACTION_FACTORY);

        meta.put(AssertionMetadata.USED_BY_CLIENT, Boolean.TRUE);

        return meta;
    }

    protected int _nonceTimeout = DEFAULT_NONCE_TIMEOUT;
    protected int _maxNonceCount = DEFAULT_NONCE_MAXUSES;
}
