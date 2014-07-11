package com.l7tech.external.assertions.websocket.server;

import com.l7tech.external.assertions.websocket.WebSocketConnectionEntity;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.entity.GenericEntityManager;
import org.springframework.context.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: abjorge
 * Date: 21/03/13
 * Time: 4:23 PM
 */
public class EntityManagerFactory {
    protected static final Logger logger = Logger.getLogger(EntityManagerFactory.class.getName());
    private static EntityManager<WebSocketConnectionEntity, GenericEntityHeader> entityManager = null;

    public static synchronized EntityManager<WebSocketConnectionEntity, GenericEntityHeader> getEntityManager(ApplicationContext context) {

        if (entityManager == null) {
            GenericEntityManager gem = context.getBean("genericEntityManager", GenericEntityManager.class);
            try {
                gem.registerClass(WebSocketConnectionEntity.class);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to register entity manager. Already registered.");
            }
            entityManager = gem.getEntityManager(WebSocketConnectionEntity.class);
        }

        return entityManager;
    }
}
