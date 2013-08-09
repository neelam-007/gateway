package com.l7tech.server.identity.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.CertVerifier;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gateway implementation of {@link TrustedCertServices}.
 */
public class TrustedCertServicesImpl implements TrustedCertServices {
    private static final Logger logger = Logger.getLogger(TrustedCertServicesImpl.class.getName());

    private final TrustedCertCache trustedCertCache;

    public TrustedCertServicesImpl( final TrustedCertCache trustedCertCache ) {
        this.trustedCertCache = trustedCertCache;
    }

    @Override
    public void checkSslTrust(X509Certificate[] serverCertChain, Set<Goid> trustedCertOids) throws CertificateException {
        String issuerDn = CertUtils.getIssuerDN(serverCertChain[0]);
        try {
            // Check if this cert is trusted as-is
            if (isTrustedAsIs(serverCertChain, trustedCertOids))
                return;

            // Check if this chain has a trusted signer
            checkIssuerIsTrusted(serverCertChain, issuerDn, trustedCertOids);

        } catch (Exception e) {
            if (e instanceof CertificateException) throw (CertificateException) e;

            logger.log(Level.WARNING, e.getMessage(), e);

            throw new CertificateException(e.getMessage(), e);
        }
    }

    @Override
    public Collection<TrustedCert> getCertsBySubjectDnFiltered(String subjectDn, boolean omitExpired, Set<TrustedCert.TrustedFor> requiredTrustFlags, Set<Goid> requiredOids) throws FindException {
        Collection<TrustedCert> trustedsWithDn = trustedCertCache.findBySubjectDn(subjectDn);
        List<TrustedCert> ret = new ArrayList<TrustedCert>();
        for (TrustedCert trusted : trustedsWithDn) {
            if (omitExpired && !CertUtils.isValid(trusted.getCertificate()))
                continue;
            if (requiredTrustFlags != null && !trusted.isTrustedForAll(requiredTrustFlags))
                continue;
            if (requiredOids != null && !requiredOids.contains(trusted.getGoid()))
                continue;
            trusted.setReadOnly();
            ret.add(trusted);
        }
        return ret;
    }

    @Override
    public Collection<TrustedCert> getAllCertsByTrustFlags(Set<TrustedCert.TrustedFor> requiredTrustFlags) throws FindException {
        Set<TrustedCert> ret = new HashSet<TrustedCert>();
        for (TrustedCert.TrustedFor trustFlag : requiredTrustFlags) {
            ret.addAll(trustedCertCache.findByTrustFlag(trustFlag));
        }
        return Collections.unmodifiableCollection(ret);
    }

    private void checkIssuerIsTrusted(X509Certificate[] serverCertChain, String issuerDn, Set<Goid> trustedCertOids) throws FindException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Collection<TrustedCert> caTrusts = getCertsBySubjectDnFiltered(issuerDn, true, EnumSet.of(TrustedCert.TrustedFor.SIGNING_SERVER_CERTS), trustedCertOids);

        if (caTrusts.isEmpty()) {
            String subjectDn = CertUtils.getSubjectDN(serverCertChain[0]);
            if ( trustedCertCache.findBySubjectDn(subjectDn).isEmpty() ) {
                throw new TrustedCertManager.UnknownCertificateException("Couldn't find CA cert with DN '" + issuerDn + "'");
            } else {
                throw new CertificateException("Server cert '" + subjectDn + "' found but not trusted for SSL.");
            }
        }

        for (TrustedCert caTrust : caTrusts) {
            X509Certificate caTrustCert = caTrust.getCertificate();
            if (CertVerifier.isVerified(serverCertChain[0], caTrustCert))
                return;
        }

        throw new CertificateException("CA Cert(s) with DN '" + issuerDn + "' found but not trusted for signing SSL Server Certs");
    }

    private boolean isTrustedAsIs(X509Certificate[] serverCertChain, Set<Goid> trustedCertOids) throws CertificateException {
        try {
            String subjectDn = CertUtils.getSubjectDN(serverCertChain[0]);
            Collection<TrustedCert> selfTrusts = getCertsBySubjectDnFiltered(subjectDn, true, EnumSet.of(TrustedCert.TrustedFor.SSL), trustedCertOids);
            for (TrustedCert selfTrust : selfTrusts) {
                final X509Certificate selfTrustCert = selfTrust.getCertificate();
                if (CertUtils.certsAreEqual(selfTrustCert, serverCertChain[0]) && CertUtils.isValid(selfTrustCert))
                        return true;
            }
            logger.fine("Server cert '" + subjectDn + "' found but not trusted for SSL. Will check issuer cert, if any");
            return false;
        } catch (FindException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            throw new CertificateException(e.getMessage());
        }
    }
}
