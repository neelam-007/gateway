/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.HttpRequestKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpTransportAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerHttpTransportAssertion extends AbstractServerAssertion implements ServerAssertion {
    public ServerHttpTransportAssertion( HttpTransportAssertion data ) {
        super(data);
        this.data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if ( context.getRequest().getKnob(HttpRequestKnob.class) != null ) {
            return AssertionStatus.NONE;
        } else {
            return AssertionStatus.FALSIFIED;
        }
    }

    private HttpTransportAssertion data;
}
