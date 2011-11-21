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
        int quantity;
        final String quantityStr = ExpandVariables.process(assertion.getQuantity(), context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (final NumberFormatException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "The quantity cannot be resolved: " + e.getMessage() }, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }

        if (quantity < UUIDGeneratorAssertion.MINIMUM_QUANTITY) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No UUID generated. Quantity less than minimum: " + quantity );
            return AssertionStatus.FAILED;
        }

        if (quantity > assertion.getMaximumQuantity()) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "No UUID generated. Quantity more than maximum: " + quantity );
            return AssertionStatus.FAILED;
        }

        context.setVariable(assertion.getTargetVariable(), generateUUIDs(quantity));

        return AssertionStatus.NONE;
    }

    /**
     * @param quantity the quantity of UUIDs to generate.
     * @return a uuid String if quantity is equal to one or an array of UUID Strings if quantity is more than one.
     */
    private Object generateUUIDs(int quantity) {
        Object value = null;
        if(quantity == UUIDGeneratorAssertion.MINIMUM_QUANTITY){
            //if we only need one uuid, store it in a string instead of an array
            value = UUID.randomUUID().toString();
        }else{
            final String[] uuids = new String[quantity];
            for (int i = 0; i < quantity; ++i) {
                final UUID uuid = UUID.randomUUID();
                uuids[i] = uuid.toString();
            }
            value = uuids;
        }
        return value;
    }

    private void validateAssertion(UUIDGeneratorAssertion assertion) throws PolicyAssertionException {
        if (assertion.getQuantity() == null || assertion.getQuantity().isEmpty()) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The quantity is not set." );
            throw new PolicyAssertionException(assertion, "Quantity is not set.");
        }

        if (assertion.getTargetVariable() == null || assertion.getTargetVariable().isEmpty()) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "The target variable is not set." );
            throw new PolicyAssertionException(assertion, "Target Variable is not set.");
        }
    }
}
