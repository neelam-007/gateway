/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.EncryptedKey;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import javax.crypto.SecretKey;
import java.util.Random;
import java.security.SecureRandom;

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
     * @param key             The SecretKey that was encoded into the original EncryptedKey
     * @param encryptedKeySha1 the EncryptedKeySHA1 identifier that refers to the original EncryptedKey
     * @return a virtual EncryptedKey instance containing the specified key and sha1.
     */
    public static EncryptedKey makeEncryptedKey(final Document factory, final SecretKey key, final String encryptedKeySha1) {
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
        new Random().nextBytes(rand);
        String id = "VirtualBinarySecurityToken-1-" + HexUtils.hexDump(rand);
        return new KerberosSecurityTokenImpl(ticket, id);
    }

    /**
     * Private implementation of EncryptedKey that can be used internally by Trogdor.
     */
    private static class VirtualEncryptedKey extends SigningSecurityTokenImpl implements EncryptedKey {
        private final String encryptedKeySha1;
        private final SecretKey key;
        private final String id;
        private final Document factory;

        public VirtualEncryptedKey(String id, Document factory, String encryptedKeySha1, SecretKey key) {
            super();
            this.encryptedKeySha1 = encryptedKeySha1;
            this.key = key;
            this.id = id;
            this.factory = factory;
        }

        protected Element makeElement() {
            Element element = factory.createElementNS(SoapUtil.XMLENC_NS, "xenc:EncryptedKey");
            element.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:xenc", SoapUtil.XMLENC_NS);
            element.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:wsu", SoapUtil.WSU_NAMESPACE);
            // TODO populate the ciphervalue and other crap, should it someday turn out to be needed
            return element;
        }

        public boolean isUnwrapped() {
            return true;
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
