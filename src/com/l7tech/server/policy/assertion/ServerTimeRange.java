package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;

/**
 * Server side processing of a TimeRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 20, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerTimeRange implements ServerAssertion {
    public ServerTimeRange(TimeRange assertion) {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");
        subject = assertion;

    }

    public AssertionStatus checkRequest(Request req, Response res) throws IOException, PolicyAssertionException {
        // todo
        return AssertionStatus.NONE;
    }

    private TimeRange subject;
}
