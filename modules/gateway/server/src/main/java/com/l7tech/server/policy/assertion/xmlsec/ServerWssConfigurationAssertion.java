package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.security.KeyStoreException;
import java.util.logging.Logger;

/**
 * Server side implementation for {@link com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion}.
 */
public class ServerWssConfigurationAssertion extends AbstractMessageTargetableServerAssertion<WssConfigurationAssertion> {
    private static final Logger logger = Logger.getLogger(ServerWssConfigurationAssertion.class.getName());
    private final Auditor auditor;
    private final KeyInfoInclusionType sigKeyReference;
    private final KeyInfoInclusionType encKeyReference;
    private final SignerInfo signerInfo;

    public ServerWssConfigurationAssertion(final WssConfigurationAssertion assertion, BeanFactory beanFactory) throws PolicyAssertionException {
        super(assertion, assertion);
        this.auditor = beanFactory instanceof ApplicationContext
                       ? new Auditor(this, (ApplicationContext)beanFactory, logger)
                       : new LogOnlyAuditor(logger);

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

        if (assertion.getWssVersion() != null)
            dreq.setWssVersion(assertion.getWssVersion());
        dreq.setUseDerivedKeys(assertion.isUseDerivedKeys());
        dreq.setIncludeTimestamp(assertion.isAddTimestamp());
        dreq.setSignTimestamp(assertion.isSignTimestamp());
        dreq.setProtectTokens(assertion.isProtectTokens());
        if (assertion.getEncryptionAlgorithmUri() != null)
            dreq.setEncryptionAlgorithm(assertion.getEncryptionAlgorithmUri());
        if (assertion.getKeyWrappingAlgorithmUri() != null)
            dreq.setKeyEncryptionAlgorithm(assertion.getKeyWrappingAlgorithmUri());
        if (assertion.getDigestAlgorithmName() != null)
            dreq.setSignatureMessageDigest(assertion.getDigestAlgorithmName());
        if (sigKeyReference != null)
            dreq.setKeyInfoInclusionType(sigKeyReference);
        if (encKeyReference != null)
            dreq.setEncryptionKeyInfoInclusionType(encKeyReference);

        if (signerInfo != null) {
            dreq.setSenderMessageSigningCertificate(signerInfo.getCertificate());
            dreq.setSenderMessageSigningPrivateKey(signerInfo.getPrivate());
        }

        return AssertionStatus.NONE;
    }

    @Override
    protected Audit getAuditor() {
        return auditor;
    }

    private static KeyInfoInclusionType asKeyInfoInclusionType(WssConfigurationAssertion assertion, String keyReferenceName) throws PolicyAssertionException {
        if (KeyReference.BST.getName().equals(keyReferenceName)) {
            return KeyInfoInclusionType.CERT;
        } else if (KeyReference.ISSUER_SERIAL.getName().equals(keyReferenceName)) {
            return KeyInfoInclusionType.ISSUER_SERIAL;
        } else if (KeyReference.SKI.getName().equals(keyReferenceName)) {
            return KeyInfoInclusionType.STR_SKI;
        } else {
            throw new PolicyAssertionException(assertion, "Invalid key reference type: " + keyReferenceName);
        }
    }
}
