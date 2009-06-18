/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.xml;

import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.EncryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.CipherData;
import com.ibm.xml.enc.type.CipherValue;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.DomUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.SoapConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.io.IOException;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Utility methods to support XML Encryption, specifically EncryptedKey elements.
 */
public class XencUtil {
    private static final Logger logger = Logger.getLogger(XencUtil.class.getName());
    public static final String TRIPLE_DES_CBC = "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";
    public static final String AES_128_CBC = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    public static final String AES_192_CBC = "http://www.w3.org/2001/04/xmlenc#aes192-cbc";
    public static final String AES_256_CBC = "http://www.w3.org/2001/04/xmlenc#aes256-cbc";

    public static class XencException extends Exception {
        public XencException() {}
        public XencException(String message) { super(message); }
        public XencException(String message, Throwable cause) { super(message, cause); }
        public XencException(Throwable cause) { super(cause); }
    }

    /**
     * Get the EncryptedKeySHA1 identifier string for the given encrypted secret key bytes.  Note that this
     * requires the encrypted bytes of the key as encoded in a particular EncryptedKey element -- knowing just the
     * SecretKey alone is not sufficient to produce the EncryptedKeySHA1 reference.
     *
     * @param encryptedKeyBytes the un-Base64'ed octets of the CipherData in the EncryptedKey being referenced.
     *                          Must not be null or empty.
     * @return the Base64'ed SHA1 hash of the encrypted key bytes, ready to include in a KeyInfo reference
     *         of type EncryptedKeySHA1.
     */
    public static String computeEncryptedKeySha1(byte[] encryptedKeyBytes) {
        final byte[] secretKeyDigest = HexUtils.getSha1Digest(encryptedKeyBytes);
        return HexUtils.encodeBase64(secretKeyDigest, true);
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
     * @param paddedKey the decrpted but still padded key
     * @return the unpadded decrypted key
     * @throws IllegalArgumentException  if padded key has wrong format
     */
    public static byte[] unPadRSADecryptedSymmetricKey(byte[] paddedKey) throws IllegalArgumentException {
        int pos = 0;
        // the first byte should be 02
        if (paddedKey[0] != 2) {
            // note, certain providers insist in appending a 0 byte in front.
            // we will honor this but might not be portable
            if (paddedKey[1] == 2) {
                logger.warning("Padding for received EncryptedKey not following to xmlenc spec. Accepting anyway.");
                pos = 1;
            } else {
                throw new IllegalArgumentException("paddedKey has wrong format");
            }
        }
        // traverse the next series of byte until we get to the first 00
        for (; pos < paddedKey.length; pos++) {
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
     * Encrypt the specified element.  Returns the new EncryptedData element.
     *
     * @param element  the element to encrypt.  Required.
     * @param encKey  with the algorithm and the key
     *                The encryption algorithm is one of (http://www.w3.org/2001/04/xmlenc#aes128-cbc,
     *                http://www.w3.org/2001/04/xmlenc#tripledes-cbc, etc)
     * @return the EncryptedData element that replaces the specified element.
     * @throws java.security.GeneralSecurityException if there is a problem encrypting the element
     * @throws XencUtil.XencException if there is a problem encrypting the element
     */
    public static Element encryptElement(Element element, XmlEncKey encKey)
      throws XencException, GeneralSecurityException
    {

        Document soapMsg = element.getOwnerDocument();

        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());

        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(encKey.algorithmUri);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setType(EncryptedData.CONTENT);
        final Element encDataElement;
        try {
            encDataElement = encData.createElement(soapMsg, true);
        } catch (StructureException e) {
            throw new XencException(e);
        }

        // Create encryption context and encrypt the header subtree
        EncryptionContext ec = new EncryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        Provider symmetricProvider = JceProvider.getInstance().getSymmetricProvider();
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        ec.setAlgorithmFactory(af);
        ec.setEncryptedType(encDataElement, EncryptedData.CONTENT, null, null);

        ec.setData(element);
        ec.setKey(encKey.getSecretKey());

        try {
            ec.encrypt();
            ec.replace();
        } catch (KeyInfoResolvingException e) {
            throw new XencException(e); // can't happen
        } catch (StructureException e) {
            throw new XencException(e); // shouldn't happen
        } catch (IOException e) {
            throw new XencException(e); // shouldn't happen
        }

        return ec.getEncryptedTypeAsElement();
    }

    /**
     * Verify that the specified EncryptedType has a supported EncryptionMethod (currently RSA1_5 or rsa-oaep-mgf1p)
     * @param encryptedType the EncryptedKey or EncryptedData to check
     * @return The encryption algorithm
     * @throws InvalidDocumentFormatException if the specified algorithm is not supported
     */
    public static String checkEncryptionMethod(Element encryptedType) throws InvalidDocumentFormatException {
        Element encryptionMethodEl = DomUtils.findOnlyOneChildElementByName(encryptedType,
                                                                           SoapConstants.XMLENC_NS,
                                                                           "EncryptionMethod");

        if (encryptionMethodEl == null) {
            throw new InvalidDocumentFormatException("Missing EncryptionMethod element (or incorrect namespace).");
        }

        String encMethodValue = encryptionMethodEl.getAttribute("Algorithm");
        Element optionalDigestEle = DomUtils.findOnlyOneChildElementByName(encryptionMethodEl,
                                                                          SoapConstants.DIGSIG_URI,
                                                                          "DigestMethod");

        String digestAlgo = null;
        if (optionalDigestEle != null && optionalDigestEle.hasAttribute("Algorithm")) {
            digestAlgo = optionalDigestEle.getAttribute("Algorithm");
        }

        if (encMethodValue == null || encMethodValue.length() < 1) {
            throw new InvalidDocumentFormatException("Algorithm not specified in EncryptionMethod element");
        }

        if (encMethodValue.equals( SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO)) {
            return encMethodValue;
        } else if(encMethodValue.equals( SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2)) {
            if (digestAlgo==null || ( SoapConstants.DIGSIG_URI+"sha1").equals(digestAlgo)) {
                return encMethodValue;
            }
        }

        throw new InvalidDocumentFormatException("Algorithm not supported " + encMethodValue +
                (digestAlgo == null ? "" : (" with DigestMethod Algorithm " + digestAlgo)));
    }

    /**
     * Extract the encrypted key from the specified EncryptedKey element.  Caller is responsible for ensuring that
     * this encrypted key is in fact addressed to the specified recipientKey, and that the algorithm is supported
     * (perhaps by calling checkEncryptionMethod() and checkKeyInfo()).
     *
     * @param encryptedKeyElement  the EncryptedKey element to decrypt
     * @param recipientKey         the private key to use to decrypt the element
     * @return the decrypted key bytes.  Will never be null.
     * @throws InvalidDocumentFormatException  if there is a problem interpreting the EncryptedKey.
     * @throws java.security.GeneralSecurityException if there was a crypto problem
     */
    public static byte[] decryptKey(Element encryptedKeyElement, PrivateKey recipientKey)
            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        String value = getEncryptedKeyCipherValue(encryptedKeyElement);
        return decryptKey(encryptedKeyElement, recipientKey, value);
    }


    /**
     * Extract the encrypted key from the specified EncryptedKey element.  Caller is responsible for ensuring that
     * this encrypted key is in fact addressed to the specified recipientKey, and that the algorithm is supported
     * (perhaps by calling checkEncryptionMethod() and checkKeyInfo()).
     *
     * @param encryptedKeyElement  the EncryptedKey element to decrypt
     * @param recipientKey         the private key to use to decrypt the element
     * @param cipherValueB64       the already-extracted base 64'ed cipher value.  Must not be null.
     * @return the decrypted key bytes.  Will never be null.
     * @throws InvalidDocumentFormatException  if there is a problem interpreting the EncryptedKey.
     * @throws java.security.GeneralSecurityException if there was a crypto problem
     */
    public static byte[] decryptKey(Element encryptedKeyElement, PrivateKey recipientKey, String cipherValueB64)
            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        // get the Algorithm / Params
        Element encryptionMethodEl = DomUtils.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                           SoapConstants.XMLENC_NS,
                                                                           "EncryptionMethod");

        // we got the value, decrypt it
        return decryptKey(cipherValueB64, getOaepBytes(encryptionMethodEl), recipientKey);
    }


    /**
     * Extract the base 64'ed CipherValue from the specified EncryptedKey element.
     *
     * @param encryptedKeyElement the EncryptedKey element to examine.  Must not be null.
     * @return the base 64'ed CipherValue string.  Never null.
     * @throws InvalidDocumentFormatException if the CipherValue element is missing.
     */
    public static String getEncryptedKeyCipherValue(Element encryptedKeyElement) throws InvalidDocumentFormatException {
        // get the xenc:CipherValue
        Element cipherValue = null;
        Element cipherData = DomUtils.findOnlyOneChildElementByName(encryptedKeyElement,
                                                                   SoapConstants.XMLENC_NS,
                                                                   "CipherData");
        if (cipherData != null)
            cipherValue = DomUtils.findOnlyOneChildElementByName(cipherData, SoapConstants.XMLENC_NS, "CipherValue");
        if (cipherValue == null)
            throw new InvalidDocumentFormatException(encryptedKeyElement.getLocalName() + " is missing CipherValue element");
        return DomUtils.getTextValue(cipherValue);
    }


    /**
     * Get the RSA OAEP bytes for the specified EncryptedKey EncryptionMethod, if applicable.
     *
     * @param encryptionMethodEl   the EncryptionMethod element.  Must not be null.
     * @return If this encryption method calls for RSA-OAEP, this method returns a byte array containing the OAEP
     *         bytes.  This byte array may be empty.
     *         If this encryption method does not call for RSA-OAEP, this method returns null.
     * @throws InvalidDocumentFormatException if an OAEPparams value is specified but it is invalid
     */
    public static byte[] getOaepBytes(Element encryptionMethodEl) throws InvalidDocumentFormatException {
        String encMethodValue = encryptionMethodEl.getAttribute("Algorithm");
        if (!SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO_2.equals(encMethodValue))
            return null;

        Element oaepParamsEle = DomUtils.findOnlyOneChildElementByName(encryptionMethodEl,
                                                                      SoapConstants.XMLENC_NS,
                                                                      "OAEPparams"); // not OAEPParams
        String oaepParams = oaepParamsEle == null ? null : DomUtils.getTextValue(oaepParamsEle);
        return oaepParams == null ? new byte[0] : HexUtils.decodeBase64(oaepParams);
    }


    /**
     * Caller is responsible for ensuring that
     * this encrypted key is in fact addressed to the specified recipientKey, and that the algorithm is supported
     * (perhaps by calling checkEncryptionMethod() and checkKeyInfo()).
     *
     * @param b64edEncryptedKey    the base64ed EncryptedKey value
     * @param recipientKey         the private key to use to decrypt the element
     * @return the decrypted key bytes.  Will never be null.
     * @throws InvalidDocumentFormatException  if there is a problem interpreting the EncryptedKey.
     * @throws java.security.GeneralSecurityException if there was a crypto problem
     */
    public static byte[] decryptKey(String b64edEncryptedKey, PrivateKey recipientKey)
            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        return decryptKey(b64edEncryptedKey, null, recipientKey);
    }

    /**
     * Caller is responsible for ensuring that
     * this encrypted key is in fact addressed to the specified recipientKey, and that the algorithm is supported
     * (perhaps by calling checkEncryptionMethod() and checkKeyInfo()).
     *
     * @param b64edEncryptedKey    the base64ed EncryptedKey value
     * @param oaepParams     optional param when using oaep (may be empty), or null to use RSA1.5 instead of OAEP
     * @param recipientKey         the private key to use to decrypt the element
     * @return the decrypted key bytes.  Will never be null.
     * @throws InvalidDocumentFormatException  if there is a problem interpreting the EncryptedKey.
     * @throws java.security.GeneralSecurityException if there was a crypto problem
     */
    public static byte[] decryptKey(String b64edEncryptedKey, byte[] oaepParams, PrivateKey recipientKey)
            throws InvalidDocumentFormatException, GeneralSecurityException
    {
        return decryptKey(HexUtils.decodeBase64(b64edEncryptedKey, true), oaepParams, recipientKey);
    }


    /**
     * Caller is responsible for ensuring that
     * this encrypted key is in fact addressed to the specified recipientKey, and that the algorithm is supported
     * (perhaps by calling checkEncryptionMethod() and checkKeyInfo()).
     *
     * @param encryptedKeyBytes    the already un-base64ed EncryptedKey value
     * @param oaepParams     optional param when using oaep (may be empty), or null to use RSA1.5 instead of OAEP
     * @param recipientKey         the private key to use to decrypt the element
     * @return the decrypted key bytes.  Will never be null or empty.
     * @throws java.security.GeneralSecurityException if there was a crypto problem
     */
    public static byte[] decryptKey(byte[] encryptedKeyBytes, byte[] oaepParams, PrivateKey recipientKey) throws GeneralSecurityException {
        final byte[] unencryptedKey;
        if(oaepParams != null) {
            // decrypt
            try {
                Cipher rsa = JceProvider.getInstance().getRsaOaepPaddingCipher();
                rsa.init(Cipher.DECRYPT_MODE, recipientKey, JDK5Dependent.buildOAEPMGF1SHA1ParameterSpec(oaepParams));
                unencryptedKey = rsa.doFinal(encryptedKeyBytes);
            }
            catch(NoClassDefFoundError ncdfe) {
                throw (GeneralSecurityException) new GeneralSecurityException("Platform support for OAEP not available.").initCause(ncdfe);
            }
        }
        else {
            // decrypt
            Cipher rsa = JceProvider.getInstance().getRsaPkcs1PaddingCipher();
            rsa.init(Cipher.DECRYPT_MODE, recipientKey);

            return rsa.doFinal(encryptedKeyBytes);
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
    public static  byte[] padSymmetricKeyForRsaEncryption(byte[] keyBytes, int modulusBytes, Random rand)
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
     * @param recipientCert   the certificate corresponding to the public key.  If null, this method will fail
     *                        unless key usage enforcement is globally disabled.
     * @param publicKey the public key of the recipient of the key
     * @return the padded and encrypted keyBytes for the passed publicKey recipient, ready to be base64 encoded
     * @throws GeneralSecurityException if the key wrapping fails
     */
    public static byte[] encryptKeyWithRsaAndPad(byte[] keyBytes, X509Certificate recipientCert, PublicKey publicKey) throws GeneralSecurityException {
        if (!(publicKey instanceof RSAPublicKey))
            throw new KeyException("Unable to encrypt -- unsupported recipient public key type " +
                                   publicKey.getClass().getName());
        KeyUsageChecker.requireActivityForKey(KeyUsageActivity.encryptXml, recipientCert, publicKey);
        Cipher rsa = JceProvider.getInstance().getRsaPkcs1PaddingCipher();
        rsa.init(Cipher.ENCRYPT_MODE, publicKey);
        return rsa.doFinal(keyBytes);
    }

    /**
     * Takes the symmetric key bytes and encrypts it for a recipient's provided the recipient's public key.
     * The encrypted key is then base64ed.
     * @param keyBytes the bytes of the symmetric key to encrypt
     * @param recipientCert the certificate corresponding to the public key.
     * @param publicKey the public key of the recipient of the key
     * @param oaepParams the OAEP mask generation arg (may be null)
     * @return the padded and encrypted keyBytes for the passed publicKey recipient, ready to be base64 encoded
     * @throws GeneralSecurityException if the key is not a valid RSA public key, a needed cipher is unavailable,
     *                                  or no support for OAEP is available.
     */
    public static byte[] encryptKeyWithRsaOaepMGF1SHA1(byte[] keyBytes, X509Certificate recipientCert, PublicKey publicKey, byte[] oaepParams) throws GeneralSecurityException {
        if (!(publicKey instanceof RSAPublicKey))
            throw new KeyException("Unable to encrypt -- unsupported recipient public key type " +
                                   publicKey.getClass().getName());

        Cipher rsa = JceProvider.getInstance().getRsaOaepPaddingCipher();
        try {
            KeyUsageChecker.requireActivityForKey(KeyUsageActivity.encryptXml, recipientCert, publicKey);
            rsa.init(Cipher.ENCRYPT_MODE, publicKey, JDK5Dependent.buildOAEPMGF1SHA1ParameterSpec(oaepParams));
        }
        catch(NoClassDefFoundError ncdfe) {
            throw (GeneralSecurityException) new GeneralSecurityException("Platform support for OAEP not available.").initCause(ncdfe);
        }
        return rsa.doFinal(keyBytes);
    }

    /**
     * Put JDK 1.5 stuff here to allow (degraded) runtime 1.4.x use (1.5 compilation required)
     */
    private static class JDK5Dependent {
        private static AlgorithmParameterSpec buildOAEPMGF1SHA1ParameterSpec(byte[] oaepParams) {
            PSource pSource = oaepParams==null ? PSource.PSpecified.DEFAULT : new PSource.PSpecified(oaepParams);
            return new OAEPParameterSpec("SHA-1", "MGF1", MGF1ParameterSpec.SHA1, pSource);
        }
    }

    /**
     * Get the FlexKey algorithm corresponding to the specified WSS algorithm URI.
     * This works for AES 128, AES 192, AES 256, and Triple-DES.
     *
     * @param algorithmUri the WSS algorithm URI.  Required.
     * @return the FlexKey algorithm corresponding to the specified WSS algorithm URI.  Never null.
     * @throws NoSuchAlgorithmException if the specified algorithm URI is unrecognized.
     */
    public static FlexKey.Alg getFlexKeyAlg(String algorithmUri) throws NoSuchAlgorithmException {
        if (EncryptionMethod.TRIPLEDES_CBC.equals(algorithmUri))
            return(FlexKey.TRIPLEDES);
        else if (EncryptionMethod.AES128_CBC.equals(algorithmUri))
            return(FlexKey.AES128);
        else if (EncryptionMethod.AES192_CBC.equals(algorithmUri))
            return(FlexKey.AES192);
        else if (EncryptionMethod.AES256_CBC.equals(algorithmUri))
            return(FlexKey.AES256);
        throw new NoSuchAlgorithmException("Unsupported WSS algorithm URI " + algorithmUri);
    }

    /**
     * Create a FlexKey already configured to use the specified key material and WSS algorithm URI.
     *
     * @param bytes  the key bytes.  Must meet minimum length for specified algorithm (ie, 16 bytes for AES 128).
     * @param algorithmUri  the WSS algorithm URI to use.  Required.
     * @return a FlexKey instance already configured to use the specified WSS algorithm.
     * @throws NoSuchAlgorithmException if there are not enough bytes for the specified algorithm, or the the algorithm
     *                                  is not recognized.
     */
    public static FlexKey makeFlexKey(byte[] bytes, String algorithmUri) throws NoSuchAlgorithmException {
        try {
            FlexKey flexKey = new FlexKey(bytes);
            flexKey.setAlgorithm(getFlexKeyAlg(algorithmUri));
            return flexKey;
        } catch (KeyException e) {
            throw new NoSuchAlgorithmException("Not enough key material to use algorithm " + algorithmUri, e);
        }
    }

    /**
     * Holds the secret key and the xml enc algorithm name
     */
    public static class XmlEncKey {
        final byte[] keyBytes;
        final String algorithmUri;
        private SecretKey sk;

        public XmlEncKey(String encryptionAlgorithm, byte[] secretKey) {
            this.algorithmUri = encryptionAlgorithm;
            this.keyBytes = secretKey;
        }

        public byte[] getSecretKeyBytes() {
            return keyBytes;
        }

        public synchronized SecretKey getSecretKey() throws NoSuchAlgorithmException {
            if (sk != null) return sk;
            return sk = makeFlexKey(keyBytes, algorithmUri);
        }

        public String getAlgorithm() {
            return algorithmUri;
        }
    }

}
