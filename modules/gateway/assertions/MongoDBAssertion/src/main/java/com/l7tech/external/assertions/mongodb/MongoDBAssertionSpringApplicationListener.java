package com.l7tech.external.assertions.mongodb;

import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdmin;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntityAdminImpl;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
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


    public MongoDBAssertionSpringApplicationListener(SecurePasswordManager securePasswordManager, SsgKeyStoreManager keyStoreManager, X509TrustManager trustManager,
                                                     SecureRandom secureRandom, DefaultKey defaultKey) {
        this.securePasswordManager = securePasswordManager;
        this.keyStoreManager = keyStoreManager;
        this.trustManager = trustManager;
        this.secureRandom = secureRandom;
        this.defaultKey = defaultKey;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {

        if (event instanceof ReadyForMessages) {
            MongoDBConnectionManager.createMongoDBConnectionManager(securePasswordManager, keyStoreManager, trustManager, secureRandom, defaultKey);
            MongoDBConnectionManager.getInstance().loadMongoDBConnections();
        }

        if (event instanceof EntityInvalidationEvent &&
                event.getSource().getClass().equals(GenericEntity.class)) {

            EntityInvalidationEvent e = (EntityInvalidationEvent) event;
            GenericEntity sourceEntity = (GenericEntity) e.getSource();
            char operation = e.getEntityOperations()[0];

            if (sourceEntity.getEntityClassName().equals(MongoDBConnectionEntity.class.getName())) {

                MongoDBConnectionEntityAdmin mongoDBConnectionEntityAdmin = MongoDBConnectionEntityAdminImpl.getInstance(null);
                MongoDBConnectionEntity connectionEntity = null;
                Goid goid = sourceEntity.getGoid();

                try {
                    connectionEntity = mongoDBConnectionEntityAdmin.findByGoid(goid);
                } catch (FindException e1) {
                    if (goid != null) {
                        logger.warning("Entity for goid, " + goid.toString() + ", not found when creating/updating " +
                                "ExtensibleSocketConnectorEntity");
                    }
                    else {
                        logger.warning("Entity for goid is NULL and not found when creating/updating " +
                                "ExtensibleSocketConnectorEntity");
                    }

                    return;
                }

                if (operation == EntityInvalidationEvent.CREATE)
                    MongoDBConnectionManager.getInstance().addConnection(connectionEntity);

                if (operation == EntityInvalidationEvent.UPDATE)
                    MongoDBConnectionManager.getInstance().updateConnection(connectionEntity);

                if (operation == EntityInvalidationEvent.DELETE)
                    MongoDBConnectionManager.getInstance().removeConnection(goid);

            }
        }
    }
}
