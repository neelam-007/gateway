/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerSslAssertion implements ServerAssertion {
    public ServerSslAssertion( SslAssertion data ) {
        _data = data;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException {
        boolean ssl = context.getHttpServletRequest().isSecure();
        AssertionStatus status;

        SslAssertion.Option option = _data.getOption();

        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        if ( option == SslAssertion.REQUIRED) {
            if (ssl) {
                status = AssertionStatus.NONE;
                auditor.logAndAudit(AssertionMessages.SSL_REQUIRED_PRESENT);
            } else {
                status = AssertionStatus.FALSIFIED;
                auditor.logAndAudit(AssertionMessages.SSL_REQUIRED_ABSENT);
            }
        } else if ( option == SslAssertion.FORBIDDEN) {
            if (ssl) {
                status = AssertionStatus.FALSIFIED;
                auditor.logAndAudit(AssertionMessages.SSL_FORBIDDEN_PRESENT);
            } else {
                status = AssertionStatus.NONE;
                auditor.logAndAudit(AssertionMessages.SSL_FORBIDDEN_ABSENT);
            }
        } else {
            status = AssertionStatus.NONE;
            if(ssl) {
                auditor.logAndAudit(AssertionMessages.SSL_OPTIONAL_PRESENT);
            } else {
                auditor.logAndAudit(AssertionMessages.SSL_OPTIONAL_ABSENT);
            }
        }

        if (status == AssertionStatus.FALSIFIED)
            context.setRequestPolicyViolated();

        return status;
    }

    protected SslAssertion _data;
    protected final Logger logger = Logger.getLogger(getClass().getName());
}
