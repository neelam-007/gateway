package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.common.security.xml.ElementSecurity;
import com.l7tech.common.security.xml.SecurityProcessor;
import com.l7tech.common.security.xml.SecurityProcessorException;
import com.l7tech.common.security.xml.WssProcessor;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.KeystoreUtils;
import com.l7tech.common.util.Locator;
import com.l7tech.identity.User;
import com.l7tech.identity.cert.ClientCertManager;
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
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import sun.security.x509.X500Name;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
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

        ElementSecurity[] elements = xmlRequestSecurity.getElements();
        SecurityProcessor verifier =
                SecurityProcessor.createRecipientSecurityProcessor(wssResults,
                                                                   elements);

        try {
            // TODO clean up sender xml security processor to behave like receiver (throwless return)
            SecurityProcessor.Result result = verifier.processInPlace(soapmsg);

            // Handle unsuccessful results
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
            }

            final X509Certificate[] xmlCertChain = result.getCertificateChain();
            if (xmlCertChain != null) {
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
            }

            if ( request.getPrincipalCredentials() == null ) {
                String msg =  "XmlRequestSecurity unable to validate partial signature because " +
                                "no credential source has been identified yet";
                logger.warning( msg );
                return AssertionStatus.AUTH_REQUIRED;
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
                            response.setParameter(Response.PARAM_HTTP_CERT_STATUS, SecureSpanConstants.INVALID);
                            return AssertionStatus.AUTH_FAILED;
                        }
                    } catch ( FindException e ) {
                        logger.log( Level.WARNING, "Caught FindException retrieving cert for user " + user.getLogin(), e );
                        return AssertionStatus.SERVER_ERROR;
                    }
                }

                if ( xmlCertChain != null && !knownCert.equals( xmlCertChain[0] ) ) {
                    logger.log( Level.WARNING,
                                "XmlRequestSecurity signing certificate did not match previously issued certificate" );
                    response.setParameter(Response.PARAM_HTTP_CERT_STATUS, SecureSpanConstants.INVALID);
                    return AssertionStatus.AUTH_FAILED;
                }

                // FALLTHROUGH - authenticated user matches cert just fine
            } else {
                // Request has credentials (possibly because we found a signed envelope)
                // but user has not yet been authenticated

                if (xmlCertChain != null) {
                    X509Certificate rootCert = getRootCertificate();
                    CertUtils.verifyCertificateChain( xmlCertChain, rootCert, 1 );
                }
            }
        } catch (SecurityProcessorException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.SERVER_ERROR;
        } catch (SignatureException e) {
            // bad signature !
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.AUTH_FAILED;
        } catch (CertUtils.CertificateUntrustedException e) {
            // bad signature !
            logger.log(Level.WARNING, e.getMessage(), e);
            return AssertionStatus.AUTH_FAILED;
        } catch (GeneralSecurityException e) {
            // bad signature or certificate or key or something
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return AssertionStatus.SERVER_ERROR;
        }

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

    protected XmlRequestSecurity xmlRequestSecurity;
    private X509Certificate rootCertificate;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
