package com.l7tech.security.xml.processor;

import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.SignedElement;
import com.l7tech.security.token.SigningSecurityToken;
import com.l7tech.security.token.X509SecurityToken;
import com.l7tech.security.xml.DsigUtil;
import com.l7tech.xml.saml.SamlAssertion;
import org.w3c.dom.Element;

import java.security.SignatureException;
import java.security.cert.X509Certificate;

/**
* A virtual signing token representing a signing certificate embedded in a SAML token signature.
*/
public final class EmbeddedSamlSignatureToken extends SigningSecurityTokenImpl implements X509SecurityToken {
    private final SamlAssertion samlToken;
    private final String signatureAlgorithmId;
    private final String[] digestAlgorithmIds;

    public EmbeddedSamlSignatureToken( final SamlAssertion samlToken ) throws SignatureException {
        super(null);
        this.samlToken = samlToken;
        this.signatureAlgorithmId = DsigUtil.findSigAlgorithm(samlToken.getEmbeddedIssuerSignature());
        this.digestAlgorithmIds = DsigUtil.findDigestAlgorithms(samlToken.getEmbeddedIssuerSignature());
        addSignedElement( new SignedElement() {
            @Override
            public SigningSecurityToken getSigningSecurityToken() {
                return EmbeddedSamlSignatureToken.this;
            }

            @Override
            public Element getSignatureElement() {
                return samlToken.getEmbeddedIssuerSignature();
            }

            @Override
            public Element asElement() {
                return samlToken.asElement();
            }

            @Override
            public String getSignatureAlgorithmId() {
                return signatureAlgorithmId;
            }

            @Override
            public String[] getDigestAlgorithmIds() {
                return digestAlgorithmIds;
            }
        } );
    }

    @Override
    public X509Certificate getCertificate() {
        return samlToken.getIssuerCertificate();
    }

    /**
     * @return true if the sender has proven its possession of the private key corresponding to this security token.
     *         This is done by signing one or more elements of the message with it.
     */
    @Override
    public boolean isPossessionProved() {
        return false;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_X509_BST;
    }

    @Override
    public String getElementId() {
        return samlToken.getElementId();
    }

    @Override
    public Element asElement() {
        return samlToken.asElement();
    }

    @Override
    public void dispose() {
        super.dispose();
        samlToken.dispose();
    }
}
