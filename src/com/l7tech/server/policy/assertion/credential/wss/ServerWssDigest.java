/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.wss.WssDigestCredentialFinder;
import com.l7tech.server.policy.assertion.ServerAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWssDigest extends ServerWssCredentialSource implements ServerAssertion {
    public ServerWssDigest( WssDigest data ) {
        super( data );
        _data = data;
    }

    public AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    public Class getCredentialFinderClass() {
        return WssDigestCredentialFinder.class;
    }

    protected WssDigest _data;
}
