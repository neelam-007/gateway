package com.l7tech.common.security.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;
import java.io.IOException;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;

import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Mac;

/**
 * Implement mechanism described in WS-Secure Conversation to derive
 * symmetric keys from a shared secret. This is not thread safe.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 * $Id$<br/>
 */
public class SecureConversationKeyDeriver {
    /**
     * Derive the symmetric key using information provided in a DerivedKeyToken xml
     * element. This would be used when decrypting or verifying the signature of a
     * soap message using a secure conversation.
     *
     * @param derivedKeyToken the DerivedKeyToken xml element
     * @param secret the secret associated with the session
     * @return the resulting derived key
     */
    public Key derivedKeyTokenToKey(Element derivedKeyToken, byte[] secret)
                                    throws NoSuchAlgorithmException, InvalidDocumentFormatException {

        String namespaceURI = derivedKeyToken.getNamespaceURI();
        // make sure we got a DerivedKeyToken
        if (derivedKeyToken.getLocalName() == null || !derivedKeyToken.getLocalName().equals(SoapUtil.WSSC_DK_EL_NAME)) {
            throw new InvalidDocumentFormatException("This is no DerivedKeyToken " + derivedKeyToken.getLocalName());
        }
        // check which version of wsc we are dealing with
        if (namespaceURI == null) {
            throw new IllegalArgumentException("DerivedKeyToken must specify wsc namespace");
        } else if (namespaceURI.equals(SoapUtil.WSSC_NAMESPACE)) {
            // we are dealing with wsc 1.1
            // do we care?
        } else {
            boolean ok = false;
            // lets make sure we are dealing with a wsse namespace
            for (int i = 0; i < SoapUtil.SECURITY_URIS_ARRAY.length; i++) {
                String secNs = SoapUtil.SECURITY_URIS_ARRAY[i];
                if (secNs.equals(namespaceURI)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                throw new InvalidDocumentFormatException("Unsuported DerivedKeyToken namespace " +
                                                         derivedKeyToken.getNamespaceURI());
            }
            // we are dealing with wsc 1.0
            // do we care?
        }

        // check that default algorithm is in effect
        String algo = derivedKeyToken.getAttributeNS(namespaceURI, ALGO_ATTRNAME);

        if (algo.equals("")) {
            throw new NoSuchAlgorithmException("Algorithm specified (" + algo + "). We only support default P_SHA-1");
        }

        String generation = null;
        String length = null;
        String label = null;
        String nonce = null;

        // get generation
        Element genNode = (Element)((XmlUtil.findChildElementsByName(derivedKeyToken, namespaceURI,  "Generation")).get(0));
        if(genNode != null) {
            generation = ((Text)genNode.getFirstChild()).getNodeValue();;
        }
        // get length
        Element lenNode = (Element)((XmlUtil.findChildElementsByName(derivedKeyToken, namespaceURI,  "Length")).get(0));
        if(lenNode != null) {
            length = ((Text)lenNode.getFirstChild()).getNodeValue();
        }

        // get label
        Element labelNode = (Element)((XmlUtil.findChildElementsByName(derivedKeyToken, namespaceURI,  "Label")).get(0));
        if(labelNode != null) {
            label = ((Text)labelNode.getFirstChild()).getNodeValue();
        }

        // get nonce
        Element nonceNode = (Element)((XmlUtil.findChildElementsByName(derivedKeyToken,
                                                                       SoapUtil.SECURITY_URIS_ARRAY,
                                                                       "Nonce")).get(0));
        if(nonceNode != null) {
            nonce = ((Text)nonceNode.getFirstChild()).getNodeValue();
        }

        byte[] nonceA = new byte[1];
        try {
            nonceA = HexUtils.decodeBase64(nonce);
        } catch (IOException e) {
            throw new InvalidDocumentFormatException(e);
        }
        // todo handle generation? -- fla
        byte[] seed = new byte[label.length() + nonceA.length];
        System.arraycopy(label.getBytes(), 0, seed, 0, label.length());
        System.arraycopy(nonceA, 0, seed, label.length(), nonceA.length);

        try{
            byte[] key = pSHA1(secret, seed, Integer.parseInt(length));
            Key dk = new SecretKeySpec(key, "SHA1");
            return dk;
        } catch (InvalidKeyException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    /**
     * Function used to generate derived key as per WS-Secure Conversation. This mechanism
     * is inspired by RFC 2246 (TLS).
     *
     * P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +
     *                        HMAC_hash(secret, A(2) + seed) +
     *                        HMAC_hash(secret, A(3) + seed) + ...
     * Where + indicates concatenation.
     *
     *  A(x) is defined as:
     *      A(0) = seed
     *      A(i) = HMAC_hash(secret, A(i-1))
     */
       public byte[] pSHA1(byte[] secret, byte[] seed, int requiredlength) throws NoSuchAlgorithmException, InvalidKeyException {
        // compute A(1)
        byte[] ai = getHMacSHA1(secret).doFinal(seed);
        // compute A(1) + seed
        byte[] aiPlusSeed = new byte[ai.length + seed.length];
        System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
        System.arraycopy(seed, 0, aiPlusSeed, ai.length, seed.length);
        // start the P_SHA-1
        byte[] output = getHMacSHA1(secret).doFinal(aiPlusSeed);
        // continue until we get at least the desired length
        while (output.length < requiredlength) {
            ai = getHMacSHA1(secret).doFinal(ai);
            if (aiPlusSeed.length == (ai.length + seed.length)) {
                System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
            } else {
                aiPlusSeed = new byte[ai.length + seed.length];
                System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
                System.arraycopy(seed, 0, aiPlusSeed, ai.length, seed.length);
            }
            byte[] bytesToAppend = getHMacSHA1(secret).doFinal(aiPlusSeed);
            byte[] newoutput = new byte[output.length + bytesToAppend.length];
            System.arraycopy(output, 0, newoutput, 0, output.length);
            System.arraycopy(bytesToAppend, 0, newoutput, output.length, bytesToAppend.length);
            output = newoutput;
        }
        // trim to desired size if necessary
        if (output.length > requiredlength) {
            byte[] trimmedoutput = new byte[requiredlength];
            System.arraycopy(output, 0, trimmedoutput, 0, requiredlength);
            output = trimmedoutput;
        }
        return output;
    }

    private Mac getHMacSHA1(byte[] secret) throws NoSuchAlgorithmException, InvalidKeyException {
        if (hmac == null) {
            hmac = Mac.getInstance("HMacSHA1");
            Key key = new SecretKeySpec(secret, "SHA1");
            hmac.init(key);
        }
        return hmac;
    }

    private Mac hmac;
    private static final String ALGO_ATTRNAME = "Algorithm";
}
