package com.l7tech.server.util;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.User;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.message.SecurityKnob;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.KeyInfoDetails;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.decorator.WssDecorator;
import com.l7tech.security.xml.processor.*;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidDocumentSignatureException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * WS-Security Processor utility methods.
 */
public class WSSecurityProcessorUtils {

    private static final Logger logger = Logger.getLogger( WSSecurityProcessorUtils.class.getName() );
    private static final AtomicReference<WssSettings> wssSettingsReference = new AtomicReference<WssSettings>();

    /**
     * Get the processor result for the given message, running the processor if necessary.
     *
     * @param msg The message whose security is being evaluated
     * @param messageDescriptionForLogging A description for the message being evaluated
     * @param securityTokenResolver The resolver to use to locate security tokens
     * @param audit The auditor to use for errors.
     * @return The processor result or null.
     */
    public static ProcessorResult getWssResults(final Message msg,
                                                final String messageDescriptionForLogging,
                                                final SecurityTokenResolver securityTokenResolver,
                                                final Audit audit)
    {
        return getWssResults( msg, messageDescriptionForLogging, securityTokenResolver, null, audit, null );
    }

    /**
     * Get the processor result for the given message, running the processor if necessary.
     *
     * @param msg The message whose security is being evaluated
     * @param messageDescriptionForLogging A description for the message being evaluated
     * @param securityTokenResolver The resolver to use to locate security tokens
     * @param securityContextFinder The finder to use to locate security contexts (null to use a contextual finder)
     * @param audit The auditor to use for errors (may be null)
     * @param errorCallback The callback for errors, return true if the error was handled so should not be audited (may be null)
     * @return The processor result or null.
     */
    public static ProcessorResult getWssResults(final Message msg,
                                                final String messageDescriptionForLogging,
                                                final SecurityTokenResolver securityTokenResolver,
                                                final SecurityContextFinder securityContextFinder,
                                                final Audit audit,
                                                final Functions.Unary<Boolean,Throwable> errorCallback )
    {
        final SecurityKnob sk = msg.getKnob(SecurityKnob.class);
        final ProcessorResult existingWssResults;
        if (sk != null && null != (existingWssResults = sk.getProcessorResult()))
            return existingWssResults;

        try {
            final boolean isSoap = msg.isSoap();
            final boolean hasSecurity = isSoap && msg.getSoapKnob().isSecurityHeaderPresent();

            if ( hasSecurity ) {
                final WssProcessorImpl impl = new WssProcessorImpl(msg);
                impl.setSecurityTokenResolver(impl.contextual(securityTokenResolver));
                impl.setSecurityContextFinder(securityContextFinder != null ? securityContextFinder : buildContextualFinder(msg));

                WssSettings settings = getWssSettings();
                impl.setSignedAttachmentSizeLimit(settings.signedAttachmentMaxSize);
                impl.setRejectOnMustUnderstand(settings.rejectOnMustUnderstand);
                impl.setPermitMultipleTimestampSignatures(settings.permitMultipleTimestampSignatures);
                impl.setPermitUnknownBinarySecurityTokens(settings.permitUnknownBinarySecurityTokens);
                impl.setStrictSignatureConfirmationValidation(settings.strictSignatureConfirmationValidation);
                impl.setErrorHandler(new WssProcessorErrorHandler() {
                    @Override
                    public void onDecryptionError(Throwable t) {
                        audit.logAndAudit(MessageProcessingMessages.ERROR_XML_DECRYPTION);
                    }
                });

                ProcessorResult wssResults = impl.processMessage();
                msg.getSecurityKnob().setProcessorResult(wssResults); // In case someone else needs it later
                return wssResults;
            }
        } catch (ProcessorValidationException e) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch (InvalidDocumentSignatureException e) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch (BadSecurityContextException e) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch ( IOException e ) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch ( GeneralSecurityException e ) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch ( InvalidDocumentFormatException e ) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch ( ProcessorException e ) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch ( SAXException e ) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch ( NoSuchPartException e ) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, true);
        } catch (Exception e) {
            handleWssProcessingError(audit, errorCallback, messageDescriptionForLogging, e, false);
        }

        return null;
    }

    private static void handleWssProcessingError( final Audit audit,
                                                  final Functions.Unary<Boolean, Throwable> errorCallback,
                                                  final String messageDescriptionForLogging,
                                                  final Throwable throwable,
                                                  final boolean debugThrowable ) {
        boolean handled = false;
        if ( errorCallback != null ) {
            handled = errorCallback.call( throwable );
        }

        if ( !handled && audit != null ) {
            audit.logAndAudit(
                    MessageProcessingMessages.MESSAGE_VAR_BAD_WSS,
                    new String[] { messageDescriptionForLogging, ExceptionUtils.getMessage(throwable) },
                    debugThrowable ? ExceptionUtils.getDebugException(throwable) : throwable );
        }
    }

    /**
     * Validate that the given elements are signed.
     *
     * <p>This method check that there is a single signature in the message and
     * that the required elements are signed.</p>
     *
     * @param authContext The authorization context to use.
     * @param wssResults The Processor Results to validate.
     * @param elementsToValidate The required signed elements
     * @param checkSigningTokenIsCredential True to check that the signing token is a credential if any identity is permitted
     * @param relatedRequestMessage a related request message whose EncryptedKey and WS-SecureConversation tokens whose secret keys are known to the Gateway are to be recognized as valid signing tokens
     *                              for the current target message (as long as a specific signing credentials have not been asked-for in the auth context), or null
     * @param audit auditor to use for recording information about rejected tokens (May be null)
     * @return True if valid
     */
    public static boolean isValidSingleSigner( final AuthenticationContext authContext,
                                               final ProcessorResult wssResults,
                                               final ParsedElement[] elementsToValidate,
                                               final boolean checkSigningTokenIsCredential,
                                               final Message relatedRequestMessage,
                                               final AuthenticationContext relatedAuthContext,
                                               final Audit audit ) {
        boolean valid;

        final SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
        final List<LoginCredentials> credentials = authContext == null ? null : authContext.getCredentials();

        // Validate that there is only a single signing identity
        final XmlSecurityToken token = getSingleSigningSecurityToken( authContext, wssResults, checkSigningTokenIsCredential, new Functions.Unary<Boolean,SigningSecurityToken>(){
            @Override
            public Boolean call( final SigningSecurityToken signingToken ) {
                // If the policy does not require specific signing credentials, permit response signature using an EncryptedKey or WS-SC session from a corresponding request (if any)
                return relatedRequestMessage != null && (credentials == null || credentials.isEmpty()) &&
                        isTokenRecognizedEncryptedKeyOrWsscSession( signingToken, relatedRequestMessage, relatedAuthContext, checkSigningTokenIsCredential, audit );
            }
        }, audit );
        valid = token != null;

        // Validate that the required elements are signed
        List<Element> signed = Functions.map(Arrays.asList(signedElements), new Functions.Unary<Element, SignedElement>() {
            @Override
            public Element call(SignedElement signedElement) {
                return signedElement.asElement();
            }
        });
        for ( ParsedElement element : elementsToValidate ) {
            if ( ! signed.contains(element.asElement()) ) {
                valid = false;
                break;
            }
        }

        return valid;
    }

    private static boolean isTokenRecognizedEncryptedKeyOrWsscSession( final SigningSecurityToken signingToken,
                                                                       final Message relatedRequestMessage,
                                                                       final AuthenticationContext relatedRequestAuthContext,
                                                                       final boolean checkSigningTokenIsCredential,
                                                                       final Audit audit ) {
        String signingEncryptedKeySha1 = null;
        if (signingToken instanceof EncryptedKey) {
            EncryptedKey ek = (EncryptedKey) signingToken;
            signingEncryptedKeySha1 = ek.getEncryptedKeySHA1();
        }

        String signingSecurityContextId = null;
        if (signingToken instanceof SecurityContextToken) {
            SecurityContextToken sct = (SecurityContextToken) signingToken;
            signingSecurityContextId = sct.getContextIdentifier();
        }

        if ( signingEncryptedKeySha1==null && signingSecurityContextId==null ) {
            return false;
        }

        // First check for decoration results, in case this was an outgoing request
        boolean hasResults = false;
        List<WssDecorator.DecorationResult> reqDecorationResults = relatedRequestMessage.getSecurityKnob().getAllDecorationResults();
        for (WssDecorator.DecorationResult decorationResult : reqDecorationResults) {
            hasResults = true;
            String eksha1 = decorationResult.getEncryptedKeySha1();
            if (eksha1 != null && eksha1.equals(signingEncryptedKeySha1))
                return true;

            String scid = decorationResult.getWsscSecurityContextId();
            if (scid != null && scid.equals(signingSecurityContextId))
                return true;
        }

        // Check for processor results, in case this was an incoming request
        if ( !hasResults ) {
            final XmlSecurityToken token =
                    getSingleSigningSecurityToken(
                            relatedRequestAuthContext, 
                            relatedRequestMessage.getSecurityKnob().getProcessorResult(),
                            checkSigningTokenIsCredential,
                            null,
                            audit );

            if (token instanceof EncryptedKey) {
                EncryptedKey ek = (EncryptedKey) token;
                if (ek.isPossessionProved() && ek.getEncryptedKeySHA1().equals(signingEncryptedKeySha1))
                    return true;
            }

            if (token instanceof SecurityContextToken) {
                SecurityContextToken sct = (SecurityContextToken) token;
                if (sct.isPossessionProved() && sct.getContextIdentifier() != null && sct.getContextIdentifier().equals(signingSecurityContextId))
                    return true;
            }
        }

        return false;
    }

    /**
     * Get the security token associated with a single signature in the given message.
     *
     * @param authenticationContext The authentication context for the message (May be null)
     * @param wssResults The processor results (May be null)
     * @param checkSigningTokenIsCredential True to enforce that the signing token is a (gathered) credential.
     * @param credentialTokenValidator Callback to validate the signing token if it is not recognised and a credential token is required (May be null)
     * @param audit auditor to use for recording info about rejected tokens (May be null)
     * @return The token, or null if there is no such token.
     */
    private static XmlSecurityToken getSingleSigningSecurityToken( final AuthenticationContext authenticationContext,
                                                                   final ProcessorResult wssResults,
                                                                   final boolean checkSigningTokenIsCredential,
                                                                   final Functions.Unary<Boolean,SigningSecurityToken> credentialTokenValidator,
                                                                   Audit audit)
    {
        if (audit == null)
            audit = new LoggingAudit(logger);

        XmlSecurityToken token = null;
        final List<LoginCredentials> credentials = authenticationContext == null ? null : authenticationContext.getCredentials();
        final SigningSecurityToken[] tokens = checkSigningTokenIsCredential ?
                getSigningSecurityTokens(credentials) :
                null;

        // Check for a single signature element, not token or certificate (bug 7157)
        Element signatureElement = null;
        if ( wssResults != null ) {
            final Set securityTokenElements = WSSecurityProcessorUtils.getSecurityTokenElements(wssResults);
            final SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
            for ( SignedElement signedElement : signedElements ) {
                if (!securityTokenElements.contains(signedElement.asElement())) {
                    if (signatureElement == null) {
                        final SigningSecurityToken signingSecurityToken = signedElement.getSigningSecurityToken();
                        //   tokens==null is a special value meaning do not check. An empty array does not have the same meaning.
                        if ( tokens==null || ArrayUtils.contains(tokens, signingSecurityToken) ) {
                            signatureElement = signedElement.getSignatureElement();
                            token = signingSecurityToken;
                        } else if ( credentialTokenValidator!=null && credentialTokenValidator.call( signingSecurityToken ) ){
                            signatureElement = signedElement.getSignatureElement();
                            token = signingSecurityToken;
                        } else {
                            audit.logAndAudit( tokens.length == 0 ?
                                    MessageProcessingMessages.WSS_NO_SIGNING_TOKEN :
                                    MessageProcessingMessages.WSS_WRONG_SIGNING_TOKEN );
                            token = null;
                            break;
                        }
                    } else {
                        if (signatureElement != signedElement.getSignatureElement()) {
                            audit.logAndAudit(MessageProcessingMessages.WSS_DIFFERENT_SIGNATURE);
                            token = null;
                            break;
                        }
                    }
                }
            }
        }

        return token;
    }

    /**
     * Validate that the given elements are signed by the given identity.
     *
     * <p>This will validate that there is a single signature present in the
     * message for the signing identity.</p>
     *
     * @param context The authentication context to use
     * @param identity The identity that must have signed the elements
     * @param wssResults the processor results to validate
     * @param elementsToValidate The required signed elements.
     * @return True if valid
     */
    public static boolean isValidSigningIdentity( final AuthenticationContext context,
                                                  final IdentityTarget identity,
                                                  final ProcessorResult wssResults,
                                                  final ParsedElement[] elementsToValidate ) {
        return isValidSigningIdentity( context, identity, wssResults.getElementsThatWereSigned(), elementsToValidate );
    }

    /**
     * Validate that the given elements are signed by the given identity.
     *
     * <p>This will validate that there is a single signature present in the
     * message for the signing identity.</p>
     *
     * @param context The authentication context to use
     * @param identity The identity that must have signed the elements
     * @param allSignedElements All signed elements in the message
     * @param elementsToValidate The required signed elements.
     * @return True if valid
     */
    public static boolean isValidSigningIdentity( final AuthenticationContext context,
                                                  final IdentityTarget identity,
                                                  final SignedElement[] allSignedElements,
                                                  final ParsedElement[] elementsToValidate ) {
        boolean valid = elementsToValidate.length>0;

        try {
            // Check for a single signature element (for the identity), not token or certificate (bug 7157)
            if( context!=null && !new IdentityTarget().equals( new IdentityTarget(identity)) ) {
                Element signatureElementForIdentity = null;
                for ( SignedElement signedElement : allSignedElements ) {
                    Set<SigningSecurityToken> signingSecurityTokens = getSigningSecurityTokens( signedElement.asElement(), elementsToValidate );
                    SigningSecurityToken signingSecurityToken = getTokenForIdentityTarget(
                            context,
                            signingSecurityTokens,
                            identity);
                    if ( signingSecurityToken != null ) {
                        signatureElementForIdentity = signingSecurityToken.getSignedElements()[0].getSignatureElement();
                        break;
                    }
                }

                if ( signatureElementForIdentity == null ) {
                    valid = false;
                } else {
                    for (ParsedElement element : elementsToValidate) {
                        if (element instanceof SignedElement) {
                            SignedElement signedElement = (SignedElement) element;

                            Set<SigningSecurityToken> signingSecurityTokens = getSigningSecurityTokens( signedElement.asElement(), elementsToValidate );
                            SigningSecurityToken signingSecurityToken = getTokenForIdentityTarget(
                                    context,
                                    signingSecurityTokens,
                                    identity);

                            if ( signatureElementForIdentity != signingSecurityToken.getSignedElements()[0].getSignatureElement() ) {
                                valid = false;
                                break;
                            }
                        }
                    }

                    // Check that nothing else in the message is signed by this identity using
                    // a different signature
                    if ( valid ) {
                        for ( SignedElement signedElement : allSignedElements ) {
                            Set<SigningSecurityToken> signingSecurityTokens = getSigningSecurityTokens( signedElement.asElement(), elementsToValidate );
                            SigningSecurityToken signingSecurityToken = getTokenForIdentityTarget(
                                    context,
                                    signingSecurityTokens,
                                    identity);

                            if ( signingSecurityToken != null &&
                                 signingSecurityToken.getSignedElements()[0].getSignatureElement() != signatureElementForIdentity ) {
                                valid = false;
                                break;
                            }
                        }
                    }
                }
            } else {
                valid = false;
            }
        } catch ( MultipleTokenException e ) {
            logger.log( Level.WARNING, ExceptionUtils.getMessage(e ));
            valid = false;
        }

        return valid;
    }

    /**
     * Get the token used to sign the message for the given identity.
     *
     * <p>This method does not validate single/multiple signers.</p>
     *
     * @param authContext The authorization context
     * @param wssResults The WSS processing results
     * @param identityTarget The target identity (may be null)
     * @return The token or null if not found
     */
    public static SigningSecurityToken getSigningSecurityTokenByIdentity( final AuthenticationContext authContext,
                                                                          final ProcessorResult wssResults,
                                                                          final IdentityTarget identityTarget ) {
        SigningSecurityToken token = null;

        final SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
        if ( new IdentityTarget().equals( new IdentityTarget(identityTarget)) ) {
            final Set<Element> securityTokenElements = WSSecurityProcessorUtils.getSecurityTokenElements(wssResults);
            for ( SignedElement signedElement : signedElements ) {
                if ( !securityTokenElements.contains(signedElement.asElement()) ) {
                    token = signedElement.getSigningSecurityToken();
                    break;
                }
            }
        } else {
            try {
                for ( SignedElement signedElement : signedElements ) {
                    Set<SigningSecurityToken> signingSecurityTokens = getSigningSecurityTokens( signedElement.asElement(), signedElements );
                    token = getTokenForIdentityTarget( authContext, signingSecurityTokens, identityTarget );
                    if ( token != null ) {
                        break;
                    }
                }
            } catch ( MultipleTokenException e ) {
                logger.log( Level.WARNING, ExceptionUtils.getMessage(e ));
            }
        }

        return token;
    }

    /**
     * Get the SignedElements for the given identity.
     *
     * <p>If there is no requested identity then this method validates that the
     * message has a single signer.</p>
     *
     * <p>If there is a requested identity then this method returns only the
     * elements signed by that identity.</p>
     *
     * <p>In either case, this method also checks that there is a single
     * signing token for the returned elements.</p>
     *
     * @param authContext The authorization context to use.
     * @param wssResults The WSS processing results
     * @param identityTarget The target identity (may be null)
     * @param checkSigningTokenIsCredential True to check that the signing token is a credential if any identity is permitted
     * @param relatedRequestMessage a related request message whose EncryptedKey and WS-SecureConversation tokens whose secret keys are known to the Gateway are to be recognized as valid signing tokens
     *                              for the current target message (as long as a specific signing credentials have not been asked-for in the auth context), or null
     * @param audit Auditor to use for recording information about rejected signing tokens (May be null)
     * @return The signed elements, may be empty but never null.
     */
    public static SignedElement[] filterSignedElementsByIdentity( final AuthenticationContext authContext,
                                                                  final ProcessorResult wssResults,
                                                                  final IdentityTarget identityTarget,
                                                                  final boolean checkSigningTokenIsCredential,
                                                                  final Message relatedRequestMessage,
                                                                  final AuthenticationContext relatedAuthContext,
                                                                  final Audit audit ) {
        final List<SignedElement> signedElementsForIdentity = new ArrayList<SignedElement>();

        final SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
        if ( new IdentityTarget().equals( new IdentityTarget(identityTarget)) ) {
            if ( isValidSingleSigner(
                    authContext,
                    wssResults,
                    new ParsedElement[0],
                    checkSigningTokenIsCredential,
                    relatedRequestMessage,
                    relatedAuthContext,
                    audit ) ) {
                signedElementsForIdentity.addAll( Arrays.asList(signedElements) );
            }
        } else {
            try {
                List<SignedElement> foundSignedElementsForIdentity = new ArrayList<SignedElement>();
                for ( SignedElement signedElement : signedElements ) {
                    Set<SigningSecurityToken> signingSecurityTokens = getSigningSecurityTokens( signedElement.asElement(), signedElements );
                    SigningSecurityToken signingSecurityToken = getTokenForIdentityTarget(
                            authContext,
                            signingSecurityTokens,
                            identityTarget );

                    // Check token equality here since there may be multiple SignedElements
                    // for any given DOM element (see bug 7718)
                    if ( signingSecurityToken != null && signingSecurityToken == signedElement.getSigningSecurityToken() ) {
                        foundSignedElementsForIdentity.add( signedElement );
                    }
                }

                if ( isValidSigningIdentity( authContext, identityTarget, signedElements,
                        foundSignedElementsForIdentity.toArray( new ParsedElement[foundSignedElementsForIdentity.size()] )) ) {
                    signedElementsForIdentity.addAll( foundSignedElementsForIdentity );   
                }
            } catch ( MultipleTokenException e ) {
                logger.log( Level.WARNING, ExceptionUtils.getMessage(e ));
            }
        }

        return signedElementsForIdentity.toArray(new SignedElement[signedElementsForIdentity.size()]);
    }

    /**
     * Extract any signing security tokens from the given login credentials.
     *
     * @param loginCredentials The credentials to process
     * @return The signing security tokens (may be empty but never null)
     */
    public static SigningSecurityToken[] getSigningSecurityTokens( final Collection<LoginCredentials> loginCredentials ) {
        final Collection<SigningSecurityToken> signingSecurityTokens = new ArrayList<SigningSecurityToken>();

        if ( loginCredentials != null ) {
            for ( LoginCredentials loginCredential : loginCredentials ) {
                for ( SecurityToken securityToken : loginCredential.getSecurityTokens() ) {
                    if ( loginCredential.isSecurityTokenPresent( securityToken ) &&
                         securityToken instanceof SigningSecurityToken ) {
                        signingSecurityTokens.add( (SigningSecurityToken) securityToken );
                    }
                }
            }
        }

        return signingSecurityTokens.toArray(new SigningSecurityToken[signingSecurityTokens.size()]);
    }

    /**
     * Get all DOM Elements that are for security tokens.
     *
     * @param wssResults The processor results to mine.
     * @return The Set of Elements (may be empty, never null)
     */
    public static Set<Element> getSecurityTokenElements( final ProcessorResult wssResults ) {
        Set<Element> tokenElements = new HashSet<Element>();
        SecurityToken[] sts = wssResults.getXmlSecurityTokens();
        if(sts!=null) {
            for (SecurityToken st : sts) {
                if (st instanceof SigningSecurityToken) {
                    SigningSecurityToken sst = (SigningSecurityToken) st;
                    tokenElements.add(sst.asElement());
                }
            }
        }
        return tokenElements;
    }

    /**
     * Check the signing tokens signed elements are covered by the same signature.
     *
     * <p>This check prevents a signaure combination attack where multiple 
     * signatures are altered to reference a single signing token
     * (e.g. 2 signatures and 1 BST).</p>
     *
     * @param signingSecurityTokens The tokens to check
     * @return True if the tokens are used by a single signature.
     */
    public static boolean isSameSignature( final SigningSecurityToken... signingSecurityTokens ) {
        boolean sameSignature = true;

        Element signatureElement = null;
        for ( SigningSecurityToken signingSecurityToken : signingSecurityTokens ) {
            SignedElement[] signedElements = signingSecurityToken.getSignedElements();
            if ( signedElements.length == 0 ) {
                sameSignature = false;
                break;
            }

            if ( signatureElement == null ) {
                signatureElement = signedElements[0].getSignatureElement();
            }

            for ( SignedElement signedElement : signedElements ) {
                if ( signatureElement != signedElement.getSignatureElement() ) {
                    sameSignature = false;
                    break;
                }
            }
        }

        return sameSignature;
    }


    /**
     * Find all the tokens that signed the given Element.
     */
    private static Set<SigningSecurityToken> getSigningSecurityTokens( final Element signedElement,
                                                                       final ParsedElement[] parsedElements ) {
        Set<SigningSecurityToken> tokens = new HashSet<SigningSecurityToken>();

        for ( ParsedElement parsedElement : parsedElements ) {
            if ( parsedElement instanceof SignedElement ) {
                SignedElement currentSignedElement = (SignedElement) parsedElement;
                if ( currentSignedElement.asElement() == signedElement ) {
                    tokens.add( currentSignedElement.getSigningSecurityToken() );
                }
            }
        }

        return tokens;
    }

    /**
     * Find the (single) security token for the given identity.
     */
    private static SigningSecurityToken getTokenForIdentityTarget( final AuthenticationContext context,
                                                                   final Collection<SigningSecurityToken> tokens,
                                                                   final IdentityTarget target ) throws MultipleTokenException {
        SigningSecurityToken signingSecurityToken = null;

        String tag = null;
        if ( target.getTargetIdentityType() == IdentityTarget.TargetIdentityType.TAG ) {
            tag = target.getIdentityId();            
        }

        for ( SigningSecurityToken token : tokens ) {
            final AuthenticationResult result = context.getAuthenticationResultForSigningSecurityToken( token, tag );
            if ( result != null ) {
                final User user = result.getUser();

                if ( target.getTargetIdentityType() != null ) {
                    switch ( target.getTargetIdentityType() ) {
                        case USER:
                            if ( target.getIdentityProviderOid().equals(user.getProviderId()) &&
                                 user.getId().equals(target.getIdentityId()) ) {
                                if ( signingSecurityToken != null ) throw new MultipleTokenException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                                signingSecurityToken = token;
                            }
                            break;
                        case GROUP:
                            GroupBean group = new GroupBean();
                            group.setProviderId(target.getIdentityProviderOid());
                            group.setUniqueIdentifier(target.getIdentityId());
                            Boolean groupMembership = result.getCachedGroupMembership(group);
                            if ( groupMembership != null && groupMembership ) {
                                if ( signingSecurityToken != null ) throw new MultipleTokenException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                                signingSecurityToken = token;
                            }
                            break;
                        case PROVIDER:
                            if ( target.getIdentityProviderOid().equals(user.getProviderId())) {
                                if ( signingSecurityToken != null ) throw new MultipleTokenException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                                signingSecurityToken = token;
                            }
                            break;
                        case TAG:
                            if ( signingSecurityToken != null ) throw new MultipleTokenException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                            signingSecurityToken = token;
                            break;
                    }
                }
            }
        }

        return signingSecurityToken;
    }

    /**
     * If the signature confirmations were processed successfully by the (identity unaware) WSS Processor,
     * the elements containing them are returned so that the identity-aware assertions can check
     * if they are covered by a signature.
     *
     * SignatureConfirmation processing has to be performed at most once, by the first WSS assertion targeted at a message.
     * Subsequent calls will return <True,null>. 
     *
     * @return a Pair with the fail/pass flag, and a collection of elements for which the signature must be checked
     */
    public static Pair<Boolean, Collection<ParsedElement>> processSignatureConfirmations(SecurityKnob securityKnob, ProcessorResult wssResults, Audit auditor) {
        if (! securityKnob.isSignatureConfirmationValidated()) {
            securityKnob.setSignatureConfirmationValidated(true);
            Collection<ParsedElement> elementsToCheck = null;
            SignatureConfirmation.Status status = wssResults.getSignatureConfirmation().getStatus();
            if (SignatureConfirmation.Status.CONFIRMED == status) {
                elementsToCheck = wssResults.getSignatureConfirmation().getConfirmationElements().values();
            }
            if (SignatureConfirmation.Status.INVALID == status) {
                auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_SIGNATURE_CONFIRMATION_FAILED, Arrays.toString(wssResults.getSignatureConfirmation().getErrors().toArray()));
            }
            return new Pair<Boolean, Collection<ParsedElement>>(SignatureConfirmation.Status.INVALID != status, elementsToCheck);
        } else {
            return new Pair<Boolean, Collection<ParsedElement>>(true, null);
        }
    }

    /**
     * Adds signature confirmations to the decoration requirements associated with the message.
     *
     * Should be called just before the decorations are applied; no further modifications to the decorations should be done.
     */
    public static void addSignatureConfirmations(Message message, Audit auditor) {

        // get the request signatures
        Message request = message.getRelated(MessageRole.REQUEST);
        if (request == null)
            return; // not a response

        SecurityKnob securityKnob = message.getSecurityKnob();
        SecurityKnob requestSecurityKnob = request.getSecurityKnob();

        boolean wss11seen = securityKnob.getProcessorResult() != null && securityKnob.getProcessorResult().isWsse11Seen();
        
        if ( ! requestSecurityKnob.isNeedsSignatureConfirmations() && ! wss11seen && ! securityKnob.hasWss11Decorations())
            return;

        // get the decoration where signature confirmation should go
        Map<String, DecorationRequirements> decorations = new HashMap<String, DecorationRequirements>();
        for (DecorationRequirements decoration : securityKnob.getDecorationRequirements()) {
            decorations.put(decoration.getSecurityHeaderActor(), decoration);
        }
        Set<String> actors = decorations.keySet();
        DecorationRequirements decoration = actors.contains(SoapConstants.L7_SOAP_ACTOR) ? decorations.get(SoapConstants.L7_SOAP_ACTOR) :
                                            actors.contains(null) ? decorations.get(null) :
                                            actors.size() == 1 ? decorations.values().toArray(new DecorationRequirements[1])[0] :
                                            null;

        ProcessorResult requestWssResult = requestSecurityKnob.getProcessorResult();
        if (requestWssResult == null) {
            auditor.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_NO_WSS, "Request");
        }
        List<String> signatureValues = requestWssResult == null ? null : requestWssResult.getValidatedSignatureValues();
        if (decoration == null) {
            // todo: bug 7277 - add signature confirmations only if the response already has decoration requirements?
            auditor.logAndAudit(MessageProcessingMessages.MESSAGE_NO_SIG_CONFIRMATION);
        } else {
            // add signature confirmations
            if (signatureValues == null || signatureValues.isEmpty()) {
                // no signatures but confirmation needed -> value-less <SignatureConfirmation>
                decoration.addSignatureConfirmation(null);
            } else {
                for (String sig : signatureValues)
                    decoration.addSignatureConfirmation(sig);
            }
        }
        requestSecurityKnob.setNeedsSignatureConfirmations(false);
        securityKnob.setSignatureConfirmationValidated(true); // don't attempt validation on responses with our own signature confirmations
    }

    /**
     * Set the given token on the given decoration requirements.
     *
     * <p>If the token is an encrypted key it must have been unwrapped to be
     * used.</p>
     *
     * @param decoration The decoration requirements to update
     * @param signingToken The token to set
     * @return true if the token was set
     */
    public static boolean setToken( final DecorationRequirements decoration,
                                    final SigningSecurityToken signingToken ) {
        boolean set = false;

        if ( signingToken instanceof KerberosSigningSecurityToken ) {
            decoration.setKerberosTicket( ((KerberosSecurityToken)signingToken).getServiceTicket() );
            set = true;
        } else if ( signingToken instanceof SecurityContextToken ) {
            final SecurityContextToken sct = (SecurityContextToken) signingToken;
            final SecurityContext context = sct.getSecurityContext();
            if ( context instanceof SecureConversationSession ) {
                decoration.setSecureConversationSession((SecureConversationSession) context);
                set = true;
            }
        } else if ( signingToken instanceof EncryptedKey ) {
            EncryptedKey encryptedKey = (EncryptedKey) signingToken;
            if ( encryptedKey.isUnwrapped() ) {
                try {
                    decoration.setEncryptedKey( encryptedKey.getSecretKey() );
                    decoration.setEncryptedKeyReferenceInfo(KeyInfoDetails.makeEncryptedKeySha1Ref( encryptedKey.getEncryptedKeySHA1() ));
                    set = true;
                } catch (InvalidDocumentFormatException e) {
                    throw new IllegalStateException(); // can't happen, it's unwrapped already
                } catch (GeneralSecurityException e) {
                    throw new IllegalStateException(); // can't happen, it's unwrapped already
                }
            }
        }

        return set;
    }

    private static SecurityContextFinder buildContextualFinder( final Message message ) {
        SecurityContextFinder contextualFinder = null;

        final Message relatedRequest = message.getRelated( MessageRole.REQUEST );
        if ( relatedRequest != null ) {
            contextualFinder = new SecurityContextFinder() {
                @Override
                public SecurityContext getSecurityContext( final String securityContextIdentifier ) {
                    final SecurityKnob securityKnob = relatedRequest.getSecurityKnob();
                    for ( final WssDecorator.DecorationResult result : securityKnob.getAllDecorationResults() ) {
                        if ( securityContextIdentifier.equals( result.getWsscSecurityContextId() ) ) {
                            return result.getWsscSecurityContext();
                        }
                    }
                    final ProcessorResult processorResult = securityKnob.getProcessorResult();
                    if ( processorResult != null ) {
                        for ( final XmlSecurityToken token : processorResult.getXmlSecurityTokens() ) {
                            if ( token instanceof SecurityContextToken ) {
                                final SecurityContextToken contextToken = (SecurityContextToken) token;  
                                if ( securityContextIdentifier.equals( contextToken.getContextIdentifier() )) {
                                    return contextToken.getSecurityContext();
                                }
                            }
                        }
                    }
                    return null;
                }
            };
        }

        return contextualFinder;
    }

    private static WssSettings getWssSettings() {
        WssSettings wssSettings = wssSettingsReference.get();

        if ( wssSettings == null || ( wssSettings.created + TimeUnit.SECONDS.toMillis( 30L ) < System.currentTimeMillis()) ) {
            final Config config = ConfigFactory.getUncachedConfig();
            wssSettings = new WssSettings(
                config.getLongProperty( ServerConfigParams.PARAM_SIGNED_PART_MAX_BYTES, 0L ),
                config.getBooleanProperty( ServerConfigParams.PARAM_SOAP_REJECT_MUST_UNDERSTAND, true),
                config.getBooleanProperty( ServerConfigParams.PARAM_WSS_ALLOW_MULTIPLE_TIMESTAMP_SIGNATURES, false),
                config.getBooleanProperty( ServerConfigParams.PARAM_WSS_ALLOW_UNKNOWN_BINARY_SECURITY_TOKENS, false),
                config.getBooleanProperty( ServerConfigParams.PARAM_WSS_PROCESSOR_STRICT_SIG_CONFIRMATION, true)
            );

            wssSettingsReference.set(  wssSettings );
        }

        return wssSettings;
    }

    private static class MultipleTokenException extends Exception {
        private MultipleTokenException( String message ) {
            super( message );
        }
    }

    private static final class WssSettings {
        private final long created = System.currentTimeMillis();
        private final long signedAttachmentMaxSize;
        private final boolean rejectOnMustUnderstand;
        private final boolean permitMultipleTimestampSignatures;
        private final boolean permitUnknownBinarySecurityTokens;
        private final boolean strictSignatureConfirmationValidation;

        private WssSettings( final long signedAttachmentMaxSize,
                             final boolean rejectOnMustUnderstand,
                             final boolean permitMultipleTimestampSignatures,
                             final boolean permitUnknownBinarySecurityTokens,
                             final boolean strictSignatureConfirmationValidation) {
            this.signedAttachmentMaxSize = signedAttachmentMaxSize;
            this.rejectOnMustUnderstand = rejectOnMustUnderstand;
            this.permitMultipleTimestampSignatures = permitMultipleTimestampSignatures;
            this.permitUnknownBinarySecurityTokens = permitUnknownBinarySecurityTokens;
            this.strictSignatureConfirmationValidation = strictSignatureConfirmationValidation;
        }
    }
}
