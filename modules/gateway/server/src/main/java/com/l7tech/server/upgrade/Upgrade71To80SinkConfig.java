package com.l7tech.server.upgrade;

import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
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

    @Override
    public void upgrade(ApplicationContext applicationContext) throws NonfatalUpgradeException, FatalUpgradeException {
        this.applicationContext = applicationContext;
        sessionFactory = getBean("sessionFactory", SessionFactory.class);
        initPrefixes();

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

                            // todo update for
                            // todo handle hard coded values ( admin user...)
//                            GatewayDiagnosticContextKeys.USER_ID

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

        emailPrefix = new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(final Session session) throws HibernateException, SQLException {
                Criteria schemaCriteria = session.createCriteria(EmailListener.class);
                for (Object schemaCriteriaObj : schemaCriteria.list()) {
                    if (schemaCriteriaObj instanceof EmailListener) {
                        Long prefix = ((EmailListener) schemaCriteriaObj).getGoid().getHi();
                        if(prefix==0) {
                            continue;
                        }
                        return prefix;
                    }
                }
                return null;
            }
        });

        listenPortPrefix = new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(final Session session) throws HibernateException, SQLException {
                Criteria schemaCriteria = session.createCriteria(SsgConnector.class);
                for (Object schemaCriteriaObj : schemaCriteria.list()) {
                    if (schemaCriteriaObj instanceof SsgConnector) {
                        Long prefix = ((SsgConnector) schemaCriteriaObj).getGoid().getHi();
                        if(prefix==0) {
                            continue;
                        }
                        return prefix;
                    }
                }
                return null;
            }
        });

        jmsEndpointPrefix =  new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(final Session session) throws HibernateException, SQLException {
                Criteria schemaCriteria = session.createCriteria(JmsEndpoint.class);
                for (Object schemaCriteriaObj : schemaCriteria.list()) {
                    if (schemaCriteriaObj instanceof JmsEndpoint) {
                        Long prefix = ((JmsEndpoint) schemaCriteriaObj).getGoid().getHi();
                        if(prefix==0) {
                            continue;
                        }
                        return prefix;
                    }
                }
                return null;
            }
        });

        publishedServicePrefix =  new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(final Session session) throws HibernateException, SQLException {
                Criteria schemaCriteria = session.createCriteria(PublishedService.class);
                for (Object schemaCriteriaObj : schemaCriteria.list()) {
                    if (schemaCriteriaObj instanceof PublishedService) {
                        Long prefix = ((PublishedService) schemaCriteriaObj).getGoid().getHi();
                        if(prefix==0) {
                            continue;
                        }
                        return prefix;
                    }
                }
                return null;
            }
        });

        policyPrefix =  new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(final Session session) throws HibernateException, SQLException {
                Criteria schemaCriteria = session.createCriteria(Policy.class);
                for (Object schemaCriteriaObj : schemaCriteria.list()) {
                    if (schemaCriteriaObj instanceof Policy) {
                        Long prefix = ((Policy) schemaCriteriaObj).getGoid().getHi();
                        if(prefix==0) {
                            continue;
                        }
                        return prefix;
                    }
                }
                return null;
            }
        });

        folderPrefix =  new HibernateTemplate(sessionFactory).execute(new HibernateCallback<Long>() {
            @Override
            public Long doInHibernate(final Session session) throws HibernateException, SQLException {
                Criteria schemaCriteria = session.createCriteria(Folder.class);
                for (Object schemaCriteriaObj : schemaCriteria.list()) {
                    if (schemaCriteriaObj instanceof Folder) {
                        Long prefix = ((Folder) schemaCriteriaObj).getGoid().getHi();
                        if(prefix==0) {
                            continue;
                        }
                        return prefix;
                    }
                }
                return null;
            }
        });

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
