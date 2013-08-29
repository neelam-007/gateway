package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.server.util.ServerGoidUpgradeMapper;
import com.l7tech.util.Functions;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.l7tech.util.Functions.map;

/**
 * This is used to upgrade the entity oid references to goid's saved in a sink config
 */
public class Upgrade71To80SinkConfig implements UpgradeTask {

    protected ApplicationContext applicationContext;
    protected SessionFactory sessionFactory;
    protected Long emailPrefix = null;
    protected Long listenPortPrefix = null;
    protected Long jmsEndpointPrefix = null;
    protected Long publishedServicePrefix = null;
    protected Long policyPrefix = null;
    protected Long folderPrefix = null;
    protected Long fedUserPrefix;
    protected Long identityProviderPrefix;
    protected Long internalUserPrefix;
    protected List<String> federatedProviderGoids;

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        this.applicationContext = applicationContext;
        sessionFactory = getBean("sessionFactory", SessionFactory.class);
        initPrefixes();

        federatedProviderGoids = new ArrayList<String>();

        try {
            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(IdentityProviderConfig.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof IdentityProviderConfig) {
                            IdentityProviderConfig idProvider = (IdentityProviderConfig) schemaCriteriaObj;
                            if(IdentityProviderType.FEDERATED.equals(idProvider.type()))
                                federatedProviderGoids.add(Long.toString(idProvider.getGoid().getLow()));

                        }
                    }
                    return null;
                }
            });
        }catch (Exception e) {
            throw new FatalUpgradeException("Could not retrieve identity providers", e);
        }

        try {
            new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                    Criteria schemaCriteria = session.createCriteria(SinkConfiguration.class);
                    for (Object schemaCriteriaObj : schemaCriteria.list()) {
                        if (schemaCriteriaObj instanceof SinkConfiguration) {
                            SinkConfiguration config = (SinkConfiguration) schemaCriteriaObj;
                            Map<String, List<String>> filters = map(config.getFilters(), null, Functions.<String>identity(), new Functions.Unary<List<String>, List<String>>() {
                                @Override
                                public List<String> call(final List<String> strings) {
                                    return new ArrayList<String>(strings);
                                }
                            });

                            // update email listener oids to GOIDs
                            if (emailPrefix != null) {
                                updateLogSinkOids(GatewayDiagnosticContextKeys.EMAIL_LISTENER_ID, emailPrefix, filters);
                            }

                            // update email listener oids to GOIDs
                            if (listenPortPrefix != null) {
                                updateLogSinkOids(GatewayDiagnosticContextKeys.LISTEN_PORT_ID, listenPortPrefix, filters);
                            }

                            // update email listener oids to GOIDs
                            if (jmsEndpointPrefix != null) {
                                updateLogSinkOids(GatewayDiagnosticContextKeys.JMS_LISTENER_ID, jmsEndpointPrefix, filters);
                            }

                            // update published service oids to GOIDs
                            if (publishedServicePrefix != null) {
                                updateLogSinkOids(GatewayDiagnosticContextKeys.SERVICE_ID, publishedServicePrefix, filters);
                            }

                            // update policy oids to GOIDs
                            if (policyPrefix != null) {
                                updateLogSinkOids(GatewayDiagnosticContextKeys.POLICY_ID, policyPrefix, filters);
                            }

                            // update folder oids to GOIDs
                            if (folderPrefix != null) {
                                updateLogSinkOids(GatewayDiagnosticContextKeys.FOLDER_ID, folderPrefix, filters);
                            }

                            if(internalUserPrefix!=null || fedUserPrefix!=null){
                                updateLogSinkUserOids(filters);
                            }

                            config.setFilters(filters);
                        }
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            throw new FatalUpgradeException(e);
        }
    }

    private boolean updateLogSinkUserOids( Map<String, List<String>> filters) {
        String contextKey = GatewayDiagnosticContextKeys.USER_ID;
        boolean updated = false;
        if (filters.containsKey(contextKey)) {
            List<String> val = filters.get(contextKey);
            List<String> newGuids = new ArrayList<String>();
            for (String user : val) {
                String providerId = user.substring(0,user.indexOf(":"));
                String userId = user.substring(user.indexOf(":")+1);
                if(providerId.equals(Long.toString(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OLD_OID))){
                    updated = true;
                    Goid userGoid;
                    if(userId.equals("3")){
                        userGoid = new Goid(0,3);
                    }else{
                        userGoid = new Goid(internalUserPrefix, Long.parseLong(userId));
                    }
                    newGuids.add(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID+":"+ Goid.toString(userGoid));
                } else if(federatedProviderGoids.contains(providerId)){
                    updated = true;
                    newGuids.add(providerId+":"+ Goid.toString(new Goid(fedUserPrefix, Long.parseLong(userId))));
                }
            }
            if(updated){
                filters.put(contextKey, newGuids);
            }
        }
        return updated;
    }



    private boolean updateLogSinkOids(final String contextKey, final long prefix, Map<String, List<String>> filters) {
        boolean updated = false;
        if (filters.containsKey(contextKey)) {
            updated = true;
            List<String> val = filters.get(contextKey);
            List<String> newGuids = new ArrayList<String>();
            for (String oid : val) {
                if(GatewayDiagnosticContextKeys.FOLDER_ID.equals(contextKey) && oid.equals("-5002")) {
                    newGuids.add(new Goid(0, Long.parseLong(oid)).toString());
                } else {
                    newGuids.add(new Goid(prefix, Long.parseLong(oid)).toString());
                }
            }
            filters.put(contextKey, newGuids);
        }
        return updated;
    }


    protected void initPrefixes() throws FatalUpgradeException {

        emailPrefix = ServerGoidUpgradeMapper.getPrefix("email_listener");
        listenPortPrefix = ServerGoidUpgradeMapper.getPrefix("connector");
        jmsEndpointPrefix = ServerGoidUpgradeMapper.getPrefix("jms_endpoint");
        publishedServicePrefix = ServerGoidUpgradeMapper.getPrefix("published_service");
        policyPrefix = ServerGoidUpgradeMapper.getPrefix("policy");
        folderPrefix = ServerGoidUpgradeMapper.getPrefix("folder");

        identityProviderPrefix  = ServerGoidUpgradeMapper.getPrefix("identity_provider");
        fedUserPrefix = ServerGoidUpgradeMapper.getPrefix("fed_user");
        internalUserPrefix = ServerGoidUpgradeMapper.getPrefix("internal_user");

        // todo get prefixes for:
//                            GatewayDiagnosticContextKeys.LISTEN_PORT_ID
//                            GatewayDiagnosticContextKeys.USER_ID
    }

    /**
     * Get a bean safely.
     *
     * @param name      the bean to get.  Must not be null.
     * @param beanClass the class of the bean to get. Must not be null.
     * @return the requested bean.  Never null.
     * @throws FatalUpgradeException if there is no application context or the requested bean was not found
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
