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
 * 
 */
@XmlRootElement(name="TrustedCertificate")
@XmlType(name="TrustedCertificateType", propOrder={"name","certificateData","extensions","properties"})
@AccessorFactory.ManagedResource(name ="trustedCertificates")
public class TrustedCertificateMO extends ManagedObject {
    
    //- PUBLIC

    /**
     * The name for the trusted certificate.
     *
     * @return The name (may be null)
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
     * Get the certificate data for the certificate
     *
     * @return The certificate data (may be null)
     */
    @XmlElement(name="CertificateData", required=true)
    public CertificateData getCertificateData() {
        return certificateData;
    }

    public void setCertificateData( final CertificateData certificateData ) {
        this.certificateData = certificateData;
    }

    /**
     * Get the properties for the trusted certificate.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the trusted certificate.
     *
     * @param properties The properties to use
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
