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

    public abstract String scheme();

    protected String _realm;

    public static String PARAM_SCHEME = "scheme";
    public static String PARAM_REALM = "realm";
}
