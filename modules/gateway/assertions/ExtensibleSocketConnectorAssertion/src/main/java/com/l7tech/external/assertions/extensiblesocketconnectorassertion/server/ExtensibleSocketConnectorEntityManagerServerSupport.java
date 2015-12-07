package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntity;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntityAdmin;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorEntityAdminImpl;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;

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
    private ClusterPropertyManager clusterPropertyManager;
    private SsgKeyStoreManager keyStoreManager;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private StashManagerFactory stashManagerFactory;
    private MessageProcessor messageProcessor;
    private DefaultKey defaultKey;
    private FirewallRulesManager firewallRulesManager;

    public static synchronized ExtensibleSocketConnectorEntityManagerServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            ExtensibleSocketConnectorEntityManagerServerSupport s = new ExtensibleSocketConnectorEntityManagerServerSupport();
            s.init(context);
            instance = s;
        }
        return instance;
    }

    public void init(ApplicationContext context) {

        GenericEntityManager gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        gem.registerClass(ExtensibleSocketConnectorEntity.class);
        entityManager = gem.getEntityManager(ExtensibleSocketConnectorEntity.class);

        clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
        keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        trustManager = context.getBean("routingTrustManager", TrustManager.class);
        secureRandom = context.getBean("secureRandom", SecureRandom.class);
        stashManagerFactory = context.getBean("stashManagerFactory", StashManagerFactory.class);
        messageProcessor = context.getBean("messageProcessor", MessageProcessor.class);
        defaultKey = context.getBean("defaultKey", DefaultKey.class);
        firewallRulesManager = context.getBean("ssgFirewallManager", FirewallRulesManager.class);
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<ExtensibleSocketConnectorEntityAdmin>(
                ExtensibleSocketConnectorEntityAdmin.class,
                null,
                new ExtensibleSocketConnectorEntityAdminImpl(entityManager, clusterPropertyManager, keyStoreManager,
                        trustManager, secureRandom, stashManagerFactory, messageProcessor, defaultKey,
                        firewallRulesManager));
        return Collections.singletonList(binding);
    }
}
