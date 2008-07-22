package com.l7tech.external.assertions.cawsdm.server;

import com.l7tech.server.audit.Auditor;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.external.assertions.cawsdm.CaWsdmAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.module.ca_wsdm.CaWsdmObserver;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Server side implementation of the CaWsdmAssertion.
 *
 * @see com.l7tech.external.assertions.cawsdm.CaWsdmAssertion
 */
public class ServerCaWsdmAssertion extends AbstractServerAssertion<CaWsdmAssertion> {
    private static final Logger logger = Logger.getLogger(ServerCaWsdmAssertion.class.getName());

    private final Auditor auditor;

    public ServerCaWsdmAssertion(CaWsdmAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        this.auditor = new Auditor(this, context, logger);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        CaWsdmObserver observer = CaWsdmObserver.getInstance();
        if (observer == null) {
            context.setVariable(CaWsdmAssertion.VAR_ENABLED, Boolean.FALSE);
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "CA WSDM Observer could not be instantiated; see server startup log for more information");
            return AssertionStatus.SERVER_ERROR;
        }

        boolean enabled = observer.isEnabled();
        context.setVariable(CaWsdmAssertion.VAR_ENABLED, enabled);
        return enabled ? AssertionStatus.NONE : AssertionStatus.FAILED;
    }
}
