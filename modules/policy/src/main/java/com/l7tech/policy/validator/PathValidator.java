package com.l7tech.policy.validator;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyType;
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
import com.l7tech.policy.assertion.identity.AuthenticationAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xml.XslTransformation;
import com.l7tech.policy.assertion.xmlsec.*;
import com.l7tech.policy.validator.DefaultPolicyValidator.DeferredValidate;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * validate single path, and collect the validation results in the
 * <code>PolicyValidatorResult</code>.
 * <p/>
 * TODO: refactor this into assertion specific validators.
 *
 * TODO: change assertion validators so that they are instantiated once per assertion instance, at the start of
 * validation, instead of once per assertion instance per path.  Validators would do assertion-specific validation
 * (things like ensuring that the xpath expression is valid -- internal to assertion and doesn't vary between paths)
 * just once per assertion instance, right after being created, instead of once per assertion instance per path.
 * PathValidator would be passed a map of assertion instances to validator instances.
 */
public class PathValidator {
    private static final Logger logger = Logger.getLogger(PathValidator.class.getName());
    private static final ResourceBundle bundle = ResourceBundle.getBundle( PathValidator.class.getName() );

    private static final Class<? extends Assertion> ASSERTION_HTTPBASIC = HttpBasic.class;
    private static final Class<? extends Assertion> ASSERTION_SECURECONVERSATION = SecureConversation.class;
    private static final Class<? extends Assertion> ASSERTION_XPATHCREDENTIALS = XpathCredentialSource.class;
    private static final Class<? extends Assertion> ASSERTION_SAMLASSERTION = RequireWssSaml.class;
    private static final Class<? extends Assertion> ASSERTION_WSSUSERNAMETOKENBASIC = WssBasic.class;
    private static final Class<? extends Assertion> ASSERTION_ENCRYPTEDUSERNAMETOKEN = EncryptedUsernameTokenAssertion.class;
    private static final Class<? extends Assertion> ASSERTION_KERBEROSTICKET = RequestWssKerberos.class;
    private static final String ASSERTION_PERMISSION_DENIED = "assertion.permission.denied";

    private static Map<String, Object> policyParseCache = Collections.synchronizedMap(new WeakHashMap<String, Object>());

    static final String REQUEST_TARGET_NAME = "Request"; // note, this is case insensitive
    static final String RESPONSE_TARGET_NAME = "Response";

    /**
     * result accumulator
     */
    private final List<DeferredValidate> deferredValidators = new ArrayList<>();
    private final PolicyValidationContext pvc;
    private final AssertionLicense assertionLicense;
    private final Map<String, MessageTargetContext> messageTargetContexts = new HashMap<>();
    private final Wsdl wsdl;
    private final boolean soap;

    private Collection<BindingOperation> wsdlBindingOperations;
    private PolicyValidatorResult result;
    private AssertionPath assertionPath;

    private boolean seenCustomAuth = false;
    boolean seenResponse = false;
    boolean seenParsing = false;
    boolean seenRouting = false;
    boolean seenResponseSecurityToken = false;

    private final static class MessageTargetContext {
        private Set<Class<? extends Assertion>> seenAssertionClasses = new HashSet<>();
        private Map<String, Boolean> seenCredentials = new HashMap<>();  // actor to flag
        private Map<String, Boolean> seenCredentialsSinceModified = new HashMap<>(); // actor to flag
        private Map<String, Boolean> seenWssSignature = new HashMap<>(); // actor to flag
        private Map<String, Boolean> seenSamlSecurity = new HashMap<>(); // actor to flag
        private boolean seenSpecificUserAssertion = false;
        private boolean seenAuthenticationAssertion = false;
        private boolean allowsMultipleSignatures = false;
        private boolean seenAccessControl = false;
    }

    PathValidator(AssertionPath ap, PolicyValidationContext pvc, AssertionLicense assertionLicense, PolicyValidatorResult r) {
        if (assertionLicense == null) throw new NullPointerException();
        result = r;
        assertionPath = ap;
        this.pvc = pvc;
        this.wsdl = pvc.getWsdl();
        this.soap = pvc.isSoap();
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
        final AssertionValidator av = pvc.getValidator(a);
        av.validate(assertionPath, pvc, result);

        // Check licensing
        if (assertionLicense != null) {
            if (a instanceof CustomAssertionHolder) {
                // Override custom feature set name of the individual custom assertion serialized in policy, with the one from
                // the registrar prototype.  This enables feature control from the module (e.g. change feature set name in the jar).
                CustomAssertionHolder customAssertionHolder = (CustomAssertionHolder) a;
                try {
                    customAssertionHolder.setRegisteredCustomFeatureSetName(pvc.getRegisteredCustomAssertionFeatureSets().get(customAssertionHolder.getCustomAssertion().getClass().getName()));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Unable to get custom feature set name from registrar: " + e.getMessage(), ExceptionUtils.getDebugException(e));
                }
            }

            if (!assertionLicense.isAssertionEnabled(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(
                        a, bundle.getString("assertion.unknown"), null));
            }
        }

        if (!(a instanceof UnknownAssertion) && pvc.getPermittedAssertionClasses() != null &&
                !pvc.getPermittedAssertionClasses().contains(a.getClass().getName())) {
            result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString(ASSERTION_PERMISSION_DENIED), null));
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
            result.addWarning(new PolicyValidatorResult.Warning(a,
                    bundle.getString("assertion.routing.normallybefore"), null));
        }

        if (a instanceof AuditAssertion) {
            AuditAssertion auditAssertion = (AuditAssertion) a;
            if (auditAssertion.isSaveRequest() || auditAssertion.isSaveResponse()) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
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
        } else if (a instanceof AddWssSecurityToken) {
            processAddWssSecurityToken((AddWssSecurityToken)a);
        } else if (a instanceof AddWssUsernameToken) {
            if ( ((AddWssUsernameToken)a).isEncrypt() ) setSeenResponseSecurityToken();
        }

        if (a instanceof XslTransformation) {
            processXslTransformation((XslTransformation)a);
        }

        if (parsesXML(a)) {
            seenParsing = true;
        }

        if (a instanceof TrueAssertion) {
            if (a.getParent() != null && a.getParent() instanceof AllAssertion) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                        bundle.getString("allassertion.notusefullchild"), null));
            }
        }

        if (isRoutingMetadata(a))
            seenResponse = true;

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
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
                    bundle.getString("wssecurity.1_1.insufficientassertions"), null));
        } else if (!foundPreRoutingWssAssertion) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
                    bundle.getString("wssecurity.nosecuritybeforeroute"), null));
        } else if (!foundPostRoutingWssAssertion) {
            result.addWarning(new PolicyValidatorResult.Warning(assertion,
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
                result.addWarning(new PolicyValidatorResult.Warning(xslt,
                        bundle.getString("assertion.stylesheetprocessinginstruction"), null));
            }
        }
    }

    private void validateRegex(Regex a) {
        // check encoding is supported (should that not be checked by the dialog?)
        if (a.getEncoding() != null &&
            a.getEncoding().length() > 0 &&
            !charsetIsSupported(a.getEncoding())) {
            result.addWarning(new PolicyValidatorResult.Warning(a,
                    MessageFormat.format(bundle.getString("unsupported.encoding"), a.getEncoding()), null));
        }
    }

    private static boolean charsetIsSupported(String charsetName) {
        try {
            return Charset.isSupported(charsetName);
        } catch (IllegalCharsetNameException e) {
            return false;
        }
    }

    private void processHtmlFormDataAssertion(HtmlFormDataAssertion htmlFormDataAssertion) {
        if (htmlFormDataAssertion != null && seenRouting && Assertion.isRequest(htmlFormDataAssertion)) {
            result.addWarning(new PolicyValidatorResult.Warning(htmlFormDataAssertion,
                    bundle.getString("assertion.routing.shouldbebefore"),
                                                                null));
        }
    }

    private void processAddWssSecurityToken( final AddWssSecurityToken addSecurityToken ) {
        if ( addSecurityToken.getTokenType() == SecurityTokenType.WSSC_CONTEXT ||
             addSecurityToken.getTokenType() == SecurityTokenType.WSS_ENCRYPTEDKEY ||
             (addSecurityToken.getTokenType() == SecurityTokenType.WSS_USERNAME && addSecurityToken.isEncrypt())) {
            setSeenResponseSecurityToken();
        }
    }

    /**
     * Checks if the assertion is declared as a routing assertion in its metadata.
     *
     * @param assertion the assertion to be checked
     * @return true if the assertion is enabled and its metadata declares that it is a routing assertion,
     *         false otherwise
     */
    private boolean isRoutingMetadata(Assertion assertion) {
        return assertion != null && assertion.isEnabled() && Boolean.TRUE.equals(assertion.meta().get(AssertionMetadata.IS_ROUTING_ASSERTION));
    }

    List<DeferredValidate> getDeferredValidators() {
        return deferredValidators;
    }

    private void processCustom(Assertion a) {
        final String targetName = AssertionUtils.getTargetName(a);
        final CustomAssertionHolder csh = (CustomAssertionHolder)a;
        if (csh.isCustomCredentialSource()) {
            if (!seenCredentials(a)) {
                result.addError(new PolicyValidatorResult.Error(a, bundle.getString("accesscontrol.noauthscheme"), null));
            }
            if (seenCustomAuth) {
                result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("accesscontrol.alreadyprovided"), null));
            } else if (getMessageTargetContext(targetName).seenAccessControl) {
                result.addError(new PolicyValidatorResult.Error(a, bundle.getString("accesscontrol.userorgroupnotallowed"), null));
            }

            if (seenRouting) {
                result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("assertion.routing.isafter"), null));
            }
            getMessageTargetContext(targetName).seenAccessControl = true;
            seenCustomAuth = true;
        }
    }


    private void processAccessControl( final IdentityAssertion a ) {
        final String targetName = AssertionUtils.getTargetName(a);

        if (!seenCredentials(a)) {
            result.addError(new PolicyValidatorResult.Error(a, bundle.getString("accesscontrol.noauthscheme"), null));
        }

        if ( seenRouting && Assertion.isRequest(a) ) {
            result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("assertion.routing.isafter"), null));
        }

        if (getMessageTargetContext(targetName).seenSpecificUserAssertion && isSpecificUser(a) && !getMessageTargetContext(targetName).allowsMultipleSignatures) {
            result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("assertion.uncommon.multipleidentities"), null));
        } else if (getMessageTargetContext(targetName).seenAuthenticationAssertion && isAuthenticationAssertion(a) && !getMessageTargetContext(targetName).allowsMultipleSignatures) {
            result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("assertion.uncommon.multipleauthassertions"), null));
        }

        if (seenCustomAuth) {
            result.addError(new PolicyValidatorResult.Error(a, bundle.getString("accesscontrol.userorgroupnotallowed"), null));
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
            result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("assertion.routing.mustbebefore"), null));
        }

        if (getMessageTargetContext(targetName).seenAccessControl && isDefaultActor(a) || seenCredentialsSinceModified(a)) {
            result.addWarning(new PolicyValidatorResult.
              Warning(a, bundle.getString("accesscontrol.uncommonuse"), null));
        }

        setSeenCredentialsSinceModified(a, false);
    }
                         
    private void processCredentialSource(Assertion a) {
        final String targetName = AssertionUtils.getTargetName(a);

        if (seenRouting && isDefaultActor(a) && Assertion.isRequest(a)) {
            result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("assertion.routing.mustbebefore"), null));
        }

        if (a instanceof RequireWssX509Cert) {
            if ( seenWssSignature(a, targetName) && !getMessageTargetContext(targetName).allowsMultipleSignatures ) {
                result.addWarning(new PolicyValidatorResult.
                  Warning(a, MessageFormat.format(bundle.getString("wssecurity.wsssignature.alreadyrequired"), targetName), null));
            }
            setSeenWssSignature(a, targetName);
            if (((RequireWssX509Cert)a).isAllowMultipleSignatures()) {
                getMessageTargetContext(targetName).allowsMultipleSignatures = true;
            } else if ( getMessageTargetContext(targetName).allowsMultipleSignatures ) {
                result.addWarning(new PolicyValidatorResult.
                  Warning(a, bundle.getString("wssecurity.multiplesignatures.conflicting"), null));
            }
        }

        // Dupe checks
        if (a instanceof SecureConversation) {
            if (haveSeen(AssertionUtils.getTargetName(a), ASSERTION_SECURECONVERSATION)) {
                result.addError(new PolicyValidatorResult.
                  Error(a, bundle.getString("wssecurity.secureconversation.alreadyspecified"), null));
            }
        }

        if (a instanceof RequireWssSaml) {
            if (haveSeen(AssertionUtils.getTargetName(a), ASSERTION_SAMLASSERTION)) {
                result.addError(new PolicyValidatorResult.
                  Error(a, bundle.getString("saml.alreadyspecified"), null));
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
                result.addWarning(new PolicyValidatorResult.Warning(a,
                        bundle.getString("assertion.routing.shouldbebefore"), null));
            } else if (a instanceof MessageTargetable) { // This warning is only for MessageTargetable assertions
                if ( Assertion.isRequest(a) && seenResponse && !hasFlag(a, ValidatorFlag.MAY_TARGET_REQUEST_AFTER_RESPONSE) ) {
                    result.addWarning(new PolicyValidatorResult.Warning(a,
                            bundle.getString("assertion.uncommon.requestusage"), null));
                }
            }
        }

        if (!seenResponse && Assertion.isResponse(a) && ! isRoutingMetadata(a)) {
            // If the assertion is ResponseXpathAssertion and uses a context variable as XML message source, ignore the below error.
            if (!(a instanceof ResponseXpathAssertion) || ((ResponseXpathAssertion)a).getXmlMsgSrc() == null) {
                result.addError(new PolicyValidatorResult.Error(a,
                        bundle.getString("assertion.response.usebeforeavailable"), null));
            }
        }

        if (a instanceof SecurityHeaderAddressable) {
            if (!SecurityHeaderAddressableSupport.isLocalRecipient(a) &&
                (hasFlag(a, ValidatorFlag.PERFORMS_VALIDATION) || a.isCredentialSource()) && 
                !hasFlag(a, ValidatorFlag.PROCESSES_NON_LOCAL_WSS_RECIPIENT)) {
                final String msg = bundle.getString("wssecurity.wssrecipient.notenforced");
                result.addWarning(new PolicyValidatorResult.Warning(a, msg, null));
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
                 !haveSeen("Request", ASSERTION_SECURECONVERSATION) &&
                 !haveSeen(targetName, ASSERTION_SECURECONVERSATION) &&
                 !seenSamlSecurity(a) &&
                 !haveSeen(targetName, ASSERTION_ENCRYPTEDUSERNAMETOKEN) &&
                 !haveSeen(targetName, ASSERTION_KERBEROSTICKET) &&
                 !(Assertion.isRequest(a) || haveSeenResponseSecurityToken()))
            {
                final String actor = assertionToActor(a);
                final String msg;
                if (actor.equals(XmlSecurityRecipientContext.LOCALRECIPIENT_ACTOR_VALUE)) {
                    msg = bundle.getString("wssecurity.missingpriorsecurityassertion");
                } else {
                    msg = MessageFormat.format(bundle.getString("wssecurity.missingpriorsecurityassertion.actor"), actor);
                }
                result.addWarning(new PolicyValidatorResult.Warning(a, msg, null));
            }
        }

        if (a instanceof RequestSwAAssertion && seenRouting && Assertion.isRequest(a)) {
            result.addError(new PolicyValidatorResult.Error(a,
                    bundle.getString("assertion.routing.mustbebefore"), null));
        }

        if (a instanceof ResponseXpathAssertion) {
            if (((ResponseXpathAssertion)a).getXmlMsgSrc()==null && !seenResponse && Assertion.isResponse(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                        bundle.getString("assertion.response.usebeforeavailable"), null));
            }
        } else if(a instanceof WsTrustCredentialExchange) {
            if(!seenUsernamePasswordCredentials(targetName)
            && !seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                        bundle.getString("wssecurity.missingpriorcredentialassertion"), null));
            }
        } else if(a instanceof WsFederationPassiveTokenRequest) {
            if(!seenUsernamePasswordCredentials(targetName)) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                        bundle.getString("wssecurity.missingpriorcredentialassertion.short"), null));
            }
        } else if(a instanceof WsFederationPassiveTokenExchange) {
            if(!seenSamlSecurity(a)) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                        bundle.getString("saml.missingbefore"), null));
            }
        } else if (a instanceof WssBasic) {
            // bugzilla 2518
            if (!(a instanceof EncryptedUsernameTokenAssertion) && Assertion.isRequest( a )) {
                if (!haveSeen(targetName, SslAssertion.class)) {
                    result.addWarning(new PolicyValidatorResult.Warning(a,
                            bundle.getString("transport.missingbefore"), null));
                }
            }
        } else if (a instanceof AddWssSecurityToken) {
            // bugzilla 2753, 2421
            final AddWssSecurityToken awst = (AddWssSecurityToken) a;
            if (awst.isIncludePassword() && awst.isUseLastGatheredCredentials() && !seenUsernamePasswordCredentials(REQUEST_TARGET_NAME)) { // NOTE: this assertion always gets request creds
                result.addWarning(new PolicyValidatorResult.Warning(a,
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
        if (a instanceof HttpRoutingAssertion) {
            processHttpRouting((HttpRoutingAssertion)a);
        } else if (a instanceof JmsRoutingAssertion) {
            processJmsRouting((JmsRoutingAssertion)a);
        }
        // todo, refactor RoutingAssertion interface so it doesn't need to be implemented
        // by assertions that dont really route like echo and template
        if (a != null) {
            if (!(a instanceof RoutingAssertionDoesNotRoute)) {
                seenRouting = true;
            }
            seenResponse = a.initializesResponse();
        }

        if (PolicyType.INTERNAL.equals(pvc.getPolicyType()) && ("debug-trace".equals(pvc.getPolicyInternalTag()) || "audit-sink".equals(pvc.getPolicyInternalTag()))) {
            result.addWarning(new PolicyValidatorResult.Warning(a, bundle.getString("routing.metapolicy.loop"), null));
        }
    }

    private void processJmsRouting(JmsRoutingAssertion a) {
        Goid oid = a.getEndpointOid();
        if (oid == null) {
            result.addWarning(new PolicyValidatorResult.Warning(a,
                    bundle.getString("jms.noqueuedefined"), null));
        }

        if (a.isAttachSamlSenderVouches() && !getMessageTargetContext(REQUEST_TARGET_NAME).seenAccessControl) {
            result.addWarning(new PolicyValidatorResult.Warning(a,
                    bundle.getString("saml.sendervouces.notauthenticated"), null));
        }
    }

    private void processHttpRouting(HttpRoutingAssertion a) {
        // If  the option "Use multiple URLs" is chosen, then we don't care what the main URL.is.
        if (a.getCustomURLs() == null) {
            String url = a.getProtectedServiceUrl();
            if (url == null) {
                result.addWarning(new PolicyValidatorResult.Warning(a,
                        bundle.getString("routing.emptyurl"), null));
            } else {
                // only do this if the url has no context variables
                if (!url.contains("${")) {
                    try {
                        new URL(url);
                    } catch (MalformedURLException e) {
                        result.addWarning(new PolicyValidatorResult.Warning(a,
                                bundle.getString("routing.malformedurl"), null));
                    }
                }
            }
        }

        if (a.isAttachSamlSenderVouches() && !getMessageTargetContext(REQUEST_TARGET_NAME).seenAccessControl) {
            result.addWarning(new PolicyValidatorResult.Warning(a,
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
            Collection<RelativeURINamespaceProblemFeedback> feedback = new ArrayList<>();
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
                StringBuilder msg = new StringBuilder(bundle.getString("wssecurity.dsig.relativenamespaceuri"));
                for (Object aFeedback : feedback) {
                    RelativeURINamespaceProblemFeedback fb = (RelativeURINamespaceProblemFeedback)aFeedback;
                    msg.append("<br>Namespace: ").append(fb.ns);
                    msg.append(", Operation Name: ").append(fb.operationName);
                    msg.append(", Message Name: ").append(fb.msgname);
                }
                result.addError(new PolicyValidatorResult.Error(a,
                        msg.toString(),
                  null));
            }
        }
    }

    private void processSoapSpecific(Assertion a) {
        if (!soap) {
            result.addWarning(new PolicyValidatorResult.Warning(a,
                    bundle.getString("assertion.requiressoap"), null));
        }
    }

    private void processComposite( final CompositeAssertion a ) {
        if ( !a.permitsEmpty() ) {
            final List<Assertion> children = a.getChildren();
            for ( final Assertion kid : children ) {
                // If a composite assertion just contains comment assertions and/or disabled assertions, then treat it as empty.
                if (!(kid instanceof CommentAssertion) && kid.isEnabled()) {
                    return;
                }
            }
            result.addWarning(new PolicyValidatorResult.Warning(a,
                    bundle.getString("assertion.composite.nochildren"),
                              null));
        }
    }

    private boolean isCustom(Assertion a) {
        return a instanceof CustomAssertionHolder;
    }

    private void processUnknown(UnknownAssertion a) {
        result.addWarning(new PolicyValidatorResult.Warning(a,
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
        Boolean currentValue = getMessageTargetContext(targetName).seenCredentials.get(actor);
        return currentValue != null && currentValue;
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
        Boolean currentValue = getMessageTargetContext(targetName).seenCredentialsSinceModified.get(actor);
        return currentValue != null && currentValue;
    }

    private void setSeenCredentialsSinceModified(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        getMessageTargetContext(AssertionUtils.getTargetName(context)).seenCredentialsSinceModified.put(actor, value);
    }

    private boolean seenWssSignature(Assertion context, String targetName) {
        String actor = assertionToActor(context);
        Boolean currentValue = getMessageTargetContext(targetName).seenWssSignature.get(actor);
        return currentValue != null && currentValue;
    }

    private void setSeenWssSignature(Assertion context, String targetName) {
        String actor = assertionToActor(context);
        getMessageTargetContext(targetName).seenWssSignature.put(actor, true);
    }

    private boolean seenSamlSecurity(Assertion context) {
        String actor = assertionToActor(context);
        Boolean currentValue = getMessageTargetContext(AssertionUtils.getTargetName(context)).seenSamlSecurity.get(actor);
        return currentValue != null && currentValue;
    }

    private void setSeenSamlStatement(Assertion context, boolean value) {
        String actor = assertionToActor(context);
        getMessageTargetContext(AssertionUtils.getTargetName(context)).seenSamlSecurity.put(actor, value);
    }

    private boolean haveSeen(String targetName, Class<? extends Assertion> assertionClass) {
        return getMessageTargetContext(targetName).seenAssertionClasses.contains(assertionClass);
    }

    private void setSeenResponseSecurityToken() {
        seenResponseSecurityToken = true;
    }

    private boolean haveSeenResponseSecurityToken() {
        return seenResponseSecurityToken;
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
