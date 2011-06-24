package com.l7tech.security.xml.processor;

import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.xml.*;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import java.security.GeneralSecurityException;

/**
 * Implementation of an EncryptedKey token.
 */
public class EncryptedKeyImpl extends SigningSecurityTokenImpl implements EncryptedKey {
    private final String elementWsuId;
    private final byte[] encryptedKeyBytes;
    private final SignerInfo signerInfo;
    private final String extraTokenType;
    private final String extraTokenId;
    private SecurityTokenResolver tokenResolver;
    private String encryptedKeySHA1 = null;
    private byte[] secretKeyBytes = null;

    /**
     * Constructor that supports lazily-unwrapping the key
     *
     * @param encryptedKeyEl The encrypted key element to process.
     * @param tokenResolver The token resolver, must be a ContextualSecurityTokenResolver for BST resolution.
     */
    public EncryptedKeyImpl( @NotNull final Element encryptedKeyEl,
                             @NotNull final SecurityTokenResolver tokenResolver)
            throws InvalidDocumentFormatException, GeneralSecurityException, UnexpectedKeyInfoException {
        this( encryptedKeyEl, tokenResolver, null, null );
    }

    /**
     * Create a new encrypted key.
     *
     * @param encryptedKeyEl The encrypted key element to process.
     * @param tokenResolver The token resolver, must be a ContextualSecurityTokenResolver for BST resolution.
     */
    public EncryptedKeyImpl( @NotNull final Element encryptedKeyEl,
                             @NotNull final SecurityTokenResolver tokenResolver,
                             @Nullable final String extraTokenType,
                             @Nullable final String extraTokenId )
            throws InvalidDocumentFormatException, GeneralSecurityException, UnexpectedKeyInfoException {
        super(encryptedKeyEl);
        this.elementWsuId = SoapUtil.getElementWsuId(encryptedKeyEl);
        this.tokenResolver = tokenResolver;
        this.signerInfo = KeyInfoElement.getTargetPrivateKeyForEncryptedType(encryptedKeyEl, tokenResolver);
        String cipherValueB64 = XencUtil.getEncryptedKeyCipherValue(encryptedKeyEl);
        this.encryptedKeyBytes = HexUtils.decodeBase64(cipherValueB64.trim());
        this.extraTokenType = extraTokenType;
        this.extraTokenId = extraTokenId;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_ENCRYPTEDKEY;
    }

    @Override
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
        if (signerInfo.isRestrictedAccess()) {
            // Prevent publishing of secret keys unwrapped using a restricted access private key
            tokenResolver = null;
        }

        // Since we've just done the expensive work, ensure that it gets saved for future reuse
        maybePublish();
    }

    @Override
    public byte[] getSecretKey() throws InvalidDocumentFormatException, GeneralSecurityException {
        if (secretKeyBytes == null)
            unwrapKey();
        return secretKeyBytes;
    }

    @Override
    public boolean isUnwrapped() {
        return secretKeyBytes != null;
    }

    void setSecretKey(byte[] secretKeyBytes) {
        this.secretKeyBytes = secretKeyBytes;
        maybePublish();
    }

    private void maybePublish() {
        boolean published = false;
        if (tokenResolver != null && encryptedKeySHA1 != null && secretKeyBytes != null) {
            tokenResolver.putSecretKeyByEncryptedKeySha1(encryptedKeySHA1, secretKeyBytes);
            published = true;
        }
        if (tokenResolver != null && extraTokenType != null && extraTokenId != null && secretKeyBytes != null) {
            tokenResolver.putSecretKeyByTokenIdentifier(extraTokenType, extraTokenId, secretKeyBytes);
            published = true;
        }
        if ( published ) {
            tokenResolver = null;
        }
    }

    @Override
    public String getEncryptedKeySHA1() {
        if (encryptedKeySHA1 != null)
            return encryptedKeySHA1;
        encryptedKeySHA1 = XencUtil.computeEncryptedKeySha1(encryptedKeyBytes);
        if (secretKeyBytes == null && tokenResolver != null) {
            // Save us a step unwrapping
            secretKeyBytes = tokenResolver.getSecretKeyByEncryptedKeySha1(encryptedKeySHA1);
            if (tokenResolver != null && extraTokenType != null && extraTokenId != null && secretKeyBytes != null) {
                // necessary in case multiple extra tokens share the same encrypted key
                tokenResolver.putSecretKeyByTokenIdentifier(extraTokenType, extraTokenId, secretKeyBytes);
            }
        } else
            maybePublish();
        return encryptedKeySHA1;
    }
}
