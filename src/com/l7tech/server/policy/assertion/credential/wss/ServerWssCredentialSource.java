/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.policy.assertion.credential.wss.WssCredentialSourceAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerWssCredentialSource extends ServerCredentialSourceAssertion implements ServerAssertion {
    public ServerWssCredentialSource( WssCredentialSourceAssertion data ) {
        super( data );
        _data = data;
    }

    protected void challenge(PolicyEnforcementContext context) {
        // Meaningless for WS-Security
    }

    protected WssCredentialSourceAssertion _data;
}
