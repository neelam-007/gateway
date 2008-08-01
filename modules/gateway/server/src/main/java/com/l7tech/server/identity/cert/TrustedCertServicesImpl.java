package com.l7tech.server.identity.cert;

import com.l7tech.common.io.CertUtils;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import org.springframework.transaction.annotation.Transactional;

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

    private final TrustedCertManager trustedCertManager;

    public TrustedCertServicesImpl(TrustedCertManager trustedCertManager) {
        this.trustedCertManager = trustedCertManager;
    }

    @Transactional(readOnly=true)
    public void checkSslTrust(X509Certificate[] serverCertChain) throws CertificateException {
        String issuerDn = serverCertChain[0].getIssuerDN().getName();
        try {
            // Check if this cert is trusted as-is
            if (isTrustedAsIs(serverCertChain))
                return;

            // Check if this chain has a trusted signer
            checkIssuerIsTrusted(serverCertChain, issuerDn);

        } catch (Exception e) {
            if (e instanceof TrustedCertManager.UnknownCertificateException)
                throw (CertificateException) e;

            logger.log(Level.WARNING, e.getMessage(), e);

            throw new CertificateException(e.getMessage(), e);
        }
    }

    public Collection<TrustedCert> getCertsBySubjectDnFiltered(String subjectDn, boolean omitExpired, Set<TrustedCert.TrustedFor> requiredTrustFlags, Set<Long> requiredOids) throws FindException {
        Collection<TrustedCert> trustedsWithDn = trustedCertManager.getCachedCertsBySubjectDn(subjectDn);
        List<TrustedCert> ret = new ArrayList<TrustedCert>();
        for (TrustedCert trusted : trustedsWithDn) {
            try {
                if (omitExpired && !CertUtils.isValid(trusted.getCertificate()))
                    continue;
                if (requiredTrustFlags != null && !trusted.isTrustedForAll(requiredTrustFlags))
                    continue;
                if (requiredOids != null && !requiredOids.contains(trusted.getOid()))
                    continue;
                ret.add(trusted);
            } catch (CertificateException e) {
                // Periodic check will eventually audit a warning about this corrupt cert
            }
        }
        return ret;
    }

    private void checkIssuerIsTrusted(X509Certificate[] serverCertChain, String issuerDn) throws FindException, CertificateException, NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException {
        Collection<TrustedCert> caTrusts = getCertsBySubjectDnFiltered(issuerDn, true, EnumSet.of(TrustedCert.TrustedFor.SIGNING_SERVER_CERTS), null);

        if (caTrusts.isEmpty())
            throw new TrustedCertManager.UnknownCertificateException("Couldn't find CA cert with DN '" + issuerDn + "'");

        for (TrustedCert caTrust : caTrusts) {
            X509Certificate caTrustCert = caTrust.getCertificate();
            if (CertUtils.isVerified(serverCertChain[0], caTrustCert.getPublicKey()))
                return;
        }

        throw new CertificateException("CA Cert(s) with DN '" + issuerDn + "' found but not trusted for signing SSL Server Certs");
    }

    private boolean isTrustedAsIs(X509Certificate[] serverCertChain) throws CertificateException {
        try {
            String subjectDn = serverCertChain[0].getSubjectDN().getName();
            Collection<TrustedCert> selfTrusts = getCertsBySubjectDnFiltered(subjectDn, true, EnumSet.of(TrustedCert.TrustedFor.SSL), null);
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
