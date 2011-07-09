package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.IdentityProviderMO;
import static com.l7tech.gateway.api.IdentityProviderMO.*;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.GroupMappingConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.MemberStrategy;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Option;
import com.l7tech.util.TimeUnit;
import org.springframework.transaction.PlatformTransactionManager;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 */
@ResourceFactory.ResourceType(type=IdentityProviderMO.class)
public class IdentityProviderResourceFactory extends EntityManagerResourceFactory<IdentityProviderMO, IdentityProviderConfig, EntityHeader>{

    //- PUBLIC

    public IdentityProviderResourceFactory( final RbacServices services,
                                            final SecurityFilter securityFilter,
                                            final PlatformTransactionManager transactionManager,
                                            final IdentityProviderConfigManager identityProviderConfigManager,
                                            final LdapConfigTemplateManager ldapConfigTemplateManager ) {
        super( false, true, services, securityFilter, transactionManager, identityProviderConfigManager );
        this.ldapConfigTemplateManager = ldapConfigTemplateManager;
    }

    //- PROTECTED

    @Override
    protected IdentityProviderMO asResource( final IdentityProviderConfig identityProviderConfig ) {
        IdentityProviderMO identityProvider = ManagedObjectFactory.createIdentityProvider();

        identityProvider.setName( identityProviderConfig.getName() );
        switch ( identityProviderConfig.getTypeVal() ) {
            case 1:
                identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.INTERNAL );
                break;
            case 2:
                identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.LDAP );
                asResource( identityProvider, (LdapIdentityProviderConfig) identityProviderConfig );
                break;
            case 3:
                identityProvider.setIdentityProviderType( IdentityProviderMO.IdentityProviderType.FEDERATED );
                asResource( identityProvider, (FederatedIdentityProviderConfig) identityProviderConfig );
                break;
            default:
                throw new ResourceAccessException("Unknown identity provider type '"+identityProviderConfig.getTypeVal()+"'.");
        }

        // add common properties
        identityProvider.setProperties( getProperties( identityProvider.getProperties(), identityProviderConfig, IdentityProviderConfig.class ) );
        if ( identityProviderConfig.getCertificateValidationType() != null ) {
            identityProvider.getProperties().put(
                    PROP_CERTIFICATE_VALIDATION,
                    EntityPropertiesHelper.getEnumText(identityProviderConfig.getCertificateValidationType()) );
        }

        return identityProvider;
    }

    @Override
    protected IdentityProviderConfig fromResource( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof IdentityProviderMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected identity provider");

        final IdentityProviderMO identityProviderResource = (IdentityProviderMO) resource;

        final IdentityProviderConfig identityProvider;
        switch ( identityProviderResource.getIdentityProviderType() ) {
            case INTERNAL:
                identityProvider = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
                break;
            case LDAP:
                identityProvider = fromResource( identityProviderResource, new LdapIdentityProviderConfig() );
                break;
            case FEDERATED:
                identityProvider = fromResource( identityProviderResource, new FederatedIdentityProviderConfig() );
                break;
            default:
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "identity provider type unknown");
        }
        identityProvider.setName( identityProviderResource.getName() );

        setProperties( identityProvider, identityProviderResource.getProperties(), identityProvider.getClass() );
        identityProvider.setCertificateValidationType( certificateValidationTypeFromProperties( identityProviderResource.getProperties() ) );

        return identityProvider;
    }

    @Override
    protected void updateEntity( final IdentityProviderConfig oldEntity,
                                 final IdentityProviderConfig newEntity ) throws InvalidResourceException {
        if ( oldEntity.getTypeVal() == newEntity.getTypeVal() ) {
            switch ( oldEntity.getTypeVal() ) {
                case 2:
                    updateEntity( (LdapIdentityProviderConfig)oldEntity, (LdapIdentityProviderConfig)newEntity );
                    break;
                case 3:
                    updateEntity( (FederatedIdentityProviderConfig)oldEntity, (FederatedIdentityProviderConfig)newEntity );
                    break;
            }
            oldEntity.setCertificateValidationType( newEntity.getCertificateValidationType() );
        } else {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "cannot change provider type");
        }
    }

    //- PRIVATE

    private static final String PROP_CERTIFICATE_VALIDATION = "certificateValidation";

    private static final String GROUP_STRATEGY_MEMBER_IS_USER_DN = "Member is User DN";
    private static final String GROUP_STRATEGY_MEMBER_IS_USER_LOGIN = "Member is User Login";
    private static final String GROUP_STRATEGY_MEMBER_IS_NV_PAIR = "Member is NV Pair";
    private static final String GROUP_STRATEGY_OU_GROUP = "OU Group";

    private final LdapConfigTemplateManager ldapConfigTemplateManager;

    private CertificateValidationType certificateValidationTypeFromProperties( final Map<String, Object> properties ) throws InvalidResourceException {
        CertificateValidationType type = null;

        final Option<String> stringValue = getProperty( properties, PROP_CERTIFICATE_VALIDATION, Option.<String>none(), String.class );
        if ( stringValue.isSome() ) {
            type = EntityPropertiesHelper.getEnumValue( CertificateValidationType.class, stringValue.some() );
        }

        return type;
    }

    private void asResource( final IdentityProviderMO identityProvider,
                             final FederatedIdentityProviderConfig federatedIdentityProviderConfig ) {
        identityProvider.setProperties( getProperties( federatedIdentityProviderConfig, FederatedIdentityProviderConfig.class ) );
        if ( federatedIdentityProviderConfig.getTrustedCertOids() != null &&
             federatedIdentityProviderConfig.getTrustedCertOids().length > 0 ) {
            final FederatedIdentityProviderDetail detail = identityProvider.getFederatedIdentityProviderDetail();
            if ( detail != null ) {
                detail.setCertificateReferences( Functions.map( Arrays.asList(ArrayUtils.box(federatedIdentityProviderConfig.getTrustedCertOids())), new Functions.Unary<String,Long>(){
                    @Override
                    public String call( final Long aLong ) {
                        return aLong.toString();
                    }
                } ));
            }
        }
    }

    private FederatedIdentityProviderConfig fromResource( final IdentityProviderMO identityProviderResource,
                                                          final FederatedIdentityProviderConfig federatedIdentityProviderConfig) throws InvalidResourceException {

        final FederatedIdentityProviderDetail detail = identityProviderResource.getFederatedIdentityProviderDetail();
        if ( detail != null && detail.getCertificateReferences() != null ) {
            try {
                federatedIdentityProviderConfig.setTrustedCertOids( ArrayUtils.unbox( Functions.map( detail.getCertificateReferences(), new Functions.Unary<Long,String>(){
                    @Override
                    public Long call( final String s ) {
                        return Long.parseLong( s );
                    }
                } ) ));
            } catch ( NumberFormatException nfe ) {
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid certificate reference");
            }
        }

        return federatedIdentityProviderConfig;
    }

    private void asResource( final IdentityProviderMO identityProvider,
                             final LdapIdentityProviderConfig ldapIdentityProviderConfig ) {
        identityProvider.setProperties( getProperties( ldapIdentityProviderConfig, LdapIdentityProviderConfig.class ) );
        final LdapIdentityProviderDetail detail = identityProvider.getLdapIdentityProviderDetail();
        if ( detail != null ) {
            detail.setSourceType( ldapIdentityProviderConfig.getTemplateName() );
            detail.setServerUrls( ldapIdentityProviderConfig.getLdapUrl()==null ? null : Arrays.asList(ldapIdentityProviderConfig.getLdapUrl()) );
            detail.setUseSslClientAuthentication( ldapIdentityProviderConfig.isClientAuthEnabled() );
            if ( ldapIdentityProviderConfig.getKeystoreId()!=null && ldapIdentityProviderConfig.getKeyAlias()!=null ) {
                detail.setSslKeyId( ldapIdentityProviderConfig.getKeystoreId() + ":" + ldapIdentityProviderConfig.getKeyAlias() );
            }
            detail.setSearchBase( ldapIdentityProviderConfig.getSearchBase() );
            detail.setBindDn( ldapIdentityProviderConfig.getBindDN() );
            detail.setUserMappings( buildMappings( ldapIdentityProviderConfig.getUserMappings() ) );
            detail.setGroupMappings( buildMappings( ldapIdentityProviderConfig.getGroupMappings() ) );
            detail.setSpecifiedAttributes( ldapIdentityProviderConfig.getReturningAttributes()==null ? null : Arrays.asList(ldapIdentityProviderConfig.getReturningAttributes()) );
        }
    }

    private List<LdapIdentityProviderMapping> buildMappings( final UserMappingConfig[] userMappings ) {
        List<LdapIdentityProviderMapping> mappings = null;
        if ( userMappings!=null ) {
            final Set<PropertyDescriptor> propertyDescriptors = BeanUtils.omitProperties(BeanUtils.getProperties(UserMappingConfig.class), "objectClass", "passwdType");

            mappings = Functions.map( Arrays.asList(userMappings), new Functions.Unary<LdapIdentityProviderMapping,UserMappingConfig>(){
                @Override
                public LdapIdentityProviderMapping call( final UserMappingConfig userMappingConfig ) {
                    final LdapIdentityProviderMapping mapping = new LdapIdentityProviderMapping();
                    mapping.setObjectClass( userMappingConfig.getObjClass() );
                    mapping.setMappings( getProperties( userMappingConfig, propertyDescriptors ) );
                    return mapping;
                }
            } );
        }
        return mappings;
    }

    private List<LdapIdentityProviderMapping> buildMappings( final GroupMappingConfig[] groupMappings ) {
        List<LdapIdentityProviderMapping> mappings = null;
        if ( groupMappings!=null ) {
            final Set<PropertyDescriptor> propertyDescriptors = BeanUtils.omitProperties(BeanUtils.getProperties(GroupMappingConfig.class), "objectClass", "memberStrategy");

            mappings = Functions.map( Arrays.asList(groupMappings), new Functions.Unary<LdapIdentityProviderMapping,GroupMappingConfig>(){
                @Override
                public LdapIdentityProviderMapping call( final GroupMappingConfig groupMappingConfig ) {
                    final LdapIdentityProviderMapping mapping = new LdapIdentityProviderMapping();
                    mapping.setObjectClass( groupMappingConfig.getObjClass() );
                    if ( groupMappingConfig.getMemberStrategy()!=null ) {
                        final Map<String,Object> properties = new HashMap<String,Object>();
                        properties.put( "memberStrategy", asString(groupMappingConfig.getMemberStrategy()) );
                        mapping.setProperties( properties );
                    }
                    mapping.setMappings( getProperties( groupMappingConfig, propertyDescriptors ) );
                    return mapping;
                }
            } );
        }
        return mappings;
    }

    private UserMappingConfig[] buildUserMappings( final List<LdapIdentityProviderMapping> mappings ) throws InvalidResourceException {
        final UserMappingConfig[] userMappings;
        if ( mappings == null ) {
            userMappings = new UserMappingConfig[0];
        } else {
            final Set<PropertyDescriptor> propertyDescriptors = BeanUtils.omitProperties(BeanUtils.getProperties(UserMappingConfig.class), "objectClass", "passwdType");
            final List<UserMappingConfig> userMappingList = Functions.map( mappings, new Functions.UnaryThrows<UserMappingConfig,LdapIdentityProviderMapping,InvalidResourceException>(){
                @Override
                public UserMappingConfig call( final LdapIdentityProviderMapping ldapIdentityProviderMapping ) throws InvalidResourceException {
                    final UserMappingConfig userMapping = new UserMappingConfig();
                    userMapping.setObjClass( ldapIdentityProviderMapping.getObjectClass() );
                    setProperties( userMapping, ldapIdentityProviderMapping.getMappings(), propertyDescriptors );
                    return userMapping;
                }
            } );
            userMappings = userMappingList.toArray( new UserMappingConfig[userMappingList.size()] );
        }
        return userMappings;
    }

    private GroupMappingConfig[] buildGroupMappings( final List<LdapIdentityProviderMapping> mappings ) throws InvalidResourceException {
        final GroupMappingConfig[] groupMappings;
        if ( mappings == null ) {
            groupMappings = new GroupMappingConfig[0];
        } else {
            final Set<PropertyDescriptor> propertyDescriptors = BeanUtils.omitProperties(BeanUtils.getProperties(GroupMappingConfig.class), "objectClass", "memberStrategy");
            final List<GroupMappingConfig> groupMappingList = Functions.map( mappings, new Functions.UnaryThrows<GroupMappingConfig,LdapIdentityProviderMapping,InvalidResourceException>(){
                @Override
                public GroupMappingConfig call( final LdapIdentityProviderMapping ldapIdentityProviderMapping ) throws InvalidResourceException {
                    final GroupMappingConfig groupMapping = new GroupMappingConfig();
                    groupMapping.setObjClass( ldapIdentityProviderMapping.getObjectClass() );
                    groupMapping.setMemberStrategy( fromString(getProperty( ldapIdentityProviderMapping.getProperties(), "memberStrategy", Option.<String>none(), String.class )) );
                    setProperties( groupMapping, ldapIdentityProviderMapping.getMappings(), propertyDescriptors );
                    return groupMapping;
                }
            } );
            groupMappings = groupMappingList.toArray( new GroupMappingConfig[groupMappingList.size()] );
        }
        return groupMappings;
    }

    private String asString( final MemberStrategy memberStrategy ) {
        final String value;
        if ( MemberStrategy.MEMBERS_ARE_DN.equals(memberStrategy) ) {
            value = GROUP_STRATEGY_MEMBER_IS_USER_DN;
        } else if ( MemberStrategy.MEMBERS_ARE_LOGIN.equals(memberStrategy)  ) {
            value = GROUP_STRATEGY_MEMBER_IS_USER_LOGIN;
        } else if ( MemberStrategy.MEMBERS_ARE_NVPAIR.equals(memberStrategy) ) {
            value = GROUP_STRATEGY_MEMBER_IS_NV_PAIR;
        } else if ( MemberStrategy.MEMBERS_BY_OU.equals(memberStrategy) ) {
            value = GROUP_STRATEGY_OU_GROUP;
        } else {
            throw new ResourceAccessException("Unknown group membership strategy : " + memberStrategy.getVal() );
        }
        return value;
    }

    private MemberStrategy fromString( final Option<String> memberStrategyText ) throws InvalidResourceException {
        if ( !memberStrategyText.isSome() ) {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.MISSING_VALUES, "Missing group membership strategy" );
        }
        final MemberStrategy memberStrategy;
        if ( GROUP_STRATEGY_MEMBER_IS_USER_DN.equals(memberStrategyText.some()) ) {
            memberStrategy = MemberStrategy.MEMBERS_ARE_DN;
        } else if ( GROUP_STRATEGY_MEMBER_IS_USER_LOGIN.equals(memberStrategyText.some()) ) {
            memberStrategy = MemberStrategy.MEMBERS_ARE_LOGIN;
        } else if ( GROUP_STRATEGY_MEMBER_IS_NV_PAIR.equals(memberStrategyText.some()) ) {
            memberStrategy = MemberStrategy.MEMBERS_ARE_NVPAIR;
        } else if ( GROUP_STRATEGY_OU_GROUP.equals(memberStrategyText.some()) ) {
            memberStrategy = MemberStrategy.MEMBERS_BY_OU;
        } else {
            throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid group membership strategy : " + memberStrategyText );
        }
        return memberStrategy;
    }

    private Map<String, Object> getProperties( final Object bean, final Set<PropertyDescriptor> propertyDescriptors ) {
        final Map<String,Object> properties = new HashMap<String,Object>();
        for ( final PropertyDescriptor propertyDescriptor : propertyDescriptors ) {
            try {
                final Object value = propertyDescriptor.getReadMethod().invoke( bean );
                if ( value instanceof String ) {
                    properties.put( propertyDescriptor.getName(), value );
                }
            } catch ( IllegalAccessException e ) {
                throw new ResourceAccessException(e);
            } catch ( InvocationTargetException e ) {
                throw new ResourceAccessException(e.getCause());
            }
        }
        return properties;
    }

    private void setProperties( final Object bean,
                                final Map<String,Object> properties,
                                final Set<PropertyDescriptor> propertyDescriptors ) {
        try {
            BeanUtils.copyProperties( properties, bean, propertyDescriptors );
        } catch ( IllegalAccessException e ) {
            throw new ResourceAccessException(e);
        } catch ( InvocationTargetException e ) {
            throw new ResourceAccessException(e.getCause());
        }
    }

    private LdapIdentityProviderConfig fromResource( final IdentityProviderMO identityProviderResource,
                                                     final LdapIdentityProviderConfig ldapIdentityProviderConfig) throws InvalidResourceException {
        final LdapIdentityProviderDetail detail = identityProviderResource.getLdapIdentityProviderDetail();
        if ( detail != null ) {
            // initialize values from the template
            final LdapIdentityProviderConfig template;
            try {
                template = ldapConfigTemplateManager.getTemplate( detail.getSourceType() );
                if ( template == null ) {
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Unknown source type '"+detail.getSourceType()+"'" );
                }
            } catch ( IOException e ) {
                throw new ResourceAccessException("Error accessing template for source type '"+detail.getSourceType()+"'", e);
            }

            // set properties from resource, overrides template values
            ldapIdentityProviderConfig.setTemplateName( template.getTemplateName() );
            ldapIdentityProviderConfig.setLdapUrl( detail.getServerUrls()==null ? null : detail.getServerUrls().toArray(new String[detail.getServerUrls().size()]) );
            ldapIdentityProviderConfig.setClientAuthEnabled( detail.isUseSslClientClientAuthentication() );
            if ( detail.getSslKeyId() != null ) {
                final String[] values = detail.getSslKeyId().split( ":", 2 );
                if ( values.length != 2 ) {
                    throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "Invalid SSL key identifier" );
                }
                ldapIdentityProviderConfig.setKeystoreId( toInternalId( values[0], "SSL key identifier" ));
                ldapIdentityProviderConfig.setKeyAlias( values[1] );
            }
            ldapIdentityProviderConfig.setSearchBase( detail.getSearchBase() );
            ldapIdentityProviderConfig.setBindDN( detail.getBindDn() );
            ldapIdentityProviderConfig.setBindPasswd( detail.getBindPassword() );
            ldapIdentityProviderConfig.setUserMappings( detail.hasUserMappings() ? buildUserMappings( detail.getUserMappings() ) : template.getUserMappings() );
            ldapIdentityProviderConfig.setGroupMappings( detail.hasGroupMappings() ? buildGroupMappings( detail.getGroupMappings() ) : template.getGroupMappings() );
            ldapIdentityProviderConfig.setReturningAttributes( detail.getSpecifiedAttributes()==null ? null : detail.getSpecifiedAttributes().toArray(new String[detail.getSpecifiedAttributes().size()]) );
        }

        return ldapIdentityProviderConfig;
    }

    private void updateEntity( final LdapIdentityProviderConfig oldConfig,
                               final LdapIdentityProviderConfig newConfig ) {
        oldConfig.setLdapUrl( newConfig.getLdapUrl() );
        oldConfig.setClientAuthEnabled( newConfig.isClientAuthEnabled() );
        oldConfig.setKeyAlias( newConfig.getKeyAlias() );
        oldConfig.setKeystoreId( newConfig.getKeystoreId() );
        oldConfig.setSearchBase( newConfig.getSearchBase() );
        oldConfig.setBindDN( newConfig.getBindDN() );
        oldConfig.setBindPasswd( newConfig.getBindPasswd() );
        oldConfig.setAdminEnabled( newConfig.isAdminEnabled() );

        oldConfig.setGroupMappings( newConfig.getGroupMappings() );
        oldConfig.setUserMappings( newConfig.getUserMappings() );

        oldConfig.setGroupCacheSize( newConfig.getGroupCacheSize() );
        oldConfig.setGroupCacheMaxAge( newConfig.getGroupCacheMaxAge() );
        oldConfig.setGroupCacheMaxAgeUnit( TimeUnit.largestUnitForValue( newConfig.getGroupCacheMaxAge(), TimeUnit.MINUTES ) );
        oldConfig.setGroupMaxNesting( newConfig.getGroupMaxNesting() );
        oldConfig.setGroupMembershipCaseInsensitive( newConfig.isGroupMembershipCaseInsensitive() );
        oldConfig.setReturningAttributes( newConfig.getReturningAttributes() );

        oldConfig.setUserCertificateUseType( newConfig.getUserCertificateUseType() );
        oldConfig.setUserCertificateIndexSearchFilter( null );
        oldConfig.setUserCertificateIssuerSerialSearchFilter( null );
        oldConfig.setUserCertificateSKISearchFilter( null );
        switch ( newConfig.getUserCertificateUseType() ) {
            case INDEX_CUSTOM:
                oldConfig.setUserCertificateIndexSearchFilter( newConfig.getUserCertificateIndexSearchFilter() );
                break;
            case SEARCH:
                oldConfig.setUserCertificateIssuerSerialSearchFilter( newConfig.getUserCertificateIssuerSerialSearchFilter() );
                oldConfig.setUserCertificateSKISearchFilter( newConfig.getUserCertificateSKISearchFilter() );
                break;
        }
    }

    private void updateEntity( final FederatedIdentityProviderConfig oldConfig,
                               final FederatedIdentityProviderConfig newConfig ) {
        oldConfig.setSamlSupported( newConfig.isSamlSupported() );
        oldConfig.setX509Supported( newConfig.isX509Supported() );
        oldConfig.setTrustedCertOids( newConfig.getTrustedCertOids() );
    }
}
