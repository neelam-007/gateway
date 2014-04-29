package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;

/**
 * Policy validator for Injection Threat Protection Assertions
 */
public abstract class InjectionThreatProtectionAssertionValidator implements AssertionValidator {
    private final InjectionThreatProtectionAssertion assertion;

    public InjectionThreatProtectionAssertionValidator(final InjectionThreatProtectionAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public void validate(final AssertionPath assertionPath,
                         final PolicyValidationContext pvc,
                         final PolicyValidatorResult result) {
        if (!isAtLeastOneProtectionEnabled()) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
                    "No protections have been specified.", null));
        }

        if ((assertion.isIncludeUrlPath() || assertion.isIncludeUrlQueryString()) &&
                assertion.getTarget() != TargetMessageType.REQUEST) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
                    "URL cannot be checked if the message is not targeted to Request.", null));
        }

        if (!assertion.isIncludeUrlPath() && !assertion.isIncludeUrlQueryString() &&
                !assertion.isIncludeBody()) {
            result.addError(new PolicyValidatorResult.Error(assertion,
                    "Neither the URL nor Body has been selected to be protected.", null));
        }
    }

    /**
     * Returns true if at least one protection has been selected to apply.
     *
     * Due to the pre-existing differences between threat protection assertions in representing selected protections,
     * this method must be implemented to query the assertion-specific Collection/Array/etc.
     *
     * @return true if at least one protection measure has been selected
     */
    protected abstract boolean isAtLeastOneProtectionEnabled();
}
