package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@XmlRootElement(name="PrivateKey")
@XmlType(name="PrivateKeyType",propOrder={"certificateChain","extensions","properties"})
@AccessorFactory.ManagedResource(name ="privateKeys")
public class PrivateKeyMO extends ManagedObject {

    //- PUBLIC

    @XmlAttribute(name="keystoreId")
    public String getKeystoreId() {
        return keystoreId;
    }

    public void setKeystoreId( final String keystoreId ) {
        this.keystoreId = keystoreId;
    }

    @XmlAttribute(name="alias")
    public String getAlias() {
        return alias;
    }

    public void setAlias( final String alias ) {
        this.alias = alias;
    }

    @XmlElementWrapper(name="CertificateChain")
    @XmlElement(name="CertificateData", required=true)
    public List<CertificateData> getCertificateChain() {
        return certificateChain;
    }

    public void setCertificateChain( final List<CertificateData> certificateChain ) {
        this.certificateChain = certificateChain;
    }

    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

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

    PrivateKeyMO(){        
    }

    //- PRIVATE

    private String keystoreId;
    private String alias;
    private List<CertificateData> certificateChain;
    private Map<String,Object> properties;
}
