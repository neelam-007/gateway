/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.cert;

import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.*;
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
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
    public List findByThumbprint(String thumbprint) throws FindException {
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

    @Override
    @Transactional(readOnly=true)
    public List findBySki(String ski) throws FindException {
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
    public long save(TrustedCert cert) throws SaveException {
        try {
            checkCachable(cert);
            return super.save(cert);
        } catch (CacheVeto e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            throw new SaveException(e.getMessage(), e.getCause());
        }
    }

    @Override
    public void update(TrustedCert cert) throws UpdateException {
        try {
            checkCachable(cert);
            super.update(cert);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException("Couldn't update cert", e);
        } catch (CacheVeto e) {
            final String msg = e.getMessage();
            logger.log(Level.WARNING, msg, e.getCause());
            throw new UpdateException(msg, e.getCause());
        }
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
    @Transactional(readOnly=true)
    public TrustedCert getCachedCertByOid(long o, int maxAge) throws FindException, CertificateException {
        try {
            return getCachedEntity(o, maxAge);
        } catch (CacheVeto e) {
            logger.log(Level.SEVERE, e.getMessage(), e.getCause());
            return null;
        }
    }

    @Override
    protected void initDao() throws Exception {
        if (transactionManager == null) {
            throw new IllegalArgumentException("Transaction Manager is required");
        }

        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                peruseTrustedCertificates();
            }
        });

        long period;
        final String value = serverConfig.getPropertyCached(ServerConfig.PARAM_CERT_EXPIRY_CHECK_PERIOD);
        try {
            period = TimeUnit.parse(value);
        } catch (Exception e) {
            auditor.logAndAudit(SystemMessages.CERT_EXPIRY_BAD_PERIOD, value, DEFAULT_CHECK_PERIOD);
            period = TimeUnit.parse(DEFAULT_CHECK_PERIOD);
        }
        reschedule(period);
    }

    private void reschedule(long period) {
        synchronized(timer) {
            if (expiryCheckerTask != null) expiryCheckerTask.cancel();
            expiryCheckerTask = new ExpiryCheckerTask();
        }
        timer.schedule(expiryCheckerTask, 0, period);
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

    private void peruseTrustedCertificates() {
        try {
            for (TrustedCert cert : findAll()) {
                checkCachable(cert);
                logger.info("Caching cert #" + cert.getOid() + " (" + cert.getSubjectDn() + ")");
            }
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Couldn't find cert", e);
        } catch (CacheVeto e) {
            logger.log(Level.WARNING, "Couldn't cache cert: " + ExceptionUtils.getMessage(e.getCause()));
        }
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
                reschedule(TimeUnit.parse(nv));
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
