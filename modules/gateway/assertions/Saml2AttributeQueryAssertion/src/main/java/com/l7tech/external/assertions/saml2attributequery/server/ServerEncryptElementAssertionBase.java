package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.util.HexUtils;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.EncryptionContext;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.util.logging.Level;
import java.security.cert.X509Certificate;
import java.security.Key;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23-Jan-2009
 * Time: 6:53:56 PM
 */
public abstract class ServerEncryptElementAssertionBase extends AbstractServerAssertion<Assertion> {
    private TrustedCertManager trustedCertManager;

    private static final String XML_ENCRYPTION_PREFIX = "xenc";
    private static final String XML_ENCRYPTION_NS = "http://www.w3.org/2001/04/xmlenc#";
    private static final String ENCRYPTED_DATA_TYPE = "http://www.w3.org/2001/04/xmlenc#Element";
    private static final String ENCRYPTION_ALGORITHM = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
    private static final String KEY_ENCRYPTION_ALGORITHM = "http://www.w3.org/2001/04/xmlenc#rsa-1_5";

    private static final String XML_ENCRYPTION_NS_PREFIX = "xenc";
    private static final String ENCRYPTED_DATA_TAG_NAME = XML_ENCRYPTION_NS_PREFIX + ":EncryptedData";
    private static final String ENCRYPTION_METHOD_TAG_NAME = XML_ENCRYPTION_NS_PREFIX + ":EncryptionMethod";
    private static final String DIGITAL_SIGNATURE_NS_PREFIX = "ds";
    private static final String KEY_INFO_TAG_NAME = DIGITAL_SIGNATURE_NS_PREFIX + ":KeyInfo";
    private static final String X509_DATA_TAG_NAME = DIGITAL_SIGNATURE_NS_PREFIX + ":X509Data";
    private static final String X509_SUBJECT_NAME_TAG_NAME = DIGITAL_SIGNATURE_NS_PREFIX + ":X509SubjectName";
    private static final String ENCRYPTED_KEY_TAG_NAME = XML_ENCRYPTION_NS_PREFIX + ":EncryptedKey";
    private static final String CIPHER_DATA_TAG_NAME = XML_ENCRYPTION_NS_PREFIX + ":CipherData";
    private static final String CIPHER_VALUE_TAG_NAME = XML_ENCRYPTION_NS_PREFIX + ":CipherValue";

    protected static final String SYMMETRIC_KEY_ALGORITHM = "AES";
    protected static final String SYMMETRIC_KEY_ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";

    //- PUBLIC

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerEncryptElementAssertionBase( final Assertion assertion,
                                    final ApplicationContext context )
        throws PolicyAssertionException
    {
        super(assertion);
        trustedCertManager = (TrustedCertManager)context.getBean("trustedCertManager");
    }

    protected TrustedCertManager getTrustedCertManager() {
        return trustedCertManager;
    }

    protected void encryptElement(Document doc,
                                         X509Certificate cert,
                                         String elementToEncrypt,
                                         String namespaceUri,
                                         String preferredPrefix,
                                         String encryptedElementName) {
        NodeList children = doc.getElementsByTagNameNS(SamlConstants.NS_SAML2, elementToEncrypt);
        if(children.getLength() > 0) {
            Element assertionElement = (Element)children.item(0);

            EncryptionElementTree tree = createEncryptionElementTree(doc, assertionElement.getParentNode(), namespaceUri, preferredPrefix, encryptedElementName);

            EncryptionContext encryptionCtx = new EncryptionContext();
            AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
            encryptionCtx.setAlgorithmFactory(af);
            //encryptionCtx.setEncryptedType(tree.encryptedData, ENCRYPTED_DATA_TYPE, tree.encryptionMethod, tree.keyInfo);
            encryptionCtx.setEncryptedType(tree.encryptedData, ENCRYPTED_DATA_TYPE, tree.encryptionMethod, null);
            encryptionCtx.setData(assertionElement);

            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(SYMMETRIC_KEY_ALGORITHM);
                Key secretKey = keyGenerator.generateKey();
                secretKey = new SecretKeySpec(secretKey.getEncoded(), SYMMETRIC_KEY_ENCRYPTION_ALGORITHM);
                //Key secretKey = encryptionCtx.generateKey();
                encryptionCtx.setKey(secretKey);
                encryptionCtx.encrypt();
                //encryptionCtx.replace();

                assertionElement.getParentNode().replaceChild(tree.root, assertionElement);

                // Fill in the encryption key details (the key name and encrypted key)
                tree.keyName.appendChild(doc.createTextNode(cert.getSubjectX500Principal().toString()));

                Cipher cipher = Cipher.getInstance(cert.getPublicKey().getAlgorithm());
                cipher.init(Cipher.ENCRYPT_MODE, cert);
                tree.keyCipherValue.appendChild(doc.createTextNode(HexUtils.encodeBase64(cipher.doFinal(secretKey.getEncoded()), true)));
            } catch(Exception e) {
                logger.log(Level.WARNING, e.toString(), e);
            }
        }
    }

    private static EncryptionElementTree createEncryptionElementTree(Document doc,
                                                                     Node parentNode,
                                                                     String namespaceUri,
                                                                     String preferredPrefix,
                                                                     String containerTagName) {
        /*
         * <[[preferredPrefix]]:[[containerTagName]]>
         *   <xenc:EncryptedData Type="http://www.w3.org/2001/04/xmlenc#Element">
         *     <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#aes128-cbc"/>
         *     <ds:KeyInfo>
         *       <xenc:EncryptedKey>
         *         <xenc:EncryptionMethod Algorithm="http://www.w3.org/2001/04/xmlenc#rsa-1_5"/>
         *         <ds:KeyInfo>
         *           <ds:X509Data>
         *             <ds:X509SubjectName></ds:X509SubjectName>
         *           </ds:X509Data>
         *         </ds:KeyInfo>
         *         <xenc:CipherData>
         *           <xenc:CipherValue></xenc:CipherValue>
         *         </xenc:CipherData>
         *       </xenc:EncryptedKey>
         *     </ds:KeyInfo>
         *     <xenc:CipherData>
         *       <xenc:CipherValue></xenc:CipherValue>
         *     <xenc:CipherData>
         *   </xenc:EncryptedData>
         * </samlp2:EncryptedAssertion>
         */
        EncryptionElementTree tree = new EncryptionElementTree();

        tree.root = doc.createElementNS(namespaceUri, containerTagName);
        String prefix = parentNode.lookupPrefix(namespaceUri);
        if(prefix == null) {
            tree.root.setAttribute("xmlns:" + preferredPrefix, namespaceUri);
            tree.root.setPrefix(preferredPrefix);
        } else {
            tree.root.setPrefix(prefix);
        }

        tree.encryptedData = doc.createElementNS(XML_ENCRYPTION_NS, ENCRYPTED_DATA_TAG_NAME);
        prefix = parentNode.lookupPrefix(XML_ENCRYPTION_NS);
        if(prefix == null) {
            tree.encryptedData.setAttribute("xmlns:" + XML_ENCRYPTION_PREFIX, XML_ENCRYPTION_NS);
            tree.encryptedData.setPrefix(XML_ENCRYPTION_PREFIX);
        } else {
            tree.encryptedData.setPrefix(prefix);
        }
        tree.encryptedData.setAttribute("Type", ENCRYPTED_DATA_TYPE);
        tree.root.appendChild(tree.encryptedData);

        tree.encryptionMethod = doc.createElementNS(XML_ENCRYPTION_NS, ENCRYPTION_METHOD_TAG_NAME);
        tree.encryptionMethod.setAttribute("Algorithm", ENCRYPTION_ALGORITHM);
        tree.encryptedData.appendChild(tree.encryptionMethod);

        tree.keyInfo = doc.createElementNS(SoapUtil.DIGSIG_URI, KEY_INFO_TAG_NAME);
        prefix = parentNode.lookupPrefix(SoapUtil.DIGSIG_URI);
        if(prefix == null) {
            tree.keyInfo.setAttribute("xmlns:" + "ds", SoapUtil.DIGSIG_URI);
            tree.keyInfo.setPrefix("ds");
        } else {
            tree.keyInfo.setPrefix(prefix);
        }
        tree.encryptedData.appendChild(tree.keyInfo);

        Element encryptedKey = doc.createElementNS(XML_ENCRYPTION_NS, ENCRYPTED_KEY_TAG_NAME);
        tree.keyInfo.appendChild(encryptedKey);

        Element keyEncryptionMethod = doc.createElementNS(XML_ENCRYPTION_NS, ENCRYPTION_METHOD_TAG_NAME);
        keyEncryptionMethod.setAttribute("Algorithm", KEY_ENCRYPTION_ALGORITHM);
        encryptedKey.appendChild(keyEncryptionMethod);

        Element encryptedKeyInfo = doc.createElementNS(SoapUtil.DIGSIG_URI, KEY_INFO_TAG_NAME);
        encryptedKey.appendChild(encryptedKeyInfo);

        Element x509Data = doc.createElementNS(SoapUtil.DIGSIG_URI, X509_DATA_TAG_NAME);
        encryptedKeyInfo.appendChild(x509Data);

        tree.keyName = doc.createElementNS(SoapUtil.DIGSIG_URI, X509_SUBJECT_NAME_TAG_NAME);
        x509Data.appendChild(tree.keyName);

        Element keyCipherData = doc.createElementNS(XML_ENCRYPTION_NS, CIPHER_DATA_TAG_NAME);
        encryptedKey.appendChild(keyCipherData);

        tree.keyCipherValue = doc.createElementNS(XML_ENCRYPTION_NS, CIPHER_VALUE_TAG_NAME);
        keyCipherData.appendChild(tree.keyCipherValue);

        Element cipherData = doc.createElementNS(XML_ENCRYPTION_NS, CIPHER_DATA_TAG_NAME);
        tree.encryptedData.appendChild(cipherData);

        tree.cipherValue = doc.createElementNS(XML_ENCRYPTION_NS, CIPHER_VALUE_TAG_NAME);
        cipherData.appendChild(tree.cipherValue);

        return tree;
    }

    private static class EncryptionElementTree {
        public Element root;
        public Element encryptedData;
        public Element encryptionMethod;
        public Element keyInfo;
        public Element keyName;
        public Element keyCipherValue;
        public Element cipherValue;
    }
}