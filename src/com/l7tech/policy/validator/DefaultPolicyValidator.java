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
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.common.util.Locator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.console.Main;

import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

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
            AssertionPath assertionPath = (AssertionPath) iterator.next();
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
        if (!pv.seenRouting) { // no routing report that
            r.addWarning(
                    new PolicyValidatorResult.
                    Warning(ap.lastAssertion(), "No route assertion.", null)
            );
        }
        if (!pv.seenCredentials) {
            r.addWarning(
                    new PolicyValidatorResult.
                    Warning(null,
                            "No authentication assertions are present in the policy. The\n" +
                            " service may be exposed to public access ", null));
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
                        new PolicyValidatorResult.Error(a, "Access control specified without authentication scheme.", null)
                );
            }

            if (seenRouting) {
                result.addWarning(
                        new PolicyValidatorResult.Warning(a, "The assertion might get ignored.", null)
                );
            }

            // if we encountered an assertion that requires a client cert, make sure the identity involved is
            // from the internal id provider
            if (seenCredAssertionThatRequiresClientCert) {
                // check that this identity is member of internal id provider
                IdentityAssertion idass = (IdentityAssertion)a;
                try {
                    if (getProviderConfigManager().findByPrimaryKey(idass.getIdentityProviderOid()).type() != IdentityProviderType.INTERNAL) {
                        result.addError(
                            new PolicyValidatorResult.Error(a, "A credential assertion requires client certs. Only internal identities support client certs.", null)
                        );
                    }
                } catch (FindException e) {
                    result.addWarning(
                      new PolicyValidatorResult.Warning(a, "This identity might no longer be valid.", null)
                    );
                    log.log(Level.INFO, "could not retrieve IdentityProvider", e);
                }
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

            if (seenCredentials) {
                result.addWarning(
                        new PolicyValidatorResult.Warning(a, "You already have a credential assertion.", null)
                );
            }

            // new fla, check whether an assertion will require a client cert to function, this is important because
            // we only support client certs for internal users
            if (a instanceof HttpClientCert || a instanceof XmlRequestSecurity) {
                seenCredAssertionThatRequiresClientCert = true;
            }
            seenCredentials = true;
        }

        private void processPrecondition(Assertion a) {
            if (a instanceof SslAssertion) {
                seenSsl = true;
                // ssl assertion might be there but it could be forbidden...
                if (((SslAssertion) a).getOption() == SslAssertion.FORBIDDEN) {
                    sslForbidden = true;
                }
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
            } else if (a instanceof HttpClientCert) {
                if (!seenSsl) {
                    result.addWarning(
                            new PolicyValidatorResult.Warning(a,
                                    "The assertion might not work as configured." +
                            "\nHttpClientCert requires to have SSL transport.", null)
                    );
                } else if (sslForbidden) {
                    result.addWarning(
                            new PolicyValidatorResult.Warning(a,
                                    "The assertion might not work as configured." +
                            "\nHttpClientCert requires to have SSL transport but SSL is forbidden.", null)
                    );
                }
                processCredentialSource(a);
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
            if (a instanceof SslAssertion || a instanceof XmlResponseSecurity || a instanceof HttpClientCert)
                return true;
            return false;
        }

        private IdentityProviderConfigManager getProviderConfigManager() throws RuntimeException {
            IdentityProviderConfigManager ipc = (IdentityProviderConfigManager)Locator.
              getDefault().lookup(IdentityProviderConfigManager.class);
            if (ipc == null)  throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
            return ipc;
        }

        boolean seenPreconditions = false;
        boolean seenCredentials = false;
        boolean seenAccessControl = false;
        boolean seenRouting = false;
        boolean seenSsl = false;
        boolean sslForbidden = false;
        boolean seenCredAssertionThatRequiresClientCert = false;

        static Logger log = Logger.getLogger(Main.class.getName());
    }

}
