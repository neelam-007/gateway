package com.l7tech.gateway.api;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import com.l7tech.gateway.api.impl.ElementExtendableManagedObject;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * Private Key export context.
 *
 * <p>The private key export context encapsulates a request for a key export.</p>
 */
@XmlRootElement(name="PrivateKeyExportContext")
@XmlType(name="PrivateKeyExportContextType", propOrder={"aliasValue","passwordValue","properties", "extension", "extensions"})
public class PrivateKeyExportContext extends ElementExtendableManagedObject {

    //- PUBLIC

    /**
     * Get the alias to use in the exported keystore (optional)
     *
     * <p>If an alias is not specified then the alias for this key is used./p>
     *
     * @return The alias or null.
     */
    public String getAlias() {
        return get(alias);
    }

    /**
     * Set the alias to use in the exported keystore (optional)
     *
     * @param alias The alias to use.
     */
    public void setAlias( final String alias ) {
        this.alias = set(this.alias,alias);
    }

    /**
     * Get the password to use in the exported keystore (required)
     *
     * @return The password
     */
    public String getPassword() {
        return get(password);
    }

    /**
     * Set the password to use in the exported keystore (required)
     *
     * @param password The password to use
     */
    public void setPassword( final String password ) {
        this.password = set(this.password,password);
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

    //- PACKAGE

    PrivateKeyExportContext() {
    }

    //- PRIVATE

    private AttributeExtensibleString alias;
    private AttributeExtensibleString password;
    private Map<String,Object> properties;
}
