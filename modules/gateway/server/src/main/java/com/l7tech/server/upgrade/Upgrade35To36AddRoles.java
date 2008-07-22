/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.upgrade;

import static com.l7tech.gateway.common.security.rbac.EntityType.ID_PROVIDER_CONFIG;
import static com.l7tech.gateway.common.security.rbac.EntityType.SERVICE;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.gateway.common.service.PublishedService;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * A database upgrade task that adds roles to the database.
 */
public class Upgrade35To36AddRoles implements UpgradeTask {
    private static final Logger logger = Logger.getLogger(Upgrade35To36AddRoles.class.getName());
    private ApplicationContext applicationContext;

    /**
     * Get a bean safely.
     *
     * @param name the bean to get.  Must not be null.
     * @return the requested bean.  Never null.
     * @throws FatalUpgradeException  if there is no application context or the requested bean was not found
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
        IdentityProviderConfigManager identityProviderConfigManager =
                (IdentityProviderConfigManager)getBean("identityProviderConfigManager");

        try {
            addRolesForPublishedServices(serviceManager, roleManager);
            addRolesForIdentityProviders(identityProviderConfigManager, roleManager);
        } catch (FindException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        } catch (SaveException e) {
            throw new NonfatalUpgradeException(e); // rollback, but continue boot, and try again another day
        }

    }

    private void addRolesForPublishedServices(ServiceManager serviceManager, RoleManager roleManager)
            throws FindException, SaveException
    {
        // Find all published services
        // If any of them doesn't have a role, try to create it
        Collection<PublishedService> services = serviceManager.findAll();
        for (PublishedService service : services) {
            Role role = roleManager.findEntitySpecificRole(SERVICE, service.getOid());
            if (role == null) {
                logger.info("Auto-creating missing admin Role for service " + service.getName() + " (#" + service.getOid() + ")");
                serviceManager.addManageServiceRole(service);
            }
        }
    }

    private void addRolesForIdentityProviders(IdentityProviderConfigManager ipcManager, RoleManager roleManager)
            throws FindException, SaveException
    {
        // Find all identity providers
        // If any of them doesn't have a role, try to create it
        Collection<IdentityProviderConfig> ipcs = ipcManager.findAll();
        for (IdentityProviderConfig ipc : ipcs) {
            if (ipc.getOid() <= 0) continue;  // Don't mess with built-in ones
            
            Role role = roleManager.findEntitySpecificRole(ID_PROVIDER_CONFIG, ipc.getOid());
            if (role == null) {
                logger.info("Auto-creating missing admin Role for identity provider " + ipc.getName() + " (#" + ipc.getOid() + ")");
                ipcManager.addManageProviderRole(ipc);
            }
        }
    }
}
