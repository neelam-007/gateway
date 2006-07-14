package com.l7tech.policy.validator;

import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;
import com.l7tech.policy.assertion.credential.WsTrustCredentialExchange;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;

import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * validate single path, and collect the validation results in the
 * <code>PolicyValidatorResult</code>.
 * <p/>
 * TODO: refactor this into asserton specific validators (expand the ValidatorFactory).
 */
class PathValidator {

    private static final Class ASSERTION_HTTPCREDENTIALS = HttpCredentialSourceAssertion.class;
    private static final Class ASSERTION_HTTPBASIC = HttpBasic.class;
    private static final Class ASSERTION_CUSTOM = CustomAssertionHolder.class;
    private static final Class ASSERTION_SECURECONVERSATION = SecureConversation.class;
    private static final Class ASSERTION_XPATHCREDENTIALS = XpathCredentialSource.class;
    private static final Class ASSERTION_SAMLASSERTION = RequestWssSaml.class;
    private static final Class ASSERTION_WSSUSERNAMETOKENBASIC = WssBasic.class;
    private static final Class ASSERTION_ENCRYPTEDUSERNAMETOKEN = EncryptedUsernameTokenAssertion.class;
    private static final Class ASSERTION_KERBEROSTICKET = RequestWssKerberos.class;
    private static final Class ASSERTION_COOKIECREDS = CookieCredentialSourceAssertion.class;


    private static Map policyParseCache = Collections.synchronizedMap(new WeakHashMap());

    /**
     * result accumulator
     */
    private PolicyValidatorResult result;
    private List deferredValidators = new ArrayList();
    private AssertionPath assertionPath;
    private PublishedService service;
    private Collection wsdlBindingOperations;
    private Set seenAssertionClasses = new HashSet();
    private Map seenCredentials = new HashMap();
    private Map seenCredentialsSinceModified = new HashMap();
    private Map seenWssSignature = new HashMap();
    private Map seenSamlSecurity = new HashMap();
    private Map seenVariables = new HashMap();
    private Map assertionFeatureName = new HashMap();
    private boolean seenSpecificUserAssertion = false;
    private final AssertionLicense assertionLicense;

    boolean seenAccessControl = false;
    boolean seenRouting = false;

    PathValidator(AssertionPath ap, PolicyValidatorResult r, PublishedService service, AssertionLicense assertionLicense) {
        result = r;
        assertionPath = ap;
        this.service = service;
        this.assertionLicense = assertionLicense;
        if (assertionLicense == null) throw new NullPointerException();
    }

    private boolean isAssertionEnabled(Assertion ass) {
        String assclass = ass.getClass().getName();
        String featureName = (String)assertionFeatureName.get(assclass);
        if (featureName == null) {
            featureName = Assertion.getFeatureSetName(assclass);
            assertionFeatureName.put(assclass, featureName);
        }

        return assertionLicense.isAssertionEnabled(featureName);
    }

    public void validate(Assertion a) {
        ValidatorFactory.getValidator(a).validate(assertionPath, service, result);

        // Check licensing
        if (assertionLicense != null) {
            if (!assertionLicense.isAssertionEnabled(a.getClass().getName())) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "This assertion is not available on this Gateway cluster", null));
            }
        }

        // has precondition
        if (hasPreconditionAssertion(a)) {
            processPrecondition(a);
        }

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

        if (isComposite(a)) {
            processComposite((CompositeAssertion)a);
        } else if (a.isCredentialModifier()) {
            processCredentialModifier(a);
        } else if (a.isCredentialSource()) {
            processCredentialSource(a);
        } else if (isAccessControl(a)) {
            processAccessControl((IdentityAssertion)a);
        } else if (isRouting(a)) {
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
        }

        if (a instanceof SetsVariables) {
            SetsVariables sv = (SetsVariables) a;
            final VariableMetadata[] vars = sv.getVariablesSet();
            for (int i = 0; i < vars.length; i++) {
                setSeenVariable(vars[i].getName().toLowerCase());
            }
        }

        setSeen(a.getClass());
    }

    private void validateRegex(Regex a) {
        // check encoding is supported (should that not be checked by the dialog?)
        if (a.getEncoding() != null && a.getEncoding().length() > 0) {
            byte[] toto = "foo".getBytes();
            try {
                new String(toto, 0, toto.length, a.getEncoding());
            } catch (UnsupportedEncodingException e) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                                                                    "The encoding '" + a.getEncoding() + "' is not supported",
                                                                    null));
            }
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
        // todo check you thing
        //result.addWarning(new PolicyValidatorResult.Warning(sqlAttackAssertion, assertionPath, "Dont dot this stupid!.", null));
    }

    List getDeferredValidators() {
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
                if (!haveSeen(ASSERTION_HTTPBASIC) && !haveSeen(ASSERTION_COOKIECREDS)){
                    result.addWarning(new PolicyValidatorResult.
                      Warning(a, assertionPath, "HTTP Basic Authentication (or HTTP Cookie session token) is usually used as the authentication " +
                        "scheme when a policy contains a Custom Assertion.", null));
                }
            }

            if (seenAccessControl && !haveSeen(ASSERTION_CUSTOM)) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, "No user or group assertion is " +
                  "allowed when a Custom Assertion is used.", null));
            }

            if (haveSeen(ASSERTION_CUSTOM)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "You already have a Custom " +
                  "Assertion in this path.", null));
            }

            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion is after " +
                  "route.", null));
            }
            seenAccessControl = true;
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
            result.addError(new PolicyValidatorResult.Error(a, assertionPath, "More than one identity in path.", null));
        }

        if (haveSeen(ASSERTION_CUSTOM)) {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath, "No user or group assertions is allowed when " +
              "a Custom Assertion is used.", null));
        }

        seenAccessControl = true;
        if (isSpecificUser(a)) {
            seenSpecificUserAssertion = true;
        }
    }

    private void processCredentialModifier(Assertion a) {
        if (seenRouting && isDefaultActor(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion must occur before routing.", null));
        }

        if (seenAccessControl && isDefaultActor(a)) {
            result.addError(new PolicyValidatorResult.
              Error(a, assertionPath, "Access control already set, this assertion might get ignored.", null));
        }

        setSeenCredentialsSinceModified(a, false);
    }

    private void processCredentialSource(Assertion a) {
        if (seenRouting && isDefaultActor(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, "The assertion must occur before routing.", null));
        }

        if (seenAccessControl && isDefaultActor(a)) {
            result.addError(new PolicyValidatorResult.
              Error(a, assertionPath, "Access control already set, this assertion might get ignored.", null));
        }

        if (seenCredentialsSinceModified(a)) {
            result.addWarning(new PolicyValidatorResult.
              Warning(a, assertionPath, "You already have a credential assertion.", null));
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
                  Error(a,assertionPath,"Secure Conversation already specified.", null));
            }
        }

        if (a instanceof RequestWssSaml) {
            if (haveSeen(ASSERTION_SAMLASSERTION)) {
                result.addError(new PolicyValidatorResult.
                  Error(a,assertionPath,"SAML Assertion already specified.", null));
            }
        }

        // kerberos is both credentials and authorization since authorization is delegated to issuer
        if (a instanceof RequestWssKerberos) {
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

            if (ass.getDirection() == XslTransformation.APPLY_TO_REQUEST && seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "XSL transformation on the request occurs after request is routed.", null));
            } else if (ass.getDirection() == XslTransformation.APPLY_TO_RESPONSE && !seenRouting) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                  "XSL transformation on the response must be positioned after routing.", null));
            }
        } else if (a instanceof RequestSwAAssertion) {
            if (seenRouting) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                  "The assertion must be positioned before the routing assertion.", null));
            }
        } else if (a instanceof RequestWssIntegrity ||
                    a instanceof ResponseWssConfidentiality ||
                   (a instanceof RequestWssTimestamp && ((RequestWssTimestamp)a).isSignatureRequired())) {
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
                    msg = "This assertion should be preceeded by an WSS Signature assertion, " +
                          "a Secure Conversation assertion, a SAML Security assertion, " +
                          "an Encrypted UsernameToken assertion, or a WSS Kerberos assertion.";
                } else {
                    msg = "This assertion should be preceeded by an WSS Signature assertion," +
                          "an Encrypted UsernameToken assertion, a WSS Kerberos assertion, or a " +
                          "SAML Security assertion (for actor " + actor + ").";
                }
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, msg, null));
            }
            // REASON FOR THIS RULE:
            // it makes no sense to check something about the request after it's routed
            if (a instanceof RequestWssIntegrity || a instanceof RequestWssTimestamp) {
                if (seenRouting && isDefaultActor(a)) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "This assertion should occur before the request is routed.", null));
                }
            }
        } else if (a instanceof RequestWssConfidentiality) {
            // REASON FOR THIS RULE:
            // it makes no sense to check something about the request after it's routed
            if (seenRouting && isDefaultActor(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should occur before the request is routed.", null));
            }
        } else if (a instanceof ResponseXpathAssertion) {
            if (!seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion will never work because there is no response yet. This assertion should be moved " +
                  "after the routing assertion.", null));
            }
        } else if (a instanceof RequestWssReplayProtection) {
            if (!seenWssSignature(a) && !haveSeen(ASSERTION_SECURECONVERSATION) && !seenSamlSecurity(a) &&
                    !haveSeen(ASSERTION_ENCRYPTEDUSERNAMETOKEN)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceeded by an WSS Signature assertion, " +
                  "a Secure Conversation assertion, or a SAML Security assertion.", null));
            }
        }
        else if(a instanceof WsTrustCredentialExchange) {
            if(!seenUsernamePasswordCredentials()
            && !seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceeded by a credential assertion (HTTP, XPath Credentials, WSS UsernameToken Basic or SAML).", null));
            }
        }
        else if(a instanceof WsFederationPassiveTokenRequest) {
            if(!seenUsernamePasswordCredentials()) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceeded by a credential assertion (HTTP, XPath Credentials or WSS UsernameToken Basic).", null));
            }
        }
        else if(a instanceof WsFederationPassiveTokenExchange) {
            if(!seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  "This assertion should be preceeded by a SAML Security assertion.", null));
            }
        }
        else if (a instanceof WssBasic) {
            // bugzilla 2518
            if (!(a instanceof EncryptedUsernameTokenAssertion)) {
                if (!haveSeen(SslAssertion.class)) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      "This assertion should be preceeded by a TLS/SSL assertion.", null));
                }
            }
        }

        if (a instanceof UsesVariables) {
            UsesVariables ua = (UsesVariables)a;
            final String[] vars = ua.getVariablesUsed();
            for (int i = 0; i < vars.length; i++) {
                String var = vars[i];
                if (!(BuiltinVariables.isSupported(var) || seenVariable(var.toLowerCase()))) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                            "This assertion refers to the variable '" + var + "', which is neither built-in nor set in the policy.", null));
                }
            }
        }
    }

    private void processRouting(RoutingAssertion a) {
        seenRouting = true;
        if (a instanceof HttpRoutingAssertion) {
            processHttpRouting((HttpRoutingAssertion)a);
            if (a instanceof BridgeRoutingAssertion) {
                processBridgeRouting((BridgeRoutingAssertion)a);
            }
        } else if (a instanceof JmsRoutingAssertion) {
            processJmsRouting((JmsRoutingAssertion)a);
        } else if (a instanceof EchoRoutingAssertion) {
            // Nothing to see here, folks. Move along...
        } else if (a instanceof HardcodedResponseAssertion) {
            //validations for this assertion
            HardcodedResponseAssertion ass = (HardcodedResponseAssertion)a;
            try {
                ContentTypeHeader.parseValue(ass.getResponseContentType());
            } catch (IOException e) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                    "the content type is invalid. " + e.getMessage(),
                    null));
            }
        } else {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath,
              "This message routing protocol is not supported.",
              null));
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
            WspReader.parseStrictly(policyXml);
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

    private void checkForRelativeURINamespaces(Assertion a) throws WSDLException {
        if (service != null && service.isSoap()) {
            Wsdl parsedWsdl = service.parsedWsdl();
            if (wsdlBindingOperations == null) {
                wsdlBindingOperations = parsedWsdl.getBindingOperations();
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
            Collection feedback = new ArrayList();
            for (Iterator iterator = wsdlBindingOperations.iterator(); iterator.hasNext();) {
                BindingOperation operation = (BindingOperation)iterator.next();
                String ns = parsedWsdl.getBindingInputNS(operation);
                if (ns != null && ns.indexOf(':') < 0) {
                    feedback.add(new RelativeURINamespaceProblemFeedback(ns,
                      operation.getName(),
                      (operation.getBindingInput().getName() != null ? operation.getBindingInput().getName() : operation.getName() + "In")));
                }
                ns = parsedWsdl.getBindingOutputNS(operation);
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
                for (Iterator iterator = feedback.iterator(); iterator.hasNext();) {
                    RelativeURINamespaceProblemFeedback fb = (RelativeURINamespaceProblemFeedback)iterator.next();
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
        if (service != null) {
            if (!service.isSoap()) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                  "This assertion only works with soap services.", null));
            }
        }
    }

    private void processComposite(CompositeAssertion a) {
        List children = a.getChildren();
        for (Iterator iterator = children.iterator(); iterator.hasNext();) {
            Assertion ass = (Assertion)iterator.next();
            if (!(ass instanceof CommentAssertion)) {
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
              "This assertion is unrecognized and may cause all requests to fail.", null));
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

    private boolean isComposite(Assertion a) {
        return a instanceof CompositeAssertion;
    }

    private boolean normallyBeforeRouting(Assertion a) {
        return a instanceof SslAssertion && !a.isCredentialSource();
    }

    private boolean involvesSignature(Assertion a) {
        return a instanceof SecureConversation ||
               a instanceof RequestWssIntegrity ||
               a instanceof RequestWssX509Cert ||
               a instanceof ResponseWssIntegrity ||
              (a instanceof RequestWssTimestamp && ((RequestWssTimestamp)a).isSignatureRequired());
    }

    private boolean onlyForSoap(Assertion a) {
        return a instanceof SecureConversation || a instanceof WssBasic ||
               a instanceof RequestWssConfidentiality || a instanceof RequestWssIntegrity ||
               a instanceof RequestWssReplayProtection || a instanceof RequestWssX509Cert ||
               a instanceof ResponseWssConfidentiality || a instanceof ResponseWssIntegrity ||
               a instanceof RequestWssSaml || a instanceof SwAAssertion ||
               a instanceof RequestWssTimestamp || a instanceof ResponseWssTimestamp || a instanceof WssTimestamp ||
               a instanceof RequestWssKerberos || a instanceof WsiBspAssertion;
    }

    private boolean hasPreconditionAssertion(Assertion a) {
        // check preconditions for both SslAssertion and  ResponseWssIntegrity assertions - see processPrecondition()
        return a instanceof XpathBasedAssertion ||
               a instanceof XslTransformation ||
               a instanceof RequestSwAAssertion ||
               a instanceof RequestWssReplayProtection ||
               a instanceof UsesVariables ||
               a instanceof WsTrustCredentialExchange ||
               a instanceof WsFederationPassiveTokenExchange ||
               a instanceof WsFederationPassiveTokenRequest ||
              (a instanceof RequestWssTimestamp && ((RequestWssTimestamp)a).isSignatureRequired()) ||
               a instanceof WssBasic;
    }

    private boolean seenCredentials(Assertion context) {
        return seenCredentials(assertionToActor(context));
    }

    public boolean seenCredentials(String actor) {
        Boolean currentvalue = (Boolean)seenCredentials.get(actor);
        return currentvalue != null && currentvalue.booleanValue();
    }

    private void setSeenCredentials(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenCredentials.put(actor, Boolean.valueOf(value));
    }

    private boolean seenCredentialsSinceModified(Assertion context) {
        String actor = assertionToActor(context);
        return seenCredentialsSinceModified(actor);
    }

    private boolean seenCredentialsSinceModified(String actor) {
        Boolean currentvalue = (Boolean)seenCredentialsSinceModified.get(actor);
        return currentvalue != null && currentvalue.booleanValue();
    }

    private void setSeenCredentialsSinceModified(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenCredentialsSinceModified.put(actor, (value) ? Boolean.TRUE : Boolean.FALSE);
    }

    private boolean seenWssSignature(Assertion context) {
        String actor = assertionToActor(context);
        Boolean currentvalue = (Boolean)seenWssSignature.get(actor);
        return currentvalue != null && currentvalue.booleanValue();
    }

    private void setSeenWssSignature(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenWssSignature.put(actor, Boolean.valueOf(value));
    }

    private boolean seenSamlSecurity(Assertion context) {
        String actor = assertionToActor(context);
        Boolean currentvalue = (Boolean)seenSamlSecurity.get(actor);
        return currentvalue != null && currentvalue.booleanValue();
    }

    private boolean seenVariable(String var) {
        Boolean cur = (Boolean)seenVariables.get(var);
        return cur != null && cur.booleanValue();
    }

    private void setSeenVariable(String var) {
        seenVariables.put(var, Boolean.TRUE);
    }

    private void setSeenSamlStatement(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        seenSamlSecurity.put(actor, Boolean.valueOf(value));
    }

    private boolean haveSeen(Class assertionClass) {
        return seenAssertionClasses.contains(assertionClass);
    }

    private boolean haveSeenInstanceOf(Class assertionClass) {
        boolean seen = false;
        for (Iterator iterator = seenAssertionClasses.iterator(); iterator.hasNext();) {
            Class currentAssertionClass = (Class) iterator.next();
            if(assertionClass.isAssignableFrom(currentAssertionClass)) {
                seen = true;
                break;
            }
        }
        return seen;
    }

    private void setSeen(Class assertionClass) {
        seenAssertionClasses.add(assertionClass);
    }

    private boolean seenUsernamePasswordCredentials() {
        return haveSeenInstanceOf(ASSERTION_HTTPCREDENTIALS)
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
