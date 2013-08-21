package com.l7tech.gateway.api.impl;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * Private Key import context.
 *
 * <p>The private key import context encapsulates a request for a key import.</p>
 */
@XmlRootElement(name="PrivateKeyImportContext")
@XmlType(name="PrivateKeyImportContextType", propOrder={"pkcs12DataValue","aliasValue","passwordValue","properties", "extension", "extensions"})
public class PrivateKeyImportContext extends ElementExtendableManagedObject {

    //- PUBLIC

    /**
     * Get the alias used in the import keystore (optional)
     *
     * <p>If an alias is not specified then there must only be one entry in the
     * import keystore./p>
     *
     * @return The alias or null.
     */
    public String getAlias() {
        return get(alias);
    }

    /**
     * Set the alias used in the import keystore (optional)
     *
     * @param alias The alias.
     */
    public void setAlias( final String alias ) {
        this.alias = set(this.alias,alias);
    }

    /**
     * Get the password used in the import keystore (required)
     *
     * @return The password
     */
    public String getPassword() {
        return get(password);
    }

    /**
     * Set the password used in the import keystore (required)
     *
     * @param password The password
     */
    public void setPassword( final String password ) {
        this.password = set(this.password,password);
    }

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

    @XmlElement(name="Alias")
    protected AttributeExtensibleString getAliasValue() {
        return alias;
    }

    protected void setAliasValue( final AttributeExtensibleString alias ) {
        this.alias = alias;
    }

    @XmlElement(name="Password", required=true)
    protected AttributeExtensibleString getPasswordValue() {
        return password;
    }

    protected void setPasswordValue( final AttributeExtensibleString password ) {
        this.password = password;
    }

    @XmlElement(name="Pkcs12Data", required=true)
    protected AttributeExtensibleByteArray getPkcs12DataValue() {
        return pkcs12Data;
    }

    protected void setPkcs12DataValue( final AttributeExtensibleByteArray pkcs12Data ) {
        this.pkcs12Data = pkcs12Data;
    }

    //- PRIVATE

    private AttributeExtensibleString alias;
    private AttributeExtensibleString password;
    private AttributeExtensibleByteArray pkcs12Data;
    private Map<String,Object> properties;
    private String securityZoneId;

}