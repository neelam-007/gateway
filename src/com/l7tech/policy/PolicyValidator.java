package com.l7tech.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.validator.DefaultPolicyValidator;
import com.l7tech.policy.validator.ServerPolicyValidator;

import java.util.Iterator;

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

    public static PolicyValidator getServerValidator() {
        return new ServerPolicyValidator();
    }

    /**
     * Validates the specified assertion tree.
     *
     * @param assertion the assertion tree to be validated.
     * @return the result of the validation
     */
    public PolicyValidatorResult validate(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        PolicyPathResult path = PolicyPathBuilder.getDefault().generate(assertion);

        // where to collect the result
        PolicyValidatorResult result = new PolicyValidatorResult();

        for (Iterator iterator = path.paths().iterator(); iterator.hasNext();) {
            AssertionPath assertionPath = (AssertionPath)iterator.next();
            validatePath(assertionPath, result);
        }
        return result;
    }

    /**
     * Validate the the asserion path and collect the result into the validator result
     *
     * @param ap the assertion path to validate
     * @param r  the result collect parameter
     */
    abstract public void validatePath(AssertionPath ap, PolicyValidatorResult r);
}
