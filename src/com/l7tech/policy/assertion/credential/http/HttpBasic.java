/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.Assertion;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpBasic extends HttpCredentialSourceAssertion {
    public String scheme() {
        return SCHEME;
    }

    public static final String SCHEME = "Basic";
}
