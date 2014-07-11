package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntityAdmin;
import com.l7tech.external.assertions.websocket.WebSocketConnectionEntityAdminImpl;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.assertion.ExtensionInterfaceBinding;
import org.springframework.context.ApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * User: cirving
 * Date: 6/4/12
 * Time: 11:37 AM
 */
public class WebSocketEntityManagerServerSupport {
    protected static final Logger logger = Logger.getLogger(WebSocketEntityManagerServerSupport.class.getName());

    private static WebSocketEntityManagerServerSupport instance;
    private EntityManager<WebSocketConnectionEntity, GenericEntityHeader> entityManager;

    public static synchronized WebSocketEntityManagerServerSupport getInstance(final ApplicationContext context) {
        if (instance == null) {
            WebSocketEntityManagerServerSupport s = new WebSocketEntityManagerServerSupport();
            s.init(context);
            instance = s;
        }
        return instance;
    }

    public void init(ApplicationContext context) {
        entityManager = EntityManagerFactory.getEntityManager(context);
    }

    public Collection<ExtensionInterfaceBinding> getExtensionInterfaceBindings() {
        ExtensionInterfaceBinding binding = new ExtensionInterfaceBinding<>(WebSocketConnectionEntityAdmin.class, null, new WebSocketConnectionEntityAdminImpl(entityManager));
        return Collections.singletonList(binding);
    }

}
