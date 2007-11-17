package com.l7tech.policy;

import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ReadOnlyEntityManager;

/**
 * A class for validating policies.
 *
 * To create a <code>PolicyValidator</code>, call one of the static factory
 * methods.
 *
 * Once a PolicyValidator object has been created, it can be used to validate
 * policy/assertion trees by calling validate method
 * and passing it the <code>Asserion</code> to be validated.
 *
 * the result is returned in an object of <code>PolicyValidatorResult</code>
 * type.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public abstract class PolicyValidator {
    protected final ReadOnlyEntityManager<Policy, EntityHeader> policyFinder;
    private final PolicyPathBuilderFactory pathBuilderFactory;

    /**
     * Protected constructor, the <code>PolicyValidator</code> instances
     * are obtained using Spring
     */
    protected PolicyValidator(ReadOnlyEntityManager<Policy, EntityHeader> policyFinder, PolicyPathBuilderFactory pathBuilderFactory) {
        this.policyFinder = policyFinder;
        this.pathBuilderFactory = pathBuilderFactory;
    }

    /**
     * Validates the specified assertion tree.
     */
    public PolicyValidatorResult validate(Assertion assertion, PolicyType policyType, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense) throws InterruptedException {
        assertion.treeChanged();

        // where to collect the result
        PolicyValidatorResult result = new PolicyValidatorResult();
        PolicyPathResult path;
        try {
            path = pathBuilderFactory.makePathBuilder().generate(assertion);
        } catch (PolicyAssertionException e) {
            result.addError(new PolicyValidatorResult.Error(e.getAssertion(), null, e.getMessage(), e));
            return result;
        }

        for (AssertionPath assertionPath : path.paths()) {
            validatePath(assertionPath, policyType, wsdl, soap, assertionLicense, result);
        }
        return result;
    }

    /**
     * Validate the the asserion path and collect the result into the validator result
     *
     * @param ap the assertion path to validate
     * @param policyType
     * @param wsdl
     * @param soap @throws InterruptedException if the thread is interrupted while validating
     * @param r  the result collect parameter
     * @throws InterruptedException if the thread is interrupted while validating
     */
    abstract public void validatePath(AssertionPath ap, PolicyType policyType, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense, PolicyValidatorResult r) throws InterruptedException;
}
