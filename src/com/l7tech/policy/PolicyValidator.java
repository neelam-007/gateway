package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.DefaultPolicyValidator;

/**
 * A class for validating policies.
 *
 * To create a <code>PolicyValidator</code>, call one of the static factory
 * methods.
 *
 * Once a PolicyValidator object has been created, it can be used to validate
 * policy/assertion trees by calling the {@link #validate(Assertion)} method
 * and passing it the <code>Asserion</code> to be validated.
 *
 * the result is returned in an object of <code>PolicyValidatorResult</code>
 * type.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class PolicyValidator {
    /**
     * Protected constructor, the <code>PolicyValidator</code> instances
     * are obtained using factory methods.
     */
    protected PolicyValidator() {
    }

    /**
     * Obtain the default policy validator
     *
     * @return the policy validator instance
     */
    public static PolicyValidator getDefault() {
        return new DefaultPolicyValidator();
    }

    /**
     * Validates the specified assertion tree.
     *
     * @param assertion the assertion tree to be validated.
     * @return the result of the validation
     */
    abstract public PolicyValidatorResult validate(Assertion assertion);
}
