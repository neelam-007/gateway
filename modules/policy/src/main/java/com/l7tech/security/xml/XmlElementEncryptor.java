package com.l7tech.security.xml;

import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.EncryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.CipherData;
import com.ibm.xml.enc.type.CipherValue;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.*;
import com.l7tech.xml.soap.SoapUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Utility class that can perform non-SOAP XML encryption of an element, using an XmlElementEncryptionConfig.
 */
public class XmlElementEncryptor {
    private static final SecureRandom random = new SecureRandom();

    private final X509Certificate recipientCert;
    private final String xencAlgorithm;

    /**
     * Create an encryptor that will encrypt elements according to the specified configuration.
     *
     * @param config the configuration to use.  Required.
     * @throws IOException if there is a problem decoding the recipient certificate.
     * @throws CertificateException if there is a problem decoding the recipient certificate.
     * @throws NoSuchAlgorithmException if the specified encryption algorithm URI is missing or unrecognized.
     */
    public XmlElementEncryptor(@NotNull XmlElementEncryptionConfig config) throws IOException, CertificateException, NoSuchAlgorithmException {
        this.recipientCert = CertUtils.decodeFromPEM(config.getRecipientCertificateBase64(), false);
        this.xencAlgorithm = config.getXencAlgorithm();
        XencUtil.getFlexKeyAlg(xencAlgorithm);
    }

    /**
     * Create a new EncryptedKey element using the specified document factory.  A new ephemeral key will be generated
     * for use according to the current element encryption config.  The new key will be wrapped for the recipient
     * certificate in the current encryption config.
     * <p/>
     * The generated EncryptedKey element will contain the generated ephemeral key as encrypted CipherData,
     * and will include an embedded KeyInfo identifying the recipient certificate (using an X509Data issuer/serial reference).
     *
     * @param factory a DOM document to use as a factory for new DOM nodes.  Required.
     * @return a Pair consisting of the new EncryptedKey DOM Element and the new SecretKey that was generated for it.  Never null, and neither element is ever null.
     * @throws GeneralSecurityException if there is an error performing the encryption.
     */
    public Pair<Element, SecretKey> createEncryptedKey(@NotNull Document factory) throws GeneralSecurityException {
        final String xencNs = SoapUtil.XMLENC_NS;
        final String xenc = "xenc";
        byte[] keybytes = new byte[32];
        random.nextBytes(keybytes);
        XencUtil.XmlEncKey xek = new XencUtil.XmlEncKey(xencAlgorithm, keybytes);
        Element encryptedKey = factory.createElementNS(xencNs, "xenc:EncryptedKey");
        encryptedKey.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:xenc", xencNs);
        Element encryptionMethod = DomUtils.createAndAppendElementNS(encryptedKey, "EncryptionMethod", xencNs, xenc);
        IssuerSerialKeyInfoDetails iskid = new IssuerSerialKeyInfoDetails(recipientCert, false);
        iskid.createAndAppendKeyInfoElement(new NamespaceFactory(), encryptedKey);
        Element cipherData = DomUtils.createAndAppendElementNS(encryptedKey, "CipherData", xencNs, xenc);
        Element cipherValue = DomUtils.createAndAppendElementNS(cipherData, "CipherValue", xencNs, xenc);
        final SecretKey secretKey = xek.getSecretKey();
        final byte[] encryptedKeyBytes;
        encryptionMethod.setAttribute("Algorithm", SoapConstants.SUPPORTED_ENCRYPTEDKEY_ALGO);
        encryptedKeyBytes = XencUtil.encryptKeyWithRsaAndPad(secretKey.getEncoded(), recipientCert, recipientCert.getPublicKey());
        final String base64 = HexUtils.encodeBase64(encryptedKeyBytes, true);
        cipherValue.appendChild(DomUtils.createTextNode(factory, base64));
        return new Pair<Element, SecretKey>(encryptedKey, secretKey);
    }

    /**
     * Encrypt the specified DOM element according to the current element encryption config and using the specified
     * encrypted key.
     * <p/>
     * The target element will be removed from the document and a new EncryptedData element will be left in its place.
     * This EncryptedData will include a KeyInfo that includes the specified EncryptedKey element.
     * Encryption will be performed using the specified SecretKey and the algorithm in the current encryption config.
     *
     * @param element the target element to encrypt.  Required.
     * @param encryptedKey an EncryptedKey element and corresponding SecretKey, such as that generated by @{link #createEncryptedKey}.  Required.
     *                     <b>Note:</b> the encrypted key element must belong to the same DOM document as the element to be encrypted.
     * @return the new EncryptedData element, already inserted into the target document.  Never null.
     * @throws StructureException if encryption cannot be performed due to the format of the target document or the specified EncryptedKey
     * @throws GeneralSecurityException if a cryptographic error occurs during the actual encryption
     * @throws IOException if an IOException occurs while performing encryption
     * @throws KeyInfoResolvingException if there is a problem with the format of the specified EncryptedKey
     */
    public Element encryptAndReplaceElement(@NotNull Element element, @NotNull Pair<Element, SecretKey> encryptedKey) throws StructureException, GeneralSecurityException, IOException, KeyInfoResolvingException {
        Document soapMsg = element.getOwnerDocument();
        final Element encryptedKeyElement = encryptedKey.left;
        final SecretKey secretKey = encryptedKey.right;

        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());

        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(xencAlgorithm);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setType(EncryptedData.ELEMENT);
        final Element encDataElement = encData.createElement(soapMsg, true);

        // Create encryption context and encrypt the header subtree
        EncryptionContext ec = new EncryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        // TODO we'll assume it's the same Provider for all symmetric crypto
        Provider symmetricProvider = JceProvider.getInstance().getProviderFor("Cipher.AES");
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        ec.setAlgorithmFactory(af);
        ec.setEncryptedType(encDataElement, EncryptedData.ELEMENT, null, null);

        ec.setData(element);
        ec.setKey(secretKey);

        ec.encrypt();
        ec.replace();

        Element encryptedData = ec.getEncryptedTypeAsElement();
        try {
            encryptedData = insertKeyInfoAndDeUglifyNamespaces(encryptedData, encryptedKeyElement, xencAlgorithm);
        } catch (InvalidDocumentFormatException e) {
            throw new RuntimeException("Internal error while encrypting element: " + ExceptionUtils.getMessage(e), e); // can't happen, we just generated that EncryptedData ourselves
        }
        return encryptedData;
    }

    private static Element insertKeyInfoAndDeUglifyNamespaces(Element ibmEncData, Element encryptedKey, String xencAlgorithm) throws InvalidDocumentFormatException {
        final String xencNs = SoapUtil.XMLENC_NS;
        final String xenc = "xenc";
        Element encryptedData = ibmEncData.getOwnerDocument().createElementNS(xencNs, xenc + ":EncryptedData");
        encryptedData.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:xenc", xencNs);
        ibmEncData.getParentNode().replaceChild(encryptedData, ibmEncData);
        Element encryptionMethod = XmlUtil.createAndAppendElementNS(encryptedData, "EncryptionMethod", xencNs, xenc);
        encryptionMethod.setAttribute("Algorithm", xencAlgorithm);
        Element keyInfo = XmlUtil.createAndAppendElementNS(encryptedData, "KeyInfo", SoapUtil.DIGSIG_URI, "dsig");
        keyInfo.appendChild(encryptedData.getOwnerDocument().importNode(encryptedKey, true));
        Element cipherData = XmlUtil.createAndAppendElementNS(encryptedData, "CipherData", xencNs, xenc);
        Element cipherValue = XmlUtil.createAndAppendElementNS(cipherData, "CipherValue", xencNs, xenc);

        Element ibmCipherData = XmlUtil.findExactlyOneChildElementByName(ibmEncData, xencNs, "CipherData");
        Element ibmCipherValue = XmlUtil.findExactlyOneChildElementByName(ibmCipherData, xencNs, "CipherValue");
        String cipherValueText = XmlUtil.getTextValue(ibmCipherValue);
        XmlUtil.setTextContent(cipherValue, cipherValueText);
        return encryptedData;
    }
}
