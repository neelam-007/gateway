package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.impl.AttributeExtensibleType.AttributeExtensibleByteArray;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 *
 */
@XmlRootElement(name="PrivateKeyGenerateCsrResult")
@XmlType(name="PrivateKeyGenerateCsrResultType", propOrder={"csrDataValue","properties", "extension", "extensions"})
public class PrivateKeyGenerateCsrResult extends ElementExtendableManagedObject  {

    //- PUBLIC

    public byte[] getCsrData() {
        return get(csrData);
    }

    public void setCsrData( final byte[] csrData ) {
        this.csrData = set(this.csrData,csrData);
    }

    /**
     * Get the properties for the result.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the result.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="CsrData", required=true)
    protected AttributeExtensibleByteArray getCsrDataValue() {
        return csrData;
    }

    protected void setCsrDataValue( final AttributeExtensibleByteArray csrData ) {
        this.csrData = csrData;
    }

    //- PRIVATE

    private AttributeExtensibleByteArray csrData;
    private Map<String,Object> properties;
}
