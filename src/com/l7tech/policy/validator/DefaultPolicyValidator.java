package com.l7tech.policy.validator;

import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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



    public void validatePath(AssertionPath ap, PolicyValidatorResult r) {
        Assertion[] ass = ap.getPath();
        PathValidator pv = new PathValidator(ap, r);
        for (int i = 0; i < ass.length; i++) {
            pv.validate(ass[i]);
        }

        // defrred validations
        Iterator dIt = pv.getDeferredValidators().iterator();
        while (dIt.hasNext()) {
            DeferredValidate dv = (DeferredValidate)dIt.next();
            dv.validate(pv, ass);
        }
        Assertion lastAssertion = ap.lastAssertion();
        if (!pv.seenRouting) { // no routing report that
            r.addWarning(new PolicyValidatorResult.
              Warning(lastAssertion, ap, "No route assertion.", null));
        }
        if (!pv.seenCredentials && pv.seenRouting) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, ap,
              "No credential assertion is present in the policy. The" +
              " service may be exposed to public access", null));
        }
        if (pv.seenCredentials && !pv.seenAccessControl && pv.seenRouting) {
            r.addWarning(new PolicyValidatorResult.Warning(lastAssertion, ap, "Credentials are collected but not authenticated." +
              " This service may be exposed to public access.", null));
        }
    }


    /**
     * validate single path. This may grow to some kind of
     * configuraiton based approach.
     */
    private static class PathValidator {
        PolicyValidatorResult result;
        List deferredValidators = new ArrayList();
        private AssertionPath assertionPath;

        PathValidator(AssertionPath ap, PolicyValidatorResult r) {
            result = r;
            assertionPath = ap;
        }

        public void validate(Assertion a) {
            ValidatorFactory.getValidator(a).validate(assertionPath, result);
            // has precondition
            if (hasPreconditionAssertion(a)) {
                processPrecondition(a);
            }
            if (isComposite(a)) {
                processComposite((CompositeAssertion)a);
            } else if (isCrendentialSource(a)) {
                processCredentialSource(a);
            } else if (isAccessControl(a)) {
                processAccessControl((IdentityAssertion)a);
            } else if (isRouting(a)) {
                processRouting((RoutingAssertion)a);
            } else if (isCustom(a)) {
                processCustom(a);
            } else {
                processUnknown();
            }
        }

        public List getDeferredValidators() {
            return deferredValidators;
        }

        private void processCustom(Assertion a) {
            CustomAssertionHolder csh = (CustomAssertionHolder)a;
            if (Category.ACCESS_CONTROL.equals(csh.getCategory())) {
                if (!seenCredentials) {
                    result.addError(new PolicyValidatorResult.Error(a, assertionPath, "Access control specified without " +
                                                                       "authentication scheme.", null));
                } else {
                    // if the credential source is not HTTP Basic
                    if (!seenHTTPBasic) {
                        result.addError(new PolicyValidatorResult.
                          Error(a, assertionPath, "Only HTTP Basic Authentication can be used as a authentication scheme" +
                                " when a policy involes Custom Assertion.", null));
                    }
                }

                if (seenAccessControl && !seenCustomAssertion) {
                    result.addError(new PolicyValidatorResult.Error(a, assertionPath, "No user or group assertion is allowed when " +
                            "a Custom Assertion is used.", null));
                }

                if (seenCustomAssertion) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "You already have a Custom Assertion " +
                                                                           "in this path.", null));
                }

                if (seenRouting) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion is after route.", null));
                }
                seenAccessControl = true;
                seenCustomAssertion = true;
            }
        }


        private void processAccessControl(IdentityAssertion a) {
            if (!seenCredentials) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, "Access control specified without authentication scheme.", null));
            }

            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion is after route.", null));
            }

            if (seenSpecificUserAssertion && isSpecificUser(a)) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, "More than one identity in path.", null));
            }

            if (seenCustomAssertion) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, "No user or group assertions is allowed when " +
                                                                   "a Custom Assertion is used.", null));
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
            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion must occur before routing.", null));
            }
            // process XmlRequestSecurity first as it may not be credential assertion
            if (a instanceof XmlRequestSecurity) {
                seenCredAssertionThatRequiresClientCert = true;
                XmlRequestSecurity xmlSec = (XmlRequestSecurity)a;
                if (!xmlSec.hasAuthenticationElement()) {
                    return;
                }
            }

            if (seenAccessControl) {
                result.addError(new PolicyValidatorResult.
                  Error(a, assertionPath, "Access control already set, this assertion might get ignored.", null));
            }

            if (seenCredentials) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "You already have a credential assertion.", null));
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

            if (a instanceof HttpBasic) {
                seenHTTPBasic = true;
            }

            // Custom Assertion can only be used with HTTP Basic
            if (seenCustomAssertion && !seenHTTPBasic) {
                result.addError(new PolicyValidatorResult.
                  Error(a, assertionPath, "Only HTTP Basic Authentication can be used as a " +
                           "authentication scheme when the policy involes a Custom Assertion.", null));
            }

            seenCredentials = true;
        }

        private void processPrecondition(final Assertion a) {
            if (a instanceof SslAssertion) {
                seenSsl = true;
                // ssl assertion might be there but it could be forbidden...
                if (((SslAssertion)a).getOption() == SslAssertion.FORBIDDEN) {
                    sslForbidden = true;
                }
                if (seenRouting) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "The assertion might not work as configured." +
                      " There is a routing assertion before this assertion.", null));
                }
            } else if (a instanceof XmlResponseSecurity) {
                if (!seenRouting) {
                    result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                      "Xml Response Security must occur after routing.", null));
                }
                if (seenXmlResponseSecurityAssertion) {
                    result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                      "Xml Response Security cannot appear more than once in path.", null));
                }
                seenXmlResponseSecurityAssertion = true;
            } else if (a instanceof HttpClientCert) {
                DeferredValidate dv = new DeferredValidate() {
                    public void validate(PathValidator pv, Assertion[] path) {
                        if (!pv.seenSsl) {
                            result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                              "HttpClientCert requires to have SSL transport.", null));
                        } else if (pv.sslForbidden) {
                            result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                              "HttpClientCert requires to have SSL transport (not forbidden).", null));
                        }
                    }
                };
                deferredValidators.add(dv);
            } else if (a instanceof XslTransformation) {
                // check that the assertion is on the right side of the routing
                XslTransformation ass = (XslTransformation)a;

                if (ass.getDirection() == XslTransformation.APPLY_TO_REQUEST && seenRouting) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "XSL transformation on the request occurs after request is routed.", null));
                } else if (ass.getDirection() == XslTransformation.APPLY_TO_RESPONSE && !seenRouting) {
                    result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                      "XSL transformation on the response must be positioned after routing.", null));
                }
            } else if (a instanceof XmlRequestSecurity) {
                XmlRequestSecurity maybepartialsignature = (XmlRequestSecurity)a;
                if (!maybepartialsignature.hasAuthenticationElement()) {
                    // check that an identity has been declared
                    if (!seenAccessControl) {
                    //if (!seenSpecificUserAssertion) {
                        result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                            "Partial signature on the request must be preceeded by an access control assertion " +
                            "so that the cert used can be compared to a valid user cert.", null));
                    }
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
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                  "This message routing protocol is not supported.",
                  null));
            }
        }

        private void processJmsRouting(JmsRoutingAssertion a) {
            Long oid = a.getEndpointOid();
            if (oid == null) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "The assertion might not work as configured." +
                  " There is no protected service JMS queue defined.", null));
            } else {
                // TODO: for now we just assume that any non-null oid is a valid JmsEndpoint
            }
        }

        private void processHttpRouting(HttpRoutingAssertion a) {
            String url = a.getProtectedServiceUrl();
            if (url == null) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "The assertion might not work as configured." +
                  " The protected service url is empty.", null));
            } else {
                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "The assertion might not work as configured." +
                      " The protected service url is malformed.", null));
                }
            }
        }

        private void processComposite(CompositeAssertion a) {
            if (a.getChildren().isEmpty()) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This composite assertion does not contain any elements.", null));
            }
        }


        private boolean isCustom(Assertion a) {
            return a instanceof CustomAssertionHolder;
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
            // an xmlrequestsecurity may or may not be considered a credential source
            if (a instanceof XmlRequestSecurity) {
                XmlRequestSecurity maybeCredSource = (XmlRequestSecurity)a;
                if (maybeCredSource.hasAuthenticationElement()) {
                    return true;
                } else return false;
            } else return a instanceof CredentialSourceAssertion;
        }

        private boolean isComposite(Assertion a) {
            return a instanceof CompositeAssertion;
        }

        private boolean hasPreconditionAssertion(Assertion a) {
            // check preconditions for both SslAssertion and  XmlResponseSecurity assertions - see processPrecondition()
            if (a instanceof SslAssertion || a instanceof XmlResponseSecurity || a instanceof HttpClientCert ||
              a instanceof XslTransformation)
                return true;
            if (a instanceof XmlRequestSecurity) {
                XmlRequestSecurity maybepartialrequestsignature = (XmlRequestSecurity)a;
                // if this is a partial xml signature on the request, we need to have authenticated a user beforehand
                if (!maybepartialrequestsignature.hasAuthenticationElement()) {
                    return true;
                }
            }
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
        private boolean seenSpecificUserAssertion = false;
        private boolean seenCustomAssertion = false;
        private boolean seenHTTPBasic = false;
    }

    /**
     * The implementations are invoked after the regular (sequential)
     * validate of the assertion path. This is useful for unordered validations,
     * that is, where some asseriton must be present but not necessarily
     * before the assertion currently examined.
     */
    static interface DeferredValidate {
        void validate(PathValidator pv, Assertion[] path);
    }
}
