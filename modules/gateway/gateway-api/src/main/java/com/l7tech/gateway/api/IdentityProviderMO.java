package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.gateway.api.impl.AttributeExtensibleReferenceList;
import com.l7tech.gateway.api.impl.AttributeExtensibleType;
import com.l7tech.gateway.api.impl.ElementExtensionSupport;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import com.l7tech.util.Functions;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;
import static com.l7tech.gateway.api.impl.AttributeExtensibleType.set;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The IdentityProviderMO managed object represents an identity provider.
 *
 * TODO [steve] document properties and extension + javadoc
 *
 * <p>The Accessor for identity providers is read only. Identity providers can
 * be accessed by name or identifier.</p>
 *
 * @see ManagedObjectFactory#createIdentityProvider()
 */
@XmlRootElement(name="IdentityProvider")
@XmlType(name="IdentityProviderType", propOrder={"nameValue","identityProviderTypeValue","properties","identityProviderExtension","extensions"})
@AccessorSupport.AccessibleResource(name ="identityProviders")
public class IdentityProviderMO extends AccessibleObject {

    //- PUBLIC

    public static final String CERTIFICATE_VALIDATE = "Validate";
    public static final String CERTIFICATE_VALIDATE_PATH = "Validate Certificate Path";
    public static final String CERTIFICATE_REVOCATION = "Revocation Checking";    

    /**
     * Get the name for the identity provider (case insensitive)
     *
     * @return The name (may be null)
     */
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

    public LdapIdentityProviderDetail getLdapIdentityProviderDetail() {
        return getIdentityProviderOptions( LdapIdentityProviderDetail.class );
    }

    public FederatedIdentityProviderDetail getFederatedIdentityProviderDetail() {
        return getIdentityProviderOptions( FederatedIdentityProviderDetail.class );
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

    @XmlType(name="FederatedIdentityProviderDetailType", propOrder={"certificateReferencesValue"})
    public static class FederatedIdentityProviderDetail extends IdentityProviderDetail {
        private AttributeExtensibleReferenceList certificateReferences;

        public List<String> getCertificateReferences() {
            return Functions.map( Arrays.asList(get(certificateReferences,new ManagedObjectReference[0])), new Functions.Unary<String,ManagedObjectReference>() {
                @Override
                public String call( final ManagedObjectReference managedObjectReference ) {
                    return managedObjectReference.getId();
                }
            } );
        }

        public void setCertificateReferences( final List<String> references ) {
            if ( references != null && !references.isEmpty() ) {
                if ( certificateReferences == null ) {
                    certificateReferences = new AttributeExtensibleReferenceList();
                }
                final List<ManagedObjectReference> referencesList = ManagedObjectReference.asReferences( references );
                certificateReferences.setReferenceType( TrustedCertificateMO.class );
                certificateReferences.setValue( referencesList.toArray( new ManagedObjectReference[ referencesList.size() ] ) );
            } else {
                certificateReferences = null;
            }
        }

        @XmlElement(name="CertificateReferences")
        protected AttributeExtensibleReferenceList getCertificateReferencesValue() {
            return certificateReferences;
        }

        protected void setCertificateReferencesValue( final AttributeExtensibleReferenceList certificateReferences ) {
            this.certificateReferences = certificateReferences;
        }
    }

    @XmlType(name="LdapIdentityProviderDetailType", propOrder={"sourceTypeValue", "serverUrlValues", "useSslClientAuthenticationValue", "sslKeyReferenceValue", "searchBaseValue", "bindDnValue", "bindPasswordValue", "userMappingValues", "groupMappingValues", "specifiedAttributeValues"})
    public static class LdapIdentityProviderDetail extends IdentityProviderDetail {
        private AttributeExtensibleString sourceType;

        private AttributeExtensibleStringList serverUrls;
        private AttributeExtensibleBoolean useSslClientAuthentication;
        private ManagedObjectReference sslKeyReference;
        private AttributeExtensibleString searchBase;
        private AttributeExtensibleString bindDn;
        private AttributeExtensibleString bindPassword;

        private AttributeExtensibleLdapIdentityProviderMappingList userMappings;
        private AttributeExtensibleLdapIdentityProviderMappingList groupMappings;

        private AttributeExtensibleStringList specifiedAttributes;

        public String getSourceType(){
            return get( sourceType );
        }

        public void setSourceType( final String sourceType ) {
            this.sourceType = set(this.sourceType,sourceType);
        }

        public List<String> getServerUrls() {
            return unwrap(get( serverUrls, new ArrayList<AttributeExtensibleString>() ));
        }

        public void setServerUrls( final List<String> serverUrls ) {
            this.serverUrls = set( this.serverUrls, wrap(serverUrls,AttributeExtensibleStringBuilder) );
        }

        public boolean isUseSslClientClientAuthentication() {
            return get( useSslClientAuthentication, Boolean.FALSE );
        }

        public void setUseSslClientAuthentication(boolean useSslClientAuthentication) {
            this.useSslClientAuthentication = set(this.useSslClientAuthentication,useSslClientAuthentication);
        }

        public String getSslKeyId() {
            return sslKeyReference==null ? null : sslKeyReference.getId();
        }

        public void setSslKeyId( final String keyId ) {
            if ( keyId != null ) {
                if ( sslKeyReference==null ) {
                    sslKeyReference = new ManagedObjectReference();
                }
                sslKeyReference.setId( keyId );
                sslKeyReference.setResourceType( PrivateKeyMO.class );
            } else {
                sslKeyReference = null;
            }
        }

        public String getSearchBase() {
            return get( searchBase );
        }

        public void setSearchBase( String searchBase ) {
            this.searchBase = set(this.searchBase,searchBase);
        }

        public String getBindDn() {
            return get( bindDn );
        }

        public void setBindDn( String bindDn ) {
            this.bindDn = set(this.bindDn,bindDn);
        }

        public String getBindPassword() {
            return get( bindPassword );
        }

        public void setBindPassword( String bindPassword ) {
            this.bindPassword = set(this.bindPassword,bindPassword);
        }

        public boolean hasUserMappings() {
            return userMappings!=null && userMappings.value != null;
        }

        public List<LdapIdentityProviderMapping> getUserMappings() {
            return get( userMappings, new ArrayList<LdapIdentityProviderMapping>() );
        }

        public void setUserMappings( final List<LdapIdentityProviderMapping> userMappings ) {
            this.userMappings = set(this.userMappings,userMappings,AttributeExtensibleLdapIdentityProviderMappingList.Builder);
        }

        public boolean hasGroupMappings() {
            return groupMappings!=null && groupMappings.value != null;
        }

        public List<LdapIdentityProviderMapping> getGroupMappings() {
            return get( groupMappings, new ArrayList<LdapIdentityProviderMapping>() );
        }

        public void setGroupMappings( final List<LdapIdentityProviderMapping> groupMappings ) {
            this.groupMappings = set(this.groupMappings,groupMappings,AttributeExtensibleLdapIdentityProviderMappingList.Builder);
        }

        public List<String> getSpecifiedAttributes() {
            return unwrap(get( specifiedAttributes, new ArrayList<AttributeExtensibleString>() ));
        }

        public void setSpecifiedAttributes( List<String> specifiedAttributes ) {
            this.specifiedAttributes = set( this.specifiedAttributes, wrap(specifiedAttributes,AttributeExtensibleStringBuilder) );
        }

        @XmlElement(name="SourceType")
        protected AttributeExtensibleString getSourceTypeValue() {
            return sourceType;
        }

        protected void setSourceTypeValue( final AttributeExtensibleString sourceType ) {
            this.sourceType = sourceType;
        }

        @XmlElement(name="ServerUrls", required=true)
        protected AttributeExtensibleStringList getServerUrlValues() {
            return serverUrls;
        }

        protected void setServerUrlValues( final AttributeExtensibleStringList serverUrls ) {
            this.serverUrls = serverUrls;
        }

        @XmlElement(name="UseSslClientAuthentication")
        protected AttributeExtensibleBoolean getUseSslClientAuthenticationValue() {
            return useSslClientAuthentication;
        }

        protected void setUseSslClientAuthenticationValue( final AttributeExtensibleBoolean useSslClientAuthentication ) {
            this.useSslClientAuthentication = useSslClientAuthentication;
        }

        @XmlElement(name="SslKeyReference")
        protected ManagedObjectReference getSslKeyReferenceValue() {
            return sslKeyReference;
        }

        protected void setSslKeyReferenceValue( final ManagedObjectReference sslKeyReference ) {
            this.sslKeyReference = sslKeyReference;
        }

        @XmlElement(name="SearchBase",required=true)
        protected AttributeExtensibleString getSearchBaseValue() {
            return searchBase;
        }

        protected void setSearchBaseValue( final AttributeExtensibleString searchBase ) {
            this.searchBase = searchBase;
        }

        @XmlElement(name="BindDn",required=true)
        protected AttributeExtensibleString getBindDnValue() {
            return bindDn;
        }

        protected void setBindDnValue( final AttributeExtensibleString bindDn ) {
            this.bindDn = bindDn;
        }

        @XmlElement(name="BindPassword")
        protected AttributeExtensibleString getBindPasswordValue() {
            return bindPassword;
        }

        protected void setBindPasswordValue( final AttributeExtensibleString bindPassword ) {
            this.bindPassword = bindPassword;
        }

        @XmlElement(name="UserMappings")
        protected AttributeExtensibleLdapIdentityProviderMappingList getUserMappingValues() {
            return userMappings;
        }

        protected void setUserMappingValues( final AttributeExtensibleLdapIdentityProviderMappingList userMappings ) {
            this.userMappings = userMappings;
        }

        @XmlElement(name="GroupMappings")
        protected AttributeExtensibleLdapIdentityProviderMappingList getGroupMappingValues() {
            return groupMappings;
        }

        protected void setGroupMappingValues( final AttributeExtensibleLdapIdentityProviderMappingList groupMappings ) {
            this.groupMappings = groupMappings;
        }

        @XmlElement(name="SpecifiedAttributes")
        protected AttributeExtensibleStringList getSpecifiedAttributeValues() {
            return specifiedAttributes;
        }

        protected void setSpecifiedAttributeValues( final AttributeExtensibleStringList specifiedAttributes ) {
            this.specifiedAttributes = specifiedAttributes;
        }
    }

    /**
     * Represents a user or group mapping
     */
    @XmlType(name="LdapIdentityProviderMappingType", propOrder={"objectClassValue","mappings","properties","extension","extensions"})
    public static class LdapIdentityProviderMapping extends ElementExtensionSupport {
        private AttributeExtensibleString objectClass;
        private Map<String,Object> mappings;
        private Map<String,Object> properties;

        public String getObjectClass() {
            return get( objectClass );
        }

        public void setObjectClass( final String objectClass ) {
            this.objectClass = set(this.objectClass,objectClass);
        }

        /**
         * Get the properties for the mapping.
         *
         * @return The properties (may be null)
         */
        @XmlElement(name="Mappings",required=true)
        @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
        public Map<String, Object> getMappings() {
            return mappings;
        }

        /**
         * Set the properties for the mapping.
         *
         * @param mappings The mappings to use.
         */
        public void setMappings( final Map<String, Object> mappings ) {
            this.mappings = mappings;
        }


        /**
         * Get the properties for the mapping.
         *
         * @return The properties (may be null)
         */
        @XmlElement(name="Properties")
        @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
        public Map<String, Object> getProperties() {
            return properties;
        }

        /**
         * Set the properties for the mapping.
         *
         * @param properties The properties to use.
         */
        public void setProperties( final Map<String, Object> properties ) {
            this.properties = properties;
        }

        @XmlElement(name="ObjectClass",required=true)
        protected AttributeExtensibleString getObjectClassValue() {
            return objectClass;
        }

        protected void setObjectClassValue( final AttributeExtensibleString objectClass ) {
            this.objectClass = objectClass;
        }
    }

    /**
     * AttributeExtensible extension for LdapIdentityProviderMapping[] properties.
     */
    @XmlType(name="LdapIdentityProviderMappingListPropertyType", propOrder={"value"})
    public static class AttributeExtensibleLdapIdentityProviderMappingList  extends AttributeExtensibleType.AttributeExtensible<List<LdapIdentityProviderMapping>> {
        private List<LdapIdentityProviderMapping> value;

        @Override
        @XmlElement(name="Mapping")
        public List<LdapIdentityProviderMapping> getValue() {
            return value;
        }

        @Override
        public void setValue( final List<LdapIdentityProviderMapping> value ) {
            this.value = value;
        }

        private static final Functions.Nullary<AttributeExtensibleLdapIdentityProviderMappingList> Builder =
                new Functions.Nullary<AttributeExtensibleLdapIdentityProviderMappingList>(){
            @Override
            public AttributeExtensibleLdapIdentityProviderMappingList call() {
                return new AttributeExtensibleLdapIdentityProviderMappingList();
            }
        };
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
    protected IdentityProviderExtension getIdentityProviderExtension() {
        return identityProviderExtension;
    }

    protected void setIdentityProviderExtension( final IdentityProviderExtension identityProviderExtension ) {
        this.identityProviderExtension = identityProviderExtension;
    }

    @XmlAnyElement(lax=true)
    @Override
    protected List<Object> getExtensions() {
        return super.getExtensions();
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

    @XmlType(name="IdentityProviderExtensionType", propOrder={"identityProviderDetail", "extension", "extensions"})
    protected static class IdentityProviderExtension extends ElementExtensionSupport {
        private IdentityProviderDetail identityProviderDetail;

        @XmlElements({
             @XmlElement(name="FederatedIdentityProviderDetail",type=FederatedIdentityProviderDetail.class),
             @XmlElement(name="LdapIdentityProviderDetail",type=LdapIdentityProviderDetail.class)
         })
        protected IdentityProviderDetail getIdentityProviderDetail() {
            return identityProviderDetail;
        }

        protected void setIdentityProviderDetail( final IdentityProviderDetail identityProviderDetail ) {
            this.identityProviderDetail = identityProviderDetail;
        }
    }

    @SuppressWarnings({ "ClassReferencesSubclass" })
    @XmlType(name="IdentityProviderDetailType", propOrder={"extension", "extensions"})
    @XmlSeeAlso({FederatedIdentityProviderDetail.class, LdapIdentityProviderDetail.class})
    protected abstract static class IdentityProviderDetail extends ElementExtensionSupport {
    }

    //- PACKAGE

    IdentityProviderMO() {
    }

    //- PRIVATE

    private AttributeExtensibleString name;
    private AttributeExtensibleIdentityProviderType identityProviderType;
    private Map<String,Object> properties;
    private IdentityProviderExtension identityProviderExtension;

    @SuppressWarnings({ "unchecked" })
    private <DT extends IdentityProviderDetail> DT getIdentityProviderOptions( final Class<DT> optionsClass ) {
        DT options = null;

        if ( identityProviderExtension != null &&
             optionsClass.isInstance( identityProviderExtension.getIdentityProviderDetail() ) ) {
            options = (DT) identityProviderExtension.getIdentityProviderDetail();
        } else if ( identityProviderExtension == null ) {
            identityProviderExtension = new IdentityProviderExtension();
            try {
                options = optionsClass.newInstance();
            } catch ( InstantiationException e ) {
                throw new ManagementRuntimeException(e);
            } catch ( IllegalAccessException e ) {
                throw new ManagementRuntimeException(e);
            }
            identityProviderExtension.setIdentityProviderDetail( options );
        }

        return options;
    }


}