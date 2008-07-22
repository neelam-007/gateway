/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml.processor;

import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.security.token.EncryptedKey;
import com.l7tech.security.token.KerberosSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.HexUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Holds utility methods of interest to users of WssProcessor.
 */
public class WssProcessorUtil {
    private static Random random = new SecureRandom();

    /**
     * Creates a virtual EncryptedKey security token that uses the specified SecretKey identified
     * by the specified EncryptedKeySHA1 identifier.
     *
     * @param factory    DOM factory to use to create the DOM for the token, if it is eventually requested
     * @param key             The SecretKey bytes that were encoded into the original EncryptedKey
     * @param encryptedKeySha1 the EncryptedKeySHA1 identifier that refers to the original EncryptedKey
     * @return a virtual EncryptedKey instance containing the specified key and sha1.
     */
    public static EncryptedKey makeEncryptedKey(final Document factory, final byte[] key, final String encryptedKeySha1) {
        byte[] rand = new byte[16];
        random.nextBytes(rand);
        String id = "VirtualEncryptedKey-1-" + HexUtils.hexDump(rand);
        return new VirtualEncryptedKey(id, factory, encryptedKeySha1, key);
    }

    /**
     * Creates a virtual KerberosSecurityToken that uses the specified ticket.
     *
     * @param ticket  the kerberos ticket to point to.  Must not be null.
     * @return a new virtual binary security token (with a null element) that can be used as a SigningSecurityToken.
     */
    public static KerberosSecurityToken makeKerberosToken(KerberosGSSAPReqTicket ticket) {
        byte[] rand = new byte[16];
        random.nextBytes(rand);
        String id = "VirtualBinarySecurityToken-1-" + HexUtils.hexDump(rand);
        return new KerberosSecurityTokenImpl(ticket, id);
    }

    /**
     * Private implementation of EncryptedKey that can be used internally by Trogdor.
     */
    private static class VirtualEncryptedKey extends SigningSecurityTokenImpl implements EncryptedKey {
        private final String encryptedKeySha1;
        private final byte[] key;
        private final String id;
        private final Document factory;

        public VirtualEncryptedKey(String id, Document factory, String encryptedKeySha1, byte[] key) {
            super();
            this.encryptedKeySha1 = encryptedKeySha1;
            this.key = key;
            this.id = id;
            this.factory = factory;
        }

        protected Element makeElement() {
            Element element = factory.createElementNS( SoapConstants.XMLENC_NS, "xenc:EncryptedKey");
            element.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:xenc", SoapConstants.XMLENC_NS);
            element.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:wsu", SoapConstants.WSU_NAMESPACE);
            // TODO populate the ciphervalue and other crap, should it someday turn out to be needed
            return element;
        }

        public boolean isUnwrapped() {
            return true;
        }

        public String getEncryptedKeySHA1() {
            return encryptedKeySha1;
        }

        public byte[] getSecretKey() {
            return key;
        }

        public String getElementId() {
            return id;
        }

        public SecurityTokenType getType() {
            return SecurityTokenType.WSS_ENCRYPTEDKEY;
        }

        public String toString() {
            return "VirtualEncryptedKey: " + key.length + " byte key";
        }
    }
}
