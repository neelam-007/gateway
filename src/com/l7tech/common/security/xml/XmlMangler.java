/*
* Copyright (C) 2003 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.XSignatureException;
import com.ibm.xml.enc.*;
import com.ibm.xml.enc.type.*;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class that encrypts and decrypts XML documents.
 * User: mike
 * Date: Aug 26, 2003
 * Time: 11:07:41 AM
 */
public class XmlMangler {
    static {
        JceProvider.init();
    }
    
    /** nice try asshole */
    private XmlMangler() { }

    // ID for EncryptedData element
    private static final String id = "bodyId";

    static final String NS_ENC_URI = "http://www.w3.org/2000/xmlns/";

    private static class XmlManglerException extends GeneralSecurityException {
        public XmlManglerException() {
        }

        public XmlManglerException(String msg) {
            super(msg);
        }

        public XmlManglerException(String msg, Throwable cause) {
            super(msg);
            initCause(cause);
        }

        public XmlManglerException(Throwable cause) {
            super();
            initCause(cause);
        }
    }

    /**
     * In-place encrypt of the specified SOAP document's body element.  The body element will be encrypted with AES128
     * using the specified AES128 key, which will be tagged with the specified KeyName.
     * 
     * @param soapMsg  the SOAP document whose body to encrypt
     * @param keyBytes the 16 byte AES128 symmetric key to use to encrypt it
     * @param keyName  an identifier for this Key that will be meaningful to a consumer of the encrypted message
     * @throws IllegalArgumentException if the key was not an array of exactly 16 bytes of key material
     * @throws GeneralSecurityException if there was a problem with a key or a crypto provider
     * @throws IOException              if there was a problem reading or writing a key or a bit of xml
     */
    public static void encryptXml(Document soapMsg, byte[] keyBytes, String keyName)
      throws GeneralSecurityException, IOException, IllegalArgumentException {
        encryptXml(soapMsg.getDocumentElement(), keyBytes, keyName, id);
    }


    /**
     * In-place encrypt of the specified SOAP document's element.  The element will be encrypted with AES128
     * using the specified AES128 key, which will be tagged with the specified KeyName.
     * 
     * @param element  the element to encrypt
     * @param keyBytes the 16 byte AES128 symmetric key to use to encrypt it
     * @param keyName  an identifier for this Key that will be meaningful to a consumer of the encrypted message
     * @param referenceId  an identifier for the encrypted element
     * @throws IllegalArgumentException if the key was not an array of exactly 16 bytes of key material
     * @throws GeneralSecurityException if there was a problem with a key or a crypto provider
     * @throws IOException              if there was a problem reading or writing a key or a bit of xml
     */
    public static void encryptXml(Element element, byte[] keyBytes, String keyName, String referenceId)
      throws GeneralSecurityException, IOException, IllegalArgumentException {
        Document soapMsg = element.getOwnerDocument();
        if (keyBytes.length < 16)
            throw new IllegalArgumentException("keyBytes must be at least 16 bytes long for AES128");


        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());
        KeyInfo keyInfo = new KeyInfo();
        KeyName kn = new KeyName();
        kn.setName(keyName);
        keyInfo.addKeyName(kn);
        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(EncryptionMethod.AES128_CBC);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setKeyInfo(keyInfo);
        encData.setId(referenceId);
        Element encDataElement = null;
        try {
            encDataElement = encData.createElement(soapMsg, true);
        } catch (StructureException e) {
            throw new XmlManglerException(e);
        }

        // Create encryption context and encrypt the header subtree
        EncryptionContext ec = new EncryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        af.setProvider(JceProvider.getSymmetricJceProvider().getName());
        ec.setAlgorithmFactory(af);
        ec.setEncryptedType(encDataElement, EncryptedData.CONTENT, null, null);

        ec.setData(element);
        ec.setKey(new AesKey(keyBytes, 128));

        try {
            ec.encrypt();
            ec.replace();
        } catch (KeyInfoResolvingException e) {
            throw new XmlManglerException(e);
        } catch (StructureException e) {
            throw new XmlManglerException(e);
        }

        // Insert a WSS style header with a ReferenceList refering to the EncryptedData element
        addWssHeader(soapMsg.getDocumentElement(), referenceId);
    }

    /**
     * Update the document WSS header with the encrypted element reference info.
     * 
     * @param element     the document element
     * @param referenceId the element reference id
     */
    private static void addWssHeader(Element element, String referenceId) {
        Document document = element.getOwnerDocument();
        // Add new namespaces to Envelope element, as per spec.

        Element rootElement = document.getDocumentElement();
        if (rootElement.getAttributeNodeNS(NS_ENC_URI, "xenc") == null) {
            rootElement.setAttributeNS(NS_ENC_URI, "xmlns:xenc", SoapUtil.XMLENC_NS);
        }

        // Add Security element to header, referencing the encrypted body
        Element dataRefEl = document.createElementNS(SoapUtil.XMLENC_NS, "xenc:DataReference");
        dataRefEl.setAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME, referenceId);

        Element refEl = document.createElementNS(SoapUtil.XMLENC_NS, "xenc:ReferenceList");
        refEl.appendChild(dataRefEl);

        Element securityEl = SoapUtil.getOrMakeSecurityElement(document);
        securityEl.appendChild(refEl);
    }

    /**
     * Determine the KeyName used to encrypt the specified encrypted SOAP document.  Callers might
     * use the returned key name to decide which PrivateKey to use with a subsequent call to decryptXml().
     * 
     * @param soapMsg The encrypted SOAP document to examine.
     * @return the value of the KeyName node.
     */
    public static String getKeyName(Document soapMsg) {
        // Derive the key alias from KeyInfo
        Element keyNameEl = (Element)soapMsg.getElementsByTagName("KeyName").item(0);
        Text keyNameText = (Text)keyNameEl.getFirstChild();
        String alias = keyNameText.getNodeValue();
        return alias;
    }

    /**
     * In-place decrypt of the specified encrypted element.  Caller is responsible for ensuring that the
     * correct key is used for the document, of the proper format for the encryption scheme it used.  Caller can use
     * getKeyName() to help decide which Key to use to decrypt the document.  Caller is responsible for
     * cleaning out any empty reference lists, security headers, or soap headers after all encrypted
     * elements have been processed.
     * <p/>
     * If this method returns normally the specified element will have been successfully decrypted.
     *
     * @param messagePartElement The Element to decrypt.
     * @param key     The key to use to decrypt it. If the document was encrypted with
     *                a call to encryptXml(), the Key will be a 16 byte AES128 symmetric key.
     * @throws GeneralSecurityException     if there was a problem with a key or crypto provider
     * @throws ParserConfigurationException if there was a problem with the XML parser
     * @throws IOException                  if there was an IO error while reading the document or a key
     * @throws SAXException                 if there was a problem parsing the document
     */
    public static void decryptElement(Element messagePartElement, Key key)
            throws GeneralSecurityException, ParserConfigurationException, IOException, SAXException,
            XMLSecurityElementNotFoundException
    {
        Document soapMsg = messagePartElement.getOwnerDocument();
        Element encryptedDataElement = XmlUtil.findFirstChildElementByName(messagePartElement,
                                                                           SoapUtil.XMLENC_NS,
                                                                           "EncryptedData");
        if (encryptedDataElement == null)
            throw new XMLSecurityElementNotFoundException("No EncryptedData found inside the 'encrypted' message part");

        String messagePartId = encryptedDataElement.getAttribute( "Id" );
        Element envelope = soapMsg.getDocumentElement();
        if (!"Envelope".equals(envelope.getLocalName())) // todo: move this validation to somewhere more sensible

            throw new IllegalArgumentException("Invalid SOAP envelope: document element is not named 'Envelope'");
        String envelopeNs = envelope.getNamespaceURI();
        if (!SoapUtil.ENVELOPE_URIS.contains(envelopeNs)) // todo: move this validation to somewhere more sensible
            throw new IllegalArgumentException("Invalid SOAP message: unrecognized envelope namespace \"" + envelopeNs + "\"");

        // Locate EncryptedData element by its reference in the Security header
        Element header = XmlUtil.findFirstChildElementByName(envelope, envelopeNs, "Header");
        if (header == null)
            throw new XMLSecurityElementNotFoundException("EncryptedData is present, but there is no SOAP header");

        Element security = XmlUtil.findFirstChildElementByName(header, SoapUtil.SECURITY_NAMESPACE2, "Security");
        if (security == null)
            security = XmlUtil.findFirstChildElementByName(header, SoapUtil.SECURITY_NAMESPACE, "Security");
        if (security == null)
            throw new XMLSecurityElementNotFoundException("EncryptedData is present, but there is no security element");

        List referenceListList = XmlUtil.findChildElementsByName(security, SoapUtil.XMLENC_NS, "ReferenceList");
        if (referenceListList == null)
            throw new XMLSecurityElementNotFoundException("EncryptedData is present, but there is no ReferenceList");
        for (Iterator i = referenceListList.iterator(); i.hasNext();) {
            Element referenceList = (Element)i.next();

            List dataRefEls = XmlUtil.findChildElementsByName(referenceList, SoapUtil.XMLENC_NS, "DataReference");
            if ( dataRefEls == null || dataRefEls.isEmpty() ) {
                logger.warning("EncryptedData is present, but contains at least one empty ReferenceList");
                continue;
            }

            for (Iterator j = dataRefEls.iterator(); j.hasNext();) {
                Element dataRefEl = (Element)j.next();
                String dataRefUri = dataRefEl.getAttribute(SoapUtil.REFERENCE_URI_ATTR_NAME);
                if ( dataRefUri != null && dataRefUri.equals(messagePartId ) ) {
                    // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
                    // soapMsg document
                    DecryptionContext dc = new DecryptionContext();
                    AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
                    af.setProvider(JceProvider.getSymmetricJceProvider().getName());
                    dc.setAlgorithmFactory(af);
                    dc.setEncryptedType(encryptedDataElement, EncryptedData.CONTENT, null, null);
                    dc.setKey(key);

                    try {
                        dc.decrypt();
                        dc.replace();
                    } catch (XSignatureException e) {
                        throw new XmlManglerException(e);
                    } catch (StructureException e) {
                        throw new XmlManglerException(e);
                    } catch (KeyInfoResolvingException e) {
                        throw new XmlManglerException(e);
                    }

                    referenceList.removeChild(dataRefEl);
                    if (!referenceList.hasChildNodes())
                        referenceList.getParentNode().removeChild(referenceList);
                    return;
                }
            }
        }
        throw new XMLSecurityElementNotFoundException("EncryptedData is present, but there are no matching DataReference tags");
    }

    /**
     * In-place decrypt of the specified document's document element.  Caller is responsible for ensuring that the
     * correct key is used for the document, of the proper format for the encryption scheme it used.  Caller can use
     * getKeyName() to help decide which Key to use to decrypt the document.  Caller is responsible for
     * cleaning out any empty reference lists, security headers, or soap headers after all encrypted
     * elements have been processed.
     * <p/>
     * If this method returns normally the specified element will have been successfully decrypted.
     *
     * @param soapMsg The SOAP document to decrypt.
     * @param key     The key to use to decrypt it. If the document was encrypted with
     *                a call to encryptXml(), the Key will be a 16 byte AES128 symmetric key.
     * @throws GeneralSecurityException     if there was a problem with a key or crypto provider
     * @throws ParserConfigurationException if there was a problem with the XML parser
     * @throws IOException                  if there was an IO error while reading the document or a key
     * @throws SAXException                 if there was a problem parsing the document
     */
    public static void decryptDocument(Document soapMsg, Key key)
            throws GeneralSecurityException, XMLSecurityElementNotFoundException, IOException,
            ParserConfigurationException, SAXException
    {
        decryptElement(soapMsg.getDocumentElement(), key);
    }

    // Use a logger that will work inside either the Agent or the Gateway.
    private static final Logger logger = Logger.getLogger(XmlMangler.class.getName());
}
