package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
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
import com.l7tech.policy.assertion.xmlsec.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * validate single path, and collect the validation results in the
 * <code>PolicyValidatorResult</code>.
 *
 * TODO: refactor this into asserton specific validators (expand the ValidatorFactory).
 */
class PathValidator {
    /** result accumulator*/
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

    List getDeferredValidators() {
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

        if (a instanceof RequestWssX509Cert) {
            if (seenWssSignature) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, "WSS Signature already set.", null));
            }
            seenWssSignature = true;
        }

        if (a instanceof SecureConversation) {
            if (seenSecureConversation) {
                result.addError(new PolicyValidatorResult.Error(a,
                                                                assertionPath,
                                                                "Secure Conversation already specified.",
                                                                null));
            }
            seenSecureConversation = true;
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
        } else if (a instanceof HttpClientCert) {
            DefaultPolicyValidator.DeferredValidate dv = new DefaultPolicyValidator.DeferredValidate() {
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
        } else if (a instanceof RequestWssIntegrity) {
            // REASON FOR THIS RULE:
            // it makes no sense to validate that an element is signed if we dont validate
            // that the authorized user is the one who signed it.
            if (!seenWssSignature && !seenSecureConversation) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceeded by an WSS Signature assertion or a Secure Conversation.", null));
            }
            // REASON FOR THIS RULE:
            // it makes no sense to check something about the request after it's routed
            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should occur before the request is routed.", null));
            }
        } else if (a instanceof RequestWssConfidentiality) {
            // REASON FOR THIS RULE:
            // it makes no sense to check something about the request after it's routed
            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should occur before the request is routed.", null));
            }
        } else if (a instanceof ResponseWssConfidentiality) {
            // REASON FOR THIS RULE:
            // the server needs to encrypt a symmetric key for the recipient
            // the server needs the client cert for this purpose. this ensures that
            // the client certis available from the request.
            if (!seenWssSignature && !seenSecureConversation) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceeded by a WSS Signature or Secure Conversation assertion.", null));
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
        return a instanceof CredentialSourceAssertion;
    }

    private boolean isComposite(Assertion a) {
        return a instanceof CompositeAssertion;
    }

    private boolean hasPreconditionAssertion(Assertion a) {
        // check preconditions for both SslAssertion and  ResponseWssIntegrity assertions - see processPrecondition()
        if (a instanceof SslAssertion || a instanceof XpathBasedAssertion || a instanceof HttpClientCert ||
          a instanceof XslTransformation)
            return true;
        return false;
    }

    boolean seenPreconditions = false;
    boolean seenCredentials = false;
    boolean seenAccessControl = false;
    boolean seenRouting = false;
    boolean seenWssSignature = false;
    boolean seenSecureConversation = false;
    boolean seenSsl = false;
    boolean sslForbidden = false;
    boolean seenCredAssertionThatRequiresClientCert = false;
    boolean seenDigestAssertion = false;
    boolean seenSpecificUserAssertion = false;
    boolean seenCustomAssertion = false;
    boolean seenHTTPBasic = false;
}
