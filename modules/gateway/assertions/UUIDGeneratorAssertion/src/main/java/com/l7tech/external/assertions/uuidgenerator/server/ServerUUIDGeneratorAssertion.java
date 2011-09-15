package com.l7tech.external.assertions.uuidgenerator.server;

import com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the UUIDGeneratorAssertion.
 *
 * @see com.l7tech.external.assertions.uuidgenerator.UUIDGeneratorAssertion
 */
public class ServerUUIDGeneratorAssertion extends AbstractServerAssertion<UUIDGeneratorAssertion> {
    private static final Logger LOGGER = Logger.getLogger(ServerUUIDGeneratorAssertion.class.getName());
    public static final int MINIMUM_AMOUNT = 1;

    private final UUIDGeneratorAssertion assertion;

    public ServerUUIDGeneratorAssertion(final UUIDGeneratorAssertion assertion) throws PolicyAssertionException {
        super(assertion);
        this.assertion = assertion;
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (assertion.getAmount() == null) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"The amount is not set."});
            return AssertionStatus.FAILED;
        }

        if (assertion.getTargetVariable() == null || assertion.getTargetVariable().isEmpty()) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"The target variable is not set."});
            return AssertionStatus.FAILED;
        }

        int amount;
        final String amountStr = ExpandVariables.process(assertion.getAmount(), context.getVariableMap(assertion.getVariablesUsed(), getAudit()), getAudit());
        try {
            amount = Integer.parseInt(amountStr);
        } catch (final NumberFormatException e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"The amount cannot be resolved: " + e.getMessage()}, e);
            return AssertionStatus.FAILED;
        }

        if (amount < MINIMUM_AMOUNT) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"No UUID generated. Amount less than minimum: " + amount});
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

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        LOGGER.log(Level.INFO, "ServerUUIDGeneratorAssertion is preparing itself to be unloaded");
    }
}
