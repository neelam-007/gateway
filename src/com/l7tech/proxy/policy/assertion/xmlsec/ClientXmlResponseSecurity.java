package com.l7tech.proxy.policy.assertion.xmlsec;

import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import com.l7tech.xmlsig.*;
import com.l7tech.xmlenc.XmlMangler;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.apache.log4j.Category;

import javax.xml.parsers.ParserConfigurationException;
import java.security.cert.X509Certificate;
import java.security.Key;
import java.security.GeneralSecurityException;
import java.io.IOException;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). May also enforce
 * xml encryption for the body element of the response.
 *
 * On the server side, this decorates a response with an xml d-sig and maybe xml-enc the response's body
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope and decrypts
 * the response's body if necessary.
 *
 * @author flascell
 */
public class ClientXmlResponseSecurity extends ClientAssertion {

    public ClientXmlResponseSecurity(XmlResponseSecurity data) {
        this.data = data;
    }

    /**
     * If this assertion includes xml-enc, the proxy will add a header to the request that tells the server
     * which xml-enc session to use.
     *
     * @param request might receive a header containing the xml-enc session
     * @return AssertionStatus.NONE (always)
     * @throws PolicyAssertionException no
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        // DECOREATE REQUEST WITH A NONCE FOR RESPONSE SIGNATURE
        long noncevalue = 0;
        // todo, mike generate a nonce and it to request as http header XmlResponseSecurity.XML_NONCE_HEADER_NAME

        // IF RESPONSE ENCRYPTION IS TURNED ON, TELL THE SERVER THE PREFFERED SESSION ID
        if (data.isEncryption()) {
            long sessionid = 0;
            // todo, mike get the session id here
            // todo, mike store this sessionid in http header XmlResponseSecurity.XML_SESSID_HEADER_NAME
        }

        return AssertionStatus.NONE;
    }

    /**
     * validate the signature of the response by the ssg server
     * @param request
     * @param response
     * @return
     * @throws PolicyAssertionException
     */
    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // GET THE RESPONSE DOCUMENT
        Document doc = null;
        try {
            doc = response.getResponseAsDocument();
        } catch (IOException e) {
            throw new PolicyAssertionException("could not get response document", e);
        } catch (SAXException e) {
            throw new PolicyAssertionException("could not get response document", e);
        }

        // LOOK FOR NONCE IN WSSC TOKEN
        try {
            long responsenonce = SecureConversationTokenHandler.readNonceFromDocument(doc);
            // todo, mike compare to nonce used in decorate request
        } catch (XMLSecurityElementNotFoundException e) {
            // if the nonce is not there, we should note that this is subject to repeat attack
            log.warn("response did not refer to a nonce - subject to repeat attack if not on secure channel");
        }

        // VERIFY SIGNATURE OF ENVELOPE
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        X509Certificate serverCert = null;
        try {
            serverCert = dsigHelper.validateSignature(doc);
        } catch (SignatureNotFoundException e) {
            throw new PolicyAssertionException("could not validate signature", e);
        } catch (InvalidSignatureException e) {
            throw new PolicyAssertionException("could not validate signature", e);
        } catch (XSignatureException e) {
            throw new PolicyAssertionException("could not validate signature", e);
        }

        // VERIFY THE SERVER CERT
        // todo, mike verify that serverCert is signed with the cert we have for this server
        log.info("signature verified");

        // must we also decrypt the body?
        if (data.isEncryption()) {
            Key keyres = null;
            // todo, mike get the keyres for this session
            try {
                XmlMangler.decryptXml(doc, keyres);
            } catch (GeneralSecurityException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            } catch (ParserConfigurationException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            } catch (IOException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            } catch (SAXException e) {
                throw new PolicyAssertionException("failure decrypting document", e);
            }
            log.info("message decrypted");
        }

        return AssertionStatus.NONE;
    }

    private XmlResponseSecurity data = null;
    private static final Category log = Category.getInstance(ClientHttpClientCert.class);
}
