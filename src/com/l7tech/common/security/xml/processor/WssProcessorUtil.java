/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.token.EncryptedKey;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.security.xml.SecurityTokenResolver;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.SecurityKnob;
import com.l7tech.common.audit.Audit;
import com.l7tech.common.audit.MessageProcessingMessages;
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

    public static ProcessorResult getWssResults(final Message msg, final String what, 
                                                final SecurityTokenResolver securityTokenResolver, final Audit audit)
    {
        final ProcessorResult wssResults;
        final SecurityKnob sk = (SecurityKnob)msg.getKnob(SecurityKnob.class);
        if (sk != null) {
            wssResults = sk.getProcessorResult();
            if (wssResults == null && audit != null) audit.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_NO_WSS, what);
            return wssResults;
        } else {
            try {
                final WssProcessorImpl impl = new WssProcessorImpl(msg);
                impl.setSecurityTokenResolver(securityTokenResolver);
                wssResults = impl.processMessage();
                msg.getSecurityKnob().setProcessorResult(wssResults); // In case someone else needs it later
                return wssResults;
            } catch (Exception e) {
                if (audit != null) audit.logAndAudit(MessageProcessingMessages.MESSAGE_VAR_BAD_WSS, new String[] { what, ExceptionUtils.getMessage(e) }, e);
                return null;
            }
        }
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
