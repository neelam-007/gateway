package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
import com.l7tech.util.GoidUpgradeMapper;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@ResourceFactory.ResourceType(type=SiteMinderConfigurationMO.class)
public class SiteMinderConfigurationResourceFactory extends SecurityZoneableEntityManagerResourceFactory<SiteMinderConfigurationMO, SiteMinderConfiguration, EntityHeader> {

    //- PUBLIC

    public SiteMinderConfigurationResourceFactory(final RbacServices rbacServices,
                                                  final SecurityFilter securityFilter,
                                                  final PlatformTransactionManager transactionManager,
                                                  final SiteMinderConfigurationManager siteminderConfigurationManager,
                                                  final SecurePasswordManager securePasswordManager,
                                                  final SecurityZoneManager securityZoneManager) {
        super(false, true, rbacServices, securityFilter, transactionManager, siteminderConfigurationManager, securityZoneManager);
        this.siteminderConfigManager = siteminderConfigurationManager;
        this.securePasswordManager = securePasswordManager;
    }

    //- PROTECTED

    @Override
    public SiteMinderConfigurationMO asResource(SiteMinderConfiguration siteMinderCfg) {
        SiteMinderConfigurationMO smResource = ManagedObjectFactory.createSiteMinderConfiguration();

        smResource.setId( siteMinderCfg.getId() );
        smResource.setName(siteMinderCfg.getName());
        // omit Secret for reads
        smResource.setAddress( siteMinderCfg.getAddress() );
        smResource.setHostname( siteMinderCfg.getHostname() );
        smResource.setHostConfiguration( siteMinderCfg.getHostConfiguration() );
        smResource.setUserName( siteMinderCfg.getUserName() );
        smResource.setPasswordId(siteMinderCfg.getPasswordGoid() == null ? null:siteMinderCfg.getPasswordGoid().toString());
        smResource.setIpCheck( siteMinderCfg.isIpcheck() );
        smResource.setUpdateSsoToken( siteMinderCfg.isUpdateSSOToken() );
        smResource.setEnabled( siteMinderCfg.isEnabled() );
        smResource.setNonClusterFailover( siteMinderCfg.isNonClusterFailover() );
        smResource.setFipsMode( siteMinderCfg.getFipsmode() );
        smResource.setClusterThreshold( siteMinderCfg.getClusterThreshold() );

        Map<String,String> properties = new HashMap<>();
        for (String propertyName : siteMinderCfg.getPropertyNames()) {
            properties.put(propertyName, siteMinderCfg.getProperties().get(propertyName));
        }
        smResource.setProperties( properties );

        // handle SecurityZone
        doSecurityZoneAsResource( smResource, siteMinderCfg );

        return smResource;
    }

    @Override
    public SiteMinderConfiguration fromResource(Object resource, boolean strict) throws InvalidResourceException {

        if ( !(resource instanceof SiteMinderConfigurationMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected CA Single Sign-On configuration");

        final SiteMinderConfigurationMO smResource = (SiteMinderConfigurationMO) resource;

        final SiteMinderConfiguration smConfiguration;
        smConfiguration = new SiteMinderConfiguration();

        smConfiguration.setName( smResource.getName() );
        smConfiguration.setAddress( smResource.getAddress() );
        smConfiguration.setSecret( smResource.getSecret() );
        smConfiguration.setHostname( smResource.getHostname() );
        smConfiguration.setHostConfiguration( smResource.getHostConfiguration() );
        smConfiguration.setUserName( smResource.getUserName() );
        if ( smResource.getPasswordId() != null && !smResource.getPasswordId().isEmpty() ) {
            try {
                try {
                    SecurePassword password = securePasswordManager.findByPrimaryKey( GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, smResource.getPasswordId()) );
                    if (password == null && strict) {
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown secure password reference");
                    }
                } catch (FindException e) {
                    if(strict)
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown secure password reference");
                }
                smConfiguration.setPasswordGoid(GoidUpgradeMapper.mapId(EntityType.SECURE_PASSWORD, smResource.getPasswordId()));
            } catch (IllegalArgumentException ile) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown secure password reference");
            }
        }
        smConfiguration.setIpcheck( smResource.getIpCheck() );
        smConfiguration.setUpdateSSOToken( smResource.getUpdateSsoToken() );
        smConfiguration.setEnabled( smResource.getEnabled() );
        smConfiguration.setNonClusterFailover( smResource.getNonClusterFailover() );
        smConfiguration.setFipsmode( smResource.getFipsMode() );
        smConfiguration.setClusterThreshold( smResource.getClusterThreshold() );

        Map<String, String> smProperties = new HashMap<>();
        if ( smResource.getProperties() != null ) {
            for (Map.Entry<String, String> entry : smResource.getProperties().entrySet()) {
                smProperties.put( entry.getKey(), entry.getValue() );
            }
        }
        smConfiguration.setProperties( smProperties );

        // handle SecurityZone
        doSecurityZoneFromResource( smResource, smConfiguration, strict );

        return smConfiguration;
    }

    @Override
    protected void updateEntity(SiteMinderConfiguration oldEntity, SiteMinderConfiguration newEntity) throws InvalidResourceException {

        oldEntity.setName( newEntity.getName() );
        oldEntity.setAddress( newEntity.getAddress() );
        if ( newEntity.getSecret() != null ) {
            oldEntity.setSecret( newEntity.getSecret() );
        }
        oldEntity.setHostname( newEntity.getHostname() );
        oldEntity.setHostConfiguration( newEntity.getHostConfiguration() );
        oldEntity.setUserName( newEntity.getUserName() );
        oldEntity.setPasswordGoid(newEntity.getPasswordGoid());
        oldEntity.setIpcheck( newEntity.isIpcheck() );
        oldEntity.setUpdateSSOToken( newEntity.isUpdateSSOToken() );
        oldEntity.setEnabled( newEntity.isEnabled() );
        oldEntity.setNonClusterFailover( newEntity.isNonClusterFailover() );
        oldEntity.setFipsmode( newEntity.getFipsmode() );
        oldEntity.setClusterThreshold( newEntity.getClusterThreshold() );
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );

        Map<String, String> newProperties = new HashMap<>( newEntity.getProperties().size() );
        for (String prop : newEntity.getPropertyNames()) {
            newProperties.put( prop, newEntity.getProperties().get( prop ) );
        }
        oldEntity.setProperties( newProperties );

    }

    //- PRIVATE

    private SiteMinderConfigurationManager siteminderConfigManager;
    private SecurePasswordManager securePasswordManager;

}
