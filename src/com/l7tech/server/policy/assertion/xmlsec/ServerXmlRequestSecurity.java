package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.util.*;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.XmlRequest;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.server.SessionManager;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SignatureException;
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
public class ServerXmlRequestSecurity implements ServerAssertion {
    public ServerXmlRequestSecurity(XmlRequestSecurity data) {
        xmlRequestSecurity = data;
        logger = LogManager.getInstance().getSystemLogger();
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        // get the document
        Document soapmsg = extractDocumentFromRequest(request);

        // get the session
        Session xmlsecSession = null;
        try {
            xmlsecSession = getXmlSecSession(soapmsg);
        } catch (SessionInvalidException e) {
            // when the session is no longer valid we must inform the client proxy so that he generates another session
            response.setParameter(Response.PARAM_HTTP_SESSION_STATUS, "invalid");
            return AssertionStatus.FALSIFIED;
        }

        if (xmlsecSession == null) {
            response.setPolicyViolated(true);
            response.setAuthenticationMissing(true);
            return AssertionStatus.FALSIFIED;
        }

        // check validity of the session
        long gotSeq;
        try {
            gotSeq = checkSeqNrValidity(soapmsg, xmlsecSession);
        } catch (InvalidSequenceNumberException e) {
            // when the session is no longer valid we must inform the client proxy so that he generates another session
            response.setParameter(Response.PARAM_HTTP_SESSION_STATUS, "invalid");
            // todo, control what the soap fault look like
            // in this case the policy is not violated
            // response.setPolicyViolated(true);
            return AssertionStatus.FALSIFIED;
        }
        Key key = xmlsecSession.getKeyReq() != null ? new AesKey(xmlsecSession.getKeyReq(), 128) : null;
        ElementSecurity[] elements = xmlRequestSecurity.getElements();
        SecurityProcessor verifier = SecurityProcessor.getVerifier(xmlsecSession, key, elements);

        try {
            SecurityProcessor.Result result = verifier.processInPlace(soapmsg);

            final X509Certificate[] xmlCertChain = result.getCertificateChain();
            if ( !result.isPreconditionMatched() ) {
                logger.log( Level.INFO, "No XML security expected in this request" );
                return AssertionStatus.NONE;
            }

            X500Name x500name = new X500Name(xmlCertChain[0].getSubjectX500Principal().getName());
            String certCN = x500name.getCommonName();
            logger.finest("cert extracted from digital signature for user " + certCN);

            // Fix for Bug #723: Check that cert is valid and matches authenticated user
            if (xmlRequestSecurity.hasAuthenticationElement() ) {
                if ( request.isAuthenticated() ) {
                    logger.fine( "Request was already authenticated but this XmlRequestSecurity is usable as a credential source" );
                } else {
                    // We don't care if there are previously-asserted credentials in the request
                    logger.info( "Using credentials from certificate in signed XML request" );
                    request.setPrincipalCredentials(new LoginCredentials(certCN, null, CredentialFormat.CLIENTCERT, null, xmlCertChain[0]));
                }
            }

            if ( request.getPrincipalCredentials() == null ) {
                String msg =  "XmlRequestSecurity unable to validate partial signature because " +
                                "no credential source has been identified yet";
                logger.warning( msg );
                return AssertionStatus.FALSIFIED;
            }

            if ( request.isAuthenticated() ) {
                User user = request.getUser();
                if (user == null)
                    throw new PolicyAssertionException( "Request authenticated but no user set" );

                X509Certificate knownCert = null;
                LoginCredentials pc = request.getPrincipalCredentials();
                if ( pc.getFormat().isClientCert() ) {
                    knownCert = (X509Certificate)request.getPrincipalCredentials().getPayload();
                }

                if (knownCert == null) {
                    ClientCertManager ccm = (ClientCertManager)Locator.getDefault().lookup( ClientCertManager.class );
                    try {
                        knownCert = (X509Certificate)ccm.getUserCert( user );
                        if ( knownCert == null ) {
                            logger.log( Level.WARNING, "User '" + user.getLogin() + "' does not currently have a certificate" );
                            return AssertionStatus.FALSIFIED;
                        }
                    } catch ( FindException e ) {
                        logger.log( Level.WARNING, "Caught FindException retrieving cert for user " + user.getLogin(), e );
                        return AssertionStatus.FALSIFIED;
                    }
                }

                if ( !knownCert.equals( xmlCertChain[0] ) ) {
                    logger.log( Level.WARNING,
                                "XmlRequestSecurity signing certificate did not match previously issued certificate" );
                    return AssertionStatus.AUTH_FAILED;
                }

                // FALLTHROUGH - authenticated user matches cert just fine
            } else {
                // Request has credentials (possibly because we found a signed envelope)
                // but user has not yet been authenticated

                X509Certificate rootCert = getRootCertificate();
                CertUtils.verifyCertificateChain( xmlCertChain, rootCert );
            }
        } catch (SecurityProcessorException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        } catch (SignatureException e) {
            if (ExceptionUtils.causedBy(e, SignatureNotFoundException.class)) {
                // no digital signature
                response.setAuthenticationMissing(true);
                response.setPolicyViolated(true);
                logger.log(Level.WARNING, e.getMessage(), e);
                return AssertionStatus.FALSIFIED;
            }
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        } catch (CertUtils.CertificateUntrustedException e) {
            // bad signature !            
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        } catch (GeneralSecurityException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.FALSIFIED;
        }

        // so mark this sequence number as used up
        xmlsecSession.hitSequenceNumber(gotSeq);
        // clean the session id from the security header
        SecureConversationTokenHandler.consumeSessionInfoFromDocument(soapmsg);

        // clean empty security element and header if necessary
        
        SoapUtil.cleanEmptyRefList(soapmsg);
        SoapUtil.cleanEmptySecurityElement(soapmsg);
        SoapUtil.cleanEmptyHeaderElement(soapmsg);

        // note, the routing should no longer use the non parsed payload
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


    private static class InvalidSequenceNumberException extends Exception {
        public InvalidSequenceNumberException(String message) {
            super(message);
        }
    }

    private long checkSeqNrValidity(Document soapmsg, Session session) throws InvalidSequenceNumberException {
        Long seqNr;
        seqNr = SecureConversationTokenHandler.readSeqNrFromDocument(soapmsg);
        if (seqNr == null) {
            logger.severe("request contains no sequence number");
            throw new InvalidSequenceNumberException("request contains no sequence number");
        }

        long seq = seqNr.longValue();

        if (seq < session.getHighestSeq()) {
            logger.severe("sequence number too low (" + seqNr + "). someone is trying replay attack?");
            throw new InvalidSequenceNumberException("request contains a sequence number which is too low");
        }
        return seq;
    }

    private Session getXmlSecSession(Document soapmsg) throws SessionInvalidException {
        // get the session id from the security context
        Long sessionID = SecureConversationTokenHandler.readSessIdFromDocument(soapmsg);
        if ( sessionID == null ) {
            String msg = "could not extract session id from msg.";
            logger.log(Level.WARNING, msg);
            return null;
        }

        // retrieve the session
        Session xmlsession = null;
        try {
            xmlsession = SessionManager.getInstance().getSession(sessionID.longValue());
        } catch (SessionNotFoundException e) {
            String msg = "Exception finding session with id=" + sessionID + ". session could reside on other cluster member or is no longer valid.";
            logger.log(Level.WARNING, msg, e);
            throw new SessionInvalidException(msg, e);
        } catch (NumberFormatException e) {
            String msg = "Session id is not long value : " + sessionID;
            logger.log(Level.SEVERE, msg, e);
            throw new SessionInvalidException(msg, e);
        }

        return xmlsession;
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

    protected XmlRequestSecurity xmlRequestSecurity;
    private Logger logger = null;
    private X509Certificate rootCertificate;


}
