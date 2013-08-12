package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.identity.IdProviderConfigUpgradeHelper;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.transport.SsgConnectorManager;
import com.l7tech.server.transport.email.EmailListenerManager;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.GoidUpgradeMapper;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import java.util.Properties;

/**
 * This is used to upgrade the entity oid references to goid's saved in property maps
 */
public class Upgrade71To80IdReferences implements UpgradeTask {

    protected ApplicationContext applicationContext;
    protected SessionFactory sessionFactory;

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        this.applicationContext = applicationContext;
        SsgConnectorManager ssgConnectorManager = getBean("ssgConnectorManager", SsgConnectorManager.class);
        ServiceManager serviceManager = getBean("serviceManager", ServiceManager.class);
        JmsConnectionManager jmsConnectionManager = getBean("jmsConnectionManager", JmsConnectionManager.class);
        EmailListenerManager emailListenerManager = getBean("emailListenerManager", EmailListenerManager.class);
        IdentityProviderConfigManager identityProviderConfigManager = getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        TrustedCertManager trustedCertManager = getBean("trustedCertManager", TrustedCertManager.class);

        try {
            for(SsgConnector ssgConnector : ssgConnectorManager.findAll()){
                String serviceOid = ssgConnector.getProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID);
                if(serviceOid != null){
                    PublishedService service = serviceManager.findByPrimaryKey(GoidUpgradeMapper.mapOid(EntityType.SERVICE,Long.parseLong(serviceOid)));
                    if(service != null) {
                        ssgConnector.putProperty(SsgConnector.PROP_HARDWIRED_SERVICE_ID, service.getGoid().toHexString());
                    } else {
                        throw new FatalUpgradeException("Cannot find service with old oid: " + serviceOid);
                    }
                    try {
                        ssgConnectorManager.update(ssgConnector);
                    } catch (UpdateException e) {
                        throw new FatalUpgradeException("Error updating Ssg Connector", e);
                    }
                }
            }
        } catch (FindException e) {
            throw new FatalUpgradeException("Could not retrieve connectors", e);
        }

        try {
            for(JmsConnection jmsConnection : jmsConnectionManager.findAll()){
                String isHardwiredService = jmsConnection.properties().getProperty(JmsConnection.PROP_IS_HARDWIRED_SERVICE);
                if (isHardwiredService != null) {
                    if (Boolean.parseBoolean(isHardwiredService)) {
                        String serviceOid = jmsConnection.properties().getProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID);
                        PublishedService service = serviceManager.findByPrimaryKey(GoidUpgradeMapper.mapOid(EntityType.SERVICE,Long.parseLong(serviceOid)));
                        if(service != null) {
                            Properties properties = jmsConnection.properties();
                            properties.setProperty(JmsConnection.PROP_HARDWIRED_SERVICE_ID, service.getGoid().toHexString());
                            jmsConnection.properties(properties);
                        } else {
                            throw new FatalUpgradeException("Cannot find service with old oid: " + serviceOid);
                        }
                        try {
                            jmsConnectionManager.update(jmsConnection);
                        } catch (UpdateException e) {
                            throw new FatalUpgradeException("Error saving jmsConnection", e);
                        }
                    }
                }
            }
        } catch (FindException e) {
            throw new FatalUpgradeException("Could not retrieve connectors", e);
        }

        try {
            for(EmailListener emailListener : emailListenerManager.findAll()){
                String isHardwiredService = emailListener.properties().getProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE);
                if (isHardwiredService != null) {
                    if (Boolean.parseBoolean(isHardwiredService)) {
                        String serviceOid = emailListener.properties().getProperty(EmailListener.PROP_HARDWIRED_SERVICE_ID);
                        PublishedService service = serviceManager.findByPrimaryKey(GoidUpgradeMapper.mapOid(EntityType.SERVICE,Long.parseLong(serviceOid)));
                        if(service != null) {
                            Properties properties = emailListener.properties();
                            properties.setProperty(EmailListener.PROP_HARDWIRED_SERVICE_ID, service.getGoid().toHexString());
                            emailListener.properties(properties);
                        } else {
                            throw new FatalUpgradeException("Cannot find service with old oid: " + serviceOid);
                        }
                        try {
                            emailListenerManager.update(emailListener);
                        } catch (UpdateException e) {
                            throw new FatalUpgradeException("Error saving emailListener", e);
                        }
                    }
                }
            }
        } catch (FindException e) {
            throw new FatalUpgradeException("Could not retrieve connectors", e);
        }

        try {
            for (IdentityProviderConfig idProviderConfig : identityProviderConfigManager.findAll()) {
                long[] oids = IdProviderConfigUpgradeHelper.getProperty(idProviderConfig, "trustedCertOids");
                if (oids != null) {
                    final Goid[] goids = GoidUpgradeMapper.mapOids(EntityType.TRUSTED_CERT, ArrayUtils.box(oids));
                    IdProviderConfigUpgradeHelper.setProperty(idProviderConfig, FederatedIdentityProviderConfig.PROP_CERT_GOIDS, goids);
                    try {
                        identityProviderConfigManager.update(idProviderConfig);
                    } catch (UpdateException e) {
                        throw new FatalUpgradeException("Error saving ID provider configuration: " + ExceptionUtils.getMessage(e), e);
                    }
                }
            }
        } catch (FindException e) {
            throw new FatalUpgradeException("Could not retrieve identity provider configs", e);
        }
    }

    /**
     * Get a bean safely.
     *
     * @param name      the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws com.l7tech.server.upgrade.FatalUpgradeException if there is no application context or the requested bean was not found
     */
    @SuppressWarnings({"unchecked"})
    private <T> T getBean(final String name,
                          final Class<T> beanClass) throws FatalUpgradeException {
        if (applicationContext == null) throw new FatalUpgradeException("ApplicationContext is required");
        try {
            return applicationContext.getBean(name, beanClass);
        } catch (BeansException be) {
            throw new FatalUpgradeException("Error accessing  bean '" + name + "' from ApplicationContext.");
        }
    }
}
