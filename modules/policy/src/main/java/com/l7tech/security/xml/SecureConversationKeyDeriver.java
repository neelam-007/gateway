package com.l7tech.security.xml;

import com.l7tech.util.*;
import org.w3c.dom.Element;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Implement mechanism described in WS-Secure Conversation to derive
 * symmetric keys from a shared secret. This is not thread safe.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 3, 2004<br/>
 */
public class SecureConversationKeyDeriver {
    public static final String URI_ALG_PHSA1_1 = SoapConstants.ALGORITHM_PSHA;
    public static final String URI_ALG_PHSA1_2 = SoapConstants.ALGORITHM_PSHA2;
    public static final String URI_ALG_PHSA1_3 = SoapConstants.ALGORITHM_PSHA3;
    private static final Collection<String> URI_ALG_PSHA1S = Collections.unmodifiableCollection(Arrays.asList(
            URI_ALG_PHSA1_1,
            URI_ALG_PHSA1_2,
            URI_ALG_PHSA1_3
    ));
    private static final String DEFAULT_DEFAULT_LABEL = "WS-SecureConversationWS-SecureConversation";
    private static final String DEFAULT_LABEL = nullAsNull(SyspropUtil.getString("com.l7tech.security.wssc.defaultLabel", DEFAULT_DEFAULT_LABEL));
    private static final boolean IGNORE_ALGORITHM_URI = SyspropUtil.getBoolean("com.l7tech.security.wssc.ignoreAlgorithmUri", false);
    private static final int MAX_GENERATION = SyspropUtil.getInteger("com.l7tech.security.wssc.maxGeneration", 100);
    private static final int MAX_LENGTH = SyspropUtil.getInteger("com.l7tech.security.wssc.maxLength", 512);

    private static final String ATTR_ALGORITHM = "Algorithm";
    private static final String ELE_GENERATION = "Generation";
    private static final String ELE_LENGTH = "Length";
    private static final String ELE_LABEL = "Label";
    private static final String ELE_NONCE = "Nonce";


    private static String nullAsNull(String s) {
        return "null".equals(s) ? null : s;
    }

    /**
     * Derive the symmetric key using information provided in a DerivedKeyToken xml
     * element. This would be used when decrypting or verifying the signature of a
     * soap message using a secure conversation.
     *
     * @param derivedKeyToken the DerivedKeyToken xml element
     * @param secret the secret associated with the session
     * @return the resulting derived key
     * @throws java.security.NoSuchAlgorithmException   if the algorithm specified in the derived key token is not available
     * @throws com.l7tech.util.InvalidDocumentFormatException  if the derived key token cannot be parsed
     */
    public byte[] derivedKeyTokenToKey( final Element derivedKeyToken,
                                        final byte[] secret)
                                    throws NoSuchAlgorithmException, InvalidDocumentFormatException {

        final String namespaceURI = derivedKeyToken.getNamespaceURI();
        // make sure we got a DerivedKeyToken
        if (derivedKeyToken.getLocalName() == null || !derivedKeyToken.getLocalName().equals( SoapConstants.WSSC_DK_EL_NAME)) {
            throw new InvalidDocumentFormatException("This is no DerivedKeyToken " + derivedKeyToken.getLocalName());
        }
        // check which version of wsc we are dealing with
        if (namespaceURI == null) {
            throw new IllegalArgumentException("DerivedKeyToken must specify wsc namespace");
        } else if (DomUtils.elementInNamespace(derivedKeyToken, SoapConstants.WSSC_NAMESPACE_ARRAY)
                || DomUtils.elementInNamespace(derivedKeyToken, SoapConstants.SECURITY_URIS_ARRAY)) {
            // ok wsc 1.0, 1.1 or whatever wsc is in WSE 3.0
        } else {
            throw new InvalidDocumentFormatException("Unsupported DerivedKeyToken namespace " +
                                                         derivedKeyToken.getNamespaceURI());
        }

        if (!IGNORE_ALGORITHM_URI) {
            // check that default algorithm is in effect
            String algo = derivedKeyToken.getAttributeNS(namespaceURI, ATTR_ALGORITHM );
            if (algo==null || algo.length()==0) algo = derivedKeyToken.getAttribute( ATTR_ALGORITHM );
            if (algo != null && algo.length() > 0 && !URI_ALG_PSHA1S.contains(algo))
                throw new NoSuchAlgorithmException("Unsupported DerivedKeyToken "+ATTR_ALGORITHM+": " + algo);
        }

        String lengthVal = null;
        String label = DEFAULT_LABEL;
        final String nonce;
        int generation = 0;
        // get generation
        Element genNode = DomUtils.findOnlyOneChildElementByName(derivedKeyToken, namespaceURI, ELE_GENERATION );
        if (genNode != null) {
            String genVal = DomUtils.getTextValue(genNode);
            if (genVal != null && genVal.trim().length() > 0) {
                try {
                    generation = Integer.parseInt(genVal.trim());
                    if ( generation < 0 || generation > MAX_GENERATION ) {
                        throw new InvalidDocumentFormatException("Illegal DerivedKeyToken " + ELE_GENERATION + ": " + generation );
                    }
                } catch (NumberFormatException e) {
                    throw new InvalidDocumentFormatException(e);
                }
            }
        }
        // get length
        final Element lenNode = DomUtils.findOnlyOneChildElementByName(derivedKeyToken, namespaceURI, ELE_LENGTH );
        if (lenNode != null) {
            lengthVal = DomUtils.getTextValue(lenNode);
        }
        if (lengthVal == null || lengthVal.length() < 1) {
            throw new InvalidDocumentFormatException("DerivedKeyToken " +ELE_LENGTH+ " is required");
        }
        final int length;
        try {
            length = Integer.parseInt(lengthVal.trim());
            if ( length < 0 || length > MAX_LENGTH ) {
                throw new InvalidDocumentFormatException("Illegal DerivedKeyToken " + ELE_LENGTH + ": " + length );
            }
        } catch (NumberFormatException e) {
            throw new InvalidDocumentFormatException(e);
        }

        // get label
        final Element labelNode = DomUtils.findOnlyOneChildElementByName(derivedKeyToken, namespaceURI, ELE_LABEL );
        if (labelNode != null) {
            label = DomUtils.getTextValue(labelNode);
        }

        // get nonce
        Element nonceNode = DomUtils.findOnlyOneChildElementByName(derivedKeyToken,
                                                                   SoapConstants.SECURITY_URIS_ARRAY,
                                                                   ELE_NONCE );

        if(nonceNode==null) nonceNode = DomUtils.findOnlyOneChildElementByName(derivedKeyToken,
                                                                               namespaceURI,
                                                                               ELE_NONCE );
        if (nonceNode == null)
            throw new InvalidDocumentFormatException("DerivedKeyToken has no " + ELE_NONCE);
        nonce = DomUtils.getTextValue(nonceNode);

        final byte[] nonceA;
        nonceA = HexUtils.decodeBase64(nonce);

        final byte[] seed = new byte[label.length() + nonceA.length];
        System.arraycopy(label.getBytes(), 0, seed, 0, label.length());
        System.arraycopy(nonceA, 0, seed, label.length(), nonceA.length);

        final int offset = generation * length;
        try{
            final byte[] generated = pSHA1(secret, seed, offset+length);
            final byte[] key = new byte[length];
            System.arraycopy(generated, offset, key, 0, length);
            return key;
        } catch (InvalidKeyException e) {
            throw new InvalidDocumentFormatException(e);
        }
    }

    /**
     * Function used to generate derived key as per WS-Secure Conversation. This mechanism
     * is inspired by RFC 2246 (TLS).
     * <pre>
     * P_hash(secret, seed) = HMAC_hash(secret, A(1) + seed) +
     *                        HMAC_hash(secret, A(2) + seed) +
     *                        HMAC_hash(secret, A(3) + seed) + ...
     * Where + indicates concatenation.
     *
     *  A(x) is defined as:
     *      A(0) = seed
     *      A(i) = HMAC_hash(secret, A(i-1))
     * </pre>
     *
     * @param secret   secret key from which to derive new key.  required
     * @param seed     seed value for hash.  Required.
     * @param requiredlength number of bytes of key material to derive
     * @return a byte array with length=requiredBytes.  Never null.
     * @throws java.security.InvalidKeyException may occur if current crypto policy disallows HMac with long keys
     * @throws java.security.NoSuchAlgorithmException if no HMacSHA1 service available from current security providers
     */
    public static byte[] pSHA1(byte[] secret, byte[] seed, int requiredlength) throws NoSuchAlgorithmException, InvalidKeyException {
        // compute A(1)
        final SecretKeySpec key = new SecretKeySpec(secret, "HMacSHA1");
        byte[] ai = doHmac(key, seed);
        // compute A(1) + seed
        byte[] aiPlusSeed = new byte[ai.length + seed.length];
        System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
        System.arraycopy(seed, 0, aiPlusSeed, ai.length, seed.length);
        // start the P_SHA-1
        byte[] output = doHmac(key, aiPlusSeed);
        // continue until we get at least the desired length
        while (output.length < requiredlength) {
            ai = doHmac(key, ai);
            if (aiPlusSeed.length == (ai.length + seed.length)) {
                System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
            } else {
                aiPlusSeed = new byte[ai.length + seed.length];
                System.arraycopy(ai, 0, aiPlusSeed, 0, ai.length);
                System.arraycopy(seed, 0, aiPlusSeed, ai.length, seed.length);
            }
            byte[] bytesToAppend = doHmac(key, aiPlusSeed);
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

    private static byte[] doHmac(Key key, byte[] seed) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac hmac = SecureConversationKeyDeriver.hmac.get();
        hmac.reset();
        hmac.init(key);
        return hmac.doFinal(seed);
    }

    private static final ThreadLocal<Mac> hmac = new ThreadLocal<Mac>() {
        @Override
        protected Mac initialValue() {
            try {
                return Mac.getInstance("HMacSHA1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
    };

}
