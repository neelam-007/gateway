package com.l7tech.server.saml;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.InetAddress;
import java.security.SignatureException;
import java.security.cert.CertificateException;

/**
 * Class <code>SamlAssertionGenerator</code> is a central entry point
 * for generating saml messages and attaching them to soap messages.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlAssertionGenerator {
    private final SignerInfo assertionSigner;

    /**
     * Instantiate the <code>SamlAssertionGenerator</code> with the assertion
     * signer (Issuing Authority). If assertion signer is null no assertion signing
     * will be performed
     * @param assertionSigner the assertion signer signer
     */
    public SamlAssertionGenerator(SignerInfo assertionSigner) {
        this.assertionSigner = assertionSigner;
    }

   /**
     * Create the and return the SAML Authentication Statement assertion The SAML assertion
     * is signed by assertion signer in this Assertion Generator.
     *
     * @param creds
     * @param options  the options
     * @return the holder of key assertion for the
     * @throws IOException        on io error
     * @throws SignatureException on signature related error
     * @throws SAXException       on xml parsing error
     * @throws CertificateException on certificate error
     */
    public Document createAutenticationStatement(LoginCredentials creds, Options options)
       throws IOException, SignatureException, SAXException, CertificateException {
         return null;
     }


    /**
     * Attach the sender vouches assertion to the soap message.
     *
     * @param document the soap message as a org.w3c.dom document
     * @param creds    the credentials the assertion is vouching for
     * @throws IOException        on io error
     * @throws SignatureException on signature related error
     * @throws SAXException       on xml parsing error
     */
    public void attachSenderVouches(Document document, LoginCredentials creds)
      throws IOException, SignatureException, SAXException, CertificateException {
        Options options = new Options();
        options.setExpiryMinutes(5);
        attachSenderVouches(document, creds, options);
    }

    /**
     * Attach the sender vouches assertion to the soap message.
     *
     * @param document the soap message as a org.w3c.dom document
     * @param creds    the credentials the assertion is vouching for
     * @param options  the sender voucher xmlOptions
     * @throws IOException        on io error
     * @throws SignatureException on signature related error
     * @throws SAXException       on xml parsing error
     */
    public void attachSenderVouches(Document document, LoginCredentials creds, Options options)
      throws IOException, SignatureException, SAXException, CertificateException {
        if (document == null || creds == null ||  options == null) {
            throw new IllegalArgumentException();
        }
        SenderVouchesHelper svh = new SenderVouchesHelper(document, options, creds, assertionSigner);
        if (options.getId() != null) {
            svh.attachAssertion(true, options.getId());
        } else {
            svh.attachAssertion(true);
        }
        if (options.isSignEnvelope()) svh.signEnvelope();
    }

    public static class Options {
        public int getExpiryMinutes() {
            return expiryMinutes;
        }

        public void setExpiryMinutes(int expiryMinutes) {
            this.expiryMinutes = expiryMinutes;
        }

        public boolean isIncludeGroupMembership() {
            return includeGroupMembership;
        }

        public void setIncludeGroupMembership(boolean includeGroupMembership) {
            this.includeGroupMembership = includeGroupMembership;
        }

        public boolean isSignEnvelope() {
            return signEnvelope;
        }

        public void setSignEnvelope(boolean signEnvelope) {
            this.signEnvelope = signEnvelope;
        }

        public boolean isSignAssertion() {
            return signAssertion;
        }

        public void setSignAssertion(boolean signAssertion) {
            this.signAssertion = signAssertion;
        }

        public InetAddress getClientAddress() {
            return clientAddress;
        }

        public void setClientAddress(InetAddress clientAddress) {
            this.clientAddress = clientAddress;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        boolean includeGroupMembership;
        int expiryMinutes = DEFAULT_EXPIRY_MINUTES;
        InetAddress clientAddress;
        boolean signEnvelope = true;
        boolean signAssertion = true;
        String id = null;

    }

    public static interface SubjectConfirmationStrategy  {

    }

    static final int DEFAULT_EXPIRY_MINUTES = 5;
}
