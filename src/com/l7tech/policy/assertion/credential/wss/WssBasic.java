/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.wss;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.wss.WssBasicCredentialFinder;
import com.l7tech.policy.assertion.AssertionError;

/**
 * @author alex
 * @version $Revision$
 */
public class WssBasic extends WssCredentialSourceAssertion {
    public AssertionError doCheckRequest(Request request, Response response) throws CredentialFinderException {
        return AssertionError.NOT_YET_IMPLEMENTED;
    }

    public Class getCredentialFinderClass() {
        return WssBasicCredentialFinder.class;
    }
}
