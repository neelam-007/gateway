package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.*;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.impl.AttributeExtensibleType.*;

/**
 * The IdentityProviderMO managed object represents an identity provider.
 *
 * <p>The Accessor for identity providers supports read and write. Identity
 * providers can be accessed by name or identifier.</p>
 *
 * <p>The following properties are used:
 * <ul>
 *   <li><code>adminEnabled</code>: True to enable administrative users from
 *   the provider (boolean, default false)</li>
 *   <li><code>certificateValidation</code>: Optional certificate validation
 *   type for the provider, one of <code>Validate</code>, <code>Validate
 *   Certificate Path</code>, <code>Revocation Checking</code> (string)</li>
 * </ul>
 * </p>
 *
 * <p>The following properties are used for Federated identity providers:
 * <ul>
 *   <li><code>enableCredentialType.saml</code>: Flag to enabled SAML
 *   credentials (boolean, default false)</li>
 *   <li><code>enableCredentialType.x509</code>: Flag to enable X.509
 *   credentials (boolean, default false)</li>
 * </ul>
 * </p>
 *
 * <p>The following properties are used for LDAP identity providers:
 * <ul>
 *   <li><code>groupCacheMaximumAge</code>: Maximum cache age in milliseconds
 *   (integer, default 60000)</li>
 *   <li><code>groupCacheSize</code>: Maximum cache size (integer, default 100)
 *   </li>
 *   <li><code>groupMaximumNesting</code>: Maximum group nesting, 0 for
 *   unlimited (integer, default 0)</li>
 *   <li><code>groupMembershipCaseInsensitive</code>: (boolean, default false)
 *   </li>
 *   <li><code>userCertificateIndexSearchFilter</code>: Optional custom index
 *   search filter, used when <code>userCertificateUsage</code> is
 *   <code>Custom</code> (string)</li>
 *   <li><code>userCertificateIssuerSerialSearchFilter</code>: Optional search
 *   filter for certificate lookup, used when <code>userCertificateUsage</code>
 *   is <code>Search</code> (string)</li>
 *   <li><code>userCertificateSKISearchFilter</code>: Optional search filter
 *   for certificate lookup, used when <code>userCertificateUsage</code> is
 *   <code>Search</code> (string)</li>
 *   <li><code>userCertificateUsage</code>: Certificate use type, one of
 *   <code>None</code>, <code>Index</code>, <code>Custom</code>,
 *   <code>Search</code> (string)</li>
 *   <li><code>userLookupByCertMode</code>: Method of looking up an LDAP user from their client certificate, one of</li>
 *      "<code>Common Name from Certificate</code>" or "<code>Entire Certificate</code>".
 * </ul>
 * </p>
 *
 * @see ManagedObjectFactory#createIdentityProvider()
 */
@XmlRootElement(name="IdentityProvider")
@XmlType(name="IdentityProviderType", propOrder={"nameValue","identityProviderTypeValue","properties","identityProviderExtension","extensions"})
@AccessorSupport.AccessibleResource(name ="identityProviders")
public class IdentityProviderMO extends SecurityZoneableObject {

    //- PUBLIC

    private AttributeExtensibleString name;
    private AttributeExtensibleIdentityProviderType identityProviderType;
    private Map<String,Object> properties;
    private IdentityProviderExtension identityProviderExtension;

    IdentityProviderMO() {
    }

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

    /**
     * Get the details for an LDAP identity provider.
     *
     * @return The details or null
     * @see #getIdentityProviderType
     */
    public LdapIdentityProviderDetail getLdapIdentityProviderDetail() {
        return getIdentityProviderOptions( LdapIdentityProviderDetail.class );
    }

    /**
     * Get the details for a simple Bind-only LDAP identity provider.
     *
     * @return The details or null
     * @see #getIdentityProviderType
     */
    public BindOnlyLdapIdentityProviderDetail getBindOnlyLdapIdentityProviderDetail() {
        return getIdentityProviderOptions( BindOnlyLdapIdentityProviderDetail.class );
    }

    /**
     * Get the details for a Federated identity provider.
     *
     * @return The details or null
     * @see #getIdentityProviderType
     */
    public FederatedIdentityProviderDetail getFederatedIdentityProviderDetail() {
        return getIdentityProviderOptions( FederatedIdentityProviderDetail.class );
    }

    /**
     * Get the details for a Policy Backed identity provider.
     *
     * @return The details or null
     * @see #getIdentityProviderType
     */
    public PolicyBackedIdentityProviderDetail getPolicyBackedIdentityProviderDetail() {
        return getIdentityProviderOptions( PolicyBackedIdentityProviderDetail.class );
    }

    @XmlElement(name="Name", required=true)
    protected AttributeExtensibleString getNameValue() {
        return name;
    }

    //- PROTECTED

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
        @XmlEnumValue("Federated") FEDERATED,

        /**
         * Simple bind-only LDAP based provider.
         */
        @XmlEnumValue("Simple LDAP") BIND_ONLY_LDAP,

        /**
         * Policy Backed based provider.
         */
        @XmlEnumValue("Policy-Backed") POLICY_BACKED
    }

    /**
     * Details for a Federated identity provider.
     */
    @XmlType(name="FederatedIdentityProviderDetailType", propOrder={"certificateReferencesValue"})
    public static class FederatedIdentityProviderDetail extends IdentityProviderDetail {
        private AttributeExtensibleReferenceList certificateReferences;

        protected FederatedIdentityProviderDetail() {
        }

        /**
         * Get the list of trusted certificate identifiers.
         *
         * @return The list of identifiers (never null)
         */
        public List<String> getCertificateReferences() {
            return Functions.map( Arrays.asList(get(certificateReferences,new ManagedObjectReference[0])), new Functions.Unary<String,ManagedObjectReference>() {
                @Override
                public String call( final ManagedObjectReference managedObjectReference ) {
                    return managedObjectReference.getId();
                }
            } );
        }

        /**
         * Set the list of trusted certificate identifiers.
         *
         * @param references The certificate identifiers
         */
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

    /**
     * Details for a simple BIND-only LDAP identity provider.
     */
    @XmlType(name="BindOnlyLdapIdentityProviderDetailType", propOrder={"serverUrlValues", "useSslClientAuthenticationValue", "sslKeyReferenceValue",
            "bindPatternPrefixValue", "bindPatternSuffixValue"})
    public static class BindOnlyLdapIdentityProviderDetail extends IdentityProviderDetail {
        private AttributeExtensibleStringList serverUrls;
        private AttributeExtensibleBoolean useSslClientAuthentication;
        private ManagedObjectReference sslKeyReference;
        private AttributeExtensibleString bindPatternPrefix;
        private AttributeExtensibleString bindPatternSuffix;

        protected BindOnlyLdapIdentityProviderDetail() {
        }

        /**
         * Get the LDAP server URLs.
         *
         * @return The list of LDAP servers (never null)
         */
        public List<String> getServerUrls() {
            return unwrap(get( serverUrls, new ArrayList<AttributeExtensibleString>() ));
        }

        /**
         * Set the LDAP server URLs.
         *
         * @param serverUrls The list of LDAP servers to use
         */
        public void setServerUrls( final List<String> serverUrls ) {
            this.serverUrls = set( this.serverUrls, wrap(serverUrls,AttributeExtensibleStringBuilder) );
        }

        /**
         * SSL/TLS client authentication flag.
         *
         * @return True if client authentication should be used
         */
        public boolean isUseSslClientClientAuthentication() {
            return get( useSslClientAuthentication, Boolean.FALSE );
        }

        /**
         * Set SSL/TLS client authentication flag.
         *
          * @param useSslClientAuthentication True to use client authentication
         */
        public void setUseSslClientAuthentication( final boolean useSslClientAuthentication ) {
            this.useSslClientAuthentication = set(this.useSslClientAuthentication,useSslClientAuthentication);
        }

        /**
         * Get the TLS/SSL client key identifier.
         *
         * @return The key identifier or null
         */
        public String getSslKeyId() {
            return sslKeyReference==null ? null : sslKeyReference.getId();
        }

        /**
         * Set the TLS/SSL client key identifier.
         *
         * @param keyId The key identifier to use
         */
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

        /**
         * Get the prefix to prepend to the username for building a bind DN.
         *
         * @return The bind DN prefix or null.
         */
        public String getBindPatternPrefix() {
            return get( bindPatternPrefix );
        }

        /**
         * Set the bind "DN" for the LDAP (required)
         *
         * @param bindPatternPrefix The bind DN prefix or null.
         */
        public void setBindPatternPrefix( final String bindPatternPrefix ) {
            this.bindPatternPrefix = set(this.bindPatternPrefix,bindPatternPrefix);
        }

        /**
         * Get the suffix to prepend to the username for building a bind DN.
         *
         * @return The bind DN suffix or null.
         */
        public String getBindPatternSuffix() {
            return get( bindPatternSuffix );
        }

        /**
         * Set the bind "DN" for the LDAP (required)
         *
         * @param bindPatternSuffix The bind DN suffix or null.
         */
        public void setBindPatternSuffix( final String bindPatternSuffix ) {
            this.bindPatternSuffix = set(this.bindPatternSuffix,bindPatternSuffix);
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

        @XmlElement(name="BindPatternPrefix",required=true)
        protected AttributeExtensibleString getBindPatternPrefixValue() {
            return bindPatternPrefix;
        }

        protected void setBindPatternPrefixValue( final AttributeExtensibleString bindPatternPrefix ) {
            this.bindPatternPrefix = bindPatternPrefix;
        }

        @XmlElement(name="BindPatternSuffix",required=true)
        protected AttributeExtensibleString getBindPatternSuffixValue() {
            return bindPatternSuffix;
        }

        protected void setBindPatternSuffixValue( final AttributeExtensibleString bindPatternSuffix ) {
            this.bindPatternSuffix = bindPatternSuffix;
        }
    }

    /**
     * Details for an LDAP identity provider.
     *
     * <p>When creating an LDAP identity provider the user and group mappings
     * do not have to be specified. If the source type is set then this is used
     * to initialize the mappings. Permitted source types are:</p>
     *
     * <ul>
     *   <li><code>GenericLDAP</code></li>
     *   <li><code>MicrosoftActiveDirectory</code></li>
     *   <li><code>Oracle</code></li>
     *   <li><code>TivoliLDAP</code></li>
     * </ul>
     */
    @XmlType(name="LdapIdentityProviderDetailType", propOrder={"sourceTypeValue", "serverUrlValues", "useSslClientAuthenticationValue", "sslKeyReferenceValue", "searchBaseValue", "bindDnValue", "bindPasswordValue", "userMappingValues", "groupMappingValues", "specifiedAttributeValues","ntlmProperties"})
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
        private Map<String,Object> ntlmProperties;

        protected LdapIdentityProviderDetail() {
        }

        /**
         * Get the source type for the provider.
         *
         * @return The source type or null
         */
        public String getSourceType(){
            return get( sourceType );
        }

        /**
         * Set the source type for the provider.
         *
         * @param sourceType The source type to use
         */
        public void setSourceType( final String sourceType ) {
            this.sourceType = set(this.sourceType,sourceType);
        }

        /**
         * Get the LDAP server URLs.
         *
         * @return The list of LDAP servers (never null)
         */
        public List<String> getServerUrls() {
            return unwrap(get( serverUrls, new ArrayList<AttributeExtensibleString>() ));
        }

        /**
         * Set the LDAP server URLs.
         *
         * @param serverUrls The list of LDAP servers to use
         */
        public void setServerUrls( final List<String> serverUrls ) {
            this.serverUrls = set( this.serverUrls, wrap(serverUrls,AttributeExtensibleStringBuilder) );
        }

        /**
         * SSL/TLS client authentication flag.
         *
         * @return True if client authentication should be used
         */
        public boolean isUseSslClientClientAuthentication() {
            return get( useSslClientAuthentication, Boolean.FALSE );
        }

        /**
         * Set SSL/TLS client authentication flag.
         *
          * @param useSslClientAuthentication True to use client authentication
         */
        public void setUseSslClientAuthentication( final boolean useSslClientAuthentication ) {
            this.useSslClientAuthentication = set(this.useSslClientAuthentication,useSslClientAuthentication);
        }

        /**
         * Get the TLS/SSL client key identifier.
         *
         * @return The key identifier or null
         */
        public String getSslKeyId() {
            return sslKeyReference==null ? null : sslKeyReference.getId();
        }

        /**
         * Set the TLS/SSL client key identifier.
         *
         * @param keyId The key identifier to use
         */
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

        /**
         * Get the search base for the LDAP (required)
         *
         * @return The search base or null
         */
        public String getSearchBase() {
            return get( searchBase );
        }

        /**
         * Set the search base for the LDAP (required)
         *
         * @param searchBase The search base to use
         */
        public void setSearchBase( final String searchBase ) {
            this.searchBase = set(this.searchBase,searchBase);
        }

        /**
         * Get the bind "DN" for the LDAP (required)
         *
         * @return The bind DN or null
         */
        public String getBindDn() {
            return get( bindDn );
        }

        /**
         * Set the bind "DN" for the LDAP (required)
         *
         * @param bindDn The bind DN or null
         */
        public void setBindDn( final String bindDn ) {
            this.bindDn = set(this.bindDn,bindDn);
        }

        /**
         * Get the bind password for the LDAP.
         *
         * @return The bind password or null
         */
        public String getBindPassword() {
            return get( bindPassword );
        }

        /**
         * Set the bind password for the LDAP.
         *
         * @param bindPassword The bind password to use
         */
        public void setBindPassword( String bindPassword ) {
            this.bindPassword = set(this.bindPassword,bindPassword);
        }

        public void setBindPassword(final String password, final String bundleKey){
            this.bindPassword = new AttributeExtensibleType.AttributeExtensibleString();
            this.bindPassword.setValue(password);
            this.bindPassword.setAttributeExtensions(CollectionUtils.<QName,Object>mapBuilder().put(new QName("bundleKey"),bundleKey).map());
        }

        public String getBindPasswordBundleKey(){
            if(this.bindPassword.getAttributeExtensions() != null){
                return (String)this.bindPassword.getAttributeExtensions().get(new QName("bundleKey"));
            }
            return null;
        }

        /**
         * Flag for user mapping presence.
         *
         * @return True if this LDAP has user mapping configuration
         */
        public boolean hasUserMappings() {
            return userMappings!=null && userMappings.value != null;
        }

        /**
         * Get the user mappings for this LDAP.
         *
         * @return The user mappings (never null)
         */
        public List<LdapIdentityProviderMapping> getUserMappings() {
            return get( userMappings, new ArrayList<LdapIdentityProviderMapping>() );
        }

        /**
         * Set the user mappings for this LDAP.
         *
         * @param userMappings The mappings to use
         */
        public void setUserMappings( final List<LdapIdentityProviderMapping> userMappings ) {
            this.userMappings = set(this.userMappings,userMappings,AttributeExtensibleLdapIdentityProviderMappingList.Builder);
        }

        /**
         * Flag for user mapping presence.
         *
         * @return True if this LDAP has group mapping configuration
         */
        public boolean hasGroupMappings() {
            return groupMappings!=null && groupMappings.value != null;
        }

        /**
         * Get the group mappings for this LDAP.
         *
         * @return The group mappings (never null)
         */
        public List<LdapIdentityProviderMapping> getGroupMappings() {
            return get( groupMappings, new ArrayList<LdapIdentityProviderMapping>() );
        }

        /**
         * Set the group mappings for this LDAP.
         *
         * @param groupMappings The group mappings to use
         */
        public void setGroupMappings( final List<LdapIdentityProviderMapping> groupMappings ) {
            this.groupMappings = set(this.groupMappings,groupMappings,AttributeExtensibleLdapIdentityProviderMappingList.Builder);
        }

        /**
         * Get the list of specified attributes for this LDAP.
         *
         * @return The list of specified attributes (never null)
         */
        public List<String> getSpecifiedAttributes() {
            return unwrap(get( specifiedAttributes, new ArrayList<AttributeExtensibleString>() ));
        }

        /**
         * Set the list of specified attributes for this LDAP.
         *
         * @param specifiedAttributes The specified attributes to use
         */
        public void setSpecifiedAttributes( List<String> specifiedAttributes ) {
            this.specifiedAttributes = set( this.specifiedAttributes, wrap(specifiedAttributes,AttributeExtensibleStringBuilder) );
        }

        @XmlElement(name="NtlmProperties")
        @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
        public Map<String, Object> getNtlmProperties() {
            return ntlmProperties;
        }

        public void setNtlmProperties(Map<String, Object> ntlmProperties) {
            this.ntlmProperties = ntlmProperties;
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

    //- PACKAGE

    /**
     * Details for a policy backed identity provider.
     */
    @XmlType(name="PolicyBackedIdentityProviderDetailType", propOrder={"authenticationPolicyIdValue", "defaultRoleAssignmentIdValue"})
    public static class PolicyBackedIdentityProviderDetail extends IdentityProviderDetail {
        private AttributeExtensibleString authenticationPolicyId;
        private AttributeExtensibleString defaultRoleAssignmentId;


        protected PolicyBackedIdentityProviderDetail() {
        }

        /**
         * Get the authentication policy ID.
         *
         * @return The authentication policy ID
         */
        public String getAuthenticationPolicyId() {
            return get(authenticationPolicyId);
        }

        /**
         * Set the authentication policy ID.
         *
         * @param authenticationPolicyId The authentication policy ID
         */
        public void setAuthenticationPolicyId( final String authenticationPolicyId ) {
            this.authenticationPolicyId = set(this.authenticationPolicyId,authenticationPolicyId);
        }

        /**
         * Get the default role assignment ID.
         *
         * @return The default role assignment ID
         */
        public String getDefaultRoleAssignmentId() {
            return get( defaultRoleAssignmentId );
        }

        /**
         * Set the default role assignment ID.
         *
         * @param defaultRoleAssignmentId The authentication policy ID
         */
        public void setDefaultRoleAssignmentId( final String defaultRoleAssignmentId ) {
            this.defaultRoleAssignmentId = set(this.defaultRoleAssignmentId,defaultRoleAssignmentId);
        }

        @XmlElement(name="AuthenticationPolicyId", required=true)
        protected AttributeExtensibleString getAuthenticationPolicyIdValue() {
            return authenticationPolicyId;
        }

        protected void setAuthenticationPolicyIdValue( final AttributeExtensibleString authenticationPolicyId ) {
            this.authenticationPolicyId = authenticationPolicyId;
        }

        @XmlElement(name="DefaultRoleAssignmentId")
        protected AttributeExtensibleString getDefaultRoleAssignmentIdValue() {
            return defaultRoleAssignmentId;
        }

        protected void setDefaultRoleAssignmentIdValue( final AttributeExtensibleString defaultRoleAssignmentId ) {
            this.defaultRoleAssignmentId = defaultRoleAssignmentId;
        }
    }

    //- PRIVATE

    /**
     * Represents a user or group mapping
     *
     * @see ManagedObjectFactory#createLdapIdentityProviderMapping()
     */
    @XmlType(name="LdapIdentityProviderMappingType", propOrder={"objectClassValue","mappings","properties","extension","extensions"})
    public static class LdapIdentityProviderMapping extends ElementExtensionSupport {
        private AttributeExtensibleString objectClass;
        private Map<String,Object> mappings;
        private Map<String,Object> properties;

        LdapIdentityProviderMapping() {
        }

        /**
         * Get the LDAP objectclass for the mapping (required)
         *
         * @return The objectclass or null
         */
        public String getObjectClass() {
            return get( objectClass );
        }

        /**
         * Set the LDAP objectclass for the mapping (required)
         *
         * @param objectClass The objectclass to use
         */
        public void setObjectClass( final String objectClass ) {
            this.objectClass = set(this.objectClass,objectClass);
        }

        /**
         * Get the mappings (required)
         *
         * @return The properties (may be null)
         */
        @XmlElement(name="Mappings",required=true)
        @XmlJavaTypeAdapter( PropertiesMapType.PropertiesMapTypeAdapter.class)
        public Map<String, Object> getMappings() {
            return mappings;
        }

        /**
         * Set the mappings (required)
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
             @XmlElement(name="LdapIdentityProviderDetail",type=LdapIdentityProviderDetail.class),
             @XmlElement(name="BindOnlyLdapIdentityProviderDetail",type=BindOnlyLdapIdentityProviderDetail.class),
             @XmlElement(name="PolicyBackedIdentityProviderDetail",type=PolicyBackedIdentityProviderDetail.class)
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

    /**
     * AttributeExtensible extension for LdapIdentityProviderMapping[] properties.
     */
    @XmlType(name="LdapIdentityProviderMappingListPropertyType", propOrder={"value"})
    protected static class AttributeExtensibleLdapIdentityProviderMappingList  extends AttributeExtensibleType.AttributeExtensible<List<LdapIdentityProviderMapping>> {
        private static final Functions.Nullary<AttributeExtensibleLdapIdentityProviderMappingList> Builder =
                new Functions.Nullary<AttributeExtensibleLdapIdentityProviderMappingList>(){
            @Override
            public AttributeExtensibleLdapIdentityProviderMappingList call() {
                return new AttributeExtensibleLdapIdentityProviderMappingList();
            }
        };
        private List<LdapIdentityProviderMapping> value;

        protected AttributeExtensibleLdapIdentityProviderMappingList() {
        }

        @Override
        @XmlElement(name="Mapping")
        public List<LdapIdentityProviderMapping> getValue() {
            return value;
        }

        @Override
        public void setValue( final List<LdapIdentityProviderMapping> value ) {
            this.value = value;
        }
    }


}