package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.ElementExtensionSupport;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The RevocationCheckingPolicyItemMO managed object represents a revocation
 * checking policy item.
 *
 * @see ManagedObjectFactory#createRevocationCheckingPolicyItem()
 */
@XmlType(name="RevocationCheckingPolicyItem", propOrder={"type","url","allowIssuerSignature","trustedSigners","extension","extensions"})
public class RevocationCheckingPolicyItemMO  extends ElementExtensionSupport {
    //- PUBLIC

    /**
     * Get the type for this item.
     *
     * @return the Type (may be null)
     */
    @XmlElement(name="Type", required=true)
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

    @XmlElement(name="Url", required=true)
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
    @XmlElement(name="AllowIssuerSignature")
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
     * Get the id list of trusted signers for this item.
     *
     * @return The list of Signers (will not be null)
     */
    @XmlElement(name="TrustedSigners")
    public List<String> getTrustedSigners() {
        return new ArrayList<String>(trustedSigners);
    }

    /**
     * Set the trusted signers' ids for this item.
     *
     * @param trustedSigners The list of signers' ids
     */
    public void setTrustedSigners(List<String> trustedSigners) {
        if (trustedSigners == null) {
            this.trustedSigners = new ArrayList<String>();
        } else {
            this.trustedSigners = new ArrayList<String>(trustedSigners);
        }
    }

    /**
     * Revocation checking type for a policy item.
     */
    @XmlEnum(String.class)
    @XmlType(name="RevocationCheckingPolicyItemType")
    public enum Type {
        /**
         * Type for checking against a CRL from a URL contained in an X.509 Certificate
         */
        @XmlEnumValue("CRL from certificate URL") CRL_FROM_CERTIFICATE,

        /**
         * Type for checking against a CRL from a specified URL
         */
        @XmlEnumValue("CRL from URL") CRL_FROM_URL,

        /**
         * Type for OCSP check against a responder URL contained in an X.509 Certificate
         */
        @XmlEnumValue("OCSP from certificate URL") OCSP_FROM_CERTIFICATE,

        /**
         * Type for OCSP check against a specified responder URL
         */
        @XmlEnumValue("OCSP from URL") OCSP_FROM_URL
    }

    //- PACKAGE

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    RevocationCheckingPolicyItemMO() {
    }

    //- PRIVATE

    private Type type;
    private String url;
    private boolean allowIssuerSignature = false;
    private List<String> trustedSigners= new ArrayList<String>();
}
