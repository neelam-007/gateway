/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssDigest;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;

import java.io.IOException;
import java.util.Map;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWssDigest extends ServerWssCredentialSource implements ServerAssertion {
    public ServerWssDigest( WssDigest data ) {
        super( data );
        _data = data;
    }

    public AssertionStatus checkCredentials(PolicyEnforcementContext context) throws CredentialFinderException {
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    protected WssDigest _data;

    protected LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
        // TODO implement someday
        return null;
    }

    protected AssertionStatus checkCredentials(LoginCredentials pc, Map authParams) throws CredentialFinderException {
        // TODO implement someday
        return null;
    }

    protected void challenge(PolicyEnforcementContext context, Map authParams) {
        // TODO implement someday
    }
}
