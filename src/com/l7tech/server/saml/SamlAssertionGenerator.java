package com.l7tech.server.saml;

import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.identity.User;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.security.SignatureException;

/**
 * Class <code>SamlAssertionGenerator</code> is a central entry point
 * for generating saml messages and attaching them to soap messages.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlAssertionGenerator {

    /**
     * Attach the sender voouches assertion to the soap message.
     *
     * @param document the soap message as a org.w3c.dom document
     * @param u the user the assertion is vouching for
     * @param signer the signer info that is vouching for the user
     * @throws IOException on io error
     * @throws SignatureException on signature related error
     * @throws SAXException on xml parsing error
     */
    public void attachSenderVouches(Document document, User u, SignerInfo signer)
      throws IOException, SignatureException, SAXException {
        Options options = new Options();
        options.setExpiryMinutes(5);
        attachSenderVouches(document, u, signer, options);
    }

    /**
     * Attach the sender voouches assertion to the soap message.
     *
     * @param document the soap message as a org.w3c.dom document
     * @param u the user the assertion is vouching for
     * @param signer the signer info that is vouching for the user
     * @param options the sender voucher xmlOptions
     * @throws IOException on io error
     * @throws SignatureException on signature related error
     * @throws SAXException on xml parsing error
     */
    public void attachSenderVouches(Document document, User u, SignerInfo signer, Options options)
      throws IOException, SignatureException, SAXException {
        if (document == null || u == null || signer == null || options == null) {
            throw new IllegalArgumentException();
        }
        SenderVouchesHelper svh = new SenderVouchesHelper(document, u,
                                                          options.includeGroupMembership,
                                                          options.expiryMinutes,
                                                          signer);
        svh.attachAssertion(true);
        svh.signEnvleope();
    }

    /**
     * the class with xmlOptions that may be passed to the saml assertion
     * generator.
     */
    public static class Options {

        public boolean isIncludeGroupMembership() {
            return includeGroupMembership;
        }

        public void setIncludeGroupMembership() {
            this.includeGroupMembership = true;
        }

        public int getExpiryMinutes() {
            return expiryMinutes;
        }

        public void setExpiryMinutes(int expiryMinutes) {
            this.expiryMinutes = expiryMinutes;
        }

        private int expiryMinutes;
        private boolean includeGroupMembership;
    }
}
