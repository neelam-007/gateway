/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.xml;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.JceProvider;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods to support XML Encryption, specifically EncryptedKey elements.
 */
public class XencUtil {
    private static final Logger logger = Logger.getLogger(XencUtil.class.getName());

    /**
     * This handles the padding of the encryption method designated by http://www.w3.org/2001/04/xmlenc#rsa-1_5.
     *
     * Exceprt from the spec:
     * the padding is of the following special form:
     * 02 | PS* | 00 | key
     * where "|" is concatenation, "02" and "00" are fixed octets of the corresponding hexadecimal value, PS is
     * a string of strong pseudo-random octets [RANDOM] at least eight octets long, containing no zero octets,
     * and long enough that the value of the quantity being CRYPTed is one octet shorter than the RSA modulus,
     * and "key" is the key being transported. The key is 192 bits for TRIPLEDES and 128, 192, or 256 bits for
     * AES. Support of this key transport algorithm for transporting 192 bit keys is MANDATORY to implement.
     *
     * @param paddedKey the decrpted but still padded key
     * @return the unpadded decrypted key
     */
    public static byte[] unPadRSADecryptedSymmetricKey(byte[] paddedKey) throws IllegalArgumentException {
        // the first byte should be 02
        if (paddedKey[0] != 2) throw new IllegalArgumentException("paddedKey has wrong format");
        // traverse the next series of byte until we get to the first 00
        int pos = 0;
        for (pos = 0; pos < paddedKey.length; pos++) {
            if (paddedKey[pos] == 0) {
                break;
            }
        }
        // the remainder is the key to return
        int keylength = paddedKey.length - 1 - pos;
        if (keylength < 16) {
            throw new IllegalArgumentException("key length " + keylength + "is not a valid length");
        }
        byte[] output = new byte[keylength];
        System.arraycopy(paddedKey, pos+1, output, 0, keylength);
        return output;
    }

    /**
     * Checks if the specified EncryptedType's KeyInfo is addressed to the specified recipient certificate.
     * @param encryptedType the EncryptedKey or EncryptedData element.  Must include a KeyInfo child.
     * @param recipientCert
     * @throws InvalidDocumentFormatException  if there was a problem with the encryptedType, or the KeyInfo didn't match.
     * @throws GeneralSecurityException        if there was a problem with the recipient certificate or a certificate
     *                                         embedded within the encryptedType.
     */
    public static void checkKeyInfo(Element encryptedType, X509Certificate recipientCert)
            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        Element kinfo = XmlUtil.findOnlyOneChildElementByName(encryptedType, SoapUtil.DIGSIG_URI, SoapUtil.KINFO_EL_NAME);
        if (kinfo == null) throw new InvalidDocumentFormatException(encryptedType.getLocalName() + " includes no KeyInfo element");
        Element str = XmlUtil.findOnlyOneChildElementByName(kinfo,
                                                            SoapUtil.SECURITY_URIS_ARRAY,
                                                            SoapUtil.SECURITYTOKENREFERENCE_EL_NAME);
        if (str == null) throw new InvalidDocumentFormatException(encryptedType.getLocalName() + "'s KeyInfo includes no SecurityTokenReference");
        Element ki = XmlUtil.findOnlyOneChildElementByName(str,
                                                           SoapUtil.SECURITY_URIS_ARRAY,
                                                           SoapUtil.KEYIDENTIFIER_EL_NAME);
        if (ki == null) throw new InvalidDocumentFormatException(encryptedType.getLocalName() + "'s KeyInfo's SecurityTokenReference includes no KeyIdentifier element");
        String valueType = ki.getAttribute("ValueType");
        String keyIdentifierValue = XmlUtil.getTextValue(ki);
        byte[] keyIdValueBytes = new byte[0];
        try {
            keyIdValueBytes = HexUtils.decodeBase64(keyIdentifierValue, true);
        } catch (IOException e) {
            throw new InvalidDocumentFormatException("Unable to parse base64 Key Identifier", e);
        }
        if (keyIdValueBytes == null || keyIdValueBytes.length < 1) throw new InvalidDocumentFormatException(encryptedType.getLocalName() + "'s KeyIdentifier was empty");
        if (valueType == null || valueType.length() <= 0) {
            logger.fine("The KeyId Value Type is not specified. We will therefore assume it is a Subject Key Identifier.");
            valueType = SoapUtil.VALUETYPE_SKI;
        }
        if (valueType.endsWith(SoapUtil.VALUETYPE_SKI_SUFFIX)) {
            // If not typed, assume it's a ski
            byte[] ski = recipientCert.getExtensionValue(CertUtils.X509_OID_SUBJECTKEYID);
            if (ski == null) {
                // TODO try making up both RFC 3280 SKIs and see if they match (Bug #1001)
                //throw new CertificateException("Unable to verify that KeyInfo is addressed to us -- " +
                //                               "our own certificate does not contain a Subject Key Identifier.");
                // TODO should reject the message until we implement fix as above
                logger.log(Level.INFO, "Unable to verify that KeyInfo is addressed to us; skipping SKI check");
                return;
            } else {
                // trim if necessary
                byte[] ski2 = ski;
                if (ski.length > keyIdValueBytes.length) {
                    ski2 = new byte[keyIdValueBytes.length];
                    System.arraycopy(ski, ski.length-keyIdValueBytes.length,
                                     ski2, 0, keyIdValueBytes.length);
                }
                if (Arrays.equals(keyIdValueBytes, ski2)) {
                    logger.fine("the Key SKI is recognized. This key is for us for sure!");
                    /* FALLTHROUGH */
                } else {
                    String msg = "This " + encryptedType.getLocalName() + " has a KeyInfo that declares a specific SKI, " +
                            "but our certificate's SKI does not match.";
                    logger.warning(msg);
                    throw new GeneralSecurityException(msg);
                }
            }
        } else if (valueType.endsWith(SoapUtil.VALUETYPE_X509_SUFFIX)) {
            // It seems to be a complete certificate
            X509Certificate referencedCert = CertUtils.decodeCert(keyIdValueBytes);
            if (CertUtils.certsAreEqual(recipientCert, referencedCert)) {
                logger.fine("The Key recipient cert is recognized");
                /* FALLTHROUGH */

            } else {
                String msg = "This " + encryptedType.getLocalName() + " has a KeyInfo that declares a specific cert, " +
                        "but our certificate does not match.";
                logger.warning(msg);
                throw new GeneralSecurityException(msg);
            }
        } else
            throw new InvalidDocumentFormatException("The EncryptedKey's KeyInfo uses an unsupported " +
                                                     "ValueType: " + valueType);
    }

    /**
     * Verify that the specified EncryptedType has a supported EncryptionMethod (currently RSA1_5 only)
     * @param encryptedType the EncryptedKey or EncryptedData to check
     * @throws com.l7tech.common.xml.InvalidDocumentFormatException if the specified algorithm is not supported
     */
    public static void checkEncryptionMethod(Element encryptedType) throws InvalidDocumentFormatException {
        Element encryptionMethodEl = XmlUtil.findOnlyOneChildElementByName(encryptedType,
                                                                           SoapUtil.XMLENC_NS,
                                                                           "EncryptionMethod");
        if (encryptionMethodEl != null) {
            String encMethodValue = encryptionMethodEl.getAttribute("Algorithm");
            if (encMethodValue == null || encMethodValue.length() < 1) {
                throw new InvalidDocumentFormatException("Algorithm not specified in EncryptionMethod element");
            } else if (!encMethodValue.equals(SoapUtil.SUPPORTED_ENCRYPTEDKEY_ALGO)) {
                throw new InvalidDocumentFormatException("Algorithm not supported " + encMethodValue);
            }
        }
    }

    /**
     * Extract the encrypted key from the specified EncryptedKey element.  Caller is responsible for ensuring that
     * this encrypted key is in fact addressed to the specified recipientKey, and that the algorithm is supported
     * (perhaps by calling checkEncryptionMethod() and checkKeyInfo()).
     * @param encryptedKeyElement  the EncryptedKey element to decrypt
     * @param recipientKey         the private key to use to decrypt the element
     * @return the decrypted key.  Will never be null.
     * @throws com.l7tech.common.xml.InvalidDocumentFormatException  if there is a problem interpreting the EncryptedKey.
     * @throws java.security.GeneralSecurityException if there was a crypto problem
     */
    public static byte[] decryptKey(Element encryptedKeyElement, PrivateKey recipientKey)
            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        // get the xenc:CipherValue
        Element cipherValue = null;
        Element cipherData = XmlUtil.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                   SoapUtil.XMLENC_NS,
                                                                   "CipherData");
        if (cipherData != null)
            cipherValue = XmlUtil.findOnlyOneChildElementByName(cipherData, SoapUtil.XMLENC_NS, "CipherValue");
        if (cipherValue == null)
            throw new InvalidDocumentFormatException(encryptedKeyElement.getLocalName() + " is missing CipherValue element");
        // we got the value, decrypt it
        String value = XmlUtil.getTextValue(cipherValue);
        byte[] encryptedKeyBytes = new byte[0];
        try {
            encryptedKeyBytes = HexUtils.decodeBase64(value, true);
        } catch (IOException e) {
            throw new InvalidDocumentFormatException("Unable to parse base64 EncryptedKey CipherValue", e);
        }
        Cipher rsa = null;
        rsa = Cipher.getInstance("RSA", JceProvider.getAsymmetricJceProvider());
        rsa.init(Cipher.DECRYPT_MODE, recipientKey);

        byte[] decryptedPadded = rsa.doFinal(encryptedKeyBytes);
        // unpad
        byte[] unencryptedKey = null;
        try {
            unencryptedKey = unPadRSADecryptedSymmetricKey(decryptedPadded);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "The key could not be unpadded", e);
            throw new InvalidDocumentFormatException(e);
        }
        return unencryptedKey;
    }

    /**
     * This handles the padding of the encryption method designated by http://www.w3.org/2001/04/xmlenc#rsa-1_5.
     *
     * Exceprt from the spec:
     * the padding is of the following special form:
     * 02 | PS* | 00 | key
     * where "|" is concatenation, "02" and "00" are fixed octets of the corresponding hexadecimal value, PS is
     * a string of strong pseudo-random octets [RANDOM] at least eight octets long, containing no zero octets,
     * and long enough that the value of the quantity being CRYPTed is one octet shorter than the RSA modulus,
     * and "key" is the key being transported. The key is 192 bits for TRIPLEDES and 128, 192, or 256 bits for
     * AES. Support of this key transport algorithm for transporting 192 bit keys is MANDATORY to implement.
     *
     * @param keyBytes the key to pad
     * @param modulusBytes the number of bytes in the RSA modulus in the private key you are going to use to encrypt keyBytes
     * @param rand a SecureRandom implementation to use for generating secure padding bytes
     * @return the padded key
     * @throws java.security.KeyException if there are too many keyBytes to fit inside this modulus
     */
    public static  byte[] padSymmetricKeyForRsaEncryption(byte[] keyBytes, int modulusBytes, SecureRandom rand)
            throws KeyException
    {
        modulusBytes-=1;
        int padbytes = modulusBytes - 3 - keyBytes.length;

        // Check just in case, although this should never happen in real life
        if (padbytes < 8)
            throw new KeyException("Recipient RSA public key has too few bits to encode this symmetric key");

        byte[] padded = new byte[modulusBytes - 1];
        int pos = 0;
        padded[pos++] = 2;
        while (padbytes > 0) {
            padded[pos++] = (byte)(rand.nextInt(255) + 1);
            padbytes--;
        }
        padded[pos++] = 0;
        System.arraycopy(keyBytes, 0, padded, pos, keyBytes.length);
        return padded;
    }

    /**
     * Takes the symmetric key bytes and encrypts it for a recipient's provided the recipient's public key.
     * The encrypted key is then padded according to http://www.w3.org/2001/04/xmlenc#rsa-1_5 and then base64ed.
     * @param keyBytes the bytes of the symmetric key to encrypt
     * @param publicKey the public key of the recipient of the key
     * @return
     * @throws GeneralSecurityException
     */
    public static String encryptKeyWithRsaAndPad(byte[] keyBytes, PublicKey publicKey, SecureRandom rand) throws GeneralSecurityException {
        Cipher rsa = Cipher.getInstance("RSA", JceProvider.getAsymmetricJceProvider());
        rsa.init(Cipher.ENCRYPT_MODE, publicKey);
        if (!(publicKey instanceof RSAPublicKey))
            throw new KeyException("Unable to encrypt -- unsupported recipient public key type " +
                                   publicKey.getClass().getName());
        final int modulusLength = ((RSAPublicKey)publicKey).getModulus().toByteArray().length;
        byte[] paddedKeyBytes = XencUtil.padSymmetricKeyForRsaEncryption(keyBytes, modulusLength, rand);
        byte[] encrypted = rsa.doFinal(paddedKeyBytes);
        return HexUtils.encodeBase64(encrypted, true);
    }

}
