package com.l7tech.gateway.api.impl;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * Private Key export result.
 *
 * <p>The private key export result encapsulates the result of a key export.</p>
 */
@XmlRootElement(name="PrivateKeyExportResult")
@XmlType(name="PrivateKeyExportResultType", propOrder={"pkcs12DataValue","properties", "extension", "extensions"})
public class PrivateKeyExportResult extends ElementExtendableManagedObject {

    //- PUBLIC

    /**
     * Get the PKCS12 keystore bytes (required)
     *
     * @return The bytes for the PKCS12 keystore
     */
    public byte[] getPkcs12Data() {
        return get(pkcs12Data);
    }

    /**
     * Set the PKCS12 keystore bytes
     *
     * @param pkcs12Data The bytes for the PKCS12 keystore
     */
    public void setPkcs12Data( final byte[] pkcs12Data ) {
        this.pkcs12Data = set(this.pkcs12Data,pkcs12Data);
    }

    /**
     * Get the properties for the context.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the context.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Pkcs12Data", required=true)
    protected AttributeExtensibleByteArray getPkcs12DataValue() {
        return pkcs12Data;
    }

    protected void setPkcs12DataValue( final AttributeExtensibleByteArray pkcs12Data ) {
        this.pkcs12Data = pkcs12Data;
    }

    //- PRIVATE

    private AttributeExtensibleByteArray pkcs12Data;
    private Map<String,Object> properties;

}
