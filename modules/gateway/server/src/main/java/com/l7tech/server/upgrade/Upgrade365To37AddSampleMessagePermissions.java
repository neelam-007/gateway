/**
 * Copyright (C) 2007-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.security.rbac.AttributePredicate;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.ScopePredicate;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import static com.l7tech.gateway.common.security.rbac.OperationType.*;
import static com.l7tech.objectmodel.EntityType.SAMPLE_MESSAGE;

/**
 * A database upgrade task that updates the Manage Specific Service Roles so that they also grant CRUD access to the
 * service's associated {@link com.l7tech.gateway.common.service.SampleMessage}s.
 */
public class Upgrade365To37AddSampleMessagePermissions implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade365To37AddSampleMessagePermissions.class.getName());
    private ApplicationContext applicationContext;

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException  if there is no application context or the requested bean was not found
     */
    private Object getBean(String name) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        Object bean = applicationContext.getBean(name);
        if (bean == null) throw new FatalUpgradeException("No bean " + name + " is available");
        return bean;
    }

    public void upgrade(ApplicationContext applicationContext) throws FatalUpgradeException, NonfatalUpgradeException {
        this.applicationContext = applicationContext;

        ServiceManager serviceManager = (ServiceManager)getBean("serviceManager");
        RoleManager roleManager = (RoleManager)getBean("roleManager");

        try {
            addPermissionsForSampleMessages(serviceManager, roleManager);
        } catch (FindException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        } catch (UpdateException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }

    }

    private void addPermissionsForSampleMessages(ServiceManager serviceManager, RoleManager roleManager)
            throws FindException, UpdateException {
        Collection<PublishedService> services = serviceManager.findAll();
        for (PublishedService service : services) {
            Collection<Role> roles = roleManager.findEntitySpecificRoles(EntityType.SERVICE, service.getGoid());
            if (roles == null) {
                logger.warning("Missing admin Role for service " + service.getName() + " (#" + service.getGoid() + ")");
                continue;
            }

            for ( Role role : roles ) {
                boolean canCreate = false, canRead = false, canUpdate = false, canDelete = false;
                for (Permission perm : role.getPermissions()) {
                    if (perm.getEntityType() == SAMPLE_MESSAGE) {
                        switch(perm.getOperation()) {
                            case CREATE:
                                canCreate = scopeIncludesThisService(perm.getScope(), service.getId());
                                break;
                            case READ:
                                canRead = scopeIncludesThisService(perm.getScope(), service.getId());
                                break;
                            case UPDATE:
                                canUpdate = scopeIncludesThisService(perm.getScope(), service.getId());
                                break;
                            case DELETE:
                                canDelete = scopeIncludesThisService(perm.getScope(), service.getId());
                                break;
                        }
                    }
                }

                if (!canCreate) role.addAttributePermission(CREATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, Goid.toString(service.getGoid()));
                if (!canRead)   role.addAttributePermission(READ,   SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, Goid.toString(service.getGoid()));
                if (!canUpdate) role.addAttributePermission(UPDATE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, Goid.toString(service.getGoid()));
                if (!canDelete) role.addAttributePermission(DELETE, SAMPLE_MESSAGE, SampleMessage.ATTR_SERVICE_GOID, Goid.toString(service.getGoid()));

                roleManager.update(role);
            }
        }
    }

    /**
     * @return true if the specified scope set is either empty (implying the operation is permitted on any object) or
     * has a single {@link AttributePredicate} referring to the provided serviceId.
     */
    private boolean scopeIncludesThisService(Set<ScopePredicate> scope, String serviceId) {
        if (scope == null || scope.isEmpty()) {
            return true;
        } else {
            Iterator<ScopePredicate> i = scope.iterator();
            ScopePredicate pred = i.next();
            if (i.hasNext()) return false; // Scope is something other than a single AttributePredicate
            if (pred instanceof AttributePredicate) {
                AttributePredicate apred = (AttributePredicate) pred;
                return SampleMessage.ATTR_SERVICE_GOID.equals(apred.getAttribute()) && serviceId.equals(apred.getValue());
            } else {
                return false;
            }
        }
    }

}
