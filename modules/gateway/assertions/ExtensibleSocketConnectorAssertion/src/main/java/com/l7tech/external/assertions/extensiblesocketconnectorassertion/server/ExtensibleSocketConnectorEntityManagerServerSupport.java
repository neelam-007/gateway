package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntityAdmin;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntityAdminImpl;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.entity.GenericEntityManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 28/03/12
 * Time: 2:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorEntityManagerServerSupport {
    private static ExtensibleSocketConnectorEntityManagerServerSupport instance;

    private EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager;

    /**
     * Private constructor to prevent instantiation
     */
    private ExtensibleSocketConnectorEntityManagerServerSupport() {
    }

    /**
     * Get the singleton instance.
     */
    public static synchronized ExtensibleSocketConnectorEntityManagerServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            ExtensibleSocketConnectorEntityManagerServerSupport s = new ExtensibleSocketConnectorEntityManagerServerSupport();
            s.init(context);
            instance = s;
        }
        return instance;
    }

    private void init(ApplicationContext context) {
        GenericEntityManager gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        entityManager = gem.getEntityManager(ExtensibleSocketConnectorEntity.class);
    }

    /**
     * Get the ExtensionInterfaceBindings
     *
     * @return Collection of ExtensionInterfaceBinding
     */
    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(
                ExtensibleSocketConnectorEntityAdmin.class,
                null,
                new ExtensibleSocketConnectorEntityAdminImpl(entityManager));
        return Collections.singletonList(binding);
    }
}
