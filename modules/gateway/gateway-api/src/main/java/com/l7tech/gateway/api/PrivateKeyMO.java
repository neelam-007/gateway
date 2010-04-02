package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
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
 * The PrivateKeyMO managed object represents a private key.
 *
 * <p>The following properties are used:
 * <ul>
 *   <li><code>keyAlgorithm</code> (read only): The algorithm for the private
 *       key (e.g. "RSA")</li>
 * </ul>
 * </p>
 *
 * <p>The Accessor for private keys is read only. Private keys can be accessed
 * by identifier only.</p>
 *
 * @see ManagedObjectFactory#createPrivateKey()
 */
@XmlRootElement(name="PrivateKey")
@XmlType(name="PrivateKeyType",propOrder={"certificateChain","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="privateKeys")
public class PrivateKeyMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the identifier for the keystore.
     *
     * @return The keystore identifier or null.
     */
    @XmlAttribute(name="keystoreId")
    public String getKeystoreId() {
        return keystoreId;
    }

    /**
     * Set the identifier for the keystore.
     *
     * @param keystoreId The keystore identifier to use.
     */
    public void setKeystoreId( final String keystoreId ) {
        this.keystoreId = keystoreId;
    }

    /**
     * Get the alias for the private key (required)
     *
     * @return The alias or null.
     */
    @XmlAttribute(name="alias", required=true)
    public String getAlias() {
        return alias;
    }

    /**
     * Set the alias for the private key.
     *
     * @param alias The alias to use.
     */
    public void setAlias( final String alias ) {
        this.alias = alias;
    }

    /**
     * Get the certificate chain for the private key (required)
     *
     * @return The certificate chain or null.
     */
    @XmlElementWrapper(name="CertificateChain", required=true)
    @XmlElement(name="CertificateData", required=true)
    public List<CertificateData> getCertificateChain() {
        return certificateChain;
    }

    /**
     * Set the certificate chain for the private key.
     *
     * @param certificateChain The certificate chain to use.
     */
    public void setCertificateChain( final List<CertificateData> certificateChain ) {
        this.certificateChain = certificateChain;
    }

    /**
     * Get the properties for the private key.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the private key.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Extension")
    @Override
    protected Extension getExtension() {
        return super.getExtension();
    }

    @Override
    protected void setExtension( final Extension extension ) {
        super.setExtension( extension );
    }

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
