/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.identity.cert;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import com.l7tech.common.security.CertificateExpiry;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.identity.cert.TrustedCertManager;
import com.l7tech.objectmodel.*;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class TrustedCertManagerImp extends HibernateEntityManager implements TrustedCertManager {

    public TrustedCert findByPrimaryKey(long oid) throws FindException {
        TrustedCert cert = (TrustedCert)findByPrimaryKey(getImpClass(), oid);
        return cert;
    }

    public TrustedCert findBySubjectDn(String dn) throws FindException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS ").append(getImpClass().getName());
        hql.append(" WHERE ").append(getTableName()).append(".subjectDn = ?");
        try {
            List found = getHibernateTemplate().find(hql.toString(), dn);
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

    public long save(TrustedCert cert) throws SaveException {
        try {
            checkCachable(cert);
            return ((Long)getHibernateTemplate().save(cert)).longValue();
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new SaveException("Couldn't save cert", e);
        } catch (CacheVeto e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            throw new SaveException(e.getMessage(), e.getCause());
        }
    }

    public void update(TrustedCert cert) throws UpdateException {
        try {
            checkCachable(cert);
            TrustedCert original = findByPrimaryKey(cert.getOid());
            if (original == null) throw new UpdateException("Can't find TrustedCert #" + cert.getOid() + ": it was probably deleted by another transaction");

            if (original.getVersion() != cert.getVersion())
                throw new StaleUpdateException("TrustedCert #" + cert.getOid() + " was modified by another transaction");

            original.copyFrom(cert);
            getHibernateTemplate().update(original);
        } catch (DataAccessException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw new UpdateException("Couldn't update cert", e);
        } catch (FindException e) {
            logger.log(Level.WARNING, e.toString(), e);
            throw new UpdateException("Couldn't find cert to be udpated");
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

            serverCertChain[0].verify(caTrustCert.getPublicKey());
            return;
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

    public TrustedCert getCachedCertBySubjectDn(String dn, int maxAge) throws FindException, IOException, CertificateException {
        Sync read = cacheLock.readLock();
        Sync write = cacheLock.writeLock();
        try {
            read.acquire();
            final Long oid = (Long)dnToOid.get(dn);
            read.release();
            read = null;
            if (oid == null) {
                TrustedCert cert = findBySubjectDn(dn);
                if (cert == null) return null;
                write.acquire();
                checkAndCache(cert);
                write.release();
                write = null;
                return cert;
            } else {
                return getCachedCertByOid(oid.longValue(), maxAge);
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted while acquiring cache lock", e);
            Thread.currentThread().interrupt();
            return null;
        } catch (CacheVeto e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            throw new CertificateException(e.getMessage());
        } finally {
            if (write != null) write.release();
            if (read != null) read.release();
        }
    }

    public TrustedCert getCachedCertByOid(long o, int maxAge) throws FindException, IOException, CertificateException {
        try {
            return (TrustedCert)getCachedEntity(o, maxAge);
        } catch (CacheVeto e) {
            logger.log(Level.SEVERE, e.getMessage(), e.getCause());
            return null;
        }
    }

    protected void addedToCache(Entity ent) {
        TrustedCert cert = (TrustedCert)ent;
        dnToOid.put(cert.getSubjectDn(), new Long(ent.getOid()));
    }

    protected void removedFromCache(Entity ent) {
        TrustedCert cert = (TrustedCert)ent;
        dnToOid.remove(cert.getSubjectDn());
    }

    public void checkCachable(Entity ent) throws CacheVeto {
        TrustedCert cert = (TrustedCert)ent;
        CertificateExpiry exp = null;
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

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
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

    private void peruseTrustedCertificates() {
        try {
            for (Iterator i = findAll().iterator(); i.hasNext();) {
                TrustedCert cert = (TrustedCert)i.next();
                checkCachable(cert);
                logger.info("Caching cert #" + cert.getOid() + " (" + cert.getSubjectDn() + ")");
            }
        } catch (FindException e) {
            logger.log(Level.SEVERE, "Couldn't find cert", e);
        } catch (CacheVeto e) {
            logger.log(Level.SEVERE, "Couldn't cache cert: " + e.getMessage(), e.getCause());
        }
    }

    private Map dnToOid = new HashMap();
    private ReadWriteLock cacheLock = new ReaderPreferenceReadWriteLock();
    private PlatformTransactionManager transactionManager; // required for TransactionTemplate
}
