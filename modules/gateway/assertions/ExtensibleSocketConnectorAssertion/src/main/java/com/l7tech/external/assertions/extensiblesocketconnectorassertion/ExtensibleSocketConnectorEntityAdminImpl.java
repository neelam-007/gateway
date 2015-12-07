package com.l7tech.external.assertions.extensiblesocketconnectorassertion;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.MinaCodecFactory;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.SocketConnectorManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.net.ssl.TrustManager;
import java.io.InputStream;
import java.io.StringReader;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 27/03/12
 * Time: 2:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorEntityAdminImpl implements ExtensibleSocketConnectorEntityAdmin {
    private static final Logger logger = Logger.getLogger(ExtensibleSocketConnectorEntityAdminImpl.class.getName());

    private static ExtensibleSocketConnectorEntityAdminImpl INSTANCE;

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static ExtensibleSocketConnectorReferenceFactory externalReferenceFactory;

    private EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager;
    private SocketConnectorManager connectionManager;

    public ExtensibleSocketConnectorEntityAdminImpl(EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager,
                                                    ClusterPropertyManager clusterPropertyManager,
                                                    SsgKeyStoreManager keyStoreManager,
                                                    TrustManager trustManager,
                                                    SecureRandom secureRandom,
                                                    StashManagerFactory stashManagerFactory,
                                                    MessageProcessor messageProcessor,
                                                    DefaultKey defaultKey,
                                                    FirewallRulesManager firewallRulesManager) {
        INSTANCE = this;

        loadCodecModules(clusterPropertyManager);
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");

        this.entityManager = entityManager;
        try {
            SocketConnectorManager.createConnectionManager(this.entityManager, clusterPropertyManager, keyStoreManager, trustManager, secureRandom, stashManagerFactory, messageProcessor, defaultKey, firewallRulesManager);
            connectionManager = SocketConnectorManager.getInstance();
            connectionManager.initializeConnectorClasses();
            connectionManager.setDirectoryContext(new InitialDirContext(env));
        } catch (IllegalStateException e) {
            logger.log(Level.WARNING, "Error creating the ExtensibleSocket connection manager.", e);
        } catch (NamingException e) {
            logger.log(Level.WARNING, "Error creating the ExtensibleSocket connection manager.", e);
        }
    }

    ExtensibleSocketConnectorEntityAdminImpl(EntityManager<ExtensibleSocketConnectorEntity, GenericEntityHeader> entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Collection<ExtensibleSocketConnectorEntity> findAll() throws FindException {
        return entityManager.findAll();
    }

    @Override
    public Goid save(ExtensibleSocketConnectorEntity entity) throws SaveException, UpdateException {
        try {
            if (entity.getGoid().equals(ExtensibleSocketConnectorEntity.DEFAULT_GOID)) {
                Goid goid = entityManager.save(entity);
                entity.setGoid(goid);
                return goid;
            } else {
                entityManager.update(entity);
                return entity.getGoid();
            }
        } catch (SaveException e) {
            throw e;
        } catch (UpdateException e) {
            throw e;
        }
    }

    @Override
    public void delete(ExtensibleSocketConnectorEntity entity) throws DeleteException, FindException {
        entityManager.delete(entity);
    }

    @Override
    public ExtensibleSocketConnectorEntity findByUniqueName(String name) throws FindException {
        return entityManager.findByUniqueName(name);
    }

    @Override
    public ExtensibleSocketConnectorEntity find(Goid goid) throws FindException {
        return entityManager.findByPrimaryKey(goid);
    }

    @Override
    public Vector<CodecModule> getCodecModules() {
        return MinaCodecFactory.getCodecModules();
    }

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        System.setProperty("mina.sslfilter.directbuffer", "false"); // RSA doesn't like direct ByteBuffers

        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }
        externalReferenceFactory = new ExtensibleSocketConnectorReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);

        // Only initialize all the ExtensibleSocketConnector inbound/outbound resource managers when the SSG is "ready for messages"
        ApplicationEventProxy applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {

                // only do this when the SSG is ready for messages
                if (event instanceof ReadyForMessages) {
                    // initialize the outbound MQ connection manager
                    if (INSTANCE != null) {
                        INSTANCE.connectionManager.start();
                    }
                }

                //startup the connection for the generic entity when one has been created
                //or updated
                if (event instanceof EntityInvalidationEvent &&
                        event.getSource().getClass().equals(GenericEntity.class)) {

                    EntityInvalidationEvent e = (EntityInvalidationEvent) event;
                    GenericEntity sourceEntity = (GenericEntity) e.getSource();
                    Goid goid = sourceEntity.getGoid();
                    char operation = e.getEntityOperations()[0];

                    if (sourceEntity.getEntityClassName().equals(ExtensibleSocketConnectorEntity.class.getName())) {

                        ExtensibleSocketConnectorEntity entity = null;
                        try {
                            entity = INSTANCE.entityManager.findByPrimaryKey(goid);
                        } catch (FindException e1) {
                            logger.warning("Entity for goid, " + goid.toString() + ", not found when creating/updating " +
                                    "ExtensibleSocketConnectorEntity");
                            return;
                        }

                        if (operation == EntityInvalidationEvent.CREATE)
                            INSTANCE.connectionManager.connectionAdded(entity);

                        if (operation == EntityInvalidationEvent.UPDATE)
                            INSTANCE.connectionManager.connectionUpdated(entity);

                        if (operation == EntityInvalidationEvent.DELETE)
                            INSTANCE.connectionManager.connectionRemoved(goid);

                    }
                }

            }
        });
    }

    public static synchronized void onModuleUnloaded() {
        INSTANCE.connectionManager.stop();

        if (policyExporterImporterManager != null && externalReferenceFactory != null) {
            policyExporterImporterManager.unregister(externalReferenceFactory);
            externalReferenceFactory = null;
        }

        System.setProperty("mina.sslfilter.directbuffer", "");
    }

    private void loadCodecModules(ClusterPropertyManager clusterPropertyManager) {
        try {
            Properties properties = new Properties();
            InputStream stream = ExtensibleSocketConnectorEntityAdminImpl.class.getClassLoader().getResourceAsStream("com/l7tech/external/assertions/extensiblesocketconnectorassertion/codecs.properties");
            properties.load(stream);
            stream.close();

            // Load the extra codecs defined in the cluster property
            String value = clusterPropertyManager.getProperty("extensibleSocketConnector.codecs");
            if (value != null) {
                properties.load(new StringReader(value));
            }

            Map<String, HashMap<String, String>> codecModulesData = new HashMap<String, HashMap<String, String>>();
            for (String propertyName : properties.stringPropertyNames()) {
                String key = propertyName.substring(0, propertyName.lastIndexOf("."));

                HashMap<String, String> codecModuleData;
                if (codecModulesData.containsKey(key)) {
                    codecModuleData = codecModulesData.get(key);
                } else {
                    codecModuleData = new HashMap<String, String>();
                    codecModulesData.put(key, codecModuleData);
                }

                String settingName = propertyName.substring(propertyName.lastIndexOf(".") + 1);
                codecModuleData.put(settingName, properties.getProperty(propertyName));
            }

            for (HashMap<String, String> data : codecModulesData.values()) {
                if (data.containsKey("dialog") && data.containsKey("configuration") && data.containsKey("codec") &&
                        data.containsKey("defaultContentType") && data.containsKey("displayName")) {
                    CodecModule codecModule = new CodecModule(
                            data.get("dialog"),
                            data.get("defaultContentType"),
                            data.get("configuration"),
                            data.get("displayName"),
                            data.get("codec")
                    );

                    MinaCodecFactory.addCodecModule(codecModule);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to load the codecs for the ExtensibleSocketConnector assertion.");
        }
    }
}
