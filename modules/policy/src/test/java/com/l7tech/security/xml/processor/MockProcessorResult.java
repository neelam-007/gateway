package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SignedPart;
import com.l7tech.security.token.EncryptedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.XmlSecurityToken;
import com.l7tech.security.token.SignatureConfirmation;
import com.l7tech.security.xml.SecurityActor;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Collections;

/**
 *
 */
public class MockProcessorResult implements ProcessorResult {

    @Override
    public EncryptedElement[] getElementsThatWereEncrypted() {
        return new EncryptedElement[0];
    }

    @Override
    public SignedElement[] getElementsThatWereSigned() {
        return new SignedElement[0];
    }

    @Override
    public SignedPart[] getPartsThatWereSigned() {
        return new SignedPart[0];
    }

    @Override
    public String getLastKeyEncryptionAlgorithm() {
        return null;
    }

    @Override
    public SecurityActor getProcessedActor() {
        return SecurityActor.NOACTOR;
    }

    @Override
    public String getProcessedActorUri() {
        return null;
    }

    @Override
    public List<String> getValidatedSignatureValues() {
        return Collections.emptyList();
    }

    @Override
    public SignatureConfirmation getSignatureConfirmation() {
        return new SignatureConfirmationImpl();
    }

    @Override
    public String getSecurityNS() {
        return SoapUtil.SECURITY_NAMESPACE;
    }

    @Override
    public SigningSecurityToken[] getSigningTokens( Element element ) {
        return new SigningSecurityToken[0];
    }

    @Override
    public WssTimestamp getTimestamp() {
        return null;
    }

    @Override
    public String getWSUNS() {
        return SoapUtil.WSU_NAMESPACE;
    }

    @Override
    public boolean isDerivedKeySeen() {
        return false;
    }

    @Override
    public boolean isWsse11Seen() {
        return false;
    }

    @Override
    public XmlSecurityToken[] getXmlSecurityTokens() {
        return new XmlSecurityToken[0];
    }
}
