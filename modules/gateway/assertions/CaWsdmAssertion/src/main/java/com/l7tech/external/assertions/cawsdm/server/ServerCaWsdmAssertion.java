package com.l7tech.external.assertions.cawsdm.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.external.assertions.cawsdm.CaWsdmAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.module.ca_wsdm.CaWsdmObserver;

import java.io.IOException;

/**
 * Server side implementation of the CaWsdmAssertion.
 *
 * @see com.l7tech.external.assertions.cawsdm.CaWsdmAssertion
 */
public class ServerCaWsdmAssertion extends AbstractServerAssertion<CaWsdmAssertion> {

    public ServerCaWsdmAssertion(CaWsdmAssertion assertion) throws PolicyAssertionException {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        CaWsdmObserver observer = CaWsdmObserver.getInstance();
        if (observer == null) {
            context.setVariable(CaWsdmAssertion.VAR_ENABLED, Boolean.FALSE);
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "CA WSDM Observer could not be instantiated; see server startup log for more information" );
            return AssertionStatus.SERVER_ERROR;
        }

        boolean enabled = observer.isEnabled();
        context.setVariable(CaWsdmAssertion.VAR_ENABLED, enabled);
        return enabled ? AssertionStatus.NONE : AssertionStatus.FAILED;
    }
}
