/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.EncryptedKey;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.crypto.SecretKey;
import java.util.Random;

/**
 * Holds utility methods of interest to users of WssProcessor.
 */
public class WssProcessorUtil {
    /**
     * Creates a virtual EncryptedKey security token that uses the specified SecretKey identified
     * by the specified EncryptedKeySHA1 identifier.
     *
     * @param key             The SecretKey that was encoded into the original EncryptedKey
     * @param encryptedKeySha1 the EncryptedKeySHA1 identifier that refers to the original EncryptedKey
     */
    public static EncryptedKey makeEncryptedKey(final SecretKey key, final String encryptedKeySha1) {
        byte[] rand = new byte[16];
        new Random().nextBytes(rand);
        String id = "VirtualEncryptedKey-1-" + HexUtils.hexDump(rand);
        Element element;
        try {
            element = XmlUtil.stringToDocument("<xenc:EncryptedKey wsu:Id=\"" + id +
                    "\" xmlns:xenc=\""+ SoapUtil.XMLENC_NS + "\" xmlns:wsu=\"" + SoapUtil.WSU_NAMESPACE +
                    "\"/>").getDocumentElement();
        } catch (SAXException e) {
            throw new RuntimeException(e); // can't happen
        }
        return new MyEncryptedKey(id, element, encryptedKeySha1, key);
    }

    /**
     * Private implementation of EncryptedKey that can be used internally by Trogdor.
     */
    private static class MyEncryptedKey extends MutableSigningSecurityToken implements EncryptedKey {
        private final String encryptedKeySha1;
        private final SecretKey key;
        private final String id;

        public MyEncryptedKey(String id, Element element, String encryptedKeySha1, SecretKey key) {
            super(element);
            this.encryptedKeySha1 = encryptedKeySha1;
            this.key = key;
            this.id = id;
        }

        public String getEncryptedKeySHA1() {
            return encryptedKeySha1;
        }

        public SecretKey getSecretKey() {
            return key;
        }

        public String getElementId() {
            return id;
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.WSS_ENCRYPTEDKEY;
        }

        public String toString() {
            return "VirtualEncryptedKey: " + key.getEncoded().length + " byte key";
        }
    }
}
