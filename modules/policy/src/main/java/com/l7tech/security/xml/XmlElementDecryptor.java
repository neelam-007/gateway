package com.l7tech.security.xml;

import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.type.EncryptedData;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class that can perform non-SOAP XML decryption of an element.  The decryption behavior is currently
 * not configurable -- it will always assume that the encrypted type includes an embedded KeyInfo which
 * can be unwrapped using a provided SecurityTokenResolver.
 */
public class XmlElementDecryptor {
    /**
     * Unwrap a decryption key and use it to decrypt an encrypted element and replace it with its plaintext contents.
     * <p/>
     * This method assumes that the encrypted element contains an embedded KeyInfo which can be unwrapped
     * (using the specified SecurityTokenResolver) in order to produce the decryption key.
     *
     * @param encryptedDataEl  the EncryptedElement or EncryptedData element to decrypt and replace.  Required.
     * @param securityTokenResolver  a security token resolver to use for locating keys for unwrapping the encrypted key.  Required.
     * @param onDecryptionError a listener which will be invoked if a decryption attempt fails.  Required.
     * @param keyInfoErrorListener  a listener which will be invoked if a key info entry is skipped over because it cannot be understood.  May be null.
     * Implementations do not need to treat these events as errors as this method will throw if the secret key cannot be unwrapped.
     * Use to log / audit as possibly interesting information.
     * @return a Triple containing the symmetric encryption algorithm URI, the list of decrypted nodes (already added to the document), and the X509Certificate that was used
     *         to unwrap the symmetric encryption key.  Never null.
     * @throws com.l7tech.security.xml.UnexpectedKeyInfoException if the keyinfo did not match any known private key
     * @throws com.l7tech.util.InvalidDocumentFormatException if the EncryptedData did not contain a ds:KeyInfo; or,
     *                                        the KeyInfo did not contain exactly one xenc:EncryptedKey; or,
     *                                        the EncryptedKey did not contain exactly one xenc:EncryptionMethod; or,
     *                                        the EncryptedKey did not contain any ds:KeyInfo elements; or,
     *                                        one of the EncryptedKey KeyInfo elements could not be understood; or,
     *                                        the EncryptedKey's CipherValue element is missing; or,
     *                                        if an OAEPparams value is specified but it is invalid; or,
     *                                        if more than one encryption algorithm URI was specified.
     * @throws java.security.GeneralSecurityException if there is a problem with a certificate or key, or a required algorithm is unavailable,
     *                                  or there is an error performing the actual decryption.
     */
    public static Triple<String, NodeList, X509Certificate> unwrapDecryptAndReplaceElement(Element encryptedDataEl,
                                                                                           SecurityTokenResolver securityTokenResolver,
                                                                                           final Functions.UnaryVoid<Throwable> onDecryptionError,
                                                                                           @Nullable KeyInfoErrorListener keyInfoErrorListener)
            throws GeneralSecurityException, InvalidDocumentFormatException, UnexpectedKeyInfoException
    {
        Pair<X509Certificate, byte[]> decryptionInfo = unwrapSecretKey(encryptedDataEl, securityTokenResolver, keyInfoErrorListener);
        X509Certificate recipientCert = decryptionInfo.left;
        final byte[] decryptionSecretKeyBytes = decryptionInfo.right;
        Pair<String, NodeList> got = decryptAndReplace(encryptedDataEl, decryptionSecretKeyBytes, onDecryptionError);
        return new Triple<String, NodeList, X509Certificate>(got.left, got.right, recipientCert);
    }

    /**
     * Decrypt an encrypted element using an already-prepared decryption key, and replace it with its decrypted contents.
     *
     * @param encryptedDataEl  the EncryptedElement or EncryptedData element to decrypt and replace.  Required.
     * @param decryptionSecretKeyBytes  the secret key bytes, already unwrapped.  Enough secret key bytes must be provided to create a key appropriate for the encryption algorithm used.  Required.
     * @param onDecryptionError a listener which will be invoked if a decryption attempt fails.  Required.
     * @return a Pair containing the symmetric encryption algorithm URI and the list of decrypted nodes (already added to the document).  Never null.
     * @throws com.l7tech.util.InvalidDocumentFormatException if more than one encryption algorithm URI was specified.
     * @throws java.security.GeneralSecurityException if there is a problem with a certificate or key, or a required algorithm is unavailable,
     *                                  or there is an error performing the actual decryption.
     */
    public static Pair<String, NodeList> decryptAndReplace(Element encryptedDataEl, byte[] decryptionSecretKeyBytes, Functions.UnaryVoid<Throwable> onDecryptionError)
            throws GeneralSecurityException, InvalidDocumentFormatException
    {
        final FlexKey flexKey = new FlexKey(decryptionSecretKeyBytes);

        // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
        // soapMsg document
        final List<String> algorithm = new ArrayList<String>();

        // override getEncryptionEngine to collect the encryptionmethod algorithm
        final XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory af =
                new XencUtil.EncryptionEngineAlgorithmCollectingAlgorithmFactory(flexKey, algorithm);

        // TODO we won't know the actual cipher until the EncryptionMethod is created, so we'll hope that the Provider will be the same for all symmetric crypto
        Provider symmetricProvider = JceProvider.getInstance().getProviderFor("Cipher.AES");
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        final DecryptionContext dc = XencUtil.createContextForDecryption( af );
        dc.setEncryptedType(encryptedDataEl, EncryptedData.ELEMENT, null, null);

        final NodeList decryptedNodes;
        try {
            decryptedNodes = XencUtil.decryptAndReplaceUsingKey(encryptedDataEl, flexKey, dc, onDecryptionError);
        } catch (XencUtil.XencException e) {
            GeneralSecurityException t = new GeneralSecurityException("Error decrypting", e);
            onDecryptionError.call(t);
            throw t;
        }

        // determine algorithm
        String algorithmName = XencAlgorithm.AES_128_CBC.getXEncName();
        if (!algorithm.isEmpty()) {
            if (algorithm.size() > 1)
                throw new InvalidDocumentFormatException("Multiple encryption algorithms found in element " + encryptedDataEl.getNodeName());
            algorithmName = algorithm.iterator().next();
        }

        return new Pair<String, NodeList>(algorithmName, decryptedNodes);
    }

    /**
     * Unwrap a secret key from an EncryptedData assumed to contain an embedded EncryptedKey addressed to a private key
     * known to our SecurityTokenResolver.
     *
     * @param outerEncryptedDataEl  an EncryptedData element expected to contain a KeyInfo with an embedded EncryptedKey whose KeyInfo
     *                         is addressed to a private key known to securityTokenResolver.  Required.
     * @param securityTokenResolver  resolver used to locate a private key for decryption.  Required.
     * @param keyInfoErrorListener invoked if a keyinfo entry is passed over because it cannot be resolved, or null to ignore this situation.
     * @return a FlexKey containing the key material from the EncryptedKey.  Never null.
     * @throws com.l7tech.security.xml.UnexpectedKeyInfoException if the keyinfo did not match any known private key
     * @throws com.l7tech.util.InvalidDocumentFormatException if the EncryptedData did not contain a ds:KeyInfo; or,
     *                                        the KeyInfo did not contain exactly one xenc:EncryptedKey; or,
     *                                        the EncryptedKey did not contain exactly one xenc:EncryptionMethod; or,
     *                                        the EncryptedKey did not contain any ds:KeyInfo elements; or,
     *                                        one of the EncryptedKey KeyInfo elements could not be understood; or,
     *                                        the EncryptedKey's CipherValue element is missing; or,
     *                                        if an OAEPparams value is specified but it is invalid.
     * @throws java.security.GeneralSecurityException if there is a problem with a certificate or key, or a required algorithm is unavailable,
     *                                  or there is an error performing the actual decryption.
     */
    public static Pair<X509Certificate, byte[]> unwrapSecretKey(Element outerEncryptedDataEl, SecurityTokenResolver securityTokenResolver, @Nullable KeyInfoErrorListener keyInfoErrorListener)
            throws UnexpectedKeyInfoException, InvalidDocumentFormatException, GeneralSecurityException
    {
        Element keyInfo = XmlUtil.findExactlyOneChildElementByName(outerEncryptedDataEl, SoapUtil.DIGSIG_URI, "KeyInfo");
        Element encryptedKey = XmlUtil.findExactlyOneChildElementByName(keyInfo, SoapUtil.XMLENC_NS, "EncryptedKey");
        Element encMethod = DomUtils.findOnlyOneChildElementByName(encryptedKey, SoapConstants.XMLENC_NS, "EncryptionMethod");
        SignerInfo signerInfo = findSignerInfoForEncryptedType(encryptedKey, securityTokenResolver, keyInfoErrorListener);
        String cipherValueB64 = XencUtil.getEncryptedKeyCipherValue(encryptedKey);
        byte[] encryptedKeyBytes = HexUtils.decodeBase64(cipherValueB64.trim());
        byte[] secretKeyBytes = XencUtil.decryptKey(encryptedKeyBytes, XencUtil.getOaepBytes(encMethod), signerInfo.getPrivate());
        // Support "flexible" answers to getAlgorithm() query when using 3des with HSM (Bug #3705)
        return new Pair<X509Certificate, byte[]>(signerInfo.getCertificate(), secretKeyBytes);
    }

    static SignerInfo findSignerInfoForEncryptedType(Element encryptedType, SecurityTokenResolver securityTokenResolver, @Nullable KeyInfoErrorListener keyInfoErrorListener)
            throws InvalidDocumentFormatException, CertificateException
    {
        List<Element> keyInfos = DomUtils.findChildElementsByName(encryptedType, SoapConstants.DIGSIG_URI, SoapConstants.KINFO_EL_NAME);
        if (keyInfos == null || keyInfos.size() < 1)
            throw new InvalidDocumentFormatException(encryptedType.getLocalName() + " includes no KeyInfo element");
        boolean sawSupportedFormat = false;
        for (Element keyInfo : keyInfos) {
            try {
                Element x509Data = DomUtils.findOnlyOneChildElementByName(keyInfo, SoapConstants.DIGSIG_URI, "X509Data");
                if (x509Data == null)
                    throw new KeyInfoElement.UnsupportedKeyInfoFormatException("KeyInfo did not contain any recognized certificate reference format");

                SignerInfo signerInfo = handleX509Data(x509Data, securityTokenResolver);
                if (signerInfo != null)
                    return signerInfo;

                sawSupportedFormat = true;

            } catch (KeyInfoElement.UnsupportedKeyInfoFormatException e) {
                if (keyInfoErrorListener != null)
                    keyInfoErrorListener.onUnsupportedKeyInfoFormat(e);
            } catch (InvalidDocumentFormatException e) {
                if (keyInfoErrorListener != null)
                    keyInfoErrorListener.onInvalidDocumentFormat(e);
            }
        }
        if (sawSupportedFormat)
            throw new InvalidDocumentFormatException("Encryption recipient was not recognized as addressed to a private key possessed by this Gateway");
        else
            throw new InvalidDocumentFormatException("EncryptedType did not contain a KeyInfo in a supported format");
    }

    static SignerInfo handleX509Data(Element x509Data, SecurityTokenResolver securityTokenResolver) throws CertificateException, InvalidDocumentFormatException {
        // Use X509Data
        Element x509CertEl = DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, "X509Certificate");
        Element x509SkiEl = DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, "X509SKI");
        Element x509IssuerSerialEl = DomUtils.findOnlyOneChildElementByName(x509Data, SoapConstants.DIGSIG_URI, "X509IssuerSerial");
        if (x509CertEl != null) {
            String certBase64 = DomUtils.getTextValue(x509CertEl);
            byte[] certBytes = HexUtils.decodeBase64(certBase64, true);
            return securityTokenResolver.lookupPrivateKeyByCert(CertUtils.decodeCert(certBytes));
        } else if (x509SkiEl != null) {
            String skiRaw = DomUtils.getTextValue(x509SkiEl);
            String ski = HexUtils.encodeBase64(HexUtils.decodeBase64(skiRaw, true), true);
            return securityTokenResolver.lookupPrivateKeyBySki(ski);
        } else if (x509IssuerSerialEl != null) {
            final Element issuerEl = DomUtils.findExactlyOneChildElementByName(x509IssuerSerialEl, SoapConstants.DIGSIG_URI, "X509IssuerName");
            final Element serialEl = DomUtils.findExactlyOneChildElementByName(x509IssuerSerialEl, SoapConstants.DIGSIG_URI, "X509SerialNumber");

            final String issuerVal = DomUtils.getTextValue(issuerEl);
            if (issuerVal.length() == 0) throw new MissingRequiredElementException("X509IssuerName was empty");
            final String serialVal = DomUtils.getTextValue(serialEl);
            if (serialVal.length() == 0) throw new MissingRequiredElementException("X509SerialNumber was empty");
            return securityTokenResolver.lookupPrivateKeyByIssuerAndSerial(new X500Principal(issuerVal), new BigInteger(serialVal));
        } else {
            throw new KeyInfoElement.UnsupportedKeyInfoFormatException("KeyInfo X509Data was not in a supported format");
        }
    }

    public interface KeyInfoErrorListener {
        void onUnsupportedKeyInfoFormat(KeyInfoElement.UnsupportedKeyInfoFormatException e);
        void onInvalidDocumentFormat(InvalidDocumentFormatException e);
    }
}
