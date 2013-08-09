package com.l7tech.server.identity.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.Component;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.SystemAuditRecord;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.cluster.ClusterNodeInfo;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.HibernateGoidEntityManager;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
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
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedCertManagerImp
        extends HibernateGoidEntityManager<TrustedCert, EntityHeader>
        implements TrustedCertManager, ApplicationContextAware, InitializingBean, PropertyChangeListener
{
    private static final Logger logger = Logger.getLogger(TrustedCertManagerImp.class.getName());

    private Auditor auditor;

    private static final String DEFAULT_CHECK_PERIOD = "12h";
    private static final long HOURS_THRESHOLD = TimeUnit.parse("3d");
    private static final long MINUTES_THRESHOLD = TimeUnit.parse("3h");

    private final ManagedTimer timer;
    private final Config config;
    private final ClusterInfoManager clusterInfoManager;

    private ExpiryCheckerTask expiryCheckerTask;
    private ApplicationContext spring;

    public TrustedCertManagerImp(Config config, ManagedTimer timer, ClusterInfoManager clusterInfoManager) {
        this.config = config;
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

    @Override
    @Transactional(readOnly=true)
    public Collection<TrustedCert> findByTrustFlag(final TrustedCert.TrustedFor trustFlag) throws FindException {
        // TODO optimize this query so it does the filtering inside the database rather than here, then update the javadoc
        return Functions.grep(findAll(), TrustedCert.TrustedFor.predicate(trustFlag));
    }

    @Override
    public TrustedCert findByOldOid(long oid) throws FindException {
        return findUnique( Collections.<String,Object>singletonMap("oldOid", oid) );
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
                crit.add(Restrictions.eq("issuerDn", CertUtils.getDN( issuer )));
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
    protected void initDao() throws Exception {
        long period;
        final String value = config.getProperty( ServerConfigParams.PARAM_CERT_EXPIRY_CHECK_PERIOD );
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
        if ( ServerConfigParams.PARAM_CERT_EXPIRY_CHECK_PERIOD.equals(evt.getPropertyName())) {
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
            final AuditContextFactory auditContextFactory = spring.getBean("auditContextFactory", AuditContextFactory.class);
            final ClusterNodeInfo nodeInfo = clusterInfoManager.getSelfNodeInf();

            // These are retrieved here on every (infrequent) run so that the frequencies can change at runtime
            final long fineExpiryPeriod = TimeUnit.parse( config.getProperty( ServerConfigParams.PARAM_CERT_EXPIRY_FINE_AGE ) );
            final long infoExpiryPeriod = TimeUnit.parse( config.getProperty( ServerConfigParams.PARAM_CERT_EXPIRY_INFO_AGE ) );
            final long warningExpiryPeriod = TimeUnit.parse( config.getProperty( ServerConfigParams.PARAM_CERT_EXPIRY_WARNING_AGE ) );

            final Collection<TrustedCert> trustedCerts;
            try {
                trustedCerts = findAll();
            } catch (FindException e) {
                auditor.logAndAudit(SystemMessages.CERT_EXPIRY_CANT_FIND, null, e);
                return;
            }

            final List<CertExpiryWarningDetail> warnings = new ArrayList<CertExpiryWarningDetail>();
            Level level = Level.FINEST;

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

                CertExpiryWarningDetail detail = null;
                if (millisUntilExpiry <= 0) {
                    detail = new CertExpiryWarningDetail(SystemMessages.CERT_EXPIRED, trustedCert, howLong);
                } else if (millisUntilExpiry <= warningExpiryPeriod) {
                    detail = new CertExpiryWarningDetail(SystemMessages.CERT_EXPIRING_WARNING, trustedCert, howLong);
                } else if (millisUntilExpiry <= infoExpiryPeriod) {
                    detail = new CertExpiryWarningDetail(SystemMessages.CERT_EXPIRING_INFO, trustedCert, howLong);
                } else if (millisUntilExpiry <= fineExpiryPeriod) {
                    detail = new CertExpiryWarningDetail(SystemMessages.CERT_EXPIRING_FINE, trustedCert, howLong);
                }
                if (detail != null) {
                    warnings.add(detail);
                    if (detail.level.intValue() > level.intValue())
                        level = detail.level;
                }
            }

            final boolean alwaysAudit = !warnings.isEmpty();
            final Level finalLevel = level;

            AuditRecord record = new SystemAuditRecord(finalLevel,
                                                      nodeInfo.getNodeIdentifier(),
                                                      Component.GW_TRUST_STORE,
                                                      "One or more trusted certificates has expired or is expiring soon",
                                                      alwaysAudit, -1, null, null, "Checking", nodeInfo.getAddress());
            auditContextFactory.doWithNewAuditContext(record, new Runnable() {
                @Override
                public void run() {
                    for (CertExpiryWarningDetail warning : warnings) {
                        auditor.logAndAudit(warning.detail, warning.oidStr, warning.subjectDnStr, warning.howLongStr);
                    }
                }
            });
        }
    }

    private static final class CertExpiryWarningDetail {
        private final AuditDetailMessage detail;
        private final Level level;
        private final String oidStr;
        private final String subjectDnStr;
        private final String howLongStr;

        private CertExpiryWarningDetail(AuditDetailMessage detail, TrustedCert trustedCert, String howLongStr) {
            this.detail = detail;
            this.level = detail.getLevel();
            this.oidStr = trustedCert.getGoid().toString();
            this.subjectDnStr = trustedCert.getSubjectDn();
            this.howLongStr = howLongStr;
        }
    }
}
