package com.l7tech.skunkworks;

import org.w3c.dom.Element;
import org.w3c.dom.Text;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidKeyException;

import com.l7tech.common.util.XmlUtil;

/**
 * Implement mechanism described in WS-Secure Conversation to derive
 * symmetric keys from a shared secret. This is not thread safe.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jun 8, 2004<br/>
 * $Id$
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
    public Key derivedKeyTokenToKey(Element derivedKeyToken, byte[] secret) throws NoSuchAlgorithmException {

        String namespaceURI = "";
        // make sure we got a DerivedKeyToken
        if (derivedKeyToken.getLocalName() == null || !derivedKeyToken.getLocalName().equals(DKT_EL_NAME)) {
            throw new IllegalArgumentException("This is no DerivedKeyToken " + derivedKeyToken.getLocalName());
        }
        // check which version of wsc we are dealing with
        if (derivedKeyToken.getNamespaceURI() == null) {
            throw new IllegalArgumentException("DerivedKeyToken must specify wsc namespace");
        } else if (derivedKeyToken.getNamespaceURI().equals(WSC11_NS)) {
            // we are dealing with wsc 1.1
            namespaceURI = WSC11_NS;

        } else if (derivedKeyToken.getNamespaceURI().equals(WSSE11_NS)) {
            // we are dealing with wsc 1.0
            namespaceURI = WSSE11_NS;
        } else {
            namespaceURI = "";
            throw new IllegalArgumentException("Unsuported DerivedKeyToken namespace " + derivedKeyToken.getNamespaceURI());
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
        Element nonceNode = (Element)((XmlUtil.findChildElementsByName(derivedKeyToken, WSSE_NS,  "Nonce")).get(0));
        if(nonceNode != null) {
            nonce = ((Text)nonceNode.getFirstChild()).getNodeValue();
        }

        byte[] seed = new byte[secret.length + label.length() + nonce.length()];
        System.arraycopy(secret, 0, seed, 0, secret.length);
        System.arraycopy(label.getBytes(), 0, seed, secret.length, label.length());
        System.arraycopy(nonce.getBytes(), 0, seed, secret.length+label.length(), nonce.length());

        try{
            byte[] key = pSHA1(secret, seed, Integer.parseInt(length));

            Key dk = new SecretKeySpec(key, "SHA1");

            return dk;
        } catch (InvalidKeyException e) {

        }

        return null;
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

    private static final String DKT_EL_NAME = "DerivedKeyToken";
    private static final String WSC11_NS = "http://schemas.xmlsoap.org/ws/2004/04/sc";
    private static final String WSSE11_NS = "http://schemas.xmlsoap.org/ws/2002/12/secext/";
    private static final String WSSE_NS = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
    private static final String ALGO_ATTRNAME = "Algorithm";
}
