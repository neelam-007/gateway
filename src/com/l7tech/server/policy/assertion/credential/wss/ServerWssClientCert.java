/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.credential.wss.WssClientCertCredentialFinder;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.wss.WssClientCert;
import com.l7tech.server.policy.assertion.ServerAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWssClientCert extends ServerWssCredentialSource implements ServerAssertion {
    public ServerWssClientCert( WssClientCert data ) {
        super( data );
        _data = data;
    }

    protected Class getCredentialFinderClass() {
        return WssClientCertCredentialFinder.class;
    }

    public AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        // FIXME: Implement
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    protected WssClientCert _data;
}
