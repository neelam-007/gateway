package com.l7tech.policy.validator;

import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;

import java.util.Iterator;

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
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
        return validateImpl(assertion);
    }

    private PolicyValidatorResult validateImpl(Assertion assertion) {
        PolicyPathResult path = PolicyPathBuilder.getDefault().generate(assertion);

        // where to collect the result
        PolicyValidatorResult result = new PolicyValidatorResult();

        for (Iterator iterator = path.paths().iterator(); iterator.hasNext();) {
            AssertionPath assertionPath = (AssertionPath)iterator.next();
            validatePath(assertionPath, result);
        }
        return result;
    }

    private void validatePath(AssertionPath ap, PolicyValidatorResult r) {
        Assertion[] ass = ap.getPath();
        PathValidator pv = new PathValidator(r);
        for (int i = 0; i < ass.length; i++) {
            pv.validate(ass[i]);
        }
    }

    /**
     * validate single path. This may grow to some kind of
     *  configuraiton based approach.
     */
    private static class PathValidator {
        private PolicyValidatorResult result;

        PathValidator(PolicyValidatorResult r) {
            result = r;
        }

        public void validate(Assertion a) {
            if (isPreconditionAssertion(a)) {
                processPrecondition(a);
            } else if (isCrendentialSource(a)) {
                processCredentialSource(a);
            } else if (isAccessControl(a)) {
                processAccessControl(a);
            } else {
                processUnknown(a);
            }
        }

        private void processAccessControl(Assertion a) {
            if (!seenCredentials) {
                result.addError(
                  new PolicyValidatorResult.Error(a, "Access control without credentials.", null)
                );
            }
            seenAccessControl = true;
        }

        private void processCredentialSource(Assertion a) {
            if (seenAccessControl) {
                result.addError(
                  new PolicyValidatorResult.
                  Error(a, "Access control already set, this assertion might be ignored.", null));
            }
            seenCredentials = true;
        }

        private void processPrecondition(Assertion a) {
            if (seenAccessControl ||
              seenCredentials ||
              seenRouting) {
                result.addError(
                  new PolicyValidatorResult.Error(a, "The assertion might be ignored.", null)
                );
            }
            seenPreconditions = true;
        }

        private void processUnknown(Assertion a) {
        }

        private boolean isAccessControl(Assertion a) {
            return a instanceof IdentityAssertion;
        }

        private boolean isCrendentialSource(Assertion a) {
            return a instanceof CredentialSourceAssertion;
        }

        private boolean isPreconditionAssertion(Assertion a) {
            return a instanceof SslAssertion;
        }

        boolean seenPreconditions = false;
        boolean seenCredentials = false;
        boolean seenAccessControl = false;
        boolean seenRouting = false;
    }
}
