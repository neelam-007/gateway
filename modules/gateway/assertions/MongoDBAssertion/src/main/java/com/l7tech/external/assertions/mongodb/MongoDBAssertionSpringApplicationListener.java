package com.l7tech.external.assertions.mongodb;

import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 6/4/13
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class MongoDBAssertionSpringApplicationListener implements ApplicationListener {
    private static final Logger logger = Logger.getLogger(MongoDBAssertionSpringApplicationListener.class.getName());

    private SecurePasswordManager securePasswordManager = null;
    private SsgKeyStoreManager keyStoreManager = null;
    private X509TrustManager trustManager = null;
    private SecureRandom secureRandom = null;
    private DefaultKey defaultKey = null;
    private EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager = null;


    public MongoDBAssertionSpringApplicationListener(EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager, SecurePasswordManager securePasswordManager, SsgKeyStoreManager keyStoreManager, X509TrustManager trustManager,
                                                     SecureRandom secureRandom, DefaultKey defaultKey) {
        this.entityManager = entityManager;
        this.securePasswordManager = securePasswordManager;
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
        this.defaultKey = defaultKey;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        if (event instanceof ReadyForMessages) {
            // Connection manager should have already been instantiated before this
            MongoDBConnectionManager.getInstance().loadMongoDBConnections(entityManager);
        }

        if (event instanceof EntityInvalidationEvent) {

            EntityInvalidationEvent e = (EntityInvalidationEvent) event;

            if (GenericEntity.class.equals(e.getEntityClass())) {
                Goid[] goids = e.getEntityIds();
                char[] ops = e.getEntityOperations();
                for (int i = 0; i < ops.length; i++) {
                    switch (ops[i]) {
                        case EntityInvalidationEvent.CREATE:
                            try {
                                MongoDBConnectionEntity entity = entityManager.findByPrimaryKey(goids[i]);
                                if (entity != null) {
                                    MongoDBConnectionManager.getInstance().addConnection(entity);
                                }
                            } catch (FindException f) {
                                logger.warning("Entity for goid, " + goids[i].toString() + ", not found when creating " +
                                        "MongoDBConnectionEntity");
                            }
                            break;

                        case EntityInvalidationEvent.UPDATE:
                            try {
                                MongoDBConnectionEntity entity = entityManager.findByPrimaryKey(goids[i]);
                                if (entity != null) {
                                    MongoDBConnectionManager.getInstance().updateConnection(entity);
                                }
                            } catch (FindException f) {
                                logger.warning("Entity for goid, " + goids[i].toString() + ", not found when updating " +
                                        "MongoDBConnectionEntity");
                            }
                            break;

                        case EntityInvalidationEvent.DELETE:
                            MongoDBConnectionManager.getInstance().removeConnection(goids[i]);
                            break;

                        default:
                            logger.log(Level.WARNING, "Unexpected EntityInvalidationEvent Operation: " + ops[i]);
                            break;
                    }
                }
            }
        }
    }
}
