package com.l7tech.external.assertions.mongodb;

import com.l7tech.server.DefaultKey;
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


    public static synchronized void onModuleUnloaded() {
        MongoDBConnectionManager.getInstance().closeConnections();
    }

    public static synchronized void onModuleLoaded(final ApplicationContext context) {

        final SecurePasswordManager securePasswordManager = context.getBean("securePasswordManager", SecurePasswordManager.class);
        final ApplicationEventProxy applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        final X509TrustManager trustManager = context.getBean("trustManager", X509TrustManager.class);
        final SecureRandom secureRandom = context.getBean("secureRandom", SecureRandom.class);
        final SsgKeyStoreManager keyStoreManager = context.getBean("ssgKeyStoreManager", SsgKeyStoreManager.class);
        final DefaultKey defaultKey = context.getBean("defaultKey", DefaultKey.class);

        applicationEventProxy.addApplicationListener(new MongoDBAssertionSpringApplicationListener(securePasswordManager, keyStoreManager, trustManager, secureRandom, defaultKey));

        registerExternalReferenceFactory(context);

    }

    private static void unregisterExternalReferenceFactory() {
        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }
    }

    private static void registerExternalReferenceFactory(final ApplicationContext context) {
        unregisterExternalReferenceFactory();

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }

        externalReferenceFactory = new MongoDBConnectionExternalReferenceFactory(MongoDBAssertion.class, MongoDBReference.class);
        policyExporterImporterManager.register(externalReferenceFactory);


    }
}