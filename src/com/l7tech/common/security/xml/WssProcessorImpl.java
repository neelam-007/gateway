package com.l7tech.common.security.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.util.logging.Logger;

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
        // look for security elements
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
            return emptyResult(soapMsg);
        }
        // todo
        return null;
    }

    private WssProcessor.ProcessorResult emptyResult(final Document original) {
        return new WssProcessor.ProcessorResult() {
            public Document getUndecoratedMessage() {
                return original;
            }
            public Element[] getElementsThatWereSigned() {
                return new Element[0];
            }
            public Element[] getElementsThatWereEncrypted() {
                return new Element[0];
            }
            public WssProcessor.SecurityToken[] getSecurityTokens() {
                return new WssProcessor.SecurityToken[0];
            }
            public WssProcessor.Timestamp getTimestamp() {
                return null;
            }
        };
    }

    private final Logger logger = Logger.getLogger(WssProcessorImpl.class.getName());
}
