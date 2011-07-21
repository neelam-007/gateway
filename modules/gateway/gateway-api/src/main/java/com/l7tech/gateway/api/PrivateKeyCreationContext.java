package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AttributeExtensibleType.AttributeExtensibleString;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;
import com.l7tech.gateway.api.impl.ElementExtendableManagedObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * TODO [steve] javadoc
 */
@XmlRootElement(name="PrivateKeyCreationContext")
@XmlType(name="PrivateKeyCreationContextType", propOrder={"dnValue", "properties", "extension", "extensions"})
public class PrivateKeyCreationContext extends ElementExtendableManagedObject {

    //- PUBLIC

    public static final String PROP_CA_CAPABLE = "caCapable";
    public static final String PROP_DAYS_UNTIL_EXPIRY = "daysUntilExpiry";
    public static final String PROP_ELLIPTIC_CURVE_NAME = "ecName";
    public static final String PROP_RSA_KEY_SIZE = "rsaKeySize";
    public static final String PROP_SIGNATURE_HASH = "signatureHashAlgorithm";

    /**
     * Get the Subject DN to use for the certificate (required)
     *
     * @return The Subject DN or null.
     */
    public String getDn() {
        return get(dn);
    }

    /**
     * Set the Subject DN to use for the certificate (required)
     *
     * @param dn The Subject DN.
     */
    public void setDn( final String dn ) {
        this.dn = set(this.dn,dn);
    }

    /**
     * Get the properties for the key creation.
     *
     * @return The properties or null.
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the key creation.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    //- PROTECTED

    @XmlElement(name="Dn", required=true)
    protected AttributeExtensibleString getDnValue() {
        return dn;
    }

    protected void setDnValue( final AttributeExtensibleString dn ) {
        this.dn = dn;
    }

    //- PACKAGE

    PrivateKeyCreationContext(){
    }

    //- PRIVATE

    private AttributeExtensibleString dn;
    private Map<String,Object> properties;

}
