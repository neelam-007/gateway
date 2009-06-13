package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.security.token.ParsedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.X509SigningSecurityToken;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.CausedIOException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Enforces that a specific element in a message is signed.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: July 14, 2004<br/>
 */
public class ServerRequireWssSignedElement extends ServerRequireWssOperation<RequireWssSignedElement> {
    private static final Logger logger = Logger.getLogger(ServerRequireWssSignedElement.class.getName());

    public ServerRequireWssSignedElement(RequireWssSignedElement data, ApplicationContext springContext) {
        super(logger, data, springContext);
    }

    @Override
    protected String getPastTenseOperationName() {
        return "signed";
    }

    @Override
    protected AssertionStatus onCheckRequestSuccess( final PolicyEnforcementContext context,
                                                     final Message message,
                                                     final String messageDesc ) {
        final ProcessorResult wssResults = message.getSecurityKnob().getProcessorResult();
        if ( wssResults != null ) {
            setVariables( context, message,  wssResults );

            if ( isRequest() ) {
                if (context.isResponseWss11() && !wssResults.getValidatedSignatureValues().isEmpty()) {
                    context.addDeferredAssertion(this, deferredSignatureConfirmation(assertion, auditor, wssResults.getValidatedSignatureValues()));
                }
            }
        }
        
        return AssertionStatus.NONE;
    }

    // A deferred job that tries to attach a SignatureConfirmation to the response, if the response is SOAP.
    public static ServerAssertion deferredSignatureConfirmation( final Assertion owner,
                                                                 final Auditor auditor,
                                                                 final List<String> signatureConfirmations) {
        return new AbstractServerAssertion<Assertion>(owner) {
            @Override
            public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException {
                DecorationRequirements wssReq;

                try {
                    if (!context.getResponse().isSoap()) {
                        auditor.logAndAudit(AssertionMessages.REQUIRE_WSS_SIGNATURE_RESPONSE_NOT_SOAP);
                        // FALLTHROUGH: We'll still send the response; it just won't contain a SignatureConfirmation
                    } else if(context.getResponse().getSecurityKnob().getDecorationRequirements().length > 0){
                        wssReq = context.getResponse().getSecurityKnob().getOrMakeDecorationRequirements();

                        for (String confirmation : signatureConfirmations)
                            wssReq.addSignatureConfirmation(confirmation);
                    }
                } catch (SAXException e) {
                    throw new CausedIOException(e);
                }
                return AssertionStatus.NONE;
            }
        };
    }

    @Override
    protected ParsedElement[] getElementsFoundByProcessor(final ProcessorResult wssResults) {
        if (wssResults == null) return new ParsedElement[0];
        return wssResults.getElementsThatWereSigned();
    }

    /**
     * Ensure that any signed elements that not security tokens are signed by
     * the same key unless multiple signatures are enabled.
     *
     * If multiple signatures are enabled then all the required elements must be
     * signed by the same token (the token for the target identity)
     */
    @Override
    protected boolean elementsFoundByProcessorAreValid( final PolicyEnforcementContext context,
                                                        final Message message,
                                                        final ProcessorResult wssResults,
                                                        final ParsedElement[] elements ) {
        boolean valid = true;

        // This check occurs before the ParsedElements relevant to this assertion have been
        // matched. Therefore we only perform this check if there is no target identity for
        // the assertion.
        if( elements.length>0 &&
           new IdentityTarget().equals( new IdentityTarget(assertion.getIdentityTarget() )) ) {
            valid = WSSecurityProcessorUtils.isValidSingleSigner(
                wssResults,
                new ParsedElement[0] // we validate that the right elements are signed elsewhere
            );
        }

        return valid;
    }

    @Override
    protected boolean elementsFoundForAssertionAreValid( final PolicyEnforcementContext context,
                                                         final Message message,
                                                         final ProcessorResult wssResults,
                                                         final ParsedElement[] elements ) {
        boolean valid = true;

        // When this check occurs the ParsedElements relevant to this assertion have been
        // matched. Therefore we only perform this check if there is a target identity for
        // the assertion.
        if ( !new IdentityTarget().equals( new IdentityTarget(assertion.getIdentityTarget() )) ) {
            return WSSecurityProcessorUtils.isValidSigningIdentity(
                context.getAuthenticationContext(message),
                assertion.getIdentityTarget(),
                wssResults,
                elements
            );            
        }

        return valid;
    }

    @Override
    protected boolean isAllowIfEmpty() {
        return false;
    }

    private String prefixVariable( final String variableName ) {
        return VariableMetadata.prefixName(  assertion.getVariablePrefix(), variableName );
    }

    private void setVariables( final PolicyEnforcementContext context,
                               final Message message,
                               final ProcessorResult wssResults ) {
        if ( assertion.getVariablePrefix() != null && assertion.getVariablePrefix().length() > 0 ) {
            SigningSecurityToken token = WSSecurityProcessorUtils.getSigningSecurityTokenByIdentity(
                    context.getAuthenticationContext(message),
                    wssResults,
                    assertion.getIdentityTarget() );

            if ( token != null ) {
                SignedElement[] signedElements = token.getSignedElements();
                if ( signedElements.length > 0 ) {
                    setVariable( context, RequireWssSignedElement.VAR_SIGNATURE_ELEMENT, signedElements[0].getSignatureElement() );
                }

            }

            X509SigningSecurityToken x509Token = null;
            if ( token instanceof X509SigningSecurityToken ) {
                x509Token = (X509SigningSecurityToken) token;
            }

            if ( x509Token != null ) {
                if ( x509Token.getType() == SecurityTokenType.WSS_X509_BST ) {
                    setVariable( context, RequireWssSignedElement.VAR_TOKEN_ELEMENT, x509Token.asElement() );
                }
                setVariable( context, RequireWssSignedElement.VAR_TOKEN_TYPE, "X.509" );
                setVariable( context, RequireWssSignedElement.VAR_TOKEN_ATTRIBUTES, x509Token.getMessageSigningCertificate() );
            }
        }
    }

    private void setVariable( final PolicyEnforcementContext context,
                              final String name,
                              final Object value ) {
        try {
            context.setVariable( prefixVariable(name), value );
        } catch ( VariableNotSettableException vnse ) {
            auditor.logAndAudit( AssertionMessages.VARIABLE_NOTSET, vnse.getVariable() );
        }
    }

}
