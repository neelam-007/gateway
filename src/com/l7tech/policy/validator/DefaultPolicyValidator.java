package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyPathBuilder;
import com.l7tech.policy.PolicyPathResult;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;

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
//System.out.println("** Start Validating assertion path");
        for (int i = 0; i < ass.length; i++) {
            pv.validate(ass[i]);
//System.out.println(ass[i].getClass());
        }
//System.out.println("** End Validating assertion path");
        if (!pv.seenRouting) { // no routing report that
            r.addWarning(
              new PolicyValidatorResult.
              Warning(ap.lastAssertion(), "No route assertion.", null)
            );
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
            } else if (isRouting(a)) {
                processRouting(a);
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

            if (seenRouting) {
                result.addWarning(
                  new PolicyValidatorResult.Warning(a, "The assertion might get ignored.", null)
                );
            }

            seenAccessControl = true;
        }

        private void processCredentialSource(Assertion a) {
            if (seenAccessControl) {
                result.addError(
                  new PolicyValidatorResult.
                  Error(a, "Access control already set, this assertion might get ignored.", null));
            }

            if (seenRouting) {
                result.addWarning(
                  new PolicyValidatorResult.Warning(a, "The assertion might get ignored.", null)
                );
            }
            seenCredentials = true;
        }

        private void processPrecondition(Assertion a) {
            if (a instanceof SslAssertion) {
                if (seenRouting) {
                    result.addWarning(
                      new PolicyValidatorResult.Warning(a,
                        "The assertion might not work as configured." +
                       "\nThere is a routing assertion before this assertion.", null)
                    );
                }
            } else if (a instanceof XmlResponseSecurity) {
                if (!seenRouting) {
                    result.addWarning(
                      new PolicyValidatorResult.Warning(a,
                        "The assertion might not work as configured." +
                       "\nXml Response Security must occur after routing.", null)
                    );
                }
            }
            seenPreconditions = true;
        }

        private void processRouting(Assertion a) {
            seenRouting = true;
        }


        private void processUnknown(Assertion a) {
        }

        private boolean isRouting(Assertion a) {
            return a instanceof RoutingAssertion;
        }

        private boolean isAccessControl(Assertion a) {
            return a instanceof IdentityAssertion;
        }

        private boolean isCrendentialSource(Assertion a) {
            return a instanceof CredentialSourceAssertion;
        }

        private boolean isPreconditionAssertion(Assertion a) {
            // check preconditions for both SslAssertion and  XmlResponseSecurity assertions - see processPrecondition()
            if (a instanceof SslAssertion || a instanceof XmlResponseSecurity)
                return true;
            return false;
        }

        boolean seenPreconditions = false;
        boolean seenCredentials = false;
        boolean seenAccessControl = false;
        boolean seenRouting = false;
    }

}
