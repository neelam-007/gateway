/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;
import java.util.Date;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerDateTimeAssertion implements ServerAssertion {
    public AssertionStatus checkRequest( Request request, Response response ) throws IOException, PolicyAssertionException {
        Date now = new Date();
        return doCheckDate( now );
    }

    protected abstract AssertionStatus doCheckDate( Date now );
}
