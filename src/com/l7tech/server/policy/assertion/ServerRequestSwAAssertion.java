package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ServerRequestSwAAssertion implements ServerAssertion {
    private final RequestSwAAssertion data;
    private final Logger logger = Logger.getLogger(getClass().getName());

     public ServerRequestSwAAssertion( RequestSwAAssertion data ) {
        this.data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        return null;  //todo:
    }
}
