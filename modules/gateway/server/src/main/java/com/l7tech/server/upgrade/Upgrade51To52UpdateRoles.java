package com.l7tech.server.upgrade;

import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import java.util.logging.Logger;
import java.util.Collection;

import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.ObjectModelException;
import static com.l7tech.objectmodel.EntityType.*;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIServiceControl;

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
            return (T) applicationContext.getBean(name, beanClass);
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
            final long serviceOid = serviceHeader.getOid();
            final Collection<Role> roles = roleManager.findEntitySpecificRoles( EntityType.SERVICE, serviceOid );

            if ( roles.isEmpty() ) {
                PublishedService service = serviceManager.findByPrimaryKey(serviceOid);
                if ( service != null ) {
                    logger.warning( "Missing role for service '" + serviceOid + "', creating new role." );
                    serviceManager.createRoles( service );
                } else {
                    logger.warning( "Missing role for service '" + serviceOid + "', not creating new role (error finding service)." );
                }
            } else if ( roles.size() == 1 ) {
                final String serviceOidStr = Long.toString( serviceOid );
                final Role role = roles.iterator().next();

                addEntityPermission(role, READ, UDDI_REGISTRY);

                //add attribute predicate to allow crud on a uddi_proxied_service
                addAttributePermission(role, CREATE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_OID, serviceOidStr);
                addAttributePermission(role, READ, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_OID, serviceOidStr);
                addAttributePermission(role, UPDATE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_OID, serviceOidStr);
                addAttributePermission(role, DELETE, UDDI_PROXIED_SERVICE_INFO, UDDIProxiedServiceInfo.ATTR_SERVICE_OID, serviceOidStr);

                //add attribute predicate to allow crud on a uddi_service_control
                addAttributePermission(role, CREATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, serviceOidStr);
                addAttributePermission(role, READ, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, serviceOidStr);
                addAttributePermission(role, UPDATE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, serviceOidStr);
                addAttributePermission(role, DELETE, UDDI_SERVICE_CONTROL, UDDIServiceControl.ATTR_SERVICE_OID, serviceOidStr);

                roleManager.update( role );
            } else {
                logger.warning( "Not upgrading roles for service '" + serviceOid + "', expected one role but found " +roles.size()+ "." );
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
