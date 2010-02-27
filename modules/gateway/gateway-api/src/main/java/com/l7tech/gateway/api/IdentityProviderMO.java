package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorFactory;
import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.List;
import java.util.Map;

/**
 * The IdentityProviderMO managed object represents an identity provider.
 *
 * <p>The Accessor for identity providers is read only. Identity providers can
 * be accessed by name or identifier.</p>
 *
 * @see ManagedObjectFactory#createIdentityProvider()
 */
@XmlRootElement(name="IdentityProvider")
@XmlType(name="IdentityProviderType", propOrder={"name","identityProviderType","extensions","properties"})
@AccessorFactory.AccessibleResource(name ="identityProviders")
public class IdentityProviderMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the identity provider (case insensitive)
     *
     * @return The name (may be null)
     */
    @XmlElement(name="Name", required=true)
    public String getName() {
        return name;
    }

    /**
     * Set the name for the identity provider.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = name;
    }

    /**
     * Get the type of the identity provider.
     *
     * @return The identity provider type (may be null)
     */
    @XmlElement(name="IdentityProviderType", required=true)
    public IdentityProviderType getIdentityProviderType() {
        return identityProviderType;
    }

    /**
     * Set the type for the identity provider.
     *
     * @param identityProviderType The type to use.
     */
    public void setIdentityProviderType( final IdentityProviderType identityProviderType ) {
        this.identityProviderType = identityProviderType;
    }

    /**
     * Get the properties for the identity provider.
     *
     * @return The properties (may be null)
     */
    @XmlElement(name="Properties")
    @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * Set the properties for the identity provider.
     *
     * @param properties The properties to use.
     */
    public void setProperties( final Map<String, Object> properties ) {
        this.properties = properties;
    }

    /**
     * Type for identity providers
     */
    @XmlEnum(String.class)
    public enum IdentityProviderType {
        /**
         * Gateway internal identity provider.
         */
        @XmlEnumValue("Internal") INTERNAL,

        /**
         * LDAP identity provider.
         */
        @XmlEnumValue("LDAP") LDAP,

        /**
         * Federated identity provider.
         */
        @XmlEnumValue("Federated") FEDERATED
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

    IdentityProviderMO() {
    }

    //- PRIVATE

    private String name;
    private IdentityProviderType identityProviderType;
    private Map<String,Object> properties;
}