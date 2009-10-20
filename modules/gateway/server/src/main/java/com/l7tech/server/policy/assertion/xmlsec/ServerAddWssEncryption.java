package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.util.WSSecurityProcessorUtils;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.IdentityTarget;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.common.io.CertUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.security.token.SecurityContextToken;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.X509SigningSecurityToken;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.SamlSecurityToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Support class for server assertions that perform encryption.
 */
public abstract class ServerAddWssEncryption<AT extends Assertion> extends AbstractMessageTargetableServerAssertion<AT> {

    //- PUBLIC

    public ServerAddWssEncryption( final AT assertion,
                                   final SecurityHeaderAddressable securityHeaderAddressable,
                                   final MessageTargetable messageTargetable,
                                   final IdentityTargetable identityTargetable,
                                   final Logger logger ) {
        super( assertion, messageTargetable );
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
        recipientContextCert = rccert;
    }

    //- PROTECTED

    protected final X509Certificate recipientContextCert;
    protected final SecurityHeaderAddressable securityHeaderAddressable;
    protected final IdentityTarget identityTarget;

    /**
     * Build an EncryptionContext for configuration of message decoration requirements. 
     *
     * @param context The current pec
     * @return The EncryptionContext to use
     * @throws PolicyAssertionException If an error occurs
     * @throws MultipleTokensException If it is not possible to find a single token to encrypt for
     */
    protected EncryptionContext buildEncryptionContext( final PolicyEnforcementContext context )
            throws PolicyAssertionException, MultipleTokensException {
        X509Certificate clientCert = null;
        SigningSecurityToken signingSecurityToken = null;
        String keyEncryptionAlgorithm = null;
        XmlSecurityRecipientContext recipientContext = null;

        if (!securityHeaderAddressable.getRecipientContext().localRecipient()) {
            if (recipientContextCert == null) {
                String msg = "cannot retrieve the recipient cert";
                getAuditor().logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, msg);
                throw new PolicyAssertionException(assertion, msg);
            }
            clientCert = recipientContextCert;
            recipientContext = securityHeaderAddressable.getRecipientContext();
        } else if ( isResponse() ) {
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

                if ( wssResult != null && signingSecurityToken instanceof X509SigningSecurityToken ) {
                    if ( signingSecurityToken instanceof SamlSecurityToken && !useDetectedKeyAlgorithmForSAML )
                    keyEncryptionAlgorithm = wssResult.getLastKeyEncryptionAlgorithm();
                }
            } else if ( ssts.length > 1 ) {
                throw new MultipleTokensException();
            }
        }

        return new EncryptionContext(
                clientCert,
                signingSecurityToken,
                keyEncryptionAlgorithm,
                recipientContext );
    }

    /**
     * Apply the previously constructed EncryptionContext to the given DecorationRequirements.
     *
     * @param policyEnforcementContext The pec to use if adding deferred assertion (may be null)
     * @param wssReq The DecorationRequirements to be udpated
     * @param encryptionContext The EncryptionContext that specifies the key to use
     */
    protected void applyDecorationRequirements( final PolicyEnforcementContext policyEnforcementContext,
                                                final DecorationRequirements wssReq,
                                                final EncryptionContext encryptionContext ) {
        if ( isResponse() && !new IdentityTarget().equals( identityTarget ) ) {
            // If decorating the response message we'll need to reset the requirements to
            // ensure that only the token we want to use for our identity is configured.
            wssReq.clearTokens();

            if ( policyEnforcementContext != null ) {
                // We also have to do this later in case credential assertions deferred
                // assertions have reset the decoration requirements.
                policyEnforcementContext.addDeferredAssertion( this, new AbstractServerAssertion<AT>(assertion){
                    @Override
                    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
                        applyDecorationRequirements( null, wssReq, encryptionContext );
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
            } else if ( token instanceof KerberosSigningSecurityToken ) {
                WSSecurityProcessorUtils.setToken( wssReq, token );
            } else if ( token instanceof SecurityContextToken ||
                        token instanceof EncryptedKey) {
                if ( WSSecurityProcessorUtils.setToken( wssReq, token ) ) {
                    wssReq.setSignTimestamp();
                }
            } 
        }
    }

    protected static class MultipleTokensException extends Exception {}


    protected static class EncryptionContext {
        private final X509Certificate clientCert;
        private final SigningSecurityToken signingSecurityToken;
        private final String keyEncryptionAlgorithm;
        private final XmlSecurityRecipientContext recipientContext;

        /**
         * Create a new EncryptionContext
         *
         * @param clientCert client cert to encrypt to, or null to use alternate means
         * @param signingSecurityToken  The token to encrypt to, or null if not available
         * @param keyEncryptionAlgorithm The key encryption algorithm to use in the response (if X.509 cert)
         * @param recipientContext the intended recipient for the Security header to create
         */
        private EncryptionContext( final X509Certificate clientCert,
                                   final SigningSecurityToken signingSecurityToken,
                                   final String keyEncryptionAlgorithm,
                                   final XmlSecurityRecipientContext recipientContext ) {
            this.clientCert = clientCert;
            this.signingSecurityToken = signingSecurityToken;
            this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
            this.recipientContext = recipientContext;
        }

        public boolean hasEncryptionKey() {
            return clientCert != null || signingSecurityToken != null;    
        }

        public XmlSecurityRecipientContext getRecipientContext() {
            return recipientContext;
        }
    }

    //- PRIVATE

    // In 5.0 we did not use the request encrypted key algorithm for anything other than
    // X.509 credentials, it seems useful for any X509SigningSecurityToken though.
    //
    // This system property allows you to return the default behaviour to that used in 5.0
    private static boolean useDetectedKeyAlgorithmForSAML = SyspropUtil.getBoolean( "com.l7tech.server.wss.decoration.useDetectedKeyAlgorithmForSaml", true );

}
