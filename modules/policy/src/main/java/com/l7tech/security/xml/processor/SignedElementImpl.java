package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import org.w3c.dom.Element;

/**
* Represents a signed element that was verified by the WSS processor.
*/
class SignedElementImpl implements SignedElement {
    private final SigningSecurityToken signingToken;
    private final Element element;
    private final Element signatureElement;
    private final String signatureAlgorithmId;
    private final String[] digestAlgorithmIds;

    SignedElementImpl(SigningSecurityToken signingToken, Element element, Element signatureElement, String signatureAlgorithmId, String[] digestAlgorithmsIds) {
        this.signingToken = signingToken;
        this.element = element;
        this.signatureElement = signatureElement;
        this.signatureAlgorithmId = signatureAlgorithmId;
        this.digestAlgorithmIds = digestAlgorithmsIds;
    }

    @Override
    public SigningSecurityToken getSigningSecurityToken() {
        return signingToken;
    }

    @Override
    public Element asElement() {
        return element;
    }

    @Override
    public Element getSignatureElement() {
        return signatureElement;
    }

    @Override
    public String getSignatureAlgorithmId() {
        return signatureAlgorithmId;
    }

    @Override
    public String[] getDigestAlgorithmIds() {
        return digestAlgorithmIds;
    }
}
