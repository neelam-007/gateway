package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AttributeExtensibleType.AttributeExtensibleString;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.get;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;
import com.l7tech.gateway.api.impl.ElementExtendableManagedObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * The PrivateKeyCreationContext encapsulates a private key creation request.
 *
 * <p>The following properties can be used:
 * <ul>
 *   <li><code>caCapable</code>: Optional CA capability flag (boolean, default
 *   false)</li>
 *   <li><code>daysUntilExpiry</code>: Optional days until expiry for the
 *   related certificate (integer)</li>
 *   <li><code>ecName</code>: Required for Elliptic Curve private keys, e.g.
 *   "secp384r1" (string)</li>
 *   <li><code>rsaKeySize</code>: Optional RSA key size in bits (integer)</li>
 *   <li><code>signatureHashAlgorithm</code>: Optional signature hash
 *   algorithm, e.g. "SHA256" (string)</li>
 * </ul>
 * </p>
 *
 * @see ManagedObjectFactory#createPrivateKeyCreationContext()
 */
@XmlRootElement(name="PrivateKeyCreationContext")
@XmlType(name="PrivateKeyCreationContextType", propOrder={"dnValue", "properties", "extension", "extensions"})
public class PrivateKeyCreationContext extends ElementExtendableManagedObject {

    //- PUBLIC

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

    /**
     * Get the security zone id for the context.
     *
     * @return the security zone id or null.
     */
    @XmlAttribute(name="securityZoneId", required=false)
    public String getSecurityZoneId() {
        return securityZoneId;
    }

    /**
    * Set the properties for the context.
    *
    * @param securityZoneId The security zone Id to reference.
    */
    public void setSecurityZoneId(String securityZoneId) {
        this.securityZoneId = securityZoneId;
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
    private String securityZoneId;

}
