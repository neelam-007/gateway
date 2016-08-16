package com.l7tech.external.assertions.mongodb;

import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.GatewayState;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;

import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 6/4/13
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class MongoDBAssertionModuleLoadListener {

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static MongoDBConnectionExternalReferenceFactory externalReferenceFactory;
    private static ApplicationEventProxy applicationEventProxy;
    private static MongoDBAssertionSpringApplicationListener applicationListener;
    private static GenericEntityManager gem;

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleUnloaded() {
        removeApplicationListeners();
        MongoDBConnectionManager.getInstance().closeConnections();
        unregisterGenericEntities();
        unregisterExternalReferenceFactory();
    }

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        final SecurePasswordManager securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
        final X509TrustManager trustManager = context.getBean("trustManager", X509TrustManager.class);
        final SecureRandom secureRandom = context.getBean("secureRandom", SecureRandom.class);
        final SsgKeyStoreManager keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        final DefaultKey defaultKey = context.getBean("defaultKey", DefaultKey.class);
        applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);

        registerGenericEntities(context);
        registerExternalReferenceFactory(context);

        if (MongoDBConnectionManager.getInstance() == null) {
            MongoDBConnectionManager.createMongoDBConnectionManager(securePasswordManager, keyStoreManager, trustManager, secureRandom, defaultKey);
        }

        EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager = gem.getEntityManager(MongoDBConnectionEntity.class);
        // If Gateway is already ready for messages, then start the connection manager
        GatewayState gatewayState = context.getBean("gatewayState", GatewayState.class);
        if (gatewayState.isReadyForMessages() && MongoDBConnectionManager.getInstance() != null) {
            MongoDBConnectionManager.getInstance().loadMongoDBConnections(entityManager);
        }

        addApplicationListener(entityManager, securePasswordManager, keyStoreManager, trustManager, secureRandom, defaultKey);
    }

    /**
     * Remove application listeners
     */
    private static void removeApplicationListeners() {
        if (applicationEventProxy != null && applicationListener != null) {
            applicationEventProxy.removeApplicationListener(applicationListener);
            applicationEventProxy = null;
            applicationListener = null;
        }
    }

    /**
     * Unregister external reference factory
     */
    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }

    /**
     * Register external reference factory
     */
    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new MongoDBConnectionExternalReferenceFactory(MongoDBAssertion.class, MongoDBReference.class);
        policyExporterImporterManager.register(externalReferenceFactory);
    }

    /**
     * Register generic entities for this module
     * @param context The application context containing the Generic Entity Manager bean
     */
    private static void registerGenericEntities(final ApplicationContext context) {
        if (gem == null) {
            gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        }

        if (gem.isRegistered(MongoDBConnectionEntity.class.getName())) {
            gem.unRegisterClass(MongoDBConnectionEntity.class.getName());
        }

        gem.registerClass(MongoDBConnectionEntity.class);
    }

    /**
     * Unregister generic entities for this module
     */

    private static void unregisterGenericEntities() {
        if (gem != null) {
            if (gem.isRegistered(MongoDBConnectionEntity.class.getName())) {
                gem.unRegisterClass(MongoDBConnectionEntity.class.getName());
            }
            gem = null;
        }
    }

    /**
     *  Add the application listener to the event proxy
     */

    private static void addApplicationListener(EntityManager<MongoDBConnectionEntity, GenericEntityHeader> entityManager,
                                               SecurePasswordManager securePasswordManager, SsgKeyStoreManager keyStoreManager,
                                               X509TrustManager trustManager, SecureRandom secureRandom, DefaultKey defaultKey) {
        applicationListener = new MongoDBAssertionSpringApplicationListener(entityManager, securePasswordManager, keyStoreManager, trustManager, secureRandom, defaultKey);
        applicationEventProxy.addApplicationListener(applicationListener);
    }
}