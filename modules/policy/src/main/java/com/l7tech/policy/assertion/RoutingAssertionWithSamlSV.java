package com.l7tech.policy.assertion;

/**
 * RoutingAssertion extension with SAML Sender-Vouches configuration
 *
 * <p>New routing assertions should not usually extend this class as this
 * functionality is now available outside of the routing assertion.</p>
 */
public abstract class RoutingAssertionWithSamlSV extends RoutingAssertion {

    // saml (model as a different bean when serializer supports it)
    private boolean attachSamlSenderVouches;
    private boolean useThumbprintInSamlSignature;
    private boolean useThumbprintInSamlSubject;
    private int samlAssertionVersion = 1; // backwards compatible
    private int samlAssertionExpiry = 5;

    public boolean isAttachSamlSenderVouches() {
        return attachSamlSenderVouches;
    }

    public void setAttachSamlSenderVouches(boolean attachSamlSenderVouches) {
        this.attachSamlSenderVouches = attachSamlSenderVouches;
    }

    /**
     * @return true if the signature of any attached Sender-Vouches SAML assertion should contain a thumbprint instead of the whole signing certificate
     */
    public boolean isUseThumbprintInSamlSignature() {
        return useThumbprintInSamlSignature;
    }

    /**
     * @param useThumbprintInSamlSignature true if the signature of any attached Sender-Vouches SAML assertion should contain a thumbprint instead of the whole signing certificate
     */
    public void setUseThumbprintInSamlSignature(boolean useThumbprintInSamlSignature) {
        this.useThumbprintInSamlSignature = useThumbprintInSamlSignature;
    }

    /**
     * @return true if the subject cert in any attached Sender-Vouches SAML assertion should be replaced with a thumbprint
     */
    public boolean isUseThumbprintInSamlSubject() {
        return useThumbprintInSamlSubject;
    }

    /**
     * @param useThumbprintInSamlSubject true if the subject cert in any attached Sender-Vouches SAML assertion should be replaced with a thumbprint
     */
    public void setUseThumbprintInSamlSubject(boolean useThumbprintInSamlSubject) {
        this.useThumbprintInSamlSubject = useThumbprintInSamlSubject;
    }

    public int getSamlAssertionVersion() {
        return samlAssertionVersion;
    }

    public void setSamlAssertionVersion(int samlAssertionVersion) {
        this.samlAssertionVersion = samlAssertionVersion;
    }

    public int getSamlAssertionExpiry() {
        return samlAssertionExpiry;
    }

    public void setSamlAssertionExpiry(int samlAssertionExpiry) {
        if (samlAssertionExpiry <= 0) {
            throw new IllegalArgumentException();
        }
        this.samlAssertionExpiry = samlAssertionExpiry;
    }

    protected void copyFrom( final RoutingAssertionWithSamlSV source ) {
        super.copyFrom( source );
        this.setUseThumbprintInSamlSignature(source.isUseThumbprintInSamlSignature());
        this.setUseThumbprintInSamlSubject(source.isUseThumbprintInSamlSubject());
        this.setAttachSamlSenderVouches(source.isAttachSamlSenderVouches());
        this.setSamlAssertionExpiry(source.getSamlAssertionExpiry());
        this.setSamlAssertionVersion(source.getSamlAssertionVersion());
    }
}
