/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class HttpCredentialSourceAssertion extends CredentialSourceAssertion {
    public String getRealm() {
        return _realm;
    }

    public void setRealm(String realm) {
        _realm = realm;
    }

    public abstract String scheme();

    protected String _realm;
}
