/*
* Copyright (C) 2003 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.common.security.xml;

import com.ibm.xml.dsig.XSignatureException;
import com.ibm.xml.enc.AlgorithmFactoryExtn;
import com.ibm.xml.enc.DecryptionContext;
import com.ibm.xml.enc.EncryptionContext;
import com.ibm.xml.enc.KeyInfoResolvingException;
import com.ibm.xml.enc.StructureException;
import com.ibm.xml.enc.type.CipherData;
import com.ibm.xml.enc.type.CipherValue;
import com.ibm.xml.enc.type.EncryptedData;
import com.ibm.xml.enc.type.EncryptionMethod;
import com.ibm.xml.enc.type.KeyInfo;
import com.ibm.xml.enc.type.KeyName;
import com.ibm.xml.enc.util.AdHocIdResolver;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.util.SoapUtil;
import org.apache.log4j.Category;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.Provider;

/**
 * Class that encrypts and decrypts XML documents.
 * User: mike
 * Date: Aug 26, 2003
 * Time: 11:07:41 AM
 */
public class XmlMangler {
    private static final Category log = Category.getInstance(XmlMangler.class);

    private static final Provider bcp = new BouncyCastleProvider();
    static {
        java.security.Security.addProvider(bcp);
    }

    // ID for EncryptedData element
    private static final String id = "bodyId";

    // Namespace constants
    private static final String soapNS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String wsseNS = "http://schemas.xmlsoap.org/ws/2002/xx/secext";
    private static final String xencNS = "http://www.w3.org/2001/04/xmlenc#";
    private static final String dsNS = "http://www.w3.org/2000/09/xmldsig#";

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
     * In-place encrypt of the specified SOAP document's body element.  The body element will be encrypted with AES256
     * using the specified AES256 key, which will be tagged with the specified KeyName.
     *
     * @param soapMsg  the SOAP document whose body to encrypt
     * @param keyBytes   the 32 byte AES256 symmetric key to use to encrypt it
     * @param keyName    an identifier for this Key that will be meaningful to a consumer of the encrypted message
     * @throws IllegalArgumentException  if the key was not an array of exactly 32 bytes of key material
     * @throws GeneralSecurityException  if there was a problem with a key or a crypto provider
     * @throws IOException  if there was a problem reading or writing a key or a bit of xml
     */
    public static void encryptXml(Document soapMsg, byte[] keyBytes, String keyName)
            throws GeneralSecurityException, IOException, IllegalArgumentException
    {
        if (keyBytes.length != 32)
            throw new IllegalArgumentException("keyBytes must be 32 bytes long for AES256");

        Element body = (Element) soapMsg.getElementsByTagNameNS(soapNS, "Body").item(0);
        if (body == null)
            throw new IllegalArgumentException("Unable to find tag Body in document with namespace " + soapNS);


        CipherData cipherData = new CipherData();
        cipherData.setCipherValue(new CipherValue());
        KeyInfo keyInfo = new KeyInfo();
        KeyName kn = new KeyName();
        kn.setName(keyName);
        keyInfo.addKeyName(kn);
        EncryptionMethod encMethod = new EncryptionMethod();
        encMethod.setAlgorithm(EncryptionMethod.AES256_CBC);
        EncryptedData encData = new EncryptedData();
        encData.setCipherData(cipherData);
        encData.setEncryptionMethod(encMethod);
        encData.setKeyInfo(keyInfo);
        encData.setId(id);
        Element encDataElement = null;
        try {
            encDataElement = encData.createElement(soapMsg, true);
        } catch (StructureException e) {
            throw new XmlManglerException(e);
        }

        // Create encryption context and encrypt the header subtree
        EncryptionContext ec = new EncryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        af.setProvider(bcp.getName());
        ec.setAlgorithmFactory(af);
        ec.setEncryptedType(encDataElement, EncryptedData.CONTENT,  null, null);

        ec.setData(body);
        ec.setKey(new AesKey(keyBytes));

        try {
            ec.encrypt();
            ec.replace();
        } catch (KeyInfoResolvingException e) {
            throw new XmlManglerException(e);
        } catch (StructureException e) {
            throw new XmlManglerException(e);
        }

        // Insert a WSS style header with a ReferenceList refering to the EncryptedData element
        addWssHeader(soapMsg);
    }

    /**
     * Create a SOAP header in the specified SOAP message if it doesn't already have one.  In any case, returns
     * a reference to the SOAP header.
     *
     * @param soapMsg  the SOAP message to work with
     * @return  the found or newly-added SOAP header element
     */
    private static Element findOrCreateSoapHeader(Document soapMsg) {
        Element hdrEl = (Element) soapMsg.getElementsByTagNameNS(soapNS, "Header").item(0);
        if (hdrEl == null) {
            // No header.. need to add one
            Element body = (Element) soapMsg.getElementsByTagNameNS(soapNS, "Body").item(0);
            if (body == null)
                throw new IllegalArgumentException("No body in this document"); // can't actually happen here
            String prefix = body.getPrefix();
            hdrEl = soapMsg.createElementNS(soapNS, prefix == null ? "Header" : prefix + ":Header");
            soapMsg.getDocumentElement().insertBefore(hdrEl, body);
        }
        return hdrEl;
    }

    /**
     * Insert a WSS style header into the specified document referring to the EncryptedData element.
     * @param soapMsg
     */
    private static void addWssHeader(Document soapMsg) {
        // Add new namespaces to Envelope element, as per spec.
        Element rootEl = soapMsg.getDocumentElement();
        rootEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:wsse", wsseNS);
        rootEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:xenc", xencNS);
        rootEl.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:ds", dsNS);

        // Add Security element to header, referencing the encrypted body
        Element dataRefEl = soapMsg.createElementNS(xencNS, "xenc:DataReference");
        dataRefEl.setAttribute("URI", id);

        Element refEl = soapMsg.createElementNS(xencNS, "xenc:ReferenceList");
        refEl.appendChild(dataRefEl);

        Element securityEl = soapMsg.createElementNS(wsseNS, "wsse:Security");
        securityEl.appendChild(refEl);

        Element hdrEl = findOrCreateSoapHeader(soapMsg);
        hdrEl.appendChild(securityEl);
    }

    /**
     * Determine the KeyName used to encrypt the specified encrypted SOAP document.  Callers might
     * use the returned key name to decide which PrivateKey to use with a subsequent call to decryptXml().
     *
     * @param soapMsg  The encrypted SOAP document to examine.
     * @return the value of the KeyName node.
     */
    public static String getKeyName(Document soapMsg) {
        // Derive the key alias from KeyInfo
        Element keyNameEl = (Element) soapMsg.getElementsByTagName("KeyName").item(0);
        Text keyNameText = (Text) keyNameEl.getFirstChild();
        String alias = keyNameText.getNodeValue();
        return alias;
    }

    /**
     * In-place decrypt of the specified encrypted SOAP document.  Caller is responsible for ensuring that the
     * correct key is used for the document, of the proper format for the encryption scheme it used.  Callers can use
     * getKeyName() to help decide which Key to use to decrypt the document.
     *
     * If this method returns normally the document will have been successfully decrypted.
     *
     * @param soapMsg  The SOAP document to decrypt.
     * @param key  The key to use to decrypt it. If the document was encrypted with
     *             a call to encryptXml(), the Key will be a 32 byte AES256 symmetric key.
     * @throws GeneralSecurityException  if there was a problem with a key or crypto provider
     * @throws ParserConfigurationException  if there was a problem with the XML parser
     * @throws IOException  if there was an IO error while reading the document or a key
     * @throws SAXException  if there was a problem parsing the document
     */
    public static void decryptXml(Document soapMsg, Key key)
            throws GeneralSecurityException, ParserConfigurationException, IOException,
                   SAXException, XMLSecurityElementNotFoundException
    {
        // Locate EncryptedData element by its reference in the Security header
        Element dataRefEl = (Element) soapMsg.getElementsByTagNameNS(xencNS, "DataReference").item(0);
        if (dataRefEl == null)
            throw new XMLSecurityElementNotFoundException("no DataReference tag in the message");
        AdHocIdResolver idResolver = new AdHocIdResolver();
        Element encryptedDataEl = idResolver.resolveID(soapMsg, dataRefEl.getAttribute("URI"));

        // Strip out processed DataReferece element
        Element RefListEl = (Element) dataRefEl.getParentNode();
        RefListEl.removeChild(dataRefEl);

        // Create decryption context and decrypt the EncryptedData subtree. Note that this effects the
        // soapMsg document
        DecryptionContext dc = new DecryptionContext();
        AlgorithmFactoryExtn af = new AlgorithmFactoryExtn();
        af.setProvider(bcp.getName());
        dc.setAlgorithmFactory(af);
        dc.setEncryptedType(encryptedDataEl, EncryptedData.CONTENT, null, null);
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

        // clean up the document
        cleanEmptyRefList(soapMsg);
    }

    public static void cleanEmptyRefList(Document soapMsg) {
        NodeList listRefElements = soapMsg.getElementsByTagNameNS(xencNS, "ReferenceList");
        if (listRefElements.getLength() < 1) return;
        Element refEl = (Element)listRefElements.item(0);
        if (!SoapUtil.elHasChildrenElements(refEl)) {
            refEl.getParentNode().removeChild(refEl);
        }
    }
}
