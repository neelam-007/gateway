package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy). Also does
 * XML Encryption of the response's body if the assertion's property dictates it.
 * <p/>
 * On the server side, this decorates a response with an xml d-sig and maybe signs the body.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope and maybe
 * decyphers the body.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerResponseWssIntegrity implements ServerAssertion {

    public ServerResponseWssIntegrity(ResponseWssIntegrity data) {
        xmlResponseSecurity = data;
    }

    /**
     * despite the name of this method, i'm actually working on the response document here
     */
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if (!(request instanceof SoapRequest)) {
            throw new PolicyAssertionException("This type of assertion is only supported with SOAP type of messages");
        }
        if (!(response instanceof SoapResponse)) {
            throw new PolicyAssertionException("This type of assertion is only supported with SOAP type of messages");
        }
        SoapRequest soapRequest = (SoapRequest)request;
        SoapResponse soapResponse = (SoapResponse)response;
        // GET THE DOCUMENT
        Document soapmsg = null;
        try {
            soapmsg = soapResponse.getDocument();
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            return AssertionStatus.SERVER_ERROR;
        }

        // TODO replace response nonce with more standard mechanism when doing replay protection in Milestone 2
        /*String nonceValue = (String)soapRequest.getParameter(Request.PARAM_HTTP_XML_NONCE);

        // (this is optional)
        if (nonceValue != null && nonceValue.length() > 0) {
            try {
                SecureConversationTokenHandler.appendNonceToDocument(soapmsg, Long.parseLong(nonceValue));
            } catch (MessageNotSoapException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                return AssertionStatus.FAILED;
            }
        } else {
            logger.finest("request did not include a nonce value to use for response's signature");
        }*/

        SignerInfo si = KeystoreUtils.getInstance().getSignerInfo();
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite
        // TODO verify rewrite     verify rewrite     verify rewrite

        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!
        // TODO rewrite rewrite!!


        ((XmlResponse)response).setDocument(soapmsg);
        return AssertionStatus.NONE;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private ResponseWssIntegrity xmlResponseSecurity;
}
