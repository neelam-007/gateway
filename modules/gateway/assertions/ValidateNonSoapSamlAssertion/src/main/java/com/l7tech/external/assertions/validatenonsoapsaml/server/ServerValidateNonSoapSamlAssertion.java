package com.l7tech.external.assertions.validatenonsoapsaml.server;

import com.l7tech.external.assertions.validatenonsoapsaml.ValidateNonSoapSamlAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.token.*;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.security.xml.processor.EmbeddedSamlSignatureToken;
import com.l7tech.security.xml.processor.ProcessorResult;
import com.l7tech.security.xml.processor.WssTimestamp;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.policy.assertion.xmlsec.ServerRequireSaml;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Triple;
import com.l7tech.xml.saml.SamlAssertion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.SignatureException;
import java.util.List;

/**
 * Server side implementation of the ValidateNonSoapSamlAssertion
 *
 * @see com.l7tech.external.assertions.validatenonsoapsaml.ValidateNonSoapSamlAssertion
 */
public class ServerValidateNonSoapSamlAssertion extends ServerRequireSaml<ValidateNonSoapSamlAssertion> {
    private final String[] variablesUsed;

    public ServerValidateNonSoapSamlAssertion(final ValidateNonSoapSamlAssertion assertion) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    @NotNull
    @Override
    protected Triple<AssertionStatus, ProcessorResult, SamlSecurityToken> getSamlSecurityTokenAndContext(Message message, String messageDesc, AuthenticationContext authContext) throws IOException {

        if (!message.isXml()) {
            logAndAudit(AssertionMessages.MESSAGE_NOT_XML, new String[]{messageDesc, ""});
            return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(AssertionStatus.FAILED, null, null);
        }

        final SamlAssertion samlAssertion;
        final Element samlElement;
        try {
            samlElement = message.getXmlKnob().getDocumentReadOnly().getDocumentElement();
            samlAssertion = SamlAssertion.newInstance(samlElement, securityTokenResolver);
        } catch (SAXException e) {
            throw new IOException(e);
        }

        final ProcessorResult procesorResult;
        final EmbeddedSamlSignatureToken samlSignatureToken;
        if (samlAssertion.hasEmbeddedIssuerSignature()) {
            try {
                samlAssertion.verifyEmbeddedIssuerSignature();
                samlSignatureToken = new EmbeddedSamlSignatureToken(samlAssertion);
            } catch (SignatureException e) {
                logAndAudit(AssertionMessages.REQUIRE_WSS_SIGNATURE_CONFIRMATION_FAILED, ExceptionUtils.getMessage(e));
                return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(AssertionStatus.FAILED, null, null);
            }

            procesorResult = getProcessorResult(samlAssertion, samlSignatureToken, samlElement);
        } else {
            procesorResult = getProcessorResult(samlAssertion, null, samlElement);
        }

        return new Triple<AssertionStatus, ProcessorResult, SamlSecurityToken>(AssertionStatus.NONE, procesorResult, samlAssertion);
    }

    // - PRIVATE

    /**
     *
     * @param samlSignatureToken SAML assertion
     * @return ProcessorResult wrapping the SAML assertion
     */
    private ProcessorResult getProcessorResult(@NotNull final SamlAssertion xmlSecurityToken,
                                               @Nullable final EmbeddedSamlSignatureToken samlSignatureToken,
                                               @NotNull final Element samlElement) {

        return new ProcessorResult() {
            @Override
            public SignedElement[] getElementsThatWereSigned() {
                if (samlSignatureToken == null) {
                    return new SignedElement[0];
                } else {
                    return samlSignatureToken.getSignedElements();
                }
            }

            @Override
            public SignedPart[] getPartsThatWereSigned() {
                return new SignedPart[0];
            }

            @Override
            public EncryptedElement[] getElementsThatWereEncrypted() {
                return new EncryptedElement[0];
            }

            @Override
            public SigningSecurityToken[] getSigningTokens(Element element) {
                if (samlSignatureToken != null) {
                    final SignedElement[] signedElements = samlSignatureToken.getSignedElements();
                    for (SignedElement signedElement : signedElements) {
                        if (signedElement.asElement() == element) {
                            // We only know of a single token in this impl. If we signed the element,
                            // then return the saml assertion.
                            return new SigningSecurityToken[]{samlSignatureToken};
                        }
                    }
                }

                return new SigningSecurityToken[0];
            }

            @Override
            public XmlSecurityToken[] getXmlSecurityTokens() {
                return new XmlSecurityToken[]{xmlSecurityToken};
            }

            @Override
            public WssTimestamp getTimestamp() {
                return null;
            }

            @Override
            public String getSecurityNS() {
                return samlElement.getNamespaceURI();
            }

            @Override
            public String getWSUNS() {
                return null;
            }

            @Override
            public String getWsscNS() {
                return null;
            }

            @Override
            public SecurityActor getProcessedActor() {
                return null;
            }

            @Override
            public String getProcessedActorUri() {
                return null;
            }

            @Override
            public List<String> getValidatedSignatureValues() {
                return null;
            }

            @Override
            public SignatureConfirmation getSignatureConfirmation() {
                return null;
            }

            @Override
            public String getLastKeyEncryptionAlgorithm() {
                return null;
            }

            @Override
            public boolean isWsse11Seen() {
                return false;
            }

            @Override
            public boolean isDerivedKeySeen() {
                return false;
            }
        };

    }
}
