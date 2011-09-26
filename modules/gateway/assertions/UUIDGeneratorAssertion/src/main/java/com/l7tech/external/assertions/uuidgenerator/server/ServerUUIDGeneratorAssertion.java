package com.l7tech.external.assertions.uuidgenerator.server;

import com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Config;

import javax.inject.Inject;
import java.io.IOException;
import java.util.UUID;

/**
 * Server side implementation of the UUIDGeneratorAssertion.
 *
 * @see com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion
 */
public class ServerUUIDGeneratorAssertion extends AbstractServerAssertion<UUIDGeneratorAssertion> {
    static final int MINIMUM_AMOUNT = 1;
    static final int MAXIMUM_AMOUNT = 100;

    @Inject
    private Config config;

    public ServerUUIDGeneratorAssertion(final UUIDGeneratorAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        validateAssertion(assertion);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        int amount;
        final String amountStr = ExpandVariables.process(assertion.getAmount(), context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        try {
            amount = Integer.parseInt(amountStr);
        } catch (final NumberFormatException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "The amount cannot be resolved: " + e.getMessage() }, e );
            return AssertionStatus.FAILED;
        }

        if (amount < MINIMUM_AMOUNT) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No UUID generated. Amount less than minimum: " + amount );
            return AssertionStatus.FAILED;
        }

        int maxAmount = config.getIntProperty(ServerConfigParams.PARAM_UUID_AMOUNT_MAX, MAXIMUM_AMOUNT);
        if (amount > maxAmount) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No UUID generated. Amount more than maximum: " + amount );
            return AssertionStatus.FAILED;
        }

        // generate UUIDs
        final String[] uuids = new String[amount];
        for (int i = 0; i < amount; ++i) {
            final UUID uuid = UUID.randomUUID();
            uuids[i] = uuid.toString();
        }

        context.setVariable(assertion.getTargetVariable(), uuids);

        return AssertionStatus.NONE;
    }

    private void validateAssertion(UUIDGeneratorAssertion assertion) throws PolicyAssertionException {
        if (assertion.getAmount() == null || assertion.getAmount().isEmpty()) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The amount is not set." );
            throw new PolicyAssertionException(assertion, "Amount is not set.");
        }

        if (assertion.getTargetVariable() == null || assertion.getTargetVariable().isEmpty()) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The target variable is not set." );
            throw new PolicyAssertionException(assertion, "Target Variable is not set.");
        }
    }
}
