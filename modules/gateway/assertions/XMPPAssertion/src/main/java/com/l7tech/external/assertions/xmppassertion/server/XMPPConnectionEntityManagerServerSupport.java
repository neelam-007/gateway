package com.l7tech.external.assertions.xmppassertion.server;

import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntity;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntityAdmin;
import com.l7tech.external.assertions.xmppassertion.XMPPConnectionEntityAdminImpl;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Collections;

/**
 * User: njordan
 * Date: 07/03/12
 * Time: 10:37 AM
 */
public class XMPPConnectionEntityManagerServerSupport {
    private static XMPPConnectionEntityManagerServerSupport instance;

    private EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager;
    private StashManagerFactory stashManagerFactory;
    private MessageProcessor messageProcessor;
    private SsgKeyStoreManager keyStoreManager;
    private TrustManager trustManager;
    private SecureRandom secureRandom;
    private DefaultKey defaultKey;
    private FirewallRulesManager firewallRulesManager;

    public static synchronized XMPPConnectionEntityManagerServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            XMPPConnectionEntityManagerServerSupport s = new XMPPConnectionEntityManagerServerSupport();
            s.init(context);
            instance = s;
        }
        return instance;
    }

    public void init(ApplicationContext context) {
        GenericEntityManager gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        gem.registerClass(XMPPConnectionEntity.class);
        entityManager = gem.getEntityManager(XMPPConnectionEntity.class);

        stashManagerFactory = context.getBean("stashManagerFactory", StashManagerFactory.class);
        messageProcessor = context.getBean("messageProcessor", MessageProcessor.class);
        keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        trustManager = context.getBean("routingTrustManager", TrustManager.class);
        secureRandom = context.getBean("secureRandom", SecureRandom.class);
        defaultKey = context.getBean("defaultKey", DefaultKey.class);
        firewallRulesManager = context.getBean("ssgFirewallManager", FirewallRulesManager.class);
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<XMPPConnectionEntityAdmin>(
                XMPPConnectionEntityAdmin.class,
                null,
                new XMPPConnectionEntityAdminImpl(entityManager, stashManagerFactory, messageProcessor, keyStoreManager, trustManager, secureRandom, defaultKey, firewallRulesManager));
        return Collections.singletonList(binding);
    }
}
