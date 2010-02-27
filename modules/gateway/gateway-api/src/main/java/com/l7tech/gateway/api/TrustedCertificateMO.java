package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;
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
 *       verified for this certificate.</li>
 * </ul>
 * </p>
 *
 * <p>The Accessor for trusted certificates is read only. Trusted certificates
 * can be accessed by identifier only.</p>
 *
 * @see ManagedObjectFactory#createTrustedCertificate()
 */
@XmlRootElement(name="TrustedCertificate")
@XmlType(name="TrustedCertificateType", propOrder={"name","certificateData","extensions","properties"})
@AccessorFactory.AccessibleResource(name ="trustedCertificates")
public class TrustedCertificateMO extends AccessibleObject {
    
    //- PUBLIC

    /**
     * The name for the trusted certificate (required)
     *
     * @return The name or null.
     */
    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the trusted certificate.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = name;
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

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
    }

    @Override
    protected void setExtensions( final List<Object> extensions ) {
        super.setExtensions( extensions );
    }

    //- PACKAGE

    TrustedCertificateMO() {        
    }

    //- PRIVATE

    private String name;
    private CertificateData certificateData;
    private Map<String,Object> properties;
}
