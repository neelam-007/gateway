package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.XmlDsigResAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.util.SoapUtil;
import com.l7tech.util.KeystoreUtils;
import com.l7tech.logging.LogManager;
import com.l7tech.xmlsig.SoapMsgSigner;
import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.PrivateKey;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 26, 2003
 * Time: 3:15:30 PM
 * $Id$
 *
 * XML Digital signature on the soap response sent from the ssg server to the requestor (probably proxy)
 *
 * On the server side, this decorates a response with an xml d-sig.
 * On the proxy side, this verifies that the Soap Response contains a valid xml d-sig for the entire envelope.
 */
public class ServerXmlDsigResAssertion implements ServerAssertion {

    public ServerXmlDsigResAssertion(XmlDsigResAssertion data) {
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {

        Document soapmsg = null;
        try {
            soapmsg = SoapUtil.getDocument(response);
        } catch (SAXException e) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            throw new PolicyAssertionException(msg, e);
        }
        if (soapmsg == null) {
            String msg = "cannot get an xml document from the response to sign";
            logger.severe(msg);
            throw new PolicyAssertionException(msg);
        }

        X509Certificate serverCert = null;
        byte[] buf = KeystoreUtils.getInstance().readSSLCert();
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        try {
            serverCert = (X509Certificate)CertificateFactory.getInstance("X.509").generateCertificate(bais);
        } catch (CertificateException e) {
            String msg = "cannot generate cert from cert file";
            logger.severe(msg);
            throw new PolicyAssertionException(msg);
        }

        PrivateKey serverPrivateKey = null;
        try {
            serverPrivateKey = KeystoreUtils.getInstance().getSSLPrivateKey();
        } catch (KeyStoreException e) {
            String msg = "cannot get ssl private key";
            logger.severe(msg);
            throw new PolicyAssertionException(msg);
        }

        SoapMsgSigner dsigHelper = new SoapMsgSigner();
        try {
            dsigHelper.signEnvelope(soapmsg, serverPrivateKey, serverCert);
        } catch (SignatureStructureException e) {
            String msg = "error signing response";
            logger.log(Level.SEVERE, msg, e);
            throw new PolicyAssertionException(msg, e);
        } catch (XSignatureException e) {
            String msg = "error signing response";
            logger.log(Level.SEVERE, msg, e);
            throw new PolicyAssertionException(msg, e);
        }
        logger.info("Response document signed successfully");
        return AssertionStatus.NONE;
    }

    private Logger logger = LogManager.getInstance().getSystemLogger();
}
