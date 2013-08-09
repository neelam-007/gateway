package com.l7tech.gateway.common.security;

import com.l7tech.objectmodel.Goid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An Item in a Certificate Revocation Checking Policy.
 *
 * @author Steve Jones
 * @see RevocationCheckPolicy
 */
public class RevocationCheckPolicyItem implements Serializable {

    //- PUBLIC

    /**
     * Revocation checking type for a policy item.
     */
    public enum Type {
        /**
         * Type for checking against a CRL from a URL contained in an X.509 Certificate
         */
        CRL_FROM_CERTIFICATE (false),
        /**
         * Type for checking against a CRL from a specified URL
         */
        CRL_FROM_URL (true),
        /**
         * Type for OCSP check against a responder URL contained in an X.509 Certificate
         */
        OCSP_FROM_CERTIFICATE (false),
        /**
         * Type for OCSP check against a specified responder URL
         */
        OCSP_FROM_URL (true);

        private final boolean urlSpecified;

        private Type(boolean urlSpecified) {
            this.urlSpecified = urlSpecified;    
        }

        /**
         * Is the URL specifed as part of the policy?
         *
         * @return true if the URL is specified, false if from an X.509 Certificate 
         */
        public boolean isUrlSpecified() {
            return urlSpecified;
        }
    }

    /**
     * Create an uninitialized RevocationCheckPolicyItem
     */
    public RevocationCheckPolicyItem() {
        trustedSigners = new ArrayList<Goid>();
    }

    /**
     * Get the type for this item.
     *
     * @return the Type (may be null)
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the type for the item.
     *
     * @param type The type to use
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Get the URL or URL Regex for this item.
     *
     * @return The url/url regex (may be null)
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the URL or URL Regex for this item.
     *
     * @param url The url/url regex
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Is an issuer signature accepted when evaluating trust?
     *
     * @return true if an issuer or issuer delegated signer is accepted
     */
    public boolean isAllowIssuerSignature() {
        return allowIssuerSignature;
    }

    /**
     * Specify if an issuer signature accepted when evaluating trust.
     *
     * @param allowIssuerSignature true to permit an issuer or issuer delegated signer
     */
    public void setAllowIssuerSignature(boolean allowIssuerSignature) {
        this.allowIssuerSignature = allowIssuerSignature;
    }

    /**
     * Get the list of trusted signers for this item.
     *
     * @return The list of Signers (will not be null)
     */
    public List<Goid> getTrustedSigners() {
        return new ArrayList<Goid>(trustedSigners);
    }

    /**
     * Set the trusted signers for this item.
     *
     * @param trustedSigners The list of signers
     */
    public void setTrustedSigners(List<Goid> trustedSigners) {
        if (trustedSigners == null) {
            this.trustedSigners = new ArrayList<Goid>();
        } else {
            this.trustedSigners = new ArrayList<Goid>(trustedSigners);
        }
    }

    /**
     * Value based equality check.
     */
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RevocationCheckPolicyItem that = (RevocationCheckPolicyItem) o;

        return allowIssuerSignature == that.allowIssuerSignature &&
                trustedSigners.equals( that.trustedSigners ) &&
                type == that.type &&
                !( url != null ? !url.equals( that.url ) : that.url != null );
    }

    /**
     * Value based hashcode.
     */
    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (allowIssuerSignature ? 1 : 0);
        result = 31 * result + trustedSigners.hashCode();
        return result;
    }

    //- PRIVATE

    private Type type;
    private String url;
    private boolean allowIssuerSignature;
    private List<Goid> trustedSigners;

}
