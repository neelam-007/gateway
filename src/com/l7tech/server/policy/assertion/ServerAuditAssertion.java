/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author mike
 */
public class ServerAuditAssertion implements ServerAssertion {
    private AuditAssertion data;
    private Level level;

    public ServerAuditAssertion(AuditAssertion data) {
        this.data = data;
        this.level = Level.parse(data.getLevel());
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        request.setAuditLevel(level);
        return AssertionStatus.NONE;
    }
}
