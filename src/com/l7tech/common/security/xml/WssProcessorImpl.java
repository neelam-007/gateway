package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;

import com.l7tech.common.util.SoapUtil;

/**
 * An implementation of the WssProcessor for use in both the SSG and the SSA.
 *
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 5, 2004<br/>
 * $Id$<br/>
 */
public class WssProcessorImpl implements WssProcessor {

    /**
     * This processes a soap message. That is, the contents of the Header/Security are processed as per the WSS rules.
     *
     * @param soapMsg the xml document containing the soap message. this document may be modified on exit
     * @param recipientCert the recipient's cert to which encrypted keys may be encoded for
     * @param recipientKey the private key corresponding to the recipientCertificate used to decypher the encrypted keys
     * @return a ProcessorResult object reffering to all the WSS related processing that happened.
     * @throws ProcessorException
     */
    public WssProcessor.ProcessorResult undecorateMessage(Document soapMsg,
                                                          X509Certificate recipientCert,
                                                          PrivateKey recipientKey) throws WssProcessor.ProcessorException {
        // Reset all potential outputs
        unprocessedDocument = soapMsg;
        elementsThatWereSigned.clear();
        elementsThatWereEncrypted.clear();
        securityTokens.clear();
        timestamp = null;

        // Get relevent Security header
        Element[] securityHeaders = SoapUtil.getSecurityElements(soapMsg);
        Element releventSecurityHeader = null;
        // find the relevent security header. that is the one with no actor
        String currentSoapNamespace = soapMsg.getDocumentElement().getNamespaceURI();
        for (int i = 0; i < securityHeaders.length; i++) {
            String thisActor = securityHeaders[i].getAttributeNS(currentSoapNamespace, "actor");
            if (thisActor == null || thisActor.length() < 1) {
                releventSecurityHeader = securityHeaders[i];
                break;
            }
        }
        // maybe there are no security headers at all in which case, there is nothing to process
        if (releventSecurityHeader == null) {
            logger.finer("No relevent security header found.");
            return produceResult();
        }

        // Process elements one by one
        NodeList securityChildren = releventSecurityHeader.getElementsByTagName("*");
        for (int i = 0; i < securityChildren.getLength(); i++) {
            Element securityChildToProcess = (Element)securityChildren.item(i);

            if (securityChildToProcess.getLocalName().equals("EncryptedKey")) {
                logger.finest("Processing EncryptedKey");
                // todo
            } else if (securityChildToProcess.getLocalName().equals("Timestamp")) {
                logger.finest("Processing Timestamp");
                // todo
            } else if (securityChildToProcess.getLocalName().equals("BinarySecurityToken")) {
                logger.finest("Processing BinarySecurityToken");
                // todo
            } else if (securityChildToProcess.getLocalName().equals("Signature")) {
                logger.finest("Processing Signature");
                // todo
            } else {
                logger.finer("Unknown element in security header: " + securityChildToProcess.getNodeName());
            }
        }
        return produceResult();
    }

    private WssProcessor.ProcessorResult produceResult() {
        return new WssProcessor.ProcessorResult() {
            public Document getUndecoratedMessage() {
                return unprocessedDocument;
            }
            public Element[] getElementsThatWereSigned() {
                Element[] output = new Element[elementsThatWereSigned.size()];
                Iterator iter = elementsThatWereSigned.iterator();
                for (int i = 0; i < output.length; i++) {
                    output[i] = (Element)iter.next();
                }
                return output;
            }
            public Element[] getElementsThatWereEncrypted() {
                Element[] output = new Element[elementsThatWereEncrypted.size()];
                Iterator iter = elementsThatWereEncrypted.iterator();
                for (int i = 0; i < output.length; i++) {
                    output[i] = (Element)iter.next();
                }
                return output;
            }
            public WssProcessor.SecurityToken[] getSecurityTokens() {
                WssProcessor.SecurityToken[] output = new WssProcessor.SecurityToken[securityTokens.size()];
                Iterator iter = securityTokens.iterator();
                for (int i = 0; i < output.length; i++) {
                    output[i] = (WssProcessor.SecurityToken)iter.next();
                }
                return output;
            }
            public WssProcessor.Timestamp getTimestamp() {
                return timestamp;
            }
        };
    }

    private final Logger logger = Logger.getLogger(WssProcessorImpl.class.getName());

    private Document unprocessedDocument = null;
    private final Collection elementsThatWereSigned = new ArrayList();
    private final Collection elementsThatWereEncrypted = new ArrayList();
    private final Collection securityTokens = new ArrayList();
    private WssProcessor.Timestamp timestamp = null;

}
