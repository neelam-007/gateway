package com.l7tech.proxy.policy.assertion.xmlsec;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlDsigResAssertion;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.l7tech.xmlsig.SignatureNotFoundException;
import com.l7tech.xmlsig.InvalidSignatureException;
import com.ibm.xml.dsig.XSignatureException;
import org.w3c.dom.Document;

import javax.xml.soap.SOAPException;
import java.security.cert.X509Certificate;

/**
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 2:54:01 PM
 * $Id$
 *
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy)
 *
 * On the server side, this decorates a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope.
 *
 * @author flascell
 */
public class ClientXmlDsigResAssertion implements ClientAssertion {

    public ClientXmlDsigResAssertion(XmlDsigResAssertion data) {
    }

    /**
     * i dont want to decorate a request but rather validate something in the response
     *
     * @param request left untouched
     * @return AssertionStatus.NONE (always)
     * @throws PolicyAssertionException no
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
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
        // return AssertionStatus.NONE;
        return AssertionStatus.NOT_YET_IMPLEMENTED;
    }
}
