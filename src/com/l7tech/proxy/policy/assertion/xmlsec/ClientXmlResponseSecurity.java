package com.l7tech.proxy.policy.assertion.xmlsec;

import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.xmlsig.InvalidSignatureException;
import com.l7tech.xmlsig.SignatureNotFoundException;
import com.l7tech.xmlsig.SoapMsgSigner;
import org.w3c.dom.Document;

import java.security.cert.X509Certificate;

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
        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        Document doc = null;
        //doc = response.getResponseAsSoapEnvelope();
        // todo, get a document
        X509Certificate cert = null;
        try {
            cert = dsigHelper.validateSignature(doc);
        } catch (SignatureNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        } catch (InvalidSignatureException e) {
            // todo
        } catch (XSignatureException e) {
            // todo
        }
        // todo, verify that this cert is signed with the root cert of this ssg which we should have access to

        // must we also decrypt the body?
        if (data.isEncryption()) {
            // todo, decrypt the body
        }

        // return AssertionStatus.NONE;
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }

    private XmlResponseSecurity data = null;
}
