package com.l7tech.server.saml;

import com.l7tech.identity.User;
import com.l7tech.common.security.xml.SignerInfo;

import javax.xml.soap.SOAPMessage;

/**
 * Class SamlAssertionGenerator.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlAssertionGenerator {

    /**
     * Attach the sender voouches assertion to the soap message.
     *
     * @param sm the soap message
     * @param u the user the assertion is vouching for
     * @param signer the signer info that is vouching for the user
     */
    public void attachSenderVouches(SOAPMessage sm, User u, SignerInfo signer) {
    }

    public void attachSenderVouches(SOAPMessage sm, User u, SignerInfo signer, Options options) {
    }

    /**
     * the class with options that may be passed to the saml assertion
     * generator.
     */
    public static class Options {

        public boolean isIncludeGroupMembership() {
            return includeGroupMembership;
        }

        public void setIncludeGroupMembership(boolean includeGroupMembership) {
            this.includeGroupMembership = includeGroupMembership;
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
