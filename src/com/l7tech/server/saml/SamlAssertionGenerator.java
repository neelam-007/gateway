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

    /**
     * Attach the sender vouches assertion to the soap message.
     *
     * @param document the soap message as a org.w3c.dom document
     * @param creds the credentials the assertion is vouching for
     * @param signer the signer info that is vouching for the user
     * @throws IOException on io error
     * @throws SignatureException on signature related error
     * @throws SAXException on xml parsing error
     */
    public void attachSenderVouches(Document document, LoginCredentials creds, SignerInfo signer)
            throws IOException, SignatureException, SAXException, CertificateException {
        Options options = new Options();
        options.setExpiryMinutes(5);
        attachSenderVouches(document, signer, creds, options);
    }

    /**
     * Attach the sender vouches assertion to the soap message.
     *
     * @param document the soap message as a org.w3c.dom document
     * @param creds the credentials the assertion is vouching for
     * @param signer the signer info that is vouching for the user
     * @param options the sender voucher xmlOptions
     * @throws IOException on io error
     * @throws SignatureException on signature related error
     * @throws SAXException on xml parsing error
     */
    public void attachSenderVouches(Document document, SignerInfo signer, LoginCredentials creds, Options options)
            throws IOException, SignatureException, SAXException, CertificateException {
        if (document == null || creds == null || signer == null || options == null) {
            throw new IllegalArgumentException();
        }
        SenderVouchesHelper svh = new SenderVouchesHelper(document, options, creds, signer);
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

        public void setIncludeGroupMembership( boolean includeGroupMembership ) {
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

        public void setClientAddress( InetAddress clientAddress ) {
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

    static final int DEFAULT_EXPIRY_MINUTES = 5;
}
