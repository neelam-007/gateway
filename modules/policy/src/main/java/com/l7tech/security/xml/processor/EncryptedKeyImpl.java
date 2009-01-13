package com.l7tech.security.xml.processor;

import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.*;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.security.GeneralSecurityException;
import java.io.IOException;

/**
 *
 */
class EncryptedKeyImpl extends SigningSecurityTokenImpl implements EncryptedKey {
    private final String elementWsuId;
    private final byte[] encryptedKeyBytes;
    private final SignerInfo signerInfo;
    private SecurityTokenResolver tokenResolver;
    private String encryptedKeySHA1 = null;
    private byte[] secretKeyBytes = null;

    // Constructor that supports lazily-unwrapping the key
    EncryptedKeyImpl(Element encryptedKeyEl, SecurityTokenResolver tokenResolver, Resolver<String,X509Certificate> x509Resolver)
            throws InvalidDocumentFormatException, IOException, GeneralSecurityException, UnexpectedKeyInfoException {
        super(encryptedKeyEl);
        this.elementWsuId = SoapUtil.getElementWsuId(encryptedKeyEl);
        this.tokenResolver = tokenResolver;
        this.signerInfo = KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyEl, tokenResolver, x509Resolver);
        String cipherValueB64 = XencUtil.getEncryptedKeyCipherValue(encryptedKeyEl);
        this.encryptedKeyBytes = HexUtils.decodeBase64(cipherValueB64.trim());
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_ENCRYPTEDKEY;
    }

    public String getElementId() {
        return elementWsuId;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("EncryptedKey: wsuId=");
        sb.append(elementWsuId).append(" unwrapped=").append(isUnwrapped());
        if (secretKeyBytes != null) sb.append(" keylength=").append(secretKeyBytes.length);
        if (encryptedKeySHA1 != null) sb.append(" encryptedKeySha1=").append(encryptedKeySHA1);
        return sb.toString();
    }

    private void unwrapKey() throws InvalidDocumentFormatException, GeneralSecurityException {
        getEncryptedKeySHA1();
        if (secretKeyBytes != null) return;

        // Extract the encrypted key
        Element encMethod = DomUtils.findOnlyOneChildElementByName(asElement(),
                                                                  SoapConstants.XMLENC_NS,
                                                                  "EncryptionMethod");
        secretKeyBytes = XencUtil.decryptKey(encryptedKeyBytes, XencUtil.getOaepBytes(encMethod), signerInfo.getPrivate());

        // Since we've just done the expensive work, ensure that it gets saved for future reuse
        maybePublish();
    }

    public byte[] getSecretKey() throws InvalidDocumentFormatException, GeneralSecurityException {
        if (secretKeyBytes == null)
            unwrapKey();
        return secretKeyBytes;
    }

    public boolean isUnwrapped() {
        return secretKeyBytes != null;
    }

    void setSecretKey(byte[] secretKeyBytes) {
        this.secretKeyBytes = secretKeyBytes;
        maybePublish();
    }

    private void maybePublish() {
        if (tokenResolver != null && encryptedKeySHA1 != null && secretKeyBytes != null) {
            tokenResolver.putSecretKeyByEncryptedKeySha1(encryptedKeySHA1, secretKeyBytes);
            tokenResolver = null;
        }
    }

    public String getEncryptedKeySHA1() {
        if (encryptedKeySHA1 != null)
            return encryptedKeySHA1;
        encryptedKeySHA1 = XencUtil.computeEncryptedKeySha1(encryptedKeyBytes);
        if (secretKeyBytes == null && tokenResolver != null) {
            // Save us a step unwrapping
            secretKeyBytes = tokenResolver.getSecretKeyByEncryptedKeySha1(encryptedKeySHA1);
        } else
            maybePublish();
        return encryptedKeySHA1;
    }
}
