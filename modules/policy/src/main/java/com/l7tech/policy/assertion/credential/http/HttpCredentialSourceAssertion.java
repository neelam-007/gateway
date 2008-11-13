/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.annotation.ProcessesRequest;

@ProcessesRequest
public abstract class HttpCredentialSourceAssertion extends Assertion {
    public String getRealm() {
        return _realm;
    }

    public void setRealm(String realm) {
        _realm = realm;
    }

    /**
     * The HTTP Credential source is always a credential source
     *
     * @return always true
     */
    public boolean isCredentialSource() {
        return true;
    }

    protected String _realm;

    public static final String PARAM_SCHEME = "scheme";
    public static final String PARAM_REALM = "realm";
}
