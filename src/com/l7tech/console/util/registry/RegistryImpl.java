package com.l7tech.console.util.registry;

import com.l7tech.identity.*;
import com.l7tech.common.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import javax.swing.tree.TreeNode;



/**
 * A central place that provides initial access to all components
 * and services used in the console.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegistryImpl extends Registry {
    /**
     * @return the identity provider config manager
     */
    public IdentityProviderConfigManager getProviderConfigManager()
      throws RuntimeException {
        IdentityProviderConfigManager ipc =
          (IdentityProviderConfigManager)Locator.
          getDefault().lookup(IdentityProviderConfigManager.class);
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }
        return ipc;
    }

    /**
     * @return the internal identity provider
     */
    public IdentityProvider getInternalProvider() {
        IdentityProviderConfigManager ipc = getProviderConfigManager();
        if (ipc == null) {
            throw new RuntimeException("Could not find registered " + IdentityProviderConfigManager.class);
        }
        return ipc.getInternalIdentityProvider();
    }

    /**
     * @return the identity provider to which this node belongs to
     */
    public IdentityProvider getIdentityProvider(EntityHeaderNode node) {
        TreeNode parentNode = node.getParent();
        while (parentNode != null) {
            if (parentNode instanceof EntityHeaderNode) {
                EntityHeader header = ((EntityHeaderNode)parentNode).getEntityHeader();
                if (header.getType().equals(EntityType.ID_PROVIDER_CONFIG)) {
                    IdentityProviderConfigManager ipc = getProviderConfigManager();
                    try {
                        return ipc.getIdentityProvider(header.getOid());
                    } catch (FindException e) {
                        throw new RuntimeException("could not find related identity provider", e);
                    }
                }
            }
            parentNode = parentNode.getParent();
        }
        return null;
    }

    /**
     * @return the internal user manager
     */
    public UserManager getInternalUserManager() {
        return getInternalProvider().getUserManager();
    }

    /**
     * @return the internal group manager
     */
    public GroupManager getInternalGroupManager() {
        return getInternalProvider().getGroupManager();
    }

    /**
     * @return the service manager
     */
    public ServiceAdmin getServiceManager() {
        ServiceAdmin sm = (ServiceAdmin)Locator.getDefault().lookup(ServiceAdmin.class);
        if (sm == null) {
            throw new RuntimeException("Could not find registered " + ServiceAdmin.class);
        }
        return sm;
    }
}
