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

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.ReaderPreferenceReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;

/**
 * @author alex
 * @version $Revision$
 */
public class TrustedCertManagerImp extends HibernateEntityManager implements TrustedCertManager {
    public TrustedCertManagerImp() {
        try {
            for ( Iterator i = findAll().iterator(); i.hasNext(); ) {
                TrustedCert cert = (TrustedCert)i.next();
                checkAndCache(cert);
                logger.info("Caching cert #" + cert.getOid() + " (" + cert.getSubjectDn() + ")");
            }
        } catch ( FindException e ) {
            logger.log( Level.SEVERE, "Couldn't cache cert", e );
        } catch ( IOException e ) {
            logger.log( Level.SEVERE, "Couldn't cache cert", e );
        } catch ( CertificateException e ) {
            logger.log( Level.SEVERE, "Couldn't cache cert", e );
        }
    }

    public TrustedCert findByPrimaryKey(long oid) throws FindException {
        try {
            TrustedCert cert = (TrustedCert)PersistenceManager.findByPrimaryKey( getContext(), getImpClass(), oid );
            return cert;
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

    public TrustedCert findBySubjectDn(String dn) throws FindException {
        StringBuffer hql = new StringBuffer("FROM ");
        hql.append(getTableName()).append(" IN CLASS " ).append(getImpClass().getName());
        hql.append(" WHERE " ).append(getTableName()).append(".subjectDn = ?");
        try {
            List found = PersistenceManager.find( getContext(), hql.toString(), dn, String.class );
            switch ( found.size() ) {
                case 0:
                    return null;
                case 1:
                    return (TrustedCert)found.get(0);
                default:
                    throw new FindException("Found multiple TrustedCerts with the same DN");
            }
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new FindException("Couldn't retrieve cert", e);
        }
    }

    public long save(TrustedCert cert) throws SaveException {
        try {
            return PersistenceManager.save( getContext(), cert );
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new SaveException("Couldn't save cert", e );
        }
    }

    public void update(TrustedCert cert) throws UpdateException {
        try {
            TrustedCert original = findByPrimaryKey(cert.getOid());
            if ( original == null ) throw new UpdateException("Can't find TrustedCert #" + cert.getOid() + ": it was probably deleted by another transaction");

            if ( original.getVersion() != cert.getVersion() )
                throw new StaleUpdateException("TrustedCert #" + cert.getOid() + " was modified by another transaction");

            original.copyFrom(cert);
            PersistenceManager.update( getContext(), original );
        } catch ( SQLException e ) {
            logger.log( Level.SEVERE, e.getMessage(), e );
            throw new UpdateException("Couldn't update cert", e );
        } catch (FindException e) {
            logger.log(Level.WARNING, e.toString(), e);
            throw new UpdateException("Couldn't find cert to be udpated");
        }
    }


    /**
     * Checks whether the certificate at the top of the specified chain is trusted for outbound SSL connections.
     * <p>
     * This will be true if either the specific certificate has the {@link com.l7tech.common.security.TrustedCert#isTrustedForSsl()}
     * option set, or the signing cert that comes next in the chain has the {@link com.l7tech.common.security.TrustedCert#isTrustedForSigningServerCerts()}
     * option set.
     * <p>
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
                if ( selfTrust != null ) {
                    if ( !selfTrust.isTrustedForSsl() ) throw new CertificateException("Server cert '" + subjectDn + "' found but not trusted for SSL" );
                    if ( !selfTrust.getCertificate().equals(serverCertChain[0]) ) throw new CertificateException("Server cert '" + subjectDn + "' found but doesn't match previously stored version" );
                    return;
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

            if ( caTrust == null )
                throw new FindException("Couldn't find CA cert with DN '" + issuerDn + "'" );

            if ( !caTrust.isTrustedForSigningServerCerts() )
                throw new CertificateException("CA Cert with DN '" + issuerDn + "' found but not trusted for signing SSL Server Certs");

            if ( serverCertChain.length < 2 ) {
                // TODO this might conceivably be normal
                throw new CertificateException("Couldn't find CA Cert in chain");
            } else if ( serverCertChain.length > 2 ) {
                // TODO support more than two levels?
                throw new CertificateException("Certificate chains with more than two levels are not supported");
            }

            X509Certificate caCert = serverCertChain[1];
            X509Certificate caTrustCert = caTrust.getCertificate();

            if ( !caCert.equals(caTrustCert) )
                throw new CertificateException("CA cert from server didn't match stored version");

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

    public TrustedCert getCachedCertBySubjectDn( String dn, int maxAge ) throws FindException, IOException, CertificateException {
        Sync read = cacheLock.readLock();
        Sync write = cacheLock.writeLock();
        try {
            read.acquire();
            final Long oid = (Long)dnToOid.get(dn);
            read.release(); read = null;
            if (oid == null) {
                TrustedCert cert = findBySubjectDn(dn);
                if (cert == null) return null;
                write.acquire();
                checkAndCache(cert);
                write.release(); write = null;
                return cert;
            } else {
                return getCachedCertByOid(oid.longValue(), maxAge);
            }
        } catch (InterruptedException e) {
            logger.log( Level.SEVERE, "Interrupted while acquiring cache lock", e );
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (write != null) write.release();
            if (read != null) read.release();
        }
    }

    public TrustedCert getCachedCertByOid( long o, int maxAge ) throws FindException, IOException, CertificateException {
        Long oid = new Long(o);
        TrustedCert cert;

        Sync read = cacheLock.readLock();
        Sync write = cacheLock.writeLock();
        CacheInfo cacheInfo = null;
        try {
            read.acquire();
            cacheInfo = (CacheInfo)cache.get(oid);
            read.release(); read = null;
            if (cacheInfo == null) {
                // Might be new, or might be first run
                cert = findByPrimaryKey(o);
                if (cert == null) return null; // Doesn't exist

                // New
                write.acquire();
                checkAndCache(cert);
                write.release(); write = null;
                return cert;
            }
        } catch ( InterruptedException e ) {
            logger.log( Level.SEVERE, "Interrupted while acquiring cache lock", e );
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (read != null) read.release();
            if (write != null) write.release();
        }

        try {
            if ( cacheInfo.timestamp + maxAge < System.currentTimeMillis() ) {
                // Time for a version check (getVersion() always goes to the database)
                Integer currentVersion = getVersion(o);
                if (currentVersion == null) {
                    // BALEETED
                    write.acquire();
                    cacheRemove(cacheInfo.trustedCert);
                    cacheInfo = null;
                    write.release(); write = null;
                    return null;
                } else if (currentVersion.intValue() != cacheInfo.version) {
                    // Updated
                    cert = findByPrimaryKey(o);
                    write.acquire();
                    cacheInfo = checkAndCache(cert);
                    write.release(); write = null;
                }
            }

            return cacheInfo.trustedCert;
        } catch (InterruptedException e) {
            logger.log( Level.SEVERE, "Interrupted while acquiring cache lock", e );
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (read != null) read.release();
            if (write != null) write.release();
        }
    }

    private static class CacheInfo {
        private TrustedCert trustedCert;
        private long timestamp;
        private int version;
    }

    private void cacheRemove(TrustedCert cert) {
        final Long oid = new Long(cert.getOid());
        cache.remove(oid);
        dnToOid.remove(cert.getSubjectDn());
    }

    private CacheInfo checkAndCache( TrustedCert cert ) throws IOException, CertificateException {
        final Long oid = new Long(cert.getOid());
        check(cert);

        CacheInfo info = (CacheInfo)cache.get(oid);
        if (info == null) {
            info = new CacheInfo();
            cache.put(oid, info);
        }

        info.trustedCert = cert;
        info.version = cert.getVersion();
        info.timestamp = System.currentTimeMillis();

        dnToOid.put(cert.getSubjectDn(), oid);

        return info;
    }

    public void check( TrustedCert cert ) throws CertificateException, IOException {
        CertificateExpiry exp = CertUtils.checkValidity(cert.getCertificate());
        if (exp.getDays() <= CertificateExpiry.FINE_DAYS) logWillExpire( cert, exp );
    }

    public void logInvalidCert( TrustedCert cert, Exception e ) {
        final String msg = "Trusted cert for " + cert.getSubjectDn() + " is invalid or corrupted.";
        if ( e == null ) {
            logger.log( Level.SEVERE, msg);
        } else {
            logger.log( Level.SEVERE, msg, e );
        }
    }

    public void logWillExpire( TrustedCert cert, CertificateExpiry e ) {
        final String msg = "Trusted cert for " + cert.getSubjectDn() +
                           " will expire in approximately " + e.getDays() + " days.";
        logger.log( e.getSeverity(), msg );
    }

    private Map dnToOid = new HashMap();
    private ReadWriteLock cacheLock = new ReaderPreferenceReadWriteLock();
    private Map cache = new HashMap();
}
