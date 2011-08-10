package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditHaver;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.HexUtils;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for assertions that add WSS encryption decoration requirements.
 */
public class AddWssEncryptionSupport implements AuditHaver {
    // In 5.0 we did not use the request encrypted key algorithm for anything other than
    // X.509 credentials, it seems useful for any X509SigningSecurityToken though.
    //
    // This system property allows you to return the default behaviour to that used in 5.0
    static boolean useDetectedKeyAlgorithmForSAML = ConfigFactory.getBooleanProperty( "com.l7tech.server.wss.decoration.useDetectedKeyAlgorithmForSaml", true );

    private final AuditHaver auditorHaver;
    private final MessageTargetable messageTargetable;
    private final X509Certificate recipientContextCert;
    private final SecurityHeaderAddressable securityHeaderAddressable;
    private final IdentityTarget identityTarget;

    public static class MultipleTokensException extends Exception {}

    public AddWssEncryptionSupport(AuditHaver auditorHaver, Logger logger, MessageTargetable messageTargetable, SecurityHeaderAddressable securityHeaderAddressable, IdentityTargetable identityTargetable) {
        this.auditorHaver = auditorHaver;
        this.messageTargetable = messageTargetable;
        this.securityHeaderAddressable = securityHeaderAddressable;
        this.identityTarget = new IdentityTarget( identityTargetable.getIdentityTarget() );

        X509Certificate rccert = null;
        if (!securityHeaderAddressable.getRecipientContext().localRecipient()) {
            try {
                rccert = CertUtils.decodeCert( HexUtils.decodeBase64(
                        securityHeaderAddressable.getRecipientContext().getBase64edX509Certificate(), true));
            } catch (CertificateException e) {
                logger.log( Level.WARNING, "Assertion will always fail: recipient cert cannot be decoded: " + e.getMessage(), e);
                rccert = null;
            }
        }
        this.recipientContextCert = rccert;
    }

    public Message getTargetMessage(PolicyEnforcementContext context) throws AssertionStatusException {
        try {
            return context.getTargetMessage(messageTargetable);
        } catch (NoSuchVariableException e) {
            getAuditor().logAndAudit(AssertionMessages.MESSAGE_TARGET_ERROR, e.getVariable(), ExceptionUtils.getMessage(e));
            throw new AssertionStatusException(AssertionStatus.FAILED);
        }
    }

    @Override
    public Audit getAuditor() {
        return auditorHaver.getAuditor();
    }

    /**
     * Build an AddWssEncryptionContext for configuration of message decoration requirements.
     *
     * @param context The current pec
     * @return The AddWssEncryptionContext to use
     * @throws com.l7tech.policy.assertion.PolicyAssertionException If an error occurs
     * @throws com.l7tech.server.policy.assertion.xmlsec.AddWssEncryptionSupport.MultipleTokensException If it is not possible to find a single token to encrypt for
     */
    public AddWssEncryptionContext buildEncryptionContext( final PolicyEnforcementContext context )
            throws PolicyAssertionException, AddWssEncryptionSupport.MultipleTokensException {
        X509Certificate clientCert = null;
        SigningSecurityToken signingSecurityToken = null;
        String keyEncryptionAlgorithm = null;
        XmlSecurityRecipientContext recipientContext = null;

        if (!securityHeaderAddressable.getRecipientContext().localRecipient()) {
            if (recipientContextCert == null) {
                String msg = "cannot retrieve the recipient cert";
                getAuditor().logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
                throw new AssertionStatusException(AssertionStatus.SERVER_ERROR, msg);
            }
            clientCert = recipientContextCert;
            recipientContext = securityHeaderAddressable.getRecipientContext();
        } else if (isResponse()) {
            final Message requestMessage = context.getRequest();
            final AuthenticationContext authContext = context.getAuthenticationContext(requestMessage);
            final ProcessorResult wssResult = requestMessage.getSecurityKnob().getProcessorResult();

            final SigningSecurityToken[] ssts;
            if ( new IdentityTarget().equals( identityTarget ) ) {
                ssts = WSSecurityProcessorUtils.getSigningSecurityTokens( authContext.getCredentials() );
            } else if ( wssResult != null ) {
                SigningSecurityToken token = WSSecurityProcessorUtils.getSigningSecurityTokenByIdentity( authContext, wssResult, identityTarget );
                if ( token != null ) {
                    ssts = new SigningSecurityToken[]{ token };
                } else {
                    ssts = new SigningSecurityToken[0];
                }
            } else {
                ssts = new SigningSecurityToken[0];
            }

            if ( ssts.length == 1 ) {
                signingSecurityToken = ssts[0];

                if ( wssResult != null && signingSecurityToken instanceof X509SigningSecurityToken) {
                    if ( signingSecurityToken instanceof SamlSecurityToken && !AddWssEncryptionSupport.useDetectedKeyAlgorithmForSAML )
                    keyEncryptionAlgorithm = wssResult.getLastKeyEncryptionAlgorithm();
                }
            } else if ( ssts.length > 1 ) {
                throw new AddWssEncryptionSupport.MultipleTokensException();
            }
        }

        return new AddWssEncryptionContext(
                clientCert,
                signingSecurityToken,
                keyEncryptionAlgorithm,
                recipientContext );
    }

    private boolean isResponse() {
        return TargetMessageType.RESPONSE.equals(messageTargetable.getTarget());
    }

    /**
     * Apply the previously constructed AddWssEncryptionContext to the given DecorationRequirements.
     *
     * @param policyEnforcementContext The pec to use if adding deferred assertion (may be null)
     * @param wssReq The DecorationRequirements to be udpated
     * @param encryptionContext The AddWssEncryptionContext that specifies the key to use
     * @param deferredAssertionOwner the ServerAssertion that owns any deferred application of encryption requirements
     *     (for cancellation of deferred behavior if the owning asertion's policy branch is falsified); or null to do no deferred application.
     */
    public void applyDecorationRequirements( final PolicyEnforcementContext policyEnforcementContext,
                                                final DecorationRequirements wssReq,
                                                final AddWssEncryptionContext encryptionContext,
                                                final ServerAssertion deferredAssertionOwner) {
        if ( isResponse() && !new IdentityTarget().equals( identityTarget ) ) {
            // If decorating the response message we'll need to reset the requirements to
            // ensure that only the token we want to use for our identity is configured.
            wssReq.clearTokens();

            if ( policyEnforcementContext != null && deferredAssertionOwner != null ) {
                // We also have to do this later in case credential assertions deferred
                // assertions have reset the decoration requirements.
                policyEnforcementContext.addDeferredAssertion( deferredAssertionOwner, new AbstractServerAssertion<Assertion>(deferredAssertionOwner.getAssertion()){
                    @Override
                    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
                        applyDecorationRequirements( null, wssReq, encryptionContext, deferredAssertionOwner );
                        return AssertionStatus.NONE;
                    }
                } );
            }
        }

        if ( wssReq.getKeyEncryptionAlgorithm() == null ) {
            wssReq.setKeyEncryptionAlgorithm( encryptionContext.keyEncryptionAlgorithm );
        }

        if ( encryptionContext.clientCert != null ) {
            wssReq.setRecipientCertificate( encryptionContext.clientCert );
        } else if ( encryptionContext.signingSecurityToken != null ) {
            SigningSecurityToken token = encryptionContext.signingSecurityToken;
            if ( token instanceof X509SigningSecurityToken ) {
                // NOTE: this includes SAML assertions
                wssReq.setRecipientCertificate( ((X509SigningSecurityToken)token).getMessageSigningCertificate() );
            } else if ( token instanceof KerberosSigningSecurityToken) {
                WSSecurityProcessorUtils.setToken( wssReq, token );
            } else if ( token instanceof SecurityContextToken ||
                        token instanceof EncryptedKey) {
                if ( WSSecurityProcessorUtils.setToken( wssReq, token ) ) {
                    wssReq.setSignTimestamp(true);
                }
            }
        }
    }
}
