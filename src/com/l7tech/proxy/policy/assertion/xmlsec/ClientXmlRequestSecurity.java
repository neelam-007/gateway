package com.l7tech.proxy.policy.assertion.xmlsec;

import com.ibm.xml.dsig.SignatureStructureException;
import com.ibm.xml.dsig.XSignatureException;
import com.l7tech.common.security.AesKey;
import com.l7tech.common.security.xml.*;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.ElementSecurity;
import com.l7tech.policy.assertion.xmlsec.SecurityProcessor;
import com.l7tech.policy.assertion.xmlsec.SecurityProcessorException;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.*;
import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.policy.assertion.credential.http.ClientHttpClientCert;
import com.l7tech.proxy.util.ClientLogger;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * XML Digital signature on the soap request sent from the proxy to the ssg server. Also does XML
 * Encryption of the request's body if the assertion's property requires it.
 * <p/>
 * On the server side, this must verify that the SoapRequest contains a valid xml d-sig for the entire envelope and
 * maybe decyphers the body.
 * <p/>
 * On the proxy side, this must decorate a request with an xml d-sig and maybe encrypt the body.
 * <p/>
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 26, 2003<br/>
 * $Id$
 */
public class ClientXmlRequestSecurity extends ClientAssertion {
    private static final ClientLogger log = ClientLogger.getInstance(ClientHttpClientCert.class);

    public ClientXmlRequestSecurity(XmlRequestSecurity data) {
        this.data = data.getElements();
        if (data == null) {
            throw new IllegalArgumentException("security elements is null");
        }
    }

    /**
     * ClientProxy client-side processing of the given request.
     *
     * @param request The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    public AssertionStatus decorateRequest(PendingRequest request)
      throws OperationCanceledException, BadCredentialsException,
      GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, ClientCertificateException {
        // GET THE SOAP DOCUMENT
        Document soapmsg = null;
        soapmsg = request.getSoapEnvelope(); // this will make a defensive copy as needed

        // get the client cert and private key
        // We must have credentials to get the private key
        Ssg ssg = request.getSsg();
        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        Session session;
        request.getCredentials();
        session = request.getOrCreateSession();

        request.prepareClientCertificate();

        try {
            userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
            userCert = SsgKeyStoreManager.getClientCert(ssg);
            final SignerInfo si = new SignerInfo(userPrivateKey, userCert);
            // decorate request with session info and seq nr
            request.setSession(session);
            long sessId = session.getId();
            long seqNr = session.nextSequenceNumber();
            byte[] keyreq = session.getKeyReq();
            Key encryptionKey = keyreq != null ? new AesKey(keyreq, 128) : null;
            SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(soapmsg, sessId, seqNr);
            SecurityProcessor signer = SecurityProcessor.getSigner(session, si, encryptionKey, data);
            signer.processInPlace(soapmsg);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (SecurityProcessorException e) {
            throw new RuntimeException(e);
        }
//        XmlUtil.documentToOutputStream(soapmsg, System.out);
        request.setSoapEnvelope(soapmsg);
        return AssertionStatus.NONE;
    }


    /**
     * ClientProxy client-side processing of the given request (do not use, this is old version for reference).
     *
     * @param request The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     */
    private AssertionStatus decorateRequestOld(PendingRequest request)
      throws OperationCanceledException, BadCredentialsException,
      GeneralSecurityException, IOException, KeyStoreCorruptException, HttpChallengeRequiredException, PolicyRetryableException, ClientCertificateException {
        // GET THE SOAP DOCUMENT
        Document soapmsg = null;
        soapmsg = request.getSoapEnvelope(); // this will make a defensive copy as needed

        // GET THE CLIENT CERT AND PRIVATE KEY
        // We must have credentials to get the private key
        Ssg ssg = request.getSsg();
        PrivateKey userPrivateKey = null;
        X509Certificate userCert = null;
        Session session;
        request.getCredentials();
        session = request.getOrCreateSession();

        request.prepareClientCertificate();

        try {
            userPrivateKey = SsgKeyStoreManager.getClientCertPrivateKey(ssg);
            userCert = SsgKeyStoreManager.getClientCert(ssg);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // can't happen
        }

        // DECORATE REQUEST WITH SESSION INFO AND SEQ NR
        request.setSession(session);
        long sessId = session.getId();
        long seqNr = session.nextSequenceNumber();
        byte[] keyreq = session.getKeyReq();
        SecureConversationTokenHandler.appendSessIdAndSeqNrToDocument(soapmsg, sessId, seqNr);

        try {
            String encReferenceId = "encref";
            String signReferenceId = "signref";
            int encReferenceIdSuffix = 1;
            int signReferenceIdSuffix = 1;
            SoapMsgSigner dsigHelper = new SoapMsgSigner();
            // encryption
            for (int i = 0; i < data.length; i++) {
                ElementSecurity elementSecurity = data[i];
                // XPath match?
                XpathExpression xpath = elementSecurity.getxPath();
// XmlUtil.documentToOutputStream(soapmsg, System.out);
                List nodes = XpathEvaluator.newEvaluator(soapmsg, xpath.getNamespaces()).select(xpath.getExpression());
                if (nodes.isEmpty()) continue; // nothing selected
                Element element = (Element)nodes.get(0);
                if (isEncryption()) {
                    checkEncryptionProperties(elementSecurity);
                    XmlMangler.encryptXml(element, keyreq, Long.toString(sessId), encReferenceId + encReferenceIdSuffix);
                    ++encReferenceIdSuffix;
                    log.info("Encrypted request OK");
                }
                // digital sighnature
                dsigHelper.signElement(soapmsg, element, signReferenceId + signReferenceIdSuffix, userPrivateKey, userCert);
                ++signReferenceIdSuffix;
                log.info("Signed request OK");
            }
        } catch (SignatureStructureException e) {
            throw new RuntimeException("error signing document", e);
        } catch (XSignatureException e) {
            throw new RuntimeException("error signing document", e);
        } catch (JaxenException e) {
            throw new RuntimeException("XPath error", e);
        }

        // SET BACK ALL THIS IN PENDING REQUEST
//        XmlUtil.documentToOutputStream(soapmsg, System.out);
        request.setSoapEnvelope(soapmsg);

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    public String getName() {
        return "XML Request Security - " + (isEncryption() ? "sign and encrypt" : "sign only");
    }

    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/xmlencryption.gif";
    }

    /**
     * Tests whether any of the security elements requires encryption
     *
     * @return true if any of the lements requires encryption
     */
    private boolean isEncryption() {
        for (int i = 0; i < data.length; i++) {
            ElementSecurity elementSecurity = data[i];
            if (elementSecurity.isEncryption()) return true;
        }
        return false;
    }

    /**
     * Check whether the encryption properties are supported
     *
     * @param elementSecurity the security element specifying the security properties
     */
    private static void checkEncryptionProperties(ElementSecurity elementSecurity)
      throws NoSuchAlgorithmException, SecurityException {
        if (!"AES".equals(elementSecurity.getCipher()))
            throw new NoSuchAlgorithmException("Unable to encrypt request: unsupported cipher: " + elementSecurity.getCipher());
        if (128 != elementSecurity.getKeyLength())
            throw new SecurityException("Unable to encrypt request: unsupported key length: " + elementSecurity.getKeyLength());
    }


    protected ElementSecurity[] data;
}
