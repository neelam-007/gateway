package com.l7tech.policy.validator;

import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * The policy validator that analyzes the policy assertion tree
 * and collects the errors.
 *
 * Errors are collected in the PolicyValidatorResult instance.
 * <p>
 * The expected order is:
 * <ul>
 * <li><i>Pre conditions</i> such as ssl, and ip address range (optional)
 * <li><i>Credential location</i> such as ssl, and ip address range (optional)
 * <li><i>Access control, identity, group membership</i> (optional), if present
 * expects the credential finder precondition
 * <li><i>Routing</i> (optional), if present expects the credential finder
 * precondition
 * </ul>
 * <p>
 * The class methods are not synchronized.
 * <p>
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyValidator extends PolicyValidator {
    /**
     * Validates the specified assertion tree.
     *
     * @param assertion the assertion tree to be validated.
     * @return the result of the validation
     */
    public PolicyValidatorResult validate(Assertion assertion) {
       throw new RuntimeException("Not yet implemented!");
    }
}
