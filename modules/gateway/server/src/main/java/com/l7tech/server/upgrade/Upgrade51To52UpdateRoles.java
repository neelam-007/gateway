package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static com.l7tech.objectmodel.EntityType.*;

/**
 * Upgrade task to update roles for 5.2.
 */
public class Upgrade51To52UpdateRoles implements UpgradeTask {

    //- PUBLIC

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        final ServiceManager serviceManager = getBean("serviceManager", ServiceManager.class);
        final RoleManager roleManager = getBean("roleManager", RoleManager.class);

        try {
            updateRolesForServices(serviceManager, roleManager);
        } catch (ObjectModelException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(Upgrade51To52UpdateRoles.class.getName());
    private ApplicationContext applicationContext;

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    @SuppressWarnings({ "unchecked" })
    private <T> T getBean( final String name,
                           final Class<T> beanClass ) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            return applicationContext.getBean(name, beanClass);
        } catch ( BeansException be ) {
            throw new FatalUpgradeException("Error accessing  bean '"+name+"' from ApplicationContext.");
        }
    }

    /**
     * Update manage service roles to include UDDI permissions.
     */
    private void updateRolesForServices( final ServiceManager serviceManager,
                                         final RoleManager roleManager ) throws FindException, SaveException, UpdateException {
        final Collection<ServiceHeader> services = serviceManager.findAllHeaders( false );

        for ( ServiceHeader serviceHeader : services ) {
            final Goid serviceGoid = serviceHeader.getGoid();
            final Collection<Role> roles = roleManager.findEntitySpecificRoles( EntityType.SERVICE, serviceGoid );

            if ( roles.isEmpty() ) {
                PublishedService service = serviceManager.findByPrimaryKey(serviceGoid);
                if ( service != null ) {
                    logger.warning( "Missing role for service '" + serviceGoid + "', creating new role." );
                    serviceManager.createRoles( service );
                } else {
                    logger.warning( "Missing role for service '" + serviceGoid + "', not creating new role (error finding service)." );
                }
            } else if ( roles.size() == 1 ) {
                final String serviceGoidStr = Goid.toString( serviceGoid );
                final Role role = roles.iterator().next();

                addEntityPermission(role, READ, UDDI_REGISTRY);

                //add attribute predicate to allow crud on a uddi_proxied_service
                addAttributePermission(role, CREATE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, serviceGoidStr);
                addAttributePermission(role, READ, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, serviceGoidStr);
                addAttributePermission(role, UPDATE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, serviceGoidStr);
                addAttributePermission(role, DELETE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_GOID, serviceGoidStr);

                //add attribute predicate to allow crud on a uddi_service_control
                addAttributePermission(role, CREATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, serviceGoidStr);
                addAttributePermission(role, READ, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, serviceGoidStr);
                addAttributePermission(role, UPDATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, serviceGoidStr);
                addAttributePermission(role, DELETE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_GOID, serviceGoidStr);

                roleManager.update( role );
            } else {
                logger.warning( "Not upgrading roles for service '" + serviceGoid + "', expected one role but found " +roles.size()+ "." );
            }
        }
    }

    /**
     * Add entity permission if not present
     */
    private void addEntityPermission( final Role role,
                                      final OperationType operationType,
                                      final EntityType entityType ) {
        boolean hasPermission = false;

        for ( Permission permission : role.getPermissions() ) {
            if ( permission.getEntityType() == entityType &&
                 permission.getOperation() == operationType &&
                 (permission.getScope() == null || permission.getScope().isEmpty()) ) {
                hasPermission = true;
                break;
            }
        }

        if ( !hasPermission ) {
            role.addEntityPermission( operationType, entityType, null );
        }
    }

    /**
     * Add attribute permission if not present 
     */
    private void addAttributePermission( final Role role,
                                         final OperationType operationType,
                                         final EntityType entityType,
                                         final String attrName,
                                         final String attrValue ) {
        boolean hasPermission = false;

        for ( Permission permission : role.getPermissions() ) {
            if ( permission.getEntityType() == entityType &&
                 permission.getOperation() == operationType &&
                 (permission.getScope() != null && permission.getScope().size()==1) ) {
                ScopePredicate predicate = permission.getScope().iterator().next();
                if ( predicate instanceof AttributePredicate &&
                     attrName.equals(((AttributePredicate)predicate).getAttribute()) &&
                     attrValue.equals(((AttributePredicate)predicate).getValue()) ) {
                    hasPermission = true;
                    break;
                }
            }
        }
        
        if ( !hasPermission ) {
            role.addAttributePermission( operationType, entityType, attrName, attrValue );
        }
    }
}
