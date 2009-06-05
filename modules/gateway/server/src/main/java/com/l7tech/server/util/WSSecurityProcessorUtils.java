package com.l7tech.server.util;

import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssProcessorImpl;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.token.SecurityToken;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ArrayUtils;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.identity.User;
import com.l7tech.identity.GroupBean;

import java.util.*;

import org.w3c.dom.Element;

/**
 * WS-Security Processor utility methods.
 */
public class WSSecurityProcessorUtils {

    /**
     * Get the processor result for the given message, running the processor if necessary.
     *
     * @param msg The message whose security is being evaluated
     * @param what A description for the mesage being evaluated
     * @param securityTokenResolver The resolver to use to locate security tokens
     * @param audit The auditor to use for errors.
     * @return The processor result or null.
     */
    public static ProcessorResult getWssResults(final Message msg,
                                                final String what,
                                                final SecurityTokenResolver securityTokenResolver,
                                                final Audit audit)
    {
        final SecurityKnob sk = msg.getKnob(SecurityKnob.class);
        final ProcessorResult existingWssResults;
        if (sk != null && null != (existingWssResults = sk.getProcessorResult()))
            return existingWssResults;

        try {
            final WssProcessorImpl impl = new WssProcessorImpl(msg);
            impl.setSecurityTokenResolver(securityTokenResolver);
            ProcessorResult wssResults = impl.processMessage();
            msg.getSecurityKnob().setProcessorResult(wssResults); // In case someone else needs it later
            return wssResults;
        } catch (Exception e) {
            if (audit != null) audit.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_BAD_WSS, new String[] { what, ExceptionUtils.getMessage(e) }, e);
            return null;
        }
    }

    /**
     * Validate that the given elements are signed.
     *
     * <p>This method check that there is a single signature in the message and
     * that the required elements are signed.</p>
     *
     * @param wssResults The Processor Results to validate.
     * @param elementsToValidate The required signed elements
     * @return True if valid
     */
    public static boolean isValidSingleSigner( final ProcessorResult wssResults,
                                               final ParsedElement[] elementsToValidate ) {
        boolean valid = true;

        SignedElement[] signedElements = wssResults.getElementsThatWereSigned();

        // Validate that there is only a single signing identity
        // Check for a single signature element, not token or certificate (bug 7157)
        Set securityTokenElements = WSSecurityProcessorUtils.getSecurityTokenElements(wssResults);
        Element signatureElement = null;
        for ( SignedElement signedElement : signedElements ) {
            if (!securityTokenElements.contains(signedElement.asElement())) {
                if (signatureElement == null) {
                    signatureElement = signedElement.getSignatureElement();
                } else {
                    if (signatureElement != signedElement.getSignatureElement()) {
                        valid = false;
                        break;
                    }
                }
            }
        }

        // Validate that the required elements are signed
        for ( ParsedElement element : elementsToValidate ) {
            if (!ArrayUtils.contains( signedElements, element ) ) {
                valid = false;
                break;
            }            
        }

        return valid;
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
     * @return
     */
    public static SigningSecurityToken getSigningSecurityTokenByIdentity( final AuthenticationContext authContext,
                                                                          final ProcessorResult wssResults,
                                                                          final IdentityTarget identityTarget ) {
        SigningSecurityToken token = null;

        final SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
        if ( new IdentityTarget().equals( new IdentityTarget(identityTarget)) ) {
            final Set securityTokenElements = WSSecurityProcessorUtils.getSecurityTokenElements(wssResults);
            for ( SignedElement signedElement : signedElements ) {
                if ( !securityTokenElements.contains(signedElement) ) {
                    token = signedElement.getSigningSecurityToken();
                    break;
                }
            }
        } else {
            for ( SignedElement signedElement : signedElements ) {
                Set<SigningSecurityToken> signingSecurityTokens = getSigningSecurityTokens( signedElement.asElement(), signedElements );
                token = getTokenForIdentityTarget( authContext, signingSecurityTokens, identityTarget );
                if ( token != null ) {
                    break;
                }
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
     * @return The signed elements, may be empty but never null.
     */
    public static SignedElement[] filterSignedElementsByIdentity( final AuthenticationContext authContext,
                                                                  final ProcessorResult wssResults,
                                                                  final IdentityTarget identityTarget ) {
        List<SignedElement> signedElementsForIdentity = new ArrayList<SignedElement>();

        final SignedElement[] signedElements = wssResults.getElementsThatWereSigned();
        if ( new IdentityTarget().equals( new IdentityTarget(identityTarget)) ) {
            if ( isValidSingleSigner( wssResults, new ParsedElement[0] ) ) {
                signedElementsForIdentity.addAll( Arrays.asList(signedElements) );
            }
        } else if ( isValidSigningIdentity( authContext, identityTarget, signedElements, new ParsedElement[0] ) ) {
            for ( SignedElement signedElement : signedElements ) {
                Set<SigningSecurityToken> signingSecurityTokens = getSigningSecurityTokens( signedElement.asElement(), signedElements );
                SigningSecurityToken signingSecurityToken = getTokenForIdentityTarget(
                        authContext,
                        signingSecurityTokens,
                        identityTarget );

                if ( signingSecurityToken != null ) {
                    signedElementsForIdentity.add( signedElement );
                }
            }
        }

        return signedElementsForIdentity.toArray(new SignedElement[signedElementsForIdentity.size()]);
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
                                                                   final IdentityTarget target ) {
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
                            if ( target.getIdentityProviderOid() == user.getProviderId() &&
                                 user.getId().equals(target.getIdentityId()) ) {
                                if ( signingSecurityToken != null ) throw new IllegalStateException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                                signingSecurityToken = token;
                            }
                            break;
                        case GROUP:
                            GroupBean group = new GroupBean();
                            group.setProviderId(target.getIdentityProviderOid());
                            group.setUniqueIdentifier(target.getIdentityId());
                            if ( result.getCachedGroupMembership(group) ) {
                                if ( signingSecurityToken != null ) throw new IllegalStateException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                                signingSecurityToken = token;
                            }
                            break;
                        case PROVIDER:
                            if ( target.getIdentityProviderOid() == user.getProviderId() ) {
                                if ( signingSecurityToken != null ) throw new IllegalStateException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                                signingSecurityToken = token;
                            }
                            break;
                        case TAG:
                            if ( signingSecurityToken != null ) throw new IllegalStateException("Multiple tokens found for identity '"+target.describeIdentity()+"'.");
                            signingSecurityToken = token;
                            break;
                    }
                }
            }
        }

        return signingSecurityToken;
    }
}
