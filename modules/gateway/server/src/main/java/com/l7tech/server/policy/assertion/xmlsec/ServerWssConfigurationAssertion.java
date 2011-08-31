package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.MessageRole;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.WsSecurityVersion;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.UnrecoverableKeyException;

import static com.l7tech.security.xml.decorator.DecorationRequirements.WsaHeaderSigningStrategy.ALWAYS_SIGN_WSA_HEADERS;
import static com.l7tech.security.xml.decorator.DecorationRequirements.WsaHeaderSigningStrategy.NEVER_SIGN_WSA_HEADERS;

/**
 * Server side implementation for {@link com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion}.
 */
public class ServerWssConfigurationAssertion extends AbstractMessageTargetableServerAssertion<WssConfigurationAssertion> {
    private final KeyInfoInclusionType sigKeyReference;
    private final KeyInfoInclusionType encKeyReference;
    private final SignerInfo signerInfo;

    public ServerWssConfigurationAssertion(final WssConfigurationAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion);

        SignerInfo signer = null;
        if (beanFactory != null) {
            try {
                signer = ServerAssertionUtils.getSignerInfo(beanFactory, assertion);
            } catch (KeyStoreException e) {
                throw new PolicyAssertionException(assertion, e.getMessage(), e);
            }
        }

        this.signerInfo = signer;
        this.sigKeyReference = assertion.getKeyReference() == null ? null : asKeyInfoInclusionType(assertion, assertion.getKeyReference());
        this.encKeyReference = assertion.getEncryptionKeyReference() == null ? null : asKeyInfoInclusionType(assertion, assertion.getEncryptionKeyReference());
    }

    @Override
    protected AssertionStatus doCheckRequest(PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext) throws IOException, PolicyAssertionException {
        DecorationRequirements dreq = message.getSecurityKnob().getAlternateDecorationRequirements(assertion.getRecipientContext());

        if ( assertion.getWssVersion() != null ) {
            dreq.setWssVersion(assertion.getWssVersion());

            final boolean isWss11 = assertion.getWssVersion()==WsSecurityVersion.WSS11;
            final Message request = message.getRelated(MessageRole.REQUEST);
            if ( request != null && !isWss11 ) {
                // turn off signature confirmation requirement if not a WSS 1.1 response
                request.getSecurityKnob().setNeedsSignatureConfirmations( false );
            }
            if ( isResponse() ) {
                context.setResponseWss11( isWss11 );
            }
        }
        dreq.setUseDerivedKeys(assertion.isUseDerivedKeys());
        if ( assertion.getSecureConversationNamespace() != null ) {
            dreq.getNamespaceFactory().setWsscNs( assertion.getSecureConversationNamespace() );   
        }
        dreq.setIncludeTimestamp(assertion.isAddTimestamp());
        dreq.setSignTimestamp(assertion.isSignTimestamp());
        dreq.setProtectTokens(assertion.isProtectTokens());
        dreq.setEncryptSignature(assertion.isEncryptSignature());

        //adding this assertion to a policy removes the default signing behaviour for WS-Addressing headers.
        if(assertion.isSignWsAddressingHeaders()){
            dreq.setWsaHeaderSignStrategy(ALWAYS_SIGN_WSA_HEADERS);
        } else {
            dreq.setWsaHeaderSignStrategy(NEVER_SIGN_WSA_HEADERS);
        }

        if (assertion.getEncryptionAlgorithmUri() != null)
            dreq.setEncryptionAlgorithm(assertion.getEncryptionAlgorithmUri());
        if (assertion.getKeyWrappingAlgorithmUri() != null)
            dreq.setKeyEncryptionAlgorithm(assertion.getKeyWrappingAlgorithmUri());
        if (assertion.getDigestAlgorithmName() != null)
            dreq.setSignatureMessageDigest(assertion.getDigestAlgorithmName());
        if (assertion.getReferenceDigestAlgorithmName() != null)
            dreq.setSignatureReferenceMessageDigest(assertion.getReferenceDigestAlgorithmName());
        if (sigKeyReference != null)
            dreq.setKeyInfoInclusionType(sigKeyReference);
        if (encKeyReference != null)
            dreq.setEncryptionKeyInfoInclusionType(encKeyReference);

        if (signerInfo != null) {
            dreq.setSenderMessageSigningCertificate(signerInfo.getCertificate());
            try {
                dreq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
            } catch (UnrecoverableKeyException e) {
                //noinspection ThrowableResultOfMethodCallIgnored
                logAndAudit(AssertionMessages.ASSERTION_MISCONFIGURED, new String[] { "Unable to access configured private key: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            }
        }

        return AssertionStatus.NONE;
    }

    private static KeyInfoInclusionType asKeyInfoInclusionType(WssConfigurationAssertion assertion, String keyReferenceName) throws PolicyAssertionException {
        if (KeyReference.BST.getName().equals(keyReferenceName)) {
            return KeyInfoInclusionType.CERT;
        } else if (KeyReference.ISSUER_SERIAL.getName().equals(keyReferenceName)) {
            return KeyInfoInclusionType.ISSUER_SERIAL;
        } else if (KeyReference.SKI.getName().equals(keyReferenceName)) {
            return KeyInfoInclusionType.STR_SKI;
        } else if (KeyReference.KEY_NAME.getName().equals(keyReferenceName)) {
            return KeyInfoInclusionType.KEY_NAME;
        } else {
            throw new PolicyAssertionException(assertion, "Invalid key reference type: " + keyReferenceName);
        }
    }
}
