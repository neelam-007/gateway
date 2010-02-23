package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.wsdl.Wsdl;

/**
 * Interface for validation of policies.
 *
 * <p>To create a <code>PolicyValidator</code>, call one of the static factory
 * methods.</p>
 *
 * <p>A PolicyValidator can be used to validate policy / assertion trees by
 * calling the validate method and passing it the <code>Assertion</code> to be
 * validated.</p>
 */
public interface PolicyValidator {

    /**
     * Validates the specified assertion tree.
     */
    PolicyValidatorResult validate( Assertion assertion, PolicyType policyType, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense) throws InterruptedException;

    /**
     * Scans the provided assertion tree looking for circular includes.
     *
     * @param policyId The identifier for the policy that the provided assertion is the root of
     * @param policyName The name of the policy that the provided assertion is the root of
     * @param rootAssertion The root assertion to start scanning
     * @param r The results of the validation check
     */
    void checkForCircularIncludes(String policyId, String policyName, Assertion rootAssertion, PolicyValidatorResult r);
}
