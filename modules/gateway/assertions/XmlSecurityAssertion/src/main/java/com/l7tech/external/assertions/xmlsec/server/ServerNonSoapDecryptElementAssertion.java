package com.l7tech.external.assertions.xmlsec.server;

import com.ibm.xml.dsig.XSignatureException;
import com.ibm.xml.enc.*;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.security.keys.FlexKey;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.*;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Server side implementation of the NonSoapDecryptElementAssertion.
 *
 * @see com.l7tech.external.assertions.xmlsec.NonSoapDecryptElementAssertion
 */
public class ServerNonSoapDecryptElementAssertion extends ServerNonSoapSecurityAssertion<NonSoapDecryptElementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerNonSoapDecryptElementAssertion.class.getName());

    private final SecurityTokenResolver securityTokenResolver;

    public ServerNonSoapDecryptElementAssertion(NonSoapDecryptElementAssertion assertion, BeanFactory beanFactory, ApplicationEventPublisher eventPub)
            throws PolicyAssertionException, InvalidXpathException, IOException, CertificateException
    {
        super(assertion, "decrypt", logger, beanFactory, eventPub);
        this.securityTokenResolver = (SecurityTokenResolver)beanFactory.getBean("securityTokenResolver", SecurityTokenResolver.class);
    }

    @Override
    protected AssertionStatus processAffectedElements(PolicyEnforcementContext context, Message message, Document doc, List<Element> elementsToDecrypt) throws Exception {
        List<String> algorithms = new ArrayList<String>();
        List<Element> elements = new ArrayList<Element>();

        for (Element encryptedDataEl : elementsToDecrypt) {
            Pair<String, NodeList> result = decryptAndReplaceElement(encryptedDataEl);
            String algorithm = result.left;
            int numNodes = result.right.getLength();
            for (int i = 0; i < numNodes; i++) {
                Node got = result.right.item(i);
                if (got instanceof Element) {
                    Element decryptedElement = (Element) got;
                    elements.add(decryptedElement);
                    algorithms.add(algorithm);
                }
            }
        }

        context.setVariable(NonSoapDecryptElementAssertion.VAR_ELEMENTS_DECRYPTED, elements.toArray(new Element[elements.size()]));
        context.setVariable(NonSoapDecryptElementAssertion.VAR_ENCRYPTION_METHOD_URIS, algorithms.toArray(new String[algorithms.size()]));

        return AssertionStatus.NONE;
    }

    private Pair<String, NodeList> decryptAndReplaceElement(Element encryptedDataEl)
            throws StructureException, GeneralSecurityException, IOException, KeyInfoResolvingException, InvalidDocumentFormatException,
                   UnexpectedKeyInfoException, XSignatureException, SAXException, ParserConfigurationException
    {
        final FlexKey flexKey = unwrapSecretKey(encryptedDataEl, securityTokenResolver);

        // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
        // soapMsg document
        final DecryptionContext dc = new DecryptionContext();
        final List<String> algorithm = new ArrayList<String>();

        // override getEncryptionEngine to collect the encryptionmethod algorithm
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn() {
            @Override
            public EncryptionEngine getEncryptionEngine(EncryptionMethod encryptionMethod)
                    throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, StructureException  {
                final String alguri = encryptionMethod.getAlgorithm();
                algorithm.add(alguri);
                try {
                    flexKey.setAlgorithm(XencUtil.getFlexKeyAlg(alguri));
                } catch (KeyException e) {
                    throw new NoSuchAlgorithmException("Unable to use algorithm " + alguri + " with provided key material", e);
                }
                return super.getEncryptionEngine(encryptionMethod);
            }
        };

        // TODO we won't know the actual cipher until the EncryptionMethod is created, so we'll hope that the Provider will be the same for all symmetric crypto
        Provider symmetricProvider = JceProvider.getInstance().getProviderFor("Cipher.AES");
        if (symmetricProvider != null)
            af.setProvider(symmetricProvider.getName());
        dc.setAlgorithmFactory(af);
        dc.setEncryptedType(encryptedDataEl, EncryptedData.ELEMENT, null, null);
        dc.setKey(flexKey);

        dc.decrypt();
        NodeList decryptedNodes = dc.getDataAsNodeList();
        dc.replace();

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
     * @return a FlexKey containing the key material from the EncryptedKey.  Never null.
     * @throws UnexpectedKeyInfoException if the keyinfo did not match any known private key
     * @throws InvalidDocumentFormatException if the EncryptedData did not contain a ds:KeyInfo; or,
     *                                        the KeyInfo did not contain exactly one xenc:EncryptedKey; or,
     *                                        the EncryptedKey did not contain exactly one xenc:EncryptionMethod; or,
     *                                        the EncryptedKey did not contain any ds:KeyInfo elements; or,
     *                                        one of the EncryptedKey KeyInfo elements could not be understood; or,
     *                                        the EncryptedKey's CipherValue element is missing; or,
     *                                        if an OAEPparams value is specified but it is invalid.
     * @throws GeneralSecurityException if there is a problem with a certificate or key, or a required algorithm is unavailable,
     *                                  or there is an error performing the actual decryption.
     */
    private FlexKey unwrapSecretKey(Element outerEncryptedDataEl, SecurityTokenResolver securityTokenResolver)
            throws UnexpectedKeyInfoException, InvalidDocumentFormatException, GeneralSecurityException
    {
        Element keyInfo = XmlUtil.findExactlyOneChildElementByName(outerEncryptedDataEl, SoapUtil.DIGSIG_URI, "KeyInfo");
        Element encryptedKey = XmlUtil.findExactlyOneChildElementByName(keyInfo, SoapUtil.XMLENC_NS, "EncryptedKey");
        Element encMethod = DomUtils.findOnlyOneChildElementByName(encryptedKey, SoapConstants.XMLENC_NS, "EncryptionMethod");
        SignerInfo signerInfo = findSignerInfoForEncryptedType(encryptedKey, securityTokenResolver);
        String cipherValueB64 = XencUtil.getEncryptedKeyCipherValue(encryptedKey);
        byte[] encryptedKeyBytes = HexUtils.decodeBase64(cipherValueB64.trim());
        byte[] secretKeyBytes = XencUtil.decryptKey(encryptedKeyBytes, XencUtil.getOaepBytes(encMethod), signerInfo.getPrivate());
        // Support "flexible" answers to getAlgorithm() query when using 3des with HSM (Bug #3705)
        return new FlexKey(secretKeyBytes);
    }

    private SignerInfo findSignerInfoForEncryptedType(Element encryptedType, SecurityTokenResolver securityTokenResolver)
            throws InvalidDocumentFormatException, CertificateException
    {
        List<Element> keyInfos = DomUtils.findChildElementsByName(encryptedType, SoapConstants.DIGSIG_URI, SoapConstants.KINFO_EL_NAME);
        if (keyInfos == null || keyInfos.size() < 1)
            throw new InvalidDocumentFormatException(encryptedType.getLocalName() + " includes no KeyInfo element");
        boolean sawSupportedFormat = false;
        for (Element keyInfo : keyInfos) {
            try {
                // Finally try the rare but appropriate KeyInfo/X509Data
                Element x509Data = DomUtils.findOnlyOneChildElementByName(keyInfo, SoapConstants.DIGSIG_URI, "X509Data");
                if (x509Data == null)
                    throw new KeyInfoElement.UnsupportedKeyInfoFormatException("KeyInfo did not contain any recognized certificate reference format");

                SignerInfo signerInfo = handleX509Data(x509Data, securityTokenResolver);
                if (signerInfo != null)
                    return signerInfo;

                sawSupportedFormat = true;

            } catch (KeyInfoElement.UnsupportedKeyInfoFormatException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] { "Unrecognized KeyInfo format: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            } catch (InvalidDocumentFormatException e) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] { "Unable to parse KeyInfo: " + ExceptionUtils.getMessage(e) }, ExceptionUtils.getDebugException(e));
            }
        }
        if (sawSupportedFormat)
            throw new InvalidDocumentFormatException("Encryption recipient was not recognized as addressed to a private key possessed by this Gateway");
        else
            throw new InvalidDocumentFormatException("EncryptedType did not contain a KeyInfo in a supported format"); 
    }

    private static SignerInfo handleX509Data(Element x509Data, SecurityTokenResolver securityTokenResolver) throws CertificateException, InvalidDocumentFormatException {
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
            if (serialVal.length() == 0) throw new MissingRequiredElementException("X509Serial was empty");
            X509Certificate keyCert = securityTokenResolver.lookupByIssuerAndSerial(new X500Principal(issuerVal), new BigInteger(serialVal));
            return keyCert == null ? null : securityTokenResolver.lookupPrivateKeyByCert(keyCert);
        } else {
            throw new KeyInfoElement.UnsupportedKeyInfoFormatException("KeyInfo X509Data was not in a supported format");
        }
    }

}