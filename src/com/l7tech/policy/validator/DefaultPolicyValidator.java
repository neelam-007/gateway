package com.l7tech.policy.validator;

import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * The policy validator that analyzes the policy assertion tree
 * and collects the errors.
 * <p/>
 * Errors are collected in the PolicyValidatorResult instance.
 * <p/>
 * The expected order is:
 * <ul>
 * <li><i>Pre conditions</i> such as ssl, and ip address range (optional)
 * <li><i>Credential location</i> such as ssl, and ip address range (optional)
 * <li><i>Access control, identity, group membership</i> (optional), if present
 * expects the credential finder precondition
 * <li><i>Routing</i> (optional), if present expects the credential finder
 * precondition
 * </ul>
 * <p/>
 * The class methods are not synchronized.
 * <p/>
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyValidator extends PolicyValidator {
    static Logger log = Logger.getLogger(DefaultPolicyValidator.class.getName());

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
        if (!pv.seenRouting) { // no routing report that
            r.addWarning(new PolicyValidatorResult.
              Warning(ap.lastAssertion(), "No route assertion.", null));
        }
        if (!pv.seenCredentials) {
            r.addWarning(new PolicyValidatorResult.Warning(null,
              "No credential assertions are present in the policy. The\n" +
              " service may be exposed to public access", null));
        }
        if (pv.seenCredentials && !pv.seenAccessControl) {
            r.addWarning(new PolicyValidatorResult.Warning(null, "Credentials are collected but are not authenticated." +
              "\nThis service may be exposed to public access.", null));
        }
    }


    /**
     * validate single path. This may grow to some kind of
     * configuraiton based approach.
     */
    private static class PathValidator {
        private PolicyValidatorResult result;

        PathValidator(PolicyValidatorResult r) {
            result = r;
        }

        public void validate(Assertion a) {
            ValidatorFactory.getValidator(a).validate(result);
            if (isPreconditionAssertion(a)) {
                processPrecondition(a);
            } else if(isComposite(a)) {
                processComposite((CompositeAssertion)a);
            } else if (isCrendentialSource(a)) {
                processCredentialSource(a);
            } else if (isAccessControl(a)) {
                processAccessControl((IdentityAssertion)a);
            } else if (isRouting(a)) {
                processRouting((RoutingAssertion)a);
            } else if (a instanceof AllAssertion) {
                processAllAss((AllAssertion)a);
            } else {
                processUnknown();
            }
        }

        private void processAllAss(AllAssertion a) {
            if (a.getChildren().isEmpty()) {
                result.addError(new PolicyValidatorResult.Error(a, "All assertion must contain at least one child.", null));
            }
        }

        private void processAccessControl(IdentityAssertion a) {
            if (!seenCredentials) {
                result.addError(new PolicyValidatorResult.Error(a, "Access control specified without authentication scheme.", null));
            }

            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, "The assertion is after route.", null));
            }

            if (seenSpecificUserAssertion && isSpecificUser(a)) {
                result.addError(new PolicyValidatorResult.Error(a, "Duplicate identity.", null));
            }


            // if we encountered an assertion that requires a client cert or digest, make sure the identity
            // involved is from the internal id provider
            if (seenCredAssertionThatRequiresClientCert || seenDigestAssertion) {
                // check that this identity is member of internal id provider
                /*IdentityAssertion idass = (IdentityAssertion)a;
                try {
                    if (getProviderConfigManager().findByPrimaryKey(idass.getIdentityProviderOid()).type()
                      != IdentityProviderType.INTERNAL) {
                        if (seenCredAssertionThatRequiresClientCert) {
                            result.addError(
                              new PolicyValidatorResult.Error(a,
                                "A credential assertion requires client certs. " +
                              "Only internal identities support client certs.", null));
                        } else if (seenDigestAssertion) {
                            result.addError(
                              new PolicyValidatorResult.Error(a,
                                "A credential assertion requires digest authentication. " +
                              "Only internal identities support digest authentication.", null));
                        }
                    }
                } catch (FindException e) {
                    result.addError(new PolicyValidatorResult.Error(a, "This identity might no longer be valid.", e));
                    log.log(Level.INFO, "could not retrieve IdentityProvider", e);
                }*/
            }
            seenAccessControl = true;
            if (isSpecificUser(a)) {
                seenSpecificUserAssertion = true;
            }
        }

        private void processCredentialSource(Assertion a) {
            // process XmlRequestSecurity first as it may not be credential assertion
            if (a instanceof XmlRequestSecurity) {
                seenCredAssertionThatRequiresClientCert = true;
                XmlRequestSecurity xmlSec = (XmlRequestSecurity)a;
                if (!xmlSec.hasAuthenticationElement()) return;
            }


            if (seenAccessControl) {
                result.addError(new PolicyValidatorResult.
                  Error(a, "Access control already set, this assertion might get ignored.", null));
            }

            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, "The assertion might get ignored.", null));
            }

            if (seenCredentials) {
                result.addWarning(new PolicyValidatorResult.Warning(a, "You already have a credential assertion.", null));
            }

            // new fla, check whether an assertion will require a client cert to function, this is important because
            // we only support client certs for internal users
            if (a instanceof HttpClientCert) {
                seenCredAssertionThatRequiresClientCert = true;
            }

            if (a instanceof HttpDigest) {
                seenDigestAssertion = true;
            }

            // new fla, those assertions cannot forbid ssl
            if (a instanceof HttpBasic || a instanceof WssBasic) {
                /* as per request to not enforce this rule (bugzilla 314) 
                if (sslForbidden) {
                    result.addError(
                            new PolicyValidatorResult.Error(a,
                            "You cannot forbid SSL and ask for basic credentials.", null)
                    );
                }*/
            }
            seenCredentials = true;
        }

        private void processPrecondition(Assertion a) {
            if (a instanceof SslAssertion) {
                seenSsl = true;
                // ssl assertion might be there but it could be forbidden...
                if (((SslAssertion)a).getOption() == SslAssertion.FORBIDDEN) {
                    sslForbidden = true;
                }
                if (seenRouting) {
                    result.addWarning(new PolicyValidatorResult.Warning(a,
                      "The assertion might not work as configured." +
                      "\nThere is a routing assertion before this assertion.", null));
                }
            } else if (a instanceof XmlResponseSecurity) {
                if (!seenRouting) {
                    result.addError(new PolicyValidatorResult.Error(a,
                      "Xml Response Security must occur after routing.", null));
                }
                if (seenXmlResponseSecurityAssertion) {
                    result.addError(new PolicyValidatorResult.Error(a,
                      "Xml Response Security cannot appear twice in path.", null));
                }
                seenXmlResponseSecurityAssertion = true;
            } else if (a instanceof HttpClientCert) {
                if (!seenSsl) {
                    result.addError(new PolicyValidatorResult.Error(a,
                      "HttpClientCert requires to have SSL transport.", null));
                } else if (sslForbidden) {
                    result.addError(new PolicyValidatorResult.Error(a,
                      "HttpClientCert requires to have SSL transport (not forbidden).", null));
                }
                processCredentialSource(a);
            } else if (a instanceof XslTransformation) {
                // check that the assertion is on the right side of the routing
                XslTransformation ass = (XslTransformation)a;

                if (ass.getDirection() == XslTransformation.APPLY_TO_REQUEST && seenRouting) {
                    result.addWarning(new PolicyValidatorResult.Warning(a,
                      "XSL transformation on the request occurs after request is routed.", null));
                } else if (ass.getDirection() == XslTransformation.APPLY_TO_RESPONSE && !seenRouting) {
                    result.addError(new PolicyValidatorResult.Error(a,
                      "XSL transformation on the response must be positioned after routing.", null));
                }
            }
            seenPreconditions = true;
        }

        private void processRouting(RoutingAssertion a) {
            seenRouting = true;
            if (a instanceof HttpRoutingAssertion) {
                processHttpRouting((HttpRoutingAssertion)a);
            } else if (a instanceof JmsRoutingAssertion) {
                processJmsRouting((JmsRoutingAssertion)a);
            } else {
                result.addError(new PolicyValidatorResult.Error(a,
                                                                "This message routing protocol is not supported.",
                                                                null));
            }
        }

        private void processJmsRouting(JmsRoutingAssertion a) {
            Long oid = a.getEndpointOid();
            if (oid == null) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                                    "The assertion might not work as configured." +
                                    "\nThere is no protected service JMS endpoint defined.", null));
            } else {
                // TODO: for now we just assume that any non-null oid is a valid JmsEndpoint
            }
        }

        private void processHttpRouting(HttpRoutingAssertion a) {
            String url = a.getProtectedServiceUrl();
            if (url == null) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                                    "The assertion might not work as configured." +
                                    "\nThe protected service url is empty.", null));
            } else {
                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    result.addWarning(new PolicyValidatorResult.Warning(a,
                                        "The assertion might not work as configured." +
                                        "\nThe protected service url is malformed.", null));
                }
            }
        }

        private void processComposite(CompositeAssertion a) {
            if (a instanceof AllAssertion && a.getChildren().isEmpty()) {
                  result.addWarning(new PolicyValidatorResult.Warning(a,
                      "This composite assertion does not contain any elements.", null));
            }
        }

        private void processUnknown() {
        }

        private boolean isRouting(Assertion a) {
            return a instanceof RoutingAssertion;
        }

        private boolean isAccessControl(Assertion a) {
            return a instanceof IdentityAssertion;
        }

        private boolean isSpecificUser(Assertion a) {
            return a instanceof SpecificUser;
        }

        private boolean isCrendentialSource(Assertion a) {
            return a instanceof CredentialSourceAssertion;
        }

        private boolean isComposite(Assertion a) {
            return a instanceof CompositeAssertion;
        }

        private boolean isPreconditionAssertion(Assertion a) {
            // check preconditions for both SslAssertion and  XmlResponseSecurity assertions - see processPrecondition()
            if (a instanceof SslAssertion || a instanceof XmlResponseSecurity || a instanceof HttpClientCert ||
                a instanceof XslTransformation)
                return true;
            return false;
        }

        boolean seenPreconditions = false;
        boolean seenCredentials = false;
        boolean seenAccessControl = false;
        boolean seenRouting = false;
        boolean seenSsl = false;
        boolean sslForbidden = false;
        boolean seenCredAssertionThatRequiresClientCert = false;
        boolean seenDigestAssertion = false;
        boolean seenXmlResponseSecurityAssertion = false;
        private boolean seenSpecificUserAssertion;
    }

}
