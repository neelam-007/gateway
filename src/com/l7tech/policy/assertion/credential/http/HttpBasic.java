/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.*;
import com.l7tech.credential.http.HttpBasicCredentialFinder;
import com.l7tech.policy.assertion.AssertionError;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpBasic extends HttpCredentialSourceAssertion {
    public AssertionError doCheckRequest(Request request, Response response) throws CredentialFinderException {
        // TODO
        return AssertionError.NOT_YET_IMPLEMENTED;
    }

    public Class getCredentialFinderClass() {
        return HttpBasicCredentialFinder.class;
    }


}
