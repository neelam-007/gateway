/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.wss.WssBasicCredentialFinder;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;

/**
 * @author alex
 * @version $Revision$
 */
public class WssBasic extends WssCredentialSourceAssertion {
    public AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        // this is only called once we have credentials
        // there is nothing more to check here, if the creds were not in the right format,
        // the WssBasicCredentialFinder would not have returned credentials

        // (just to make sure)
        PrincipalCredentials pc = request.getPrincipalCredentials();
        // yes, we're good
        if (pc != null) return AssertionStatus.NONE;
        else return AssertionStatus.AUTH_REQUIRED;
    }

    public Class getCredentialFinderClass() {
        return WssBasicCredentialFinder.class;
    }
}
