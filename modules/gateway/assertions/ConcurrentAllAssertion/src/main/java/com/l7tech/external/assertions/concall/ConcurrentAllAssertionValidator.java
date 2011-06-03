package com.l7tech.external.assertions.concall;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyUtil;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.validator.AssertionValidatorSupport;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.util.Functions;

/**
 * Policy validator for ConcurrentAllAssertion.
 */
@SuppressWarnings({"UnusedDeclaration"})
public class ConcurrentAllAssertionValidator extends AssertionValidatorSupport<ConcurrentAllAssertion> {
    private ConcurrentAllAssertion assertion;

    public ConcurrentAllAssertionValidator(ConcurrentAllAssertion assertion) {
        super(assertion);
        this.assertion = assertion;

        // Check for children that don't belong within a ConcurrentAllAssertion
        PolicyUtil.visitDescendantsAndSelf(assertion, new Functions.UnaryVoid<com.l7tech.policy.assertion.Assertion>() {
            @Override
            public void call(Assertion assertion) {
                if (Assertion.isRequest(assertion)) {
                    addMessage(new PendingMessage(assertion, "This assertion uses the default request, which is not available when running within a Concurrent All assertion."));
                } else if (assertion instanceof RoutingAssertion && ((RoutingAssertion)assertion).needsInitializedRequest()) {
                    addMessage(new PendingMessage(assertion, "This assertion requires the default request, which is not available when running within a Concurrent All assertion."));
                } else if (assertion instanceof RoutingAssertion && ((RoutingAssertion)assertion).initializesRequest()) {
                    addWarningMessage(new PendingMessage(assertion, "This assertion initializes the default request while running within a Concurrent All assertion.  This will not affect the default request outside the Concurrent All."));
                }

                if (Assertion.isResponse(assertion)) {
                    addWarningMessage(new PendingMessage(assertion, "This assertion uses the default response, which is not available when running within a Concurrent All assertion."));
                } else if (assertion instanceof RoutingAssertion && ((RoutingAssertion)assertion).needsInitializedResponse()) {
                    addMessage(new PendingMessage(assertion, "This assertion requires the default response, which is not available when running within a Concurrent All assertion."));
                } else if (assertion instanceof RoutingAssertion && ((RoutingAssertion)assertion).initializesResponse()) {
                    addWarningMessage(new PendingMessage(assertion, "This assertion initializes the default response while running within a Concurrent All assertion.  This will not affect the default response outside the Concurrent All."));
                }
            }
        });
    }

    @Override
    public void validate(AssertionPath path, PolicyValidationContext pvc, PolicyValidatorResult result) {
        super.validate(path, pvc, result);

        //If the concurrent all assertion is found in the audit viewer policy, a validator warning will be generated.
        if (PolicyType.TAG_AUDIT_VIEWER.equals(pvc.getPolicyInternalTag())) {
            final String warningMessage = "The concurrent all assertion is included in the audit viewer policy. The audit viewer private key would not be available within this assertion.";
            result.addWarning(new PolicyValidatorResult.Warning(assertion, warningMessage, null));
        }
    }
}