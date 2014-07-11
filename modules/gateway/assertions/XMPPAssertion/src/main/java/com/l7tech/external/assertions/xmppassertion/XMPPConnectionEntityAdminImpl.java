package com.l7tech.external.assertions.xmppassertion;

import com.l7tech.external.assertions.xmppassertion.server.XMPPConnectionManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.admin.Deleted;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.security.keystore.SsgKeyStoreManager;
import com.l7tech.server.service.FirewallRulesManager;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: njordan
 * Date: 07/03/12
 * Time: 10:32 AM
 */
public class XMPPConnectionEntityAdminImpl implements XMPPConnectionEntityAdmin {
    private static final Logger logger = Logger.getLogger(XMPPConnectionEntityAdminImpl.class.getName());

    private static XMPPConnectionEntityAdminImpl INSTANCE;

    private static PolicyExporterImporterManager policyExporterImporterManager;
    private static XMPPConnectionExternalReferenceFactory externalReferenceFactory;

    private EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager;
    private static XMPPConnectionManager connectionManager;

    public XMPPConnectionEntityAdminImpl(EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager,
                                         StashManagerFactory stashManagerFactory,
                                         MessageProcessor messageProcessor,
                                         SsgKeyStoreManager keyStoreManager,
                                         TrustManager trustManager,
                                         SecureRandom secureRandom,
                                         DefaultKey defaultKey,
                                         FirewallRulesManager firewallRulesManager)
    {
        INSTANCE = this;

        this.entityManager = entityManager;
        try {
            XMPPConnectionManager.createConnectionManager(this.entityManager, stashManagerFactory, messageProcessor, keyStoreManager, trustManager, secureRandom, defaultKey, firewallRulesManager);
            connectionManager = XMPPConnectionManager.getInstance();
        } catch(IllegalStateException e) {
            logger.log(Level.WARNING, "Error creating the XMPP connection manager.", e);
        }
    }

    XMPPConnectionEntityAdminImpl(EntityManager<XMPPConnectionEntity, GenericEntityHeader> entityManager)
    {
        this.entityManager = entityManager;
    }

    @Override
    public Collection<XMPPConnectionEntity> findAll() throws FindException {
        return entityManager.findAll();
    }

    @Override
    public Goid save(XMPPConnectionEntity entity) throws SaveException, UpdateException {
        if (entity.getGoid().equals(XMPPConnectionEntity.DEFAULT_GOID)) {
            Goid goid = entityManager.save(entity);
            entity.setGoid(goid);
            return goid;
        } else {
            entityManager.update(entity);
            return entity.getGoid();
        }
    }

    @Override
    public void delete(XMPPConnectionEntity entity) throws DeleteException, FindException {
        entityManager.delete(entity);
    }

    @Override
    public XMPPConnectionEntity findByUniqueName(String name) throws FindException {
        return entityManager.findByUniqueName(name);
    }

    @Override
    public XMPPConnectionEntity find(Goid goid) throws FindException {
        return entityManager.findByPrimaryKey(goid);
    }

    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (policyExporterImporterManager == null) {
            policyExporterImporterManager = context.getBean("policyExporterImporterManager", PolicyExporterImporterManager.class);
        }
        externalReferenceFactory = new XMPPConnectionExternalReferenceFactory();
        policyExporterImporterManager.register(externalReferenceFactory);

        // Only initialize all the XMPP inbound/outbound resource managers when the SSG is "ready for messages"
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
                } else if (event instanceof Created) {
                    if (((Created) event).getEntity() instanceof GenericEntity) {
                        GenericEntity entity = (GenericEntity) ((Created) event).getEntity();
                        if (entity.getEntityClassName().equals(XMPPConnectionEntity.class.getName())) {
                            logger.log(Level.INFO, "Created XMPP Connection " + entity.getId());
                            try {
                                XMPPConnectionEntity connectionEntity = XMPPConnectionUtils.asConcreteEntity(entity, XMPPConnectionEntity.class);
                                connectionManager.connectionAdded(connectionEntity);
                            } catch (FindException e) {
                                logger.log(Level.WARNING, "Unable to find XMPP Connection Entity");
                            }
                        }
                    }
                } else if (event instanceof Updated) {
                    if (((Updated) event).getEntity() instanceof GenericEntity) {
                        GenericEntity entity = (GenericEntity) ((Updated) event).getEntity();
                        if (entity.getEntityClassName().equals(XMPPConnectionEntity.class.getName())) {
                            logger.log(Level.INFO, "Changed XMPP Connection " + entity.getId());
                            try {
                                XMPPConnectionEntity connectionEntity = XMPPConnectionUtils.asConcreteEntity(entity, XMPPConnectionEntity.class);
                                connectionManager.connectionUpdated(connectionEntity);
                            } catch (FindException e) {
                                logger.log(Level.WARNING, "Unable to find XMPP Connection Entity");
                            }
                        }
                    }
                }  else if (event instanceof Deleted) {
                    if (((Deleted) event).getEntity() instanceof GenericEntity) {
                        GenericEntity entity = (GenericEntity) ((Deleted) event).getEntity();
                        if (entity.getEntityClassName().equals(XMPPConnectionEntity.class.getName())) {
                            logger.log(Level.INFO, "Deleted XMPP Connection " + entity.getId());
                            try {
                                XMPPConnectionEntity connectionEntity = XMPPConnectionUtils.asConcreteEntity(entity, XMPPConnectionEntity.class);
                                connectionManager.connectionRemoved(connectionEntity);
                            } catch (FindException e) {
                                logger.log(Level.WARNING, "Unable to find XMPP Connection Entity");
                            }
                        }
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
    }
}
