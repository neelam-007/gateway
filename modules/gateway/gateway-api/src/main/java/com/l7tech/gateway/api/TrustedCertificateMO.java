package com.l7tech.gateway.api;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;
import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.ElementExtensionSupport;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * The TrustedCertificateMO managed object represents a trusted certificate.
 *
 * <p>The following properties are used:
 * <ul>
 *   <li><code>revocationCheckingEnabled</code>: When true revocation checking
 *       is performed for the certificate.</li>
 *   <li><code>trustAnchor</code>: When true the certificate is a trust anchor
 *       (terminates a certificate path)</li>
 *   <li><code>trustedAsSamlAttestingEntity</code>: When true the certificate
 *       is trusted as the attesting entity for SAML tokens.</li>
 *   <li><code>trustedAsSamlIssuer</code>: When true the certificate is trusted
 *       as the issuer for SAML tokens.</li>
 *   <li><code>trustedForSigningClientCerts</code>: When true the certificate
 *       is trusted for signing client certificates.</li>
 *   <li><code>trustedForSigningServerCerts</code>: When true the certificate
 *       is trusted for signing server certificates (TLS/SSL)</li>
 *   <li><code>trustedForSsl</code>: When true the certificate is directly
 *       trusted as a server certificate.</li>
 *   <li><code>verifyHostname</code>: Should SSL/TLS server host names be
 *       verified for this certificate (true if not specified)</li>
 * </ul>
 * Properties default to false unless otherwise noted.
 * </p>
 *
 * <p>The Accessor for trusted certificates supports read and write. Trusted certificates
 * can be accessed by identifier only.</p>
 *
 * @see ManagedObjectFactory#createTrustedCertificate()
 */
@XmlRootElement(name="TrustedCertificate")
@XmlType(name="TrustedCertificateType", propOrder={"nameValue","certificateData","properties","trustedCertificateExtension","extensions"})
@AccessorSupport.AccessibleResource(name ="trustedCertificates")
public class TrustedCertificateMO extends SecurityZoneableObject {
    
    //- PUBLIC

    /**
     * The name for the trusted certificate (required)
     *
     * @return The name or null.
     */
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the trusted certificate.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the certificate data for the trusted certificate (required)
     *
     * @return The certificate data or null.
     */
    @XmlElement(name="CertificateData", required=true)
    public CertificateData getCertificateData() {
        return certificateData;
    }

    /**
     * Set the certificate data for the trusted certificate.
     *
     * @param certificateData The certificate data to use.
     */
    public void setCertificateData( final CertificateData certificateData ) {
        this.certificateData = certificateData;
    }

    /**
     * Get the identifier of the revocation checking policy (optional)
     *
     * @return The identifier for the policy or null
     */
    public String getRevocationCheckingPolicyId() {
        return trustedCertificateExtension.revocationCheckingPolicyReference == null ?
                null :
                trustedCertificateExtension.revocationCheckingPolicyReference.getId();
    }

    /**
     * Set the identifier for the revocation checking policy or null for none
     *
     * @param id The policy identifier.
     */
    public void setRevocationCheckingPolicyId( final String id ) {
        trustedCertificateExtension.revocationCheckingPolicyReference = id==null ?
                null :
                new ManagedObjectReference( RevocationCheckingPolicyMO.class, id );
    }

    /**
     * Get the properties for the trusted certificate.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the trusted certificate.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="Extension")
    protected TrustedCertificateExtension getTrustedCertificateExtension() {
        return trustedCertificateExtension.revocationCheckingPolicyReference == null ?
                null :
                trustedCertificateExtension;
    }

    protected void setTrustedCertificateExtension( final TrustedCertificateExtension trustedCertificateExtension ) {
        this.trustedCertificateExtension = trustedCertificateExtension==null ?
                new TrustedCertificateExtension() :
                trustedCertificateExtension;
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @XmlType(name="TrustedCertificateExtensionType", propOrder={"revocationCheckingPolicyReference", "extension", "extensions"})
    protected static class TrustedCertificateExtension extends ElementExtensionSupport {
        private ManagedObjectReference revocationCheckingPolicyReference;

        @XmlElement(name="RevocationCheckingPolicyReference")
        protected ManagedObjectReference getRevocationCheckingPolicyReference() {
            return revocationCheckingPolicyReference;
        }

        protected void setRevocationCheckingPolicyReference( final ManagedObjectReference revocationCheckingPolicyReference ) {
            this.revocationCheckingPolicyReference = revocationCheckingPolicyReference;
        }
    }

    //- PACKAGE

    TrustedCertificateMO() {        
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private CertificateData certificateData;
    private Map<String,Object> properties;
    private TrustedCertificateExtension trustedCertificateExtension = new TrustedCertificateExtension();
}
