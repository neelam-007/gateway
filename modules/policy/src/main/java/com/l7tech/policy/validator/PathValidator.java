package com.l7tech.policy.validator;

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
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpNegotiate;
import com.l7tech.policy.assertion.credential.wss.EncryptedUsernameTokenAssertion;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.validator.DefaultPolicyValidator.DeferredValidate;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.util.Functions;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.text.MessageFormat;

/**
 * validate single path, and collect the validation results in the
 * <code>PolicyValidatorResult</code>.
 * <p/>
 * TODO: refactor this into asserton specific validators.
 *
 * TODO: change assertion validators so that they are instantiated once per assertion instance, at the start of
 * validation, instead of once per assertion instance per path.  Validators would do assertion-specific validation
 * (things like ensuring that the xpath expression is valid -- internal to assertion and doesn't vary between paths)
 * just once per assertion instance, right after being created, instead of once per assertion instance per path.
 * PathValidator would be passed a map of assertion instances to validator instances.
 */
public class PathValidator {

    private static final ResourceBundle bundle = ResourceBundle.getBundle( PathValidator.class.getName() );

    private static final Class<? extends Assertion> ASSERTION_HTTPBASIC = HttpBasic.class;
    private static final Class<? extends Assertion> ASSERTION_SECURECONVERSATION = SecureConversation.class;
    private static final Class<? extends Assertion> ASSERTION_XPATHCREDENTIALS = XpathCredentialSource.class;
    private static final Class<? extends Assertion> ASSERTION_SAMLASSERTION = RequireWssSaml.class;
    private static final Class<? extends Assertion> ASSERTION_WSSUSERNAMETOKENBASIC = WssBasic.class;
    private static final Class<? extends Assertion> ASSERTION_ENCRYPTEDUSERNAMETOKEN = EncryptedUsernameTokenAssertion.class;
    private static final Class<? extends Assertion> ASSERTION_KERBEROSTICKET = RequestWssKerberos.class;

    private static Map<String, Object> policyParseCache = Collections.synchronizedMap(new WeakHashMap<String, Object>());

    static final String REQUEST_TARGET_NAME = "Request"; // note, this is case insensitive 
    static final String RESPONSE_TARGET_NAME = "Response";

    /**
     * result accumulator
     */
    private final List<DeferredValidate> deferredValidators = new ArrayList<DeferredValidate>();
    private final AssertionLicense assertionLicense;
    private final Map<String, MessageTargetContext> messageTargetContexts = new HashMap<String, MessageTargetContext>();
    private final Wsdl wsdl;
    private final boolean soap;

    private Collection<BindingOperation> wsdlBindingOperations;
    private PolicyValidatorResult result;
    private AssertionPath assertionPath;

    private boolean seenCustomAuth = false;
    boolean seenResponse = false;
    boolean seenParsing = false;
    boolean seenRouting = false;

    private final static class MessageTargetContext {
        private Set<Class<? extends Assertion>> seenAssertionClasses = new HashSet<Class<? extends Assertion>>();
        private Map<String, Boolean> seenCredentials = new HashMap<String, Boolean>();  // actor to flag
        private Map<String, Boolean> seenCredentialsSinceModified = new HashMap<String, Boolean>(); // actor to flag
        private Map<String, Boolean> seenWssSignature = new HashMap<String, Boolean>(); // actor to flag
        private Map<String, Boolean> seenSamlSecurity = new HashMap<String, Boolean>(); // actor to flag
        private boolean seenSpecificUserAssertion = false;
        private boolean seenAuthenticationAssertion = false;
        private boolean allowsMultipleSignatures = false;
        private boolean seenAccessControl = false;
    }

    PathValidator(AssertionPath ap, PolicyValidatorResult r, Wsdl wsdl, boolean soap, AssertionLicense assertionLicense) {
        if (assertionLicense == null) throw new NullPointerException();
        result = r;
        assertionPath = ap;
        this.wsdl = wsdl;
        this.soap = soap;
        this.assertionLicense = assertionLicense;
    }

    /**
     * Validate the specific assertion.
     * Precondition: the assertion "a" must have been pre-checked to be enabled.
     * @see {@link com.l7tech.policy.validator.DefaultPolicyValidator#validatePath} for the prechecking.
     * @param a: the assertion to be validated.
     * @throws InterruptedException
     */
    public void validate( final Assertion a ) throws InterruptedException {
        if (Thread.interrupted())
            throw new InterruptedException();

        final String targetName = AssertionUtils.getTargetName(a);
        final AssertionValidator av = ValidatorFactory.getValidator(a);
        av.validate(assertionPath, wsdl, soap, result);

        // Check licensing
        if (assertionLicense != null) {
            if (!assertionLicense.isAssertionEnabled(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(
                        a, assertionPath, bundle.getString("assertion.unknown"), null));
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
                  bundle.getString("assertion.routing.normallybefore"), null));
        }

        if (a instanceof AuditAssertion) {
            AuditAssertion auditAssertion = (AuditAssertion) a;
            if (auditAssertion.isSaveRequest() || auditAssertion.isSaveResponse()) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  bundle.getString("auditing.excessivediskspace"), null));
            }
        }

        if (a instanceof WssVersionAssertion) {
            processWssVersionAssertion((WssVersionAssertion) a);
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
        } else if (a instanceof SamlBrowserArtifact) {
            getMessageTargetContext(targetName).seenAccessControl = true;
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
                  bundle.getString("allassertion.notusefullchild"), null));
            }
        }

        //process other assertions, ie modular assertions
        processOtherRemainingAssertion(a);

        setSeen(targetName, a.getClass());
    }

    private void processWssVersionAssertion(WssVersionAssertion assertion) {
        boolean foundPreRoutingWssAssertion = false;
        boolean foundPostRoutingWssAssertion = false;

        int routingAssertionIndex = getRoutingAssertionIndex();
        if (routingAssertionIndex == -1) return;

        int pathLength = assertionPath.getPathCount();
        for (int i = 0; i < pathLength && (!foundPreRoutingWssAssertion || !foundPostRoutingWssAssertion); i++) {
            //get the assertion at this index
            Assertion a = assertionPath.getPathAssertion(i);
            if (!foundPreRoutingWssAssertion) {
                //check if this assertion satisfies the pre-routing WSS requirement
                if (isPreRoutingRequestWssSigningOrEncryptionAssertion(a) && i < routingAssertionIndex) {
                    foundPreRoutingWssAssertion = true;
                }
            }
            if (!foundPostRoutingWssAssertion) {
                //check if this assertion satisfies the post-routing WSS requirement
                if (isPostRoutingResponseWssSigningOrEncryptionAssertion(a) && i > routingAssertionIndex) {
                    foundPostRoutingWssAssertion = true;
                }
            }
        }

        if (!foundPreRoutingWssAssertion && !foundPostRoutingWssAssertion) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, assertionPath,
                    bundle.getString("wssecurity.1_1.insufficientassertions"), null));
        } else if (!foundPreRoutingWssAssertion) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, assertionPath,
                    bundle.getString("wssecurity.nosecuritybeforeroute"), null));
        } else if (!foundPostRoutingWssAssertion) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion, assertionPath,
                    bundle.getString("wssecurity.nosecurityafterroute"), null));
        }
    }

    private int getRoutingAssertionIndex() {
        Assertion[] pathAssertions = assertionPath.getPath();
        for (int i = 0; i < pathAssertions.length; i++) {
            Assertion a = pathAssertions[i];
            if (a instanceof RoutingAssertion) {
                return i;
            }
        }
        return -1;
    }

    private boolean isPreRoutingRequestWssSigningOrEncryptionAssertion(Assertion a) {
        //TODO WssKerberos & WssSaml requests won't necessarily be signed

        return Assertion.isRequest(a) &&
                ((a instanceof RequireWssX509Cert) ||
                 (a instanceof RequireWssSignedElement) ||
                 (a instanceof SecureConversation) ||
                 (a instanceof EncryptedUsernameTokenAssertion) ||
                 (a instanceof RequireWssEncryptedElement) ||
                 (a instanceof RequestWssKerberos) ||
                 (a instanceof RequireWssSaml) ||
                 ((a instanceof RequireWssTimestamp) && ((RequireWssTimestamp) a).isSignatureRequired()));
    }

    private boolean isPostRoutingResponseWssSigningOrEncryptionAssertion(Assertion a) {
        return Assertion.isResponse(a) &&
                ((a instanceof WssSignElement) ||
                 (a instanceof WssEncryptElement) ||
                 (a instanceof AddWssSecurityToken) ||
                 ((a instanceof AddWssTimestamp) && ((AddWssTimestamp) a).isSignatureRequired()));
    }

    private void processXslTransformation(XslTransformation xslt) {
        if (xslt.getResourceInfo().getType() == AssertionResourceType.MESSAGE_URL) {
            if (soap) {
                result.addWarning(new PolicyValidatorResult.Warning(xslt, assertionPath,
                  bundle.getString("assertion.stylesheetprocessinginstruction"), null));
            }
        }
    }

    private void validateRegex(Regex a) {
        // check encoding is supported (should that not be checked by the dialog?)
        if (a.getEncoding() != null &&
            a.getEncoding().length() > 0 &&
            !Charset.isSupported(a.getEncoding())) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                    MessageFormat.format(bundle.getString("unsupported.encoding"), a.getEncoding()), null));
        }
    }

    private void processHtmlFormDataAssertion(HtmlFormDataAssertion htmlFormDataAssertion) {
        if (htmlFormDataAssertion != null && seenRouting && Assertion.isRequest(htmlFormDataAssertion)) {
            result.addWarning(new PolicyValidatorResult.Warning(htmlFormDataAssertion, assertionPath,
                                                                bundle.getString("assertion.routing.shouldbebefore"),
                                                                null));
        }
    }

    /**
     * This method will be able to process other assertions that cannot be differentiated by their instance by using
     * "instanceof" because of modular dependencies.  And because of that, one way is to use the meta data within
     * the assertion to assist in the processing.
     *
     * @param assertion The assertion that will be processed.
     */
    private void processOtherRemainingAssertion(Assertion assertion) {
        //check if assertion is considered to be enabled
        if (assertion != null && assertion.isEnabled() ){
            if (assertion.meta().get(AssertionMetadata.IS_ROUTING_ASSERTION) != null) {
                boolean isRoutingAssertion = (Boolean) assertion.meta().get(AssertionMetadata.IS_ROUTING_ASSERTION);
                if (isRoutingAssertion) {
                    seenResponse = true;
                }
            }
        }
    }

    List<DeferredValidate> getDeferredValidators() {
        return deferredValidators;
    }

    private void processCustom(Assertion a) {
        final String targetName = AssertionUtils.getTargetName(a);
        final CustomAssertionHolder csh = (CustomAssertionHolder)a;
        if (Category.ACCESS_CONTROL.equals(csh.getCategory())) {
            if (!seenCredentials(a)) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, bundle.getString("accesscontrol.noauthscheme"), null));
            }
            if (seenCustomAuth) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, bundle.getString("accesscontrol.alreadyprovided"), null));
            } else if (getMessageTargetContext(targetName).seenAccessControl) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath, bundle.getString("accesscontrol.userorgroupnotallowed"), null));
            }

            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, bundle.getString("assertion.routing.isafter"), null));
            }
            getMessageTargetContext(targetName).seenAccessControl = true;
            seenCustomAuth = true;
        }
    }


    private void processAccessControl( final IdentityAssertion a ) {
        final String targetName = AssertionUtils.getTargetName(a);

        if (!seenCredentials(a)) {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath, bundle.getString("accesscontrol.noauthscheme"), null));
        }

        if ( seenRouting && Assertion.isRequest(a) ) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, bundle.getString("assertion.routing.isafter"), null));
        }

        if (getMessageTargetContext(targetName).seenSpecificUserAssertion && isSpecificUser(a) && !getMessageTargetContext(targetName).allowsMultipleSignatures) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, bundle.getString("assertion.uncommon.multipleidentities"), null));
        } else if (getMessageTargetContext(targetName).seenAuthenticationAssertion && isAuthenticationAssertion(a) && !getMessageTargetContext(targetName).allowsMultipleSignatures) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, bundle.getString("assertion.uncommon.multipleauthassertions"), null));
        }

        if (seenCustomAuth) {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath, bundle.getString("accesscontrol.userorgroupnotallowed"), null));
        }

        getMessageTargetContext(targetName).seenAccessControl = true;
        if (isSpecificUser(a)) {
            getMessageTargetContext(targetName).seenSpecificUserAssertion = true;
        } else if (isAuthenticationAssertion(a)) {
            getMessageTargetContext(targetName).seenAuthenticationAssertion = true;
        }
    }

    private void processCredentialModifier( final Assertion a ) {
        final String targetName = AssertionUtils.getTargetName(a);

        if (seenRouting && isDefaultActor(a) && Assertion.isRequest(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, bundle.getString("assertion.routing.mustbebefore"), null));
        }

        if (getMessageTargetContext(targetName).seenAccessControl && isDefaultActor(a) || seenCredentialsSinceModified(a)) {
            result.addWarning(new PolicyValidatorResult.
              Warning(a, assertionPath, bundle.getString("accesscontrol.uncommonuse"), null));
        }

        setSeenCredentialsSinceModified(a, false);
    }
                         
    private void processCredentialSource(Assertion a) {
        final String targetName = AssertionUtils.getTargetName(a);

        if (seenRouting && isDefaultActor(a) && Assertion.isRequest(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, bundle.getString("assertion.routing.mustbebefore"), null));
        }

        if (a instanceof RequireWssX509Cert) {
            if ( seenWssSignature(a, targetName) && !getMessageTargetContext(targetName).allowsMultipleSignatures ) {
                result.addWarning(new PolicyValidatorResult.
                  Warning(a, assertionPath, MessageFormat.format(bundle.getString("wssecurity.wsssignature.alreadyrequired"), targetName), null));
            }
            setSeenWssSignature(a, targetName);
            if (((RequireWssX509Cert)a).isAllowMultipleSignatures()) {
                getMessageTargetContext(targetName).allowsMultipleSignatures = true;
            } else if ( getMessageTargetContext(targetName).allowsMultipleSignatures ) {
                result.addWarning(new PolicyValidatorResult.
                  Warning(a, assertionPath, bundle.getString("wssecurity.multiplesignatures.conflicting"), null));
            }
        }

        // Dupe checks
        if (a instanceof SecureConversation) {
            if (haveSeen(AssertionUtils.getTargetName(a), ASSERTION_SECURECONVERSATION)) {
                result.addError(new PolicyValidatorResult.
                  Error(a,assertionPath,bundle.getString("wssecurity.secureconversation.alreadyspecified"), null));
            }
        }

        if (a instanceof RequireWssSaml) {
            if (haveSeen(AssertionUtils.getTargetName(a), ASSERTION_SAMLASSERTION)) {
                result.addError(new PolicyValidatorResult.
                  Error(a,assertionPath,bundle.getString("saml.alreadyspecified"), null));
            }
        }

        // kerberos is both credentials and authorization since authorization is delegated to issuer
        if (a instanceof RequestWssKerberos || a instanceof HttpNegotiate) {
            getMessageTargetContext(targetName).seenAccessControl = true;
        }

        //
        if (a instanceof RequireWssSaml)
            setSeenSamlStatement(a, true);

        setSeenCredentials(a, true);
        setSeenCredentialsSinceModified(a, true);
    }

    private void processPrecondition(final Assertion a) {
        final  String targetName = AssertionUtils.getTargetName(a);

        // credential sources have separate validation rules
        if ( !(a.isCredentialSource() || a.isCredentialModifier()) ) { 
            if ( seenRouting && hasFlag(a, ValidatorFlag.PERFORMS_VALIDATION) &&
                 Assertion.isRequest(a) && isDefaultActor(a)) {
                // REASON FOR THIS RULE:
                // it makes no sense to validate something about the request after it's routed
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  bundle.getString("assertion.routing.shouldbebefore"), null));
            } else if (a instanceof MessageTargetable) { // This warning is only for MessageTargetable assertions
                if ( Assertion.isRequest(a) && seenResponse) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      bundle.getString("assertion.uncommon.requestusage"), null));
                }
            }
        }

        if (Assertion.isResponse(a)  && !seenResponse) {
            // If the asserion is ResponseXpathAssertion and uses a context variable as XML message source, ignore the below error.
            if (!(a instanceof ResponseXpathAssertion) || ((ResponseXpathAssertion)a).getXmlMsgSrc() == null) {
                result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                    bundle.getString("assertion.response.usebeforeavailable"), null));
            }
        }

        if (a instanceof SecurityHeaderAddressable) {
            if (!SecurityHeaderAddressableSupport.isLocalRecipient(a) &&
                (hasFlag(a, ValidatorFlag.PERFORMS_VALIDATION) || a.isCredentialSource()) && 
                !hasFlag(a, ValidatorFlag.PROCESSES_NON_LOCAL_WSS_RECIPIENT)) {
                final String msg = bundle.getString("wssecurity.wssrecipient.notenforced");
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, msg, null));
            }
        }

        if ( (a instanceof RequestSwAAssertion && ((RequestSwAAssertion)a).requiresSignature()) ||
             hasFlag(a, ValidatorFlag.REQUIRE_SIGNATURE) ) {
            // REASONS FOR THIS RULE
            //
            // For RequireWssSignedElement:
            // it makes no sense to validate that an element is signed if we dont validate
            // that the authorized user is the one who signed it.
            if ( !seenWssSignature(a, targetName) &&
                 !haveSeen(targetName, ASSERTION_SECURECONVERSATION) &&
                 !seenSamlSecurity(a) &&
                 !haveSeen(targetName, ASSERTION_ENCRYPTEDUSERNAMETOKEN) &&
                 !haveSeen(targetName, ASSERTION_KERBEROSTICKET))
            {
                final String actor = assertionToActor(a);
                final String msg;
                if (actor.equals(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE)) {
                    msg = bundle.getString("wssecurity.missingpriorsecurityassertion");
                } else {
                    msg = MessageFormat.format(bundle.getString("wssecurity.missingpriorsecurityassertion.actor"), actor);
                }
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath, msg, null));
            }
        }

        if (a instanceof RequestSwAAssertion && seenRouting && Assertion.isRequest(a)) {
            result.addError(new PolicyValidatorResult.Error(a, assertionPath,
              bundle.getString("assertion.routing.mustbebefore"), null));
        }

        if (a instanceof ResponseXpathAssertion) {
            if (((ResponseXpathAssertion)a).getXmlMsgSrc()==null && !seenResponse && Assertion.isResponse(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  bundle.getString("assertion.response.beforeresponse"), null));
            }
        } else if(a instanceof WsTrustCredentialExchange) {
            if(!seenUsernamePasswordCredentials(targetName)
            && !seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  bundle.getString("wssecurity.missingpriorcredentialassertion"), null));
            }
        } else if(a instanceof WsFederationPassiveTokenRequest) {
            if(!seenUsernamePasswordCredentials(targetName)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  bundle.getString("wssecurity.missingpriorcredentialassertion.short"), null));
            }
        } else if(a instanceof WsFederationPassiveTokenExchange) {
            if(!seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  bundle.getString("saml.missingbefore"), null));
            }
        } else if (a instanceof WssBasic) {
            // bugzilla 2518
            if (!(a instanceof EncryptedUsernameTokenAssertion) && Assertion.isRequest( a )) {
                if (!haveSeen(targetName, SslAssertion.class)) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      bundle.getString("transport.missingbefore"), null));
                }
            }
        } else if (a instanceof AddWssSecurityToken) {
            // bugzilla 2753, 2421
            if (((AddWssSecurityToken)a).isIncludePassword() && !seenUsernamePasswordCredentials(REQUEST_TARGET_NAME)) { // NOTE: this assertion always gets request creds
                result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                  bundle.getString("assertion.missingpasswordcollection"), null));
            }
        }
    }

    /**
     * Check if the assertions validation metadata contains the given flag 
     */
    private boolean hasFlag(final Assertion a, final ValidatorFlag flag) {
        boolean flagged = false;

        Functions.Unary<Set<ValidatorFlag>,Assertion> flagAccessor =
             a.meta().get(AssertionMetadata.POLICY_VALIDATOR_FLAGS_FACTORY);

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
            if (!(a instanceof RoutingAssertionDoesNotRoute)) {
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
                                                            bundle.getString("assertion.policyxml.invalid"),
                                                            (Throwable)cachedResult));
            return;
        }
        try {
            WspReader.getDefault().parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
            policyParseCache.put(policyXml, Boolean.TRUE);
        } catch (IOException e) {
            policyParseCache.put(policyXml, e);
            result.addError(new PolicyValidatorResult.Error(a, assertionPath,
                                                            bundle.getString("assertion.policyxml.invalid"),
                                                            e));
        }
    }

    private void processJmsRouting(JmsRoutingAssertion a) {
        Long oid = a.getEndpointOid();
        if (oid == null) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
              bundle.getString("jms.noqueuedefined"), null));
        }

        if (a.isAttachSamlSenderVouches() && !getMessageTargetContext(REQUEST_TARGET_NAME).seenAccessControl) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                    bundle.getString("saml.sendervouces.notauthenticated"), null));
        }
    }

    private void processHttpRouting(HttpRoutingAssertion a) {
        String url = a.getProtectedServiceUrl();
        if (url == null) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
              bundle.getString("routing.emptyurl"), null));
        } else {
            // only do this if the url has no context variables
            if (url.indexOf("${") < 0) {
                try {
                    new URL(url);
                } catch (MalformedURLException e) {
                    result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                      bundle.getString("routing.malformedurl"), null));
                }
            }
        }

        if (a.isAttachSamlSenderVouches() && !getMessageTargetContext(REQUEST_TARGET_NAME).seenAccessControl) {
            result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
                    bundle.getString("saml.sendervouces.notauthenticated"), null));
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
                StringBuffer msg = new StringBuffer(bundle.getString("wssecurity.dsig.relativenamespaceuri"));
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
              bundle.getString("assertion.requiressoap"), null));
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
                          bundle.getString("assertion.composite.nochildren"),
                          null));
    }

    private boolean isCustom(Assertion a) {
        return a instanceof CustomAssertionHolder;
    }

    private void processUnknown(UnknownAssertion a) {
        result.addWarning(new PolicyValidatorResult.Warning(a, assertionPath,
              bundle.getString("assertion.unrecognized"), null));
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
        return hasFlag(a, ValidatorFlag.REQUIRE_SIGNATURE) ||
                a instanceof SecureConversation ||
                a instanceof RequireWssX509Cert ||
                a instanceof WssSignElement;
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
        return seenCredentials(assertionToActor(context), AssertionUtils.getTargetName(context));
    }

    private MessageTargetContext getMessageTargetContext( final String targetName ) {
        MessageTargetContext context = messageTargetContexts.get( targetName.toLowerCase() );
        if ( context == null ) {
            context = new MessageTargetContext();
            messageTargetContexts.put( targetName.toLowerCase(), context );
        }
        return context;
    }

    boolean seenCredentials( final String actor, final String targetName ) {
        Boolean currentvalue = getMessageTargetContext(targetName).seenCredentials.get(actor);
        return currentvalue != null && currentvalue;
    }

    boolean seenAccessControl( final String targetName ) {
        return getMessageTargetContext(targetName).seenAccessControl;
    }

    public boolean seenAssertion(String targetName, Class<? extends Assertion> assertionClass) {
        return this.haveSeenInstanceOf(targetName, assertionClass);
    }

    private void setSeenCredentials(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        getMessageTargetContext(AssertionUtils.getTargetName(context)).seenCredentials.put(actor, value);
    }

    private boolean seenCredentialsSinceModified(Assertion context) {
        String actor = assertionToActor(context);
        return seenCredentialsSinceModified(actor, AssertionUtils.getTargetName(context));
    }

    private boolean seenCredentialsSinceModified(String actor, String targetName) {
        Boolean currentvalue = getMessageTargetContext(targetName).seenCredentialsSinceModified.get(actor);
        return currentvalue != null && currentvalue;
    }

    private void setSeenCredentialsSinceModified(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        getMessageTargetContext(AssertionUtils.getTargetName(context)).seenCredentialsSinceModified.put(actor, value);
    }

    private boolean seenWssSignature(Assertion context, String targetName) {
        String actor = assertionToActor(context);
        Boolean currentvalue = getMessageTargetContext(targetName).seenWssSignature.get(actor);
        return currentvalue != null && currentvalue;
    }

    private void setSeenWssSignature(Assertion context, String targetName) {
        String actor = assertionToActor(context);
        getMessageTargetContext(targetName).seenWssSignature.put(actor, true);
    }

    private boolean seenSamlSecurity(Assertion context) {
        String actor = assertionToActor(context);
        Boolean currentvalue = getMessageTargetContext(AssertionUtils.getTargetName(context)).seenSamlSecurity.get(actor);
        return currentvalue != null && currentvalue;
    }

    private void setSeenSamlStatement(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        getMessageTargetContext(AssertionUtils.getTargetName(context)).seenSamlSecurity.put(actor, value);
    }

    private boolean haveSeen(String targetName, Class<? extends Assertion> assertionClass) {
        return getMessageTargetContext(targetName).seenAssertionClasses.contains(assertionClass);
    }

    private boolean haveSeenInstanceOf(String targetName, Class<? extends Assertion> assertionClass) {
        boolean seen = false;
        for (Object seenAssertionClass : getMessageTargetContext(targetName).seenAssertionClasses) {
            Class currentAssertionClass = (Class)seenAssertionClass;
            if (assertionClass.isAssignableFrom(currentAssertionClass)) {
                seen = true;
                break;
            }
        }
        return seen;
    }

    private void setSeen(String targetName, Class<? extends Assertion> assertionClass) {
        getMessageTargetContext(targetName).seenAssertionClasses.add(assertionClass);
    }

    private boolean seenUsernamePasswordCredentials( String targetName ) {
        return haveSeenInstanceOf(targetName, ASSERTION_HTTPBASIC)
            || haveSeen(targetName, ASSERTION_XPATHCREDENTIALS)
            || haveSeen(targetName, ASSERTION_WSSUSERNAMETOKENBASIC);
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
