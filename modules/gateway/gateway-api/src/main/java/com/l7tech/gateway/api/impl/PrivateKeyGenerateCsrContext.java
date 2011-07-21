package com.l7tech.gateway.api.impl;

import com.l7tech.gateway.api.impl.AttributeExtensibleType.AttributeExtensibleString;
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
@XmlRootElement(name="PrivateKeyGenerateCsrContext")
@XmlType(name="PrivateKeyGenerateCsrContextType", propOrder={"dnValue","properties", "extension", "extensions"})
public class PrivateKeyGenerateCsrContext extends ElementExtendableManagedObject {

    //- PUBLIC

    public static final String PROP_SIGNATURE_HASH = "signatureHashAlgorithm";

    /**
     * Get the DN use in the CSR (optional)
     *
     * <p>If a DN is not specified then the DN of the current certificate is used./p>
     *
     * @return The DN or null.
     */
    public String getDn() {
        return get(dn);
    }

    /**
     * Set the DN to use in the CSR (optional)
     *
     * @param dn The DN to use.
     */
    public void setDn( final String dn ) {
        this.dn = set(this.dn,dn);
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

    @XmlElement(name="Dn")
    protected AttributeExtensibleString getDnValue() {
        return dn;
    }

    protected void setDnValue( final AttributeExtensibleString dn ) {
        this.dn = dn;
    }

    //- PRIVATE

    private AttributeExtensibleString dn;
    private Map<String,Object> properties;
}
