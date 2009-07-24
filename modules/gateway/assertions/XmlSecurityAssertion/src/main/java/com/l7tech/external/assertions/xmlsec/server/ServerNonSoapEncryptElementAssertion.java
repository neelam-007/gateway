package com.l7tech.external.assertions.xmlsec.server;

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
import com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.IssuerSerialKeyInfoDetails;
import com.l7tech.security.xml.XencUtil;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;

/**
 * Server side implementation of the XmlSecurityAssertion.
 *
 * @see com.l7tech.external.assertions.xmlsec.NonSoapEncryptElementAssertion
 */
public class ServerNonSoapEncryptElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapEncryptElementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNonSoapEncryptElementAssertion.class.getName());

    private static final SecureRandom random = new SecureRandom();

    private final X509Certificate recipientCert;

    public ServerNonSoapEncryptElementAssertion(NonSoapEncryptElementAssertion assertion, ApplicationContext context)
            throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException
    {
        super(assertion, "encrypt", logger, context, context);
        this.recipientCert = CertUtils.decodeFromPEM(assertion.getRecipientCertificateBase64(), false);
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> elementsToEncrypt) throws Exception {
        Pair<Element, SecretKey> ek = createEncryptedKey(doc, recipientCert);

        for (Element element : elementsToEncrypt)
            encryptAndReplaceElement(element, ek.left, ek.right);
        return AssertionStatus.NONE;
    }

    private Pair<Element, SecretKey> createEncryptedKey(Document factory, X509Certificate recipientCert) throws GeneralSecurityException, InvalidDocumentFormatException {
        final String xencNs = SoapUtil.XMLENC_NS;
        final String xenc = "xenc";
        byte[] keybytes = new byte[32];
        random.nextBytes(keybytes);
        XencUtil.XmlEncKey xek = new XencUtil.XmlEncKey(assertion.getXencAlgorithm(), keybytes);
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

    private Element encryptAndReplaceElement(Element element, Element encryptedKey, SecretKey secretKey) throws StructureException, GeneralSecurityException, IOException, KeyInfoResolvingException, InvalidDocumentFormatException {
        Document soapMsg = element.getOwnerDocument();

        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());

        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(assertion.getXencAlgorithm());
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
        encryptedData = insertKeyInfoAndDeUglifyNamespaces(encryptedData, encryptedKey);
        return encryptedData;
    }

    private Element insertKeyInfoAndDeUglifyNamespaces(Element ibmEncData, Element encryptedKey) throws InvalidDocumentFormatException {
        final String xencNs = SoapUtil.XMLENC_NS;
        final String xenc = "xenc";
        Element encryptedData = ibmEncData.getOwnerDocument().createElementNS(xencNs, xenc + ":EncryptedData");
        encryptedData.setAttributeNS(XmlUtil.XMLNS_NS, "xmlns:xenc", xencNs);
        ibmEncData.getParentNode().replaceChild(encryptedData, ibmEncData);
        Element encryptionMethod = XmlUtil.createAndAppendElementNS(encryptedData, "EncryptionMethod", xencNs, xenc);
        encryptionMethod.setAttribute("Algorithm", assertion.getXencAlgorithm());
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
