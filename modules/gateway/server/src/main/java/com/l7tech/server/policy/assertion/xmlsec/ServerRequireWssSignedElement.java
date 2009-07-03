package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.util.SyspropUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.VariableNotSettableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.message.Message;
import org.springframework.context.ApplicationContext;
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
    private static final boolean requireCredentialSigningToken = SyspropUtil.getBoolean( "com.l7tech.server.policy.requireSigningTokenCredential", true );

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
            if ( isRequest() && context.isResponseWss11() ) {
                    message.getSecurityKnob().setNeedsSignatureConfirmations(true);
            }
        }

        return AssertionStatus.NONE;
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
            WSSecurityProcessorUtils.processSignatureConfirmations(message.getSecurityKnob(), wssResults, elements);
            valid = WSSecurityProcessorUtils.isValidSignatureConfirmations(wssResults.getSignatureConfirmation(), auditor) &&
                    WSSecurityProcessorUtils.isValidSingleSigner(
                        context.getAuthenticationContext(message),
                        wssResults,
                        new ParsedElement[0], // we validate that the right elements are signed elsewhere
                        requireCredentialSigningToken
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

            ParsedElement[] elementsToCheck = WSSecurityProcessorUtils.processSignatureConfirmations(message.getSecurityKnob(), wssResults, elements);

            return WSSecurityProcessorUtils.isValidSignatureConfirmations(wssResults.getSignatureConfirmation(), auditor) &&
                   WSSecurityProcessorUtils.isValidSigningIdentity(
                       context.getAuthenticationContext(message),
                       assertion.getIdentityTarget(),
                       wssResults,
                       elementsToCheck
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

                setVariable( context, RequireWssSignedElement.VAR_TOKEN_TYPE, token.getType().getCategory() );
                if ( token instanceof SamlSecurityToken ) {
                    SamlSecurityToken samlToken = (SamlSecurityToken) token;
                    setVariable( context, RequireWssSignedElement.VAR_TOKEN_ELEMENT, samlToken.asElement() );
                    setVariable( context, RequireWssSignedElement.VAR_TOKEN_ATTRIBUTES + ".issuer.certificate", samlToken.getIssuerCertificate() );
                    setVariable( context, RequireWssSignedElement.VAR_TOKEN_ATTRIBUTES + ".subject.certificate", samlToken.getSubjectCertificate() );
                    setVariable( context, RequireWssSignedElement.VAR_TOKEN_ATTRIBUTES + ".signing.certificate", samlToken.getMessageSigningCertificate() );
                } else if ( token instanceof X509SigningSecurityToken ) {
                    X509SigningSecurityToken x509Token = (X509SigningSecurityToken) token;
                    setVariable( context, RequireWssSignedElement.VAR_TOKEN_ELEMENT, x509Token.asElement() );
                    setVariable( context, RequireWssSignedElement.VAR_TOKEN_ATTRIBUTES, x509Token.getMessageSigningCertificate() );
                } 
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
