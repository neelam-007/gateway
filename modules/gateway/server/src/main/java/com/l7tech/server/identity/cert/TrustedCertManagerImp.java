/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.cert;

import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.gateway.common.Component;
import com.l7tech.common.io.CertUtils;
import com.l7tech.common.io.CertificateExpiry;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.audit.AuditContext;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    @Transactional(readOnly=true)
    public TrustedCert findBySubjectDn(final String dn) throws FindException {
        final StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass().getName());
        hql.append(" WHERE ").append(getTableName()).append(".subjectDn = ?");
        try {
            List found = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
                public Object doInHibernateReadOnly(Session session) throws HibernateException {
                    return session.createQuery(hql.toString()).setString(0, dn).list();
                }
            });

            switch (found.size()) {
                case 0:
                    return null;
                case 1:
                    return (TrustedCert)found.get(0);
                default:
                    throw new FindException("Found multiple TrustedCerts with the same DN");
            }
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

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

    public long save(TrustedCert cert) throws SaveException {
        try {
            checkCachable(cert);
            return super.save(cert);
        } catch (CacheVeto e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            throw new SaveException(e.getMessage(), e.getCause());
        }
    }

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


    /**
     * Checks whether the certificate at the top of the specified chain is trusted for outbound SSL connections.
     * <p/>
     * This will be true if either the specific certificate has the {@link com.l7tech.security.cert.TrustedCert#isTrustedForSsl()}
     * option set, or the signing cert that comes next in the chain has the {@link com.l7tech.security.cert.TrustedCert#isTrustedForSigningServerCerts()}
     * option set.
     * <p/>
     *
     * @param serverCertChain the certificate chain
     * @throws CertificateException
     */
    @Transactional(readOnly=true)
    public void checkSslTrust(X509Certificate[] serverCertChain) throws CertificateException {
        String subjectDn = serverCertChain[0].getSubjectDN().getName();
        String issuerDn = serverCertChain[0].getIssuerDN().getName();
        try {
            // Check if this cert is trusted as-is
            try {
                TrustedCert selfTrust = getCachedCertBySubjectDn(subjectDn, 30000);
                if (selfTrust != null) {
                    if (!CertUtils.certsAreEqual(selfTrust.getCertificate(), serverCertChain[0]))
                        throw new CertificateException("Server cert '" + subjectDn +
                          "' found but doesn't match previously stored version");
                    if (selfTrust.isTrustedForSsl()) {
                        // Good enough
                        return;
                    } else if (!selfTrust.isTrustedForSsl())
                        logger.fine("Server cert '" + subjectDn + "' found but not trusted for SSL. Will check issuer cert, if any");

                    // FALLTHROUGH - Check if its signer is trusted
                }
            } catch (FindException e) {
                logger.log(Level.WARNING, e.getMessage(), e);
                throw new CertificateException(e.getMessage());
            }

            // Check that signer is trusted
            TrustedCert caTrust = getCachedCertBySubjectDn(issuerDn, 30000);

            if (caTrust == null)
                throw new UnknownCertificateException("Couldn't find CA cert with DN '" + issuerDn + "'");

            if (!caTrust.isTrustedForSigningServerCerts())
                throw new CertificateException("CA Cert with DN '" + issuerDn + "' found but not trusted for signing SSL Server Certs");

            X509Certificate caTrustCert = caTrust.getCertificate();

            CertUtils.cachedVerify(serverCertChain[0], caTrustCert.getPublicKey());
        } catch (Exception e) {
            if (e instanceof UnknownCertificateException)
                throw (CertificateException) e;

            logger.log(Level.WARNING, e.getMessage(), e);

            throw new CertificateException(e.getMessage(), e);
        }
    }


    public Class<TrustedCert> getImpClass() {
        return TrustedCert.class;
    }

    public Class<TrustedCert> getInterfaceClass() {
        return TrustedCert.class;
    }

    public String getTableName() {
        return "trusted_cert";
    }

    public EntityType getEntityType() {
        return EntityType.TRUSTED_CERT;
    }

    @Transactional(readOnly=true)
    public TrustedCert getCachedCertBySubjectDn(String dn, int maxAge) throws FindException, CertificateException {
        Lock read = cacheLock.readLock();
        Lock write = null;
        try {
            read.lock();
            final Long oid = dnToOid.get(dn);
            read.unlock();
            read = null;
            if (oid == null) {
                TrustedCert cert = findBySubjectDn(dn);
                if (cert == null) return null;
                write = cacheLock.writeLock(); 
                write.lock();
                checkAndCache(cert);
                write.unlock();
                write = null;
                return cert;
            } else {
                return getCachedCertByOid(oid.longValue(), maxAge);
            }
        } catch (CacheVeto e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            throw new CertificateException(e.getMessage());
        } finally {
            if (write != null) write.unlock();
            if (read != null) read.unlock();
        }
    }

    @Transactional(readOnly=true)
    public TrustedCert getCachedCertByOid(long o, int maxAge) throws FindException, CertificateException {
        try {
            return getCachedEntity(o, maxAge);
        } catch (CacheVeto e) {
            logger.log(Level.SEVERE, e.getMessage(), e.getCause());
            return null;
        }
    }

    protected void addedToCache(PersistentEntity ent) {
        TrustedCert cert = (TrustedCert)ent;
        dnToOid.put(cert.getSubjectDn(), ent.getOid());
    }

    protected void removedFromCache(Entity ent) {
        TrustedCert cert = (TrustedCert)ent;
        dnToOid.remove(cert.getSubjectDn());
    }

    public void checkCachable(Entity ent) throws CacheVeto {
        TrustedCert cert = (TrustedCert)ent;
        CertificateExpiry exp;
        try {
            exp = CertUtils.checkValidity(cert.getCertificate());
        } catch (CertificateException e) {
            throw new CacheVeto("Certificate not valid or could not be decoded", e);
        }
        if (exp.getDays() <= CertificateExpiry.FINE_DAYS) logWillExpire(cert, exp);
    }

    public void logWillExpire(TrustedCert cert, CertificateExpiry e) {
        final String msg = "Trusted cert for " + cert.getSubjectDn() +
          " will expire in approximately " + e.getDays() + " days.";
        logger.log(e.getSeverity(), msg);
    }

    protected void initDao() throws Exception {
        if (transactionManager == null) {
            throw new IllegalArgumentException("Transaction Manager is required");
        }

        new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
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

    protected Map<String,Object> getUniqueAttributeMap(TrustedCert cert) {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("name", cert.getName());
        map.put("subjectDn", cert.getSubjectDn());
        return map;
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

    private Map<String, Long> dnToOid = new HashMap<String, Long>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock(false);

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
        this.spring = applicationContext;
    }

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
                    try {
                        cert = trustedCert.getCertificate();
                    } catch (CertificateException e) {
                        auditor.logAndAudit(SystemMessages.CERT_EXPIRY_BAD_CERT, Long.toString(trustedCert.getOid()), trustedCert.getSubjectDn());
                        continue;
                    }

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
