/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AuditAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;
import java.util.logging.Level;

/**
 * This assertion sets the audit level and parameters for the current request.
 */
public class ServerAuditAssertion implements ServerAssertion {
    private AuditAssertion data;
    private Level level;

    public ServerAuditAssertion(AuditAssertion data) {
        this.data = data;
        this.level = Level.parse(data.getLevel());
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setAuditLevel(level);
        context.setAuditSaveRequest(data.isSaveRequest());
        context.setAuditSaveResponse(data.isSaveResponse());
        return AssertionStatus.NONE;
    }
}
