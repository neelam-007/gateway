package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.XmlRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * XML Digital signature on the soap request sent from a requestor (probably proxy) to the ssg server. Also does XML
 * Encryption of the request's body if the assertion's property dictates it.
 * <p/>
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope and
 * maybe decyphers the body.
 * On the proxy side, this must decorate a request with an xml d-sig and maybe encrypt the body.
 * <p/>
 * This extends ServerWssCredentialSource because once the validity of the signature if confirmed, the cert is used
 * as credentials.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ServerRequestWssConfidentiality implements ServerAssertion {
    public ServerRequestWssConfidentiality(RequestWssConfidentiality data) {
        requestWssConfidentiality = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if (!(request instanceof SoapRequest)) {
            throw new PolicyAssertionException("This type of assertion is only supported with SOAP type of messages");
        }
        SoapRequest soapreq = (SoapRequest)request;
        final WssProcessor.ProcessorResult wssResults = soapreq.getWssProcessorOutput();
        if (wssResults == null) {
            throw new PolicyAssertionException("This request was not processed for WSS level security.");
        }

        // get the document
        Document soapmsg = null;
        try {
            soapmsg = soapreq.getDocument();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Cannot get payload document.", e);
            return AssertionStatus.BAD_REQUEST;
        }

        // TODO rewrite rewrite rewrite
//        SecurityProcessor verifier =
//                SecurityProcessor.createRecipientSecurityProcessor(wssResults,
//                                                                   elements);
        // TODO rewrite rewrite rewrite
        // TODO rewrite rewrite rewrite
        // TODO rewrite rewrite rewrite
        // TODO rewrite rewrite rewrite
        // TODO rewrite rewrite rewrite
        // TODO rewrite rewrite rewrite
        // TODO rewrite rewrite rewrite

            // TODO rewrite rewrite
            //SecurityProcessor.Result result = verifier.processInPlace(soapmsg);

            // Handle unsuccessful results
            /*
            if (result.getType() == SecurityProcessor.Result.Type.NOT_APPLICABLE) {
                logger.log(Level.INFO, "No XML security expected in this request");
                return AssertionStatus.NONE;
            } else if (result.getType() == SecurityProcessor.Result.Type.POLICY_VIOLATION) {
                if (result.getThrowable() != null)
                    logger.log(Level.INFO, result.getType().desc, result.getThrowable());
                else
                    logger.log(Level.INFO, result.getType().desc);
                logger.info("Returning " + AssertionStatus.AUTH_REQUIRED.getMessage());
                response.setAuthenticationMissing(true);
                response.setPolicyViolated(true);
                return AssertionStatus.AUTH_REQUIRED;
            } else if ( result.getType() == SecurityProcessor.Result.Type.ERROR ) {
                if (result.getThrowable() != null)
                    logger.log( Level.WARNING, result.getType().desc, result.getThrowable() );
                else
                    logger.log( Level.WARNING, result.getType().desc );
                return AssertionStatus.FAILED;
            }*/

            // TODO rewrite rewrite
            // TODO rewrite rewrite
            // TODO rewrite rewrite
            // TODO rewrite rewrite
            // TODO rewrite rewrite

        // todo note, the routing should no longer use the non parsed payload
        ((XmlRequest)request).setDocument(soapmsg);

        return AssertionStatus.NONE;
    }

    private synchronized X509Certificate getRootCertificate() throws CertificateException, IOException {
        if ( rootCertificate == null ) {
            CertificateFactory certFactory = CertificateFactory.getInstance( "X.509" );
            rootCertificate = (X509Certificate)certFactory.generateCertificate( new ByteArrayInputStream( KeystoreUtils.getInstance().readRootCert() ) );
        }
        return rootCertificate;
    }

    private Document extractDocumentFromRequest(Request req) throws PolicyAssertionException {
        // try to get credentials out of the digital signature
        Document soapmsg = null;
        try {
            soapmsg = ((SoapRequest)req).getDocument();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "could not get request's xml document", e);
            throw new PolicyAssertionException("cannot extract name from cert", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "could not get request's xml document", e);
            throw new PolicyAssertionException("cannot extract name from cert", e);
        }

        return soapmsg;
    }

    protected RequestWssConfidentiality requestWssConfidentiality;
    private X509Certificate rootCertificate;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
