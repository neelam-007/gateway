/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;

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

    protected String _realm;
}
