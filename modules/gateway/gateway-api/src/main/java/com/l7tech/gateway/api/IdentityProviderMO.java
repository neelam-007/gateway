package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.Extension;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
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
@XmlType(name="IdentityProviderType", propOrder={"nameValue","identityProviderTypeValue","properties","extension","extensions"})
@AccessorSupport.AccessibleResource(name ="identityProviders")
public class IdentityProviderMO extends AccessibleObject {

    //- PUBLIC

    /**
     * Get the name for the identity provider (case insensitive)
     *
     * @return The name (may be null)
     */
    @XmlTransient
    public String getName() {
        return get(name);
    }

    /**
     * Set the name for the identity provider.
     *
     * @param name The name to use.
     */
    public void setName( final String name ) {
        this.name = set(this.name,name);
    }

    /**
     * Get the type of the identity provider.
     *
     * @return The identity provider type (may be null)
     */
    @XmlTransient
    public IdentityProviderType getIdentityProviderType() {
        return get(identityProviderType);
    }

    /**
     * Set the type for the identity provider.
     *
     * @param identityProviderType The type to use.
     */
    public void setIdentityProviderType( final IdentityProviderType identityProviderType ) {
        this.identityProviderType = setNonNull(
                this.identityProviderType==null ? new AttributeExtensibleIdentityProviderType() : this.identityProviderType,
                identityProviderType );
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
    @XmlType(name="IdentityProviderTypeType")
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

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    protected void setNameValue( final AttributeExtensibleString name ) {
        this.name = name;
    }

    @XmlElement(name="IdentityProviderType", required=true)
    protected AttributeExtensibleIdentityProviderType getIdentityProviderTypeValue() {
        return identityProviderType;
    }

    protected void setIdentityProviderTypeValue( final AttributeExtensibleIdentityProviderType identityProviderType ) {
        this.identityProviderType = identityProviderType;
    }

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

    @XmlType(name="IdentityProviderTypePropertyType")
    protected static class AttributeExtensibleIdentityProviderType extends AttributeExtensible<IdentityProviderType> {
        private IdentityProviderType value;

        @XmlValue
        @Override
        public IdentityProviderType getValue() {
            return value;
        }

        @Override
        public void setValue( final IdentityProviderType value ) {
            this.value = value;
        }
    }

    //- PACKAGE

    IdentityProviderMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleIdentityProviderType identityProviderType;
    private Map<String,Object> properties;
}