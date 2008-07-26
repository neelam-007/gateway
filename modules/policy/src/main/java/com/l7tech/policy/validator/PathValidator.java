package com.l7tech.policy.validator;

import com.l7tech.util.Functions;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.annotation.RequiresSOAP;
import com.l7tech.policy.assertion.annotation.RequiresXML;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.validator.DefaultPolicyValidator.DeferredValidate;
import com.l7tech.policy.wsp.WspReader;

import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;

/**
 * validate single path, and collect the validation results in the
 * <code>PolicyValidatorResult</code>.
 * <p/>
 * TODO: refactor this into asserton specific validators (expand the ValidatorFactory).
 *
 * TODO: change assertion validators so that they are instantiated once per assertion instance, at the start of
 * validation, instead of once per assertion instance per path.  Validators would do assertion-specific validation
 * (things like ensuring that the xpath expression is valid -- internal to assertion and doesn't vary between paths)
 * just once per assertion instance, right after being created, instead of once per assertion instance per path.
 * PathValidator would be passed a map of assertion instances to validator instances.
 */
class PathValidator {

    private static final Class<? extends Assertion> ASSERTION_HTTPBASIC = HttpBasic.class;
    private static final Class<? extends Assertion> ASSERTION_SECURECONVERSATION = SecureConversation.class;
    private static final Class<? extends Assertion> ASSERTION_XPATHCREDENTIALS = XpathCredentialSource.class;
    private static final Class<? extends Assertion> ASSERTION_SAMLASSERTION = RequestWssSaml.class;
    private static final Class<? extends Assertion> ASSERTION_WSSUSERNAMETOKENBASIC = WssBasic.class;
    private static final Class<? extends Assertion> ASSERTION_ENCRYPTEDUSERNAMETOKEN = EncryptedUsernameTokenAssertion.class;
    private static final Class<? extends Assertion> ASSERTION_KERBEROSTICKET = RequestWssKerberos.class;
    private static final Class<? extends Assertion> ASSERTION_COOKIECREDS = CookieCredentialSourceAssertion.class;


    private static Map<String, Object> policyParseCache = Collections.synchronizedMap(new WeakHashMap<String, Object>());

    /**
     * result accumulator
     */
    private PolicyValidatorResult result;
    private List<DeferredValidate> deferredValidators = new ArrayList<DeferredValidate>();
    private AssertionPath assertionPath;
    private Collection<BindingOperation> wsdlBindingOperations;
    private Set<Class<? extends Assertion>> seenAssertionClasses = new HashSet<Class<? extends Assertion>>();
    private Map<String, Boolean> seenCredentials = new HashMap<String, Boolean>();
    private Map<String, Boolean> seenCredentialsSinceModified = new HashMap<String, Boolean>();
    private Map<String, Boolean> seenWssSignature = new HashMap<String, Boolean>();
    private Map<String, Boolean> seenSamlSecurity = new HashMap<String, Boolean>();
    private boolean seenSpecificUserAssertion = false;
    private boolean seenAuthenticationAssertion = false;
    private boolean seenCustomAuth = false;
    private final AssertionLicense assertionLicense;

    boolean seenAccessControl = false;
    boolean seenResponse = false;
    boolean seenParsing = false;
    boolean seenRouting = false;
    private final Wsdl wsdl;
    private final boolean soap;

    PathValidator(AssertionPath ap, PolicyValidatorResult r, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense) {
        result = r;
        assertionPath = ap;
        this.wsdl = wsdl;
        this.soap = soap;
        this.assertionLicense = assertionLicense;
        if (assertionLicense == null) throw new NullPointerException();
    }

    /**
     * Validate the specific assertion.
     * Precondition: the assertion "a" must have been pre-checked to be enabled.
     * @see {@link com.l7tech.policy.validator.DefaultPolicyValidator#validatePath} for the prechecking.
     * @param a: the assertion to be validated.
     * @throws InterruptedException
     */
    public void validate(Assertion a) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        final AssertionValidator av = ValidatorFactory.getValidator(a);
        av.validate(assertionPath, wsdl, soap, result);

        // Check licensing
        if (assertionLicense != null) {
            if (!assertionLicense.isAssertionEnabled(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "This assertion is not available on this Gateway cluster and may cause all requests to this service to fail.", null));
            }
        }

        // process any preconditions
        processPrecondition(a);

        if (onlyForSoap(a)) {
            processSoapSpecific(a);
        }

        if (involvesSignature(a)) {
            try {
                checkForRelativeURINamespaces(a);
            } catch (WSDLException e) {
                throw new RuntimeException(e); // can't happen
            }
        }

        if (normallyBeforeRouting(a) && seenRouting) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion is typically positioned before routing", null));
        }

        if (a instanceof AuditAssertion) {
            AuditAssertion auditAssertion = (AuditAssertion) a;
            if (auditAssertion.isSaveRequest() || auditAssertion.isSaveResponse()) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "Saving request or response XML may result in excessive disk space usage; do not enable for production services", null));
            }
        }

        if (a instanceof IdentityAssertion) {
            processAccessControl((IdentityAssertion)a);
        }

        if (a instanceof CompositeAssertion) {
            processComposite((CompositeAssertion)a);
        } else if (a.isCredentialModifier()) {
            processCredentialModifier(a);
        } else if (a.isCredentialSource()) {
            processCredentialSource(a);
        } else if (a instanceof RoutingAssertion) {
            processRouting((RoutingAssertion)a);
        } else if (isCustom(a)) {
            processCustom(a);
        } else if (a instanceof UnknownAssertion){
            processUnknown((UnknownAssertion)a);
        } else if (a instanceof SqlAttackAssertion) {
            processSQL((SqlAttackAssertion)a);
        } else if (a instanceof OversizedTextAssertion) {
            processOversizedText((OversizedTextAssertion)a);
        } else if (a instanceof SamlBrowserArtifact) {
            seenAccessControl = true;
            setSeenCredentials(a, true);
        } else if (a instanceof Regex) {
            validateRegex((Regex)a);
        } else if (a instanceof HtmlFormDataAssertion) {
            processHtmlFormDataAssertion((HtmlFormDataAssertion)a);
        }

        if (a instanceof XslTransformation) {
            processXslTransformation((XslTransformation)a);
        }

        if (parsesXML(a)) {
            seenParsing = true;
        }

        if (a instanceof TrueAssertion) {
            if (a.getParent() != null && a.getParent() instanceof AllAssertion) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion is not useful when the child of an All assertion", null));
            }
        }

        setSeen(a.getClass());
    }

    private void processXslTransformation(XslTransformation xslt) {
        if (xslt.getResourceInfo().getType() == AssertionResourceType.MESSAGE_URL) {
            if (soap) {
                result.addWarning(new PolicyValidatorResult.Warning(xslt, assertionPath,
                  "This assertion is configured to require an &lt;?xml-stylesheet?&gt; processing instruction, but SOAP messages do not allow them.", null));
            }
        }
    }

    private void validateRegex(Regex a) {
        // check encoding is supported (should that not be checked by the dialog?)
        if (a.getEncoding() != null &&
            a.getEncoding().length() > 0 &&
            !Charset.isSupported(a.getEncoding())) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                                                                "The encoding '" + a.getEncoding() + "' is not supported",
                                                                null));
        }
    }

    private void processOversizedText(OversizedTextAssertion oversizedTextAssertion) {
        if (oversizedTextAssertion != null && seenRouting) {
            result.addWarning(new PolicyValidatorResult.Warning(oversizedTextAssertion, assertionPath,
                                                                "This assertion should occur before the request is routed.",
                                                                null));
        }
    }

    private void processSQL(SqlAttackAssertion sqlAttackAssertion) {
        if (sqlAttackAssertion != null) {
            if (sqlAttackAssertion.getProtections().isEmpty()) {
                result.addWarning(new PolicyValidatorResult.Warning(sqlAttackAssertion, assertionPath, "No SQL protections have been specified", null));
            }
        }
    }

    private void processHtmlFormDataAssertion(HtmlFormDataAssertion htmlFormDataAssertion) {
        if (htmlFormDataAssertion != null && seenRouting) {
            result.addWarning(new PolicyValidatorResult.Warning(htmlFormDataAssertion, assertionPath,
                                                                "This assertion should occur before the request is routed.",
                                                                null));
        }
    }

    List<DeferredValidate> getDeferredValidators() {
        return deferredValidators;
    }

    private void processCustom(Assertion a) {
        CustomAssertionHolder csh = (CustomAssertionHolder)a;
        if (Category.ACCESS_CONTROL.equals(csh.getCategory())) {
            if (!seenCredentials(a)) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, "Access control specified without " +
                  "authentication scheme.", null));
            } else {
                // if the credential source is not HTTP Basic
                if (!haveSeen(ASSERTION_HTTPBASIC) && !haveSeen(ASSERTION_COOKIECREDS) && !haveSeen(ASSERTION_XPATHCREDENTIALS)){
                    result.addWarning(new PolicyValidatorResult.
                      Warning(a, assertionPath, "HTTP Basic Authentication, HTTP Cookie session token or XPath Credentials are usually used as the authentication " +
                        "schemes when a policy contains a Custom Assertion.", null));
                }
            }

            if (seenCustomAuth) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "You already have an access control Custom " +
                  "Assertion in this path.", null));
            } else if (seenAccessControl) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, "No user or group assertion is " +
                  "allowed when an access control Custom Assertion is used.", null));
            }

            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion is after " +
                  "route.", null));
            }
            seenAccessControl = true;
            seenCustomAuth = true;
        }
    }


    private void processAccessControl(IdentityAssertion a) {
        if (!seenCredentials(a)) {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath, "Access control specified without authentication scheme.", null));
        }

        if (seenRouting) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion is after route.", null));
        }

        if (seenSpecificUserAssertion && isSpecificUser(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "Uncommon use of multiple user identities in the same path.", null));
        } else if (seenAuthenticationAssertion && isAuthenticationAssertion(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "Uncommon use of multiple authentication assertions in the same path.", null));
        }

        if (seenCustomAuth) {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath, "No user or group assertions are allowed when " +
              "an access control Custom Assertion is used.", null));
        }

        seenAccessControl = true;
        if (isSpecificUser(a)) {
            seenSpecificUserAssertion = true;
        } else if (isAuthenticationAssertion(a)) {
            seenAuthenticationAssertion = true;
        }
    }

    private void processCredentialModifier(Assertion a) {
        if (seenRouting && isDefaultActor(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion must occur before routing.", null));
        }

        if (seenAccessControl && isDefaultActor(a) || seenCredentialsSinceModified(a)) {
            result.addWarning(new PolicyValidatorResult.
              Warning(a, assertionPath, "Uncommon use of multiple access control.", null));
        }

        setSeenCredentialsSinceModified(a, false);
    }

    private void processCredentialSource(Assertion a) {
        if (seenRouting && isDefaultActor(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion must occur before routing.", null));
        }

        if (seenAccessControl && isDefaultActor(a) || seenCredentialsSinceModified(a)) {
            result.addWarning(new PolicyValidatorResult.
              Warning(a, assertionPath, "Uncommon use of multiple access control.", null));
        }

        // Dupe checks
        if (a instanceof RequestWssX509Cert) {
            if (seenWssSignature(a)) {
                result.addError(new PolicyValidatorResult.
                  Error(a, assertionPath, "WSS Signature already set.", null));
            }
            setSeenWssSignature(a, true);
        }

        if (a instanceof SecureConversation) {
            if (haveSeen(ASSERTION_SECURECONVERSATION)) {
                result.addError(new PolicyValidatorResult.
                  Error(a,assertionPath,"WS Secure Conversation already specified.", null));
            }
        }

        if (a instanceof RequestWssSaml) {
            if (haveSeen(ASSERTION_SAMLASSERTION)) {
                result.addError(new PolicyValidatorResult.
                  Error(a,assertionPath,"SAML Assertion already specified.", null));
            }
        }

        // kerberos is both credentials and authorization since authorization is delegated to issuer
        if (a instanceof RequestWssKerberos || a instanceof HttpNegotiate) {
            seenAccessControl = true;
        }

        //
        if (a instanceof RequestWssSaml)
            setSeenSamlStatement(a, true);

        setSeenCredentials(a, true);
        setSeenCredentialsSinceModified(a, true);
    }

    private void processPrecondition(final Assertion a) {
        if (a instanceof XslTransformation) {
            // check that the assertion is on the right side of the routing
            XslTransformation ass = (XslTransformation)a;

            if (ass.getDirection() == XslTransformation.APPLY_TO_REQUEST && seenResponse) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "XSL transformation on the request occurs after a response is available.", null));
            } else if (ass.getDirection() == XslTransformation.APPLY_TO_RESPONSE && !seenResponse) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                  "XSL transformation on the response must not be positioned before a response is available.", null));
            }


        } else if (a instanceof RequestWssIntegrity ||
                    a instanceof ResponseWssConfidentiality ||
                   (a instanceof RequestWssTimestamp && ((RequestWssTimestamp)a).isSignatureRequired() && ((RequestWssTimestamp)a).getTarget() == TargetMessageType.REQUEST) ||
                   (a instanceof RequestSwAAssertion && ((RequestSwAAssertion)a).requiresSignature()) ||
                   (hasFlag(a, ValidatorFlag.REQUIRE_SIGNATURE))) {
            // REASONS FOR THIS RULE
            //
            // 1. For RequestWssIntegrity:
            // it makes no sense to validate that an element is signed if we dont validate
            // that the authorized user is the one who signed it.
            //
            // 2. For ResponseWssConfidentiality:
            // the server needs to encrypt a symmetric key for the recipient
            // the server needs the client cert for this purpose. this ensures that
            // the client certis available from the request.
            if (!seenWssSignature(a) && !haveSeen(ASSERTION_SECURECONVERSATION) && !seenSamlSecurity(a) &&
                    !haveSeen(ASSERTION_ENCRYPTEDUSERNAMETOKEN) && !haveSeen(ASSERTION_KERBEROSTICKET))
            {
                String actor = assertionToActor(a);
                String msg;
                if (actor.equals(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE)) {
                    msg = "This assertion should be preceded by a WSS Signature assertion, " +
                          "a WS Secure Conversation assertion, a SAML assertion, " +
                          "an Encrypted UsernameToken assertion, or a WSS Kerberos assertion.";
                } else {
                    msg = "This assertion should be preceded by a WSS Signature assertion," +
                          "an Encrypted UsernameToken assertion, a WSS Kerberos assertion, or a " +
                          "SAML assertion (for actor " + actor + ").";
                }
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, msg, null));
            }
            // REASON FOR THIS RULE:
            // it makes no sense to check something about the request after it's routed
            if (a instanceof RequestWssIntegrity || (a instanceof RequestWssTimestamp && ((RequestWssTimestamp)a).getTarget() == TargetMessageType.REQUEST)) {
                if (seenRouting && isDefaultActor(a)) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "This assertion should occur before the request is routed.", null));
                }
            } else if (a instanceof RequestSwAAssertion && seenRouting) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                  "The assertion must be positioned before the routing assertion.", null));
            }
        } else if (a instanceof RequestSwAAssertion) {
            if (seenRouting) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                  "The assertion must be positioned before the routing assertion.", null));
            }
        } else if (a instanceof RequestWssConfidentiality) {
            // REASON FOR THIS RULE:
            // it makes no sense to check something about the request after it's routed
            if (seenRouting && isDefaultActor(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should occur before the request is routed.", null));
            }
        } else if (a instanceof ResponseXpathAssertion) {
            if (!seenResponse) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion will not work because there is no response yet. " +
                  "Move this assertion after a routing assertion.", null));
            }
        } else if(a instanceof WsTrustCredentialExchange) {
            if(!seenUsernamePasswordCredentials()
            && !seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceded by a credential assertion (HTTP Basic, XPath Credentials, WSS UsernameToken Basic or SAML).", null));
            }
        } else if(a instanceof WsFederationPassiveTokenRequest) {
            if(!seenUsernamePasswordCredentials()) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceded by a credential assertion (HTTP Basic, XPath Credentials or WSS UsernameToken Basic).", null));
            }
        } else if(a instanceof WsFederationPassiveTokenExchange) {
            if(!seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceded by a SAML assertion.", null));
            }
        } else if (a instanceof WssBasic) {
            // bugzilla 2518
            if (!(a instanceof EncryptedUsernameTokenAssertion)) {
                if (!haveSeen(SslAssertion.class)) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "This assertion should be preceded by a SSL or TLS Transport assertion.", null));
                }
            }
        } else if (a instanceof ResponseWssSecurityToken) {
            // bugzilla 2753, 2421
            if (((ResponseWssSecurityToken)a).isIncludePassword() && !seenUsernamePasswordCredentials()) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceded by an assertion that collects a password.", null));
            }
        }

        if (a instanceof ResponseWssConfig || a instanceof ResponseWssConfidentiality) {
            if (!seenResponse) {
                result.addWarning(
                        new PolicyValidatorResult.Warning(a,
                                                          assertionPath,
                                                          "This assertion acts on the response and should be preceded by a routing assertion",
                                                          null));
            }
        }
    }

    /**
     * Check if the assertions validation metadata contains the given flag 
     */
    private boolean hasFlag(final Assertion a, final ValidatorFlag flag) {
        boolean flagged = false;

        Functions.Unary<Set<ValidatorFlag>,Assertion> flagAccessor =
            (Functions.Unary<Set<ValidatorFlag>,Assertion>) a.meta().get(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY);

        if ( flagAccessor != null ) {
            Set<ValidatorFlag> flags = flagAccessor.call(a);
            flagged = flags!=null && flags.contains(flag);            
        }

        return flagged;
    }

    private void processRouting(RoutingAssertion a) {
        seenResponse = true;
        if (a instanceof HttpRoutingAssertion) {
            processHttpRouting((HttpRoutingAssertion)a);
            if (a instanceof BridgeRoutingAssertion) {
                processBridgeRouting((BridgeRoutingAssertion)a);
            }
        } else if (a instanceof JmsRoutingAssertion) {
            processJmsRouting((JmsRoutingAssertion)a);
        }
        // todo, refactor RoutingAssertion interface so it doesn't need to be implemented
        // by assertions that dont really route like echo and template
        if (a != null) {
            if (!(a.getClass().getName().contains("EchoRoutingAssertion") || a instanceof HardcodedResponseAssertion)) {
                seenRouting = true;
            }
        }
    }

    private void processBridgeRouting(BridgeRoutingAssertion a) {
        String policyXml = a.getPolicyXml();
        if (policyXml == null)
            return;
        Object cachedResult = policyParseCache.get(policyXml);
        if (cachedResult instanceof Boolean)
            return;
        if (cachedResult instanceof Throwable) {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                                                            "This manually specified policy XML is not valid.",
                                                            (Throwable)cachedResult));
            return;
        }
        try {
            WspReader.getDefault().parseStrictly(policyXml);
            policyParseCache.put(policyXml, Boolean.TRUE);
        } catch (IOException e) {
            policyParseCache.put(policyXml, e);
            result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                                                            "This manually specified policy XML is not valid.",
                                                            e));
        }
    }

    private void processJmsRouting(JmsRoutingAssertion a) {
        Long oid = a.getEndpointOid();
        if (oid == null) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
              "The assertion might not work as configured." +
              " There is no protected service JMS queue defined.", null));
        }

        if (a.isAttachSamlSenderVouches() && !seenAccessControl) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                    "Routing with SAML Sender-Vouches but credentials are not authenticated.", null));
        }
    }

    private void processHttpRouting(HttpRoutingAssertion a) {
        String url = a.getProtectedServiceUrl();
        if (url == null) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
              "The assertion might not work as configured." +
              " The protected service URL is empty.", null));
        } else {
            // only do this if the url has no context variables
            if (url.indexOf("${") < 0) {
                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "The assertion might not work as configured." +
                      " The protected service URL is malformed.", null));
                }
            }
        }

        if (a.isAttachSamlSenderVouches() && !seenAccessControl) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                    "Routing with SAML Sender-Vouches but credentials are not authenticated.", null));
        }
    }

    private void checkForRelativeURINamespaces(Assertion a) throws WSDLException {
        if (soap && wsdl != null) {
            if (wsdlBindingOperations == null) {
                //noinspection unchecked
                wsdlBindingOperations = wsdl.getBindingOperations();
            }
            class RelativeURINamespaceProblemFeedback {
                String ns;
                String operationName;
                String msgname;

                public RelativeURINamespaceProblemFeedback(String ns, String operationName, String msgname) {
                    this.ns = ns;
                    this.operationName = operationName;
                    this.msgname = msgname;
                }
            }
            Collection<RelativeURINamespaceProblemFeedback> feedback = new ArrayList<RelativeURINamespaceProblemFeedback>();
            for (BindingOperation operation : wsdlBindingOperations) {
                String ns = wsdl.getBindingInputNS(operation);
                if (ns != null && ns.indexOf(':') < 0) {
                    feedback.add(new RelativeURINamespaceProblemFeedback(ns,
                                                                         operation.getName(),
                                                                         (operation.getBindingInput().getName() != null ? operation.getBindingInput().getName() : operation.getName() + "In")));
                }
                ns = wsdl.getBindingOutputNS(operation);
                if (ns != null && ns.indexOf(':') < 0) {
                    feedback.add(new RelativeURINamespaceProblemFeedback(ns,
                                                                         operation.getName(),
                                                                         (operation.getBindingOutput().getName() != null ? operation.getBindingOutput().getName() : operation.getName() + "Out")));
                }
            }
            if (!feedback.isEmpty()) {
                StringBuffer msg = new StringBuffer("The service refers to a relative namespace URI, " +
                  "which will prevent XML digital signatures from " +
                  "functioning properly.");
                for (Object aFeedback : feedback) {
                    RelativeURINamespaceProblemFeedback fb = (RelativeURINamespaceProblemFeedback)aFeedback;
                    msg.append("<br>Namespace: ").append(fb.ns);
                    msg.append(", Operation Name: ").append(fb.operationName);
                    msg.append(", Message Name: ").append(fb.msgname);
                }
                result.addError(new PolicyValidatorResult.Error(a,
                  assertionPath,
                  msg.toString(),
                  null));
            }
        }
    }

    private void processSoapSpecific(Assertion a) {
        if (!soap) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
              "This assertion only works with SOAP services.", null));
        }
    }

    private void processComposite(CompositeAssertion a) {
        //noinspection unchecked
        List<Assertion> children = a.getChildren();
        for (Assertion kid : children) {
            // If a composite assertion just contains comment assertions and/or disabled assertions, then treat it as empty.
            if (!(kid instanceof CommentAssertion) && kid.isEnabled()) {
                return;
            }
        }
        result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                          "This composite assertion does not contain any children and will always fail.",
                          null));
    }

    private boolean isCustom(Assertion a) {
        return a instanceof CustomAssertionHolder;
    }

    private void processUnknown(UnknownAssertion a) {
        result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
              "This assertion is unrecognized and may cause all requests to this service fail.", null));
    }

    private boolean isSpecificUser(Assertion a) {
        return a instanceof SpecificUser;
    }

    private boolean isAuthenticationAssertion(Assertion a) {
        return a instanceof AuthenticationAssertion;
    }

    private boolean normallyBeforeRouting(Assertion a) {
        return a instanceof SslAssertion && !a.isCredentialSource();
    }

    private boolean involvesSignature(Assertion a) {
        return a instanceof SecureConversation ||
               a instanceof RequestWssIntegrity ||
               a instanceof RequestWssX509Cert ||
               a instanceof ResponseWssIntegrity ||
              (a instanceof RequestWssTimestamp && ((RequestWssTimestamp)a).isSignatureRequired() && ((RequestWssTimestamp)a).getTarget() == TargetMessageType.REQUEST);
    }

    private boolean onlyForSoap(Assertion a) {
        return a != null && a.getClass().isAnnotationPresent(RequiresSOAP.class);
    }

    private boolean parsesXML(Assertion a) {
        return a != null &&
              (a.getClass().isAnnotationPresent(RequiresXML.class) ||
               a.getClass().isAnnotationPresent(RequiresSOAP.class));
    }

    private boolean seenCredentials(Assertion context) {
        return seenCredentials(assertionToActor(context));
    }

    public boolean seenCredentials(String actor) {
        Boolean currentvalue = seenCredentials.get(actor);
        return currentvalue != null && currentvalue;
    }

    public boolean seenAssertion(Class<? extends Assertion> assertionClass) {
        return this.haveSeenInstanceOf(assertionClass);
    }

    private void setSeenCredentials(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenCredentials.put(actor, value);
    }

    private boolean seenCredentialsSinceModified(Assertion context) {
        String actor = assertionToActor(context);
        return seenCredentialsSinceModified(actor);
    }

    private boolean seenCredentialsSinceModified(String actor) {
        Boolean currentvalue = seenCredentialsSinceModified.get(actor);
        return currentvalue != null && currentvalue;
    }

    private void setSeenCredentialsSinceModified(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenCredentialsSinceModified.put(actor, (value) ? Boolean.TRUE : Boolean.FALSE);
    }

    private boolean seenWssSignature(Assertion context) {
        String actor = assertionToActor(context);
        Boolean currentvalue = seenWssSignature.get(actor);
        return currentvalue != null && currentvalue;
    }

    private void setSeenWssSignature(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenWssSignature.put(actor, value);
    }

    private boolean seenSamlSecurity(Assertion context) {
        String actor = assertionToActor(context);
        Boolean currentvalue = seenSamlSecurity.get(actor);
        return currentvalue != null && currentvalue;
    }

    private void setSeenSamlStatement(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenSamlSecurity.put(actor, value);
    }

    private boolean haveSeen(Class<? extends Assertion> assertionClass) {
        return seenAssertionClasses.contains(assertionClass);
    }

    private boolean haveSeenInstanceOf(Class<? extends Assertion> assertionClass) {
        boolean seen = false;
        for (Object seenAssertionClass : seenAssertionClasses) {
            Class currentAssertionClass = (Class)seenAssertionClass;
            if (assertionClass.isAssignableFrom(currentAssertionClass)) {
                seen = true;
                break;
            }
        }
        return seen;
    }

    private void setSeen(Class<? extends Assertion> assertionClass) {
        seenAssertionClasses.add(assertionClass);
    }

    private boolean seenUsernamePasswordCredentials() {
        return haveSeenInstanceOf(ASSERTION_HTTPBASIC)
            || haveSeen(ASSERTION_XPATHCREDENTIALS)
            || haveSeen(ASSERTION_WSSUSERNAMETOKENBASIC);
    }

    private String assertionToActor(Assertion a) {
        String actor = XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE;
        if (a instanceof SecurityHeaderAddressable) {
            actor = ((SecurityHeaderAddressable)a).getRecipientContext().getActor();
        }
        return actor;
    }

    private boolean isDefaultActor(Assertion a) {
        String actor = assertionToActor(a);
        return actor.equals(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE);
    }
}
