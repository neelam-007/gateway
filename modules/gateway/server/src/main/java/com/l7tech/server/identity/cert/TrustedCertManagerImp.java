/*
 * Copyright (C) 2004-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.cert;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.TimeUnit;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.security.auth.x500.X500Principal;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedCertManagerImp
        extends HibernateEntityManager<TrustedCert, EntityHeader>
        implements TrustedCertManager, ApplicationContextAware, InitializingBean, PropertyChangeListener
{
    private static final Logger logger = Logger.getLogger(TrustedCertManagerImp.class.getName());

    private Auditor auditor;

    private static final String DEFAULT_CHECK_PERIOD = "12h";
    private static final long HOURS_THRESHOLD = TimeUnit.parse("3d");
    private static final long MINUTES_THRESHOLD = TimeUnit.parse("3h");

    private final ManagedTimer timer;
    private final ServerConfig serverConfig;
    private final ClusterInfoManager clusterInfoManager;

    private ExpiryCheckerTask expiryCheckerTask;
    private ApplicationContext spring;

    public TrustedCertManagerImp(ServerConfig serverConfig, ManagedTimer timer, ClusterInfoManager clusterInfoManager) {
        this.serverConfig = serverConfig;
        this.timer = timer;
        this.clusterInfoManager = clusterInfoManager;
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<TrustedCert> findBySubjectDn(final String dn) throws FindException {
        final StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass().getName());
        hql.append(" WHERE ").append(getTableName()).append(".subjectDn = ?");
        try {
            //noinspection unchecked
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    return session.createQuery(hql.toString()).setString(0, dn).list();
                }
            });
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<TrustedCert> findByName(final String name) throws FindException {
        final StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass().getName());
        hql.append(" WHERE ").append(getTableName()).append(".name = ?");
        try {
            //noinspection unchecked
            return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    return session.createQuery(hql.toString()).setString(0, name).list();
                }
            });
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    @Transactional(readOnly=true)
    public List<TrustedCert> findByThumbprint(String thumbprint) throws FindException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass().getName());
        hql.append(" WHERE ").append(getTableName()).append(".thumbprintSha1 ");
        try {
            if (thumbprint == null) {
                hql.append("is null");
                return getHibernateTemplate().find(hql.toString());
            }

            hql.append(" = ?");
            return getHibernateTemplate().find(hql.toString(), thumbprint.trim());
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException("Couldn't find cert(s)", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    @Override
    @Transactional(readOnly=true)
    public List<TrustedCert> findBySki(String ski) throws FindException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass().getName());
        hql.append(" WHERE ").append(getTableName()).append(".ski ");
        try {
            if (ski == null) {
                hql.append("is null");
                return getHibernateTemplate().find(hql.toString());
            }

            hql.append(" = ?");
            return getHibernateTemplate().find(hql.toString(), ski.trim());
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException("Couldn't find cert(s)", e);
        }
    }

    @Override
    public List<TrustedCert> findByIssuerAndSerial(final X500Principal issuer, final BigInteger serial) throws FindException {
        //noinspection unchecked
        return getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            @Override
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                final Criteria crit = session.createCriteria(getImpClass());
                crit.add(Restrictions.eq("issuerDn", issuer.getName()));
                crit.add(Restrictions.eq("serial", serial));
                return crit.list();
            }
        });
    }

    @Override
    public Class<TrustedCert> getImpClass() {
        return TrustedCert.class;
    }

    @Override
    public Class<TrustedCert> getInterfaceClass() {
        return TrustedCert.class;
    }

    @Override
    public String getTableName() {
        return "trusted_cert";
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TRUSTED_CERT;
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<TrustedCert> getCachedCertsBySubjectDn(final String dn) throws FindException {
        Collection<TrustedCert> certs = findBySubjectDn(dn);
        for (TrustedCert cert : certs)
            cert.setReadOnly();
        return certs;
    }

    @Override
    protected void initDao() throws Exception {
        long period;
        final String value = serverConfig.getPropertyCached(ServerConfig.PARAM_CERT_EXPIRY_CHECK_PERIOD);
        try {
            period = TimeUnit.parse(value);
        } catch (Exception e) {
            auditor.logAndAudit(SystemMessages.CERT_EXPIRY_BAD_PERIOD, value, DEFAULT_CHECK_PERIOD);
            period = TimeUnit.parse(DEFAULT_CHECK_PERIOD);
        }
        reschedule(period, false);
    }

    private void reschedule(long period, boolean immediately) {
        synchronized(timer) {
            if (expiryCheckerTask != null) expiryCheckerTask.cancel();
            expiryCheckerTask = new ExpiryCheckerTask();
        }
        long delay = immediately?0: TimeUnit.MINUTES.toMillis(1);

        timer.schedule(expiryCheckerTask, delay, period);
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(final TrustedCert cert) {
        Map<String,Object> map1 = new HashMap<String, Object>() {{ put("thumbprintSha1", cert.getThumbprintSha1()); }};
        return Collections.singletonList(map1);
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
        this.spring = applicationContext;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (ServerConfig.PARAM_CERT_EXPIRY_CHECK_PERIOD.equals(evt.getPropertyName())) {
            final String ov = evt.getOldValue() == null ? null : evt.getOldValue().toString();
            final String nv = evt.getNewValue().toString();
            try {
                reschedule(TimeUnit.parse(nv), true);
            } catch (Exception e) {
                auditor.logAndAudit(SystemMessages.CERT_EXPIRY_BAD_PERIOD, nv, ov);
            }
        }
    }

    /**
     * Runs periodically (by default once per hour) to check whether any trusted certs are expiring soon, and log and
     * audit messages accordingly.
     */
    private class ExpiryCheckerTask extends ManagedTimerTask {
        @Override
        protected void doRun() {
            final long nowUTC = System.currentTimeMillis();
            final AuditContext auditContext = (AuditContext)spring.getBean("auditContext");
            final ClusterNodeInfo nodeInfo = clusterInfoManager.getSelfNodeInf();

            // These are retrieved here on every (infrequent) run so that the frequencies can change at runtime
            final long fineExpiryPeriod = TimeUnit.parse(serverConfig.getPropertyCached(ServerConfig.PARAM_CERT_EXPIRY_FINE_AGE));
            final long infoExpiryPeriod = TimeUnit.parse(serverConfig.getPropertyCached(ServerConfig.PARAM_CERT_EXPIRY_INFO_AGE));
            final long warningExpiryPeriod = TimeUnit.parse(serverConfig.getPropertyCached(ServerConfig.PARAM_CERT_EXPIRY_WARNING_AGE));

            final Collection<TrustedCert> trustedCerts;
            try {
                trustedCerts = findAll();
            } catch (FindException e) {
                auditor.logAndAudit(SystemMessages.CERT_EXPIRY_CANT_FIND, null, e);
                return;
            }

            auditContext.setCurrentRecord(new SystemAuditRecord(Level.INFO, nodeInfo.getNodeIdentifier(), Component.GW_TRUST_STORE, "One or more trusted certificates has expired or is expiring soon", false, -1, null, null, "Checking", nodeInfo.getAddress()));

            try {
                for (TrustedCert trustedCert : trustedCerts) {
                    final X509Certificate cert;
                    cert = trustedCert.getCertificate();

                    final long expiresUTC = cert.getNotAfter().getTime();
                    final long millisUntilExpiry = expiresUTC - nowUTC;

                    if (millisUntilExpiry > fineExpiryPeriod) continue;

                    final TimeUnit displayUnit;
                    final long abs = Math.abs(millisUntilExpiry);
                    if (abs <= MINUTES_THRESHOLD) {
                        displayUnit = TimeUnit.MINUTES;
                    } else if (abs <= HOURS_THRESHOLD) {
                        displayUnit = TimeUnit.HOURS;
                    } else {
                        displayUnit = TimeUnit.DAYS;
                    }

                    String howLong = new Formatter().format("%.1f %s", (double)abs / displayUnit.getMultiplier(), displayUnit.getName()).toString();

                    if (millisUntilExpiry <= 0) {
                        auditor.logAndAudit(SystemMessages.CERT_EXPIRED, Long.toString(trustedCert.getOid()), trustedCert.getSubjectDn(), howLong);
                    } else if (millisUntilExpiry <= warningExpiryPeriod) {
                        auditor.logAndAudit(SystemMessages.CERT_EXPIRING_WARNING, Long.toString(trustedCert.getOid()), trustedCert.getSubjectDn(), howLong);
                    } else if (millisUntilExpiry <= infoExpiryPeriod) {
                        auditor.logAndAudit(SystemMessages.CERT_EXPIRING_INFO, Long.toString(trustedCert.getOid()), trustedCert.getSubjectDn(), howLong);
                    } else if (millisUntilExpiry <= fineExpiryPeriod) {
                        auditor.logAndAudit(SystemMessages.CERT_EXPIRING_FINE, Long.toString(trustedCert.getOid()), trustedCert.getSubjectDn(), howLong);
                    }
                }
            } finally {
                auditContext.flush();
            }
        }
    }
}
