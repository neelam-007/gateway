package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssVersionAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of WssVersionAssertion.
 */
public class ServerWssVersionAssertion extends AbstractServerAssertion<WssVersionAssertion> {
    private static final Logger logger = Logger.getLogger(ServerWssVersionAssertion.class.getName());
    private final Auditor auditor;

    public ServerWssVersionAssertion(WssVersionAssertion assertion, ApplicationContext applicationContext) {
        super(assertion);
        this.auditor = applicationContext != null ? new Auditor(this, applicationContext, logger) : new LogOnlyAuditor(logger);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setResponseWss11();
        context.getRequest().getSecurityKnob().setNeedsSignatureConfirmations(true);
        return AssertionStatus.NONE;
    }
}
