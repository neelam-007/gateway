package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssVersionAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.gateway.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.List;
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

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        context.setResponseWss11();

        // We are done here, but we will also ensure that response SignatureConfirmation values get sent if needed,
        // even if the policy does not include any ResponseWssIntegrity assertions.
        final ProcessorResult requestWssResults = context.getRequest().getSecurityKnob().getProcessorResult();
        if (requestWssResults == null) {
            auditor.logAndAudit(AssertionMessages.REQUESTWSS_NO_SECURITY);
            return AssertionStatus.NONE;
        }

        List<String> signatureValues = requestWssResults.getValidatedSignatureValues();
        context.addDeferredAssertion(this, ServerRequireWssSignedElement.deferredSignatureConfirmation(assertion, auditor, signatureValues));

        return AssertionStatus.NONE;
    }
}
