/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.cert;

import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.*;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedCertManagerImp
        extends HibernateEntityManager<TrustedCert, EntityHeader>
        implements TrustedCertManager
{
    private static final Logger logger = Logger.getLogger(TrustedCertManagerImp.class.getName());

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
     * This will be true if either the specific certificate has the {@link com.l7tech.common.security.TrustedCert#isTrustedForSsl()}
     * option set, or the signing cert that comes next in the chain has the {@link com.l7tech.common.security.TrustedCert#isTrustedForSigningServerCerts()}
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
            } catch (IOException e) {
                final String msg = "Couldn't decode stored certificate";
                logger.log(Level.SEVERE, msg, e);
                throw new CertificateException(msg);
            }

            // Check that signer is trusted
            TrustedCert caTrust = getCachedCertBySubjectDn(issuerDn, 30000);

            if (caTrust == null)
                throw new CertificateException("Couldn't find CA cert with DN '" + issuerDn + "'");

            if (!caTrust.isTrustedForSigningServerCerts())
                throw new CertificateException("CA Cert with DN '" + issuerDn + "' found but not trusted for signing SSL Server Certs");

            X509Certificate caTrustCert = caTrust.getCertificate();

            CertUtils.cachedVerify(serverCertChain[0], caTrustCert.getPublicKey());
        } catch (IOException e) {
            final String msg = "Couldn't decode stored CA certificate with DN '" + issuerDn + "'";
            logger.log(Level.SEVERE, msg, e);
            throw new CertificateException(msg);
        } catch (Exception e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new CertificateException(e.getMessage());
        }
    }


    public Class getImpClass() {
        return TrustedCert.class;
    }

    public Class getInterfaceClass() {
        return TrustedCert.class;
    }

    public String getTableName() {
        return "trusted_cert";
    }

    public EntityType getEntityType() {
        return EntityType.TRUSTED_CERT;
    }

    @Transactional(readOnly=true)
    public TrustedCert getCachedCertBySubjectDn(String dn, int maxAge) throws FindException, IOException, CertificateException {
        Lock read = cacheLock.readLock();
        Lock write = cacheLock.writeLock();
        try {
            read.lock();
            final Long oid = dnToOid.get(dn);
            read.unlock();
            read = null;
            if (oid == null) {
                TrustedCert cert = findBySubjectDn(dn);
                if (cert == null) return null;
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
    public TrustedCert getCachedCertByOid(long o, int maxAge) throws FindException, IOException, CertificateException {
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
            throw new CacheVeto("Certificate not valid", e);
        } catch (IOException e) {
            throw new CacheVeto("Certificate could not be decoded", e);
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
            logger.log(Level.SEVERE, "Couldn't cache cert: " + e.getMessage(), e.getCause());
        }
    }

    private Map<String, Long> dnToOid = new HashMap<String, Long>();
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock(false);
}
