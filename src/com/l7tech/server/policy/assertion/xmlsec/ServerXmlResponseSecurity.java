package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.xml.MessageNotSoapException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import com.l7tech.server.SessionManager;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.util.ServerSoapUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.util.logging.Level;
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
public class ServerXmlResponseSecurity implements ServerAssertion {

    public ServerXmlResponseSecurity(XmlResponseSecurity data) {
        xmlResponseSecurity = data;
    }

    /**
     * despite the name of this method, i'm actually working on the response document here
     */
    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        // GET THE DOCUMENT
        Document soapmsg = null;
        try {
            soapmsg = ServerSoapUtil.getDocument(response);
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            return AssertionStatus.SERVER_ERROR;
        }
        if (soapmsg == null) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            return AssertionStatus.SERVER_ERROR;
        }

        String nonceValue = (String)request.getParameter(Request.PARAM_HTTP_XML_NONCE);

        // (this is optional)
        if (nonceValue != null && nonceValue.length() > 0) {
            try {
                SecureConversationTokenHandler.appendNonceToDocument(soapmsg, Long.parseLong(nonceValue));
            } catch ( MessageNotSoapException e ) {
                logger.log( Level.WARNING, e.getMessage(), e );
                return AssertionStatus.FAILED;
            }
        } else {
            logger.finest("request did not include a nonce value to use for response's signature");
        }

        Session xmlsession = null;
        Key encryptionKey = null;
        Object sessionObj = request.getParameter(Request.PARAM_HTTP_XML_SESSID);
        if (sessionObj != null) {
            // construct Session object based on the type of the context's object
            if (sessionObj instanceof Session) {
                xmlsession = (Session)sessionObj;
            } else if (sessionObj instanceof String) {
                String sessionIDHeaderValue = (String)sessionObj;
                // retrieve the session
                try {
                    xmlsession = SessionManager.getInstance().getSession(Long.parseLong(sessionIDHeaderValue));
                    encryptionKey = xmlsession.getKeyRes() != null ? new AesKey(xmlsession.getKeyRes(), 128) : null;
                } catch (SessionNotFoundException e) {
                    String msg = "Exception finding session with id=" + sessionIDHeaderValue;
                    response.setParameter(Response.PARAM_HTTP_SESSION_STATUS, SecureSpanConstants.INVALID);
                    response.setPolicyViolated(true);
                    logger.log(Level.WARNING, msg, e);
                    return AssertionStatus.FAILED;
                } catch (NumberFormatException e) {
                    String msg = "Session id is not long value : " + sessionIDHeaderValue;
                    response.setParameter(Response.PARAM_HTTP_SESSION_STATUS, SecureSpanConstants.INVALID);
                    logger.log(Level.WARNING, msg, e);
                    return AssertionStatus.FAILED;
                }
            }
        }
        SignerInfo si = KeystoreUtils.getInstance().getSignerInfo();
        ElementSecurity[] elements = xmlResponseSecurity.getElements();

        // TODO verify TROGDOR integration
        SecurityProcessor signer = SecurityProcessor.createSenderSecurityProcessor(si, elements);
        try {
            signer.processInPlace(soapmsg);
        } catch (SecurityProcessorException e) {
            String msg = "error signing/encrypting response";
            logger.log(Level.SEVERE, msg, e);
            return AssertionStatus.FAILED;
        } catch (GeneralSecurityException e) {
            String msg = "error signing response";
            logger.log(Level.SEVERE, msg, e);
            return AssertionStatus.FAILED;
        }
        ((XmlResponse)response).setDocument(soapmsg);
        return AssertionStatus.NONE;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private XmlResponseSecurity xmlResponseSecurity;
}
