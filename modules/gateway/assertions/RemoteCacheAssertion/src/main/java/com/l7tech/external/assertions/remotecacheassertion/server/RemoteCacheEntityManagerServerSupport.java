package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntityAdmin;
import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntityAdminImpl;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.entity.GenericEntityMetadata;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by IntelliJ IDEA.
 * User: ashah
 * Date: 14/05/12
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheEntityManagerServerSupport {
    private static RemoteCacheEntityManagerServerSupport instance;

    private EntityManager<RemoteCacheEntity, GenericEntityHeader> entityManager;
    private ServerConfig serverConfig;
    private ClusterPropertyManager clusterPropertyManager;

    private Collection<ExtensionInterfaceBinding> bindingInterface;

    public static synchronized RemoteCacheEntityManagerServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            RemoteCacheEntityManagerServerSupport s = new RemoteCacheEntityManagerServerSupport();
            s.init(context);
            instance = s;
        }
        return instance;
    }

    private void init(ApplicationContext context) {
        GenericEntityManager gem = context.getBean("genericEntityManager", GenericEntityManager.class);

        entityManager = gem.getEntityManager(RemoteCacheEntity.class);

        serverConfig = context.getBean("serverConfig", ServerConfig.class);
        clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        if (bindingInterface != null) {
            return bindingInterface;
        }
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(
                RemoteCacheEntityAdmin.class,
                null,
                new RemoteCacheEntityAdminImpl(entityManager, serverConfig, clusterPropertyManager));
        bindingInterface = Collections.singletonList(binding);
        return bindingInterface;
    }
}
