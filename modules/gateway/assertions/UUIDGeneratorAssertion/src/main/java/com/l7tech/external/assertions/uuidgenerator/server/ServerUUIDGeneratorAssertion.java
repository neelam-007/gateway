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
import com.l7tech.util.ExceptionUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.util.UUID;

/**
 * Server side implementation of the UUIDGeneratorAssertion.
 *
 * @see com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion
 */
public class ServerUUIDGeneratorAssertion extends AbstractServerAssertion<UUIDGeneratorAssertion> {
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
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "The amount cannot be resolved: " + e.getMessage() }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }

        if (amount < UUIDGeneratorAssertion.MINIMUM_AMOUNT) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No UUID generated. Amount less than minimum: " + amount );
            return AssertionStatus.FAILED;
        }

        final int maxAmount = config.getIntProperty(ServerConfigParams.PARAM_UUID_AMOUNT_MAX, UUIDGeneratorAssertion.MAXIMUM_AMOUNT);
        if (amount > maxAmount) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No UUID generated. Amount more than maximum: " + amount );
            return AssertionStatus.FAILED;
        }

        context.setVariable(assertion.getTargetVariable(), generateUUIDs(amount));

        return AssertionStatus.NONE;
    }

    /**
     * @param amount the amount of UUIDs to generate.
     * @return a uuid String if amount is equal to one or an array of UUID Strings if amount is more than one.
     */
    private Object generateUUIDs(int amount) {
        Object value = null;
        if(amount == UUIDGeneratorAssertion.MINIMUM_AMOUNT){
            //if we only need one uuid, store it in a string instead of an array
            value = UUID.randomUUID().toString();
        }else{
            final String[] uuids = new String[amount];
            for (int i = 0; i < amount; ++i) {
                final UUID uuid = UUID.randomUUID();
                uuids[i] = uuid.toString();
            }
            value = uuids;
        }
        return value;
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
