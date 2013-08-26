package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.SiteMinderConfigurationMO;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.siteminder.SiteMinderConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.siteminder.SiteMinderConfigurationManager;
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
    protected SiteMinderConfigurationMO asResource(SiteMinderConfiguration siteMinderCfg) {
        SiteMinderConfigurationMO smResource = ManagedObjectFactory.createSiteMinderConfiguration();

        smResource.setId( siteMinderCfg.getId() );
        smResource.setName(siteMinderCfg.getName());
        smResource.setAgentName(siteMinderCfg.getAgentName());
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
        smResource.setIpCheck( siteMinderCfg.isIpcheck() );
        smResource.setFipsMode( siteMinderCfg.getFipsmode() );
        smResource.setClusterThreshold( siteMinderCfg.getClusterThreshold() );

        Map<String,String> properties = new HashMap<String,String>();
        for (String propertyName : siteMinderCfg.getPropertyNames()) {
            properties.put(propertyName, siteMinderCfg.getProperties().get(propertyName));
        }
        smResource.setProperties( properties );

        // handle SecurityZone
        doSecurityZoneAsResource( smResource, siteMinderCfg );

        return smResource;
    }

    @Override
    protected SiteMinderConfiguration fromResource(Object resource) throws InvalidResourceException {

        if ( !(resource instanceof SiteMinderConfigurationMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected SiteMinder configuration");

        final SiteMinderConfigurationMO smResource = (SiteMinderConfigurationMO) resource;

        final SiteMinderConfiguration smConfiguration;
        smConfiguration = new SiteMinderConfiguration();

        smConfiguration.setName( smResource.getName() );
        smConfiguration.setAgentName( smResource.getAgentName() );
        smConfiguration.setAddress( smResource.getAddress() );
        smConfiguration.setSecret( smResource.getSecret() );
        smConfiguration.setHostname( smResource.getHostname() );
        smConfiguration.setHostConfiguration( smResource.getHostConfiguration() );
        smConfiguration.setUserName( smResource.getUserName() );
        if ( smResource.getPasswordId() != null && !smResource.getPasswordId().isEmpty() ) {
            try {
                SecurePassword password = securePasswordManager.findByPrimaryKey( Goid.parseGoid( smResource.getPasswordId() ) );

                if (password == null) {
                    throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown secure password reference");
                }
                smConfiguration.setPasswordGoid(Goid.parseGoid(smResource.getPasswordId()));
            } catch (FindException e) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown secure password reference");
            }
        } else {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "invalid or unknown secure password reference");
        }
        smConfiguration.setIpcheck( smResource.getIpCheck() );
        smConfiguration.setUpdateSSOToken( smResource.getUpdateSsoToken() );
        smConfiguration.setEnabled( smResource.getEnabled() );
        smConfiguration.setNonClusterFailover( smResource.getNonClusterFailover() );
        smConfiguration.setIpcheck( smResource.getEnabled() );
        smConfiguration.setFipsmode( smResource.getFipsMode() );
        smConfiguration.setClusterThreshold( smResource.getClusterThreshold() );

        Map<String, String> smProperties = new HashMap<String, String>( smResource.getProperties().size() );
        for (Map.Entry<String, String> entry : smResource.getProperties().entrySet()) {
            smProperties.put( entry.getKey(), entry.getValue() );
        }
        smConfiguration.setProperties( smProperties );

        // handle SecurityZone
        doSecurityZoneFromResource( smResource, smConfiguration );

        return smConfiguration;
    }

    @Override
    protected void updateEntity(SiteMinderConfiguration oldEntity, SiteMinderConfiguration newEntity) throws InvalidResourceException {

        oldEntity.setName( newEntity.getName() );
        oldEntity.setAgentName( newEntity.getAgentName() );
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
        oldEntity.setIpcheck( newEntity.isIpcheck() );
        oldEntity.setFipsmode( newEntity.getFipsmode() );
        oldEntity.setClusterThreshold( newEntity.getClusterThreshold() );
        oldEntity.setSecurityZone( newEntity.getSecurityZone() );

        Map<String, String> newProperties = new HashMap<String, String>( newEntity.getProperties().size() );
        for (String prop : newEntity.getPropertyNames()) {
            newProperties.put( prop, newEntity.getProperties().get( prop ) );
        }
        oldEntity.setProperties( newProperties );

    }

    //- PRIVATE

    private SiteMinderConfigurationManager siteminderConfigManager;
    private SecurePasswordManager securePasswordManager;

}
