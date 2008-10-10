package com.l7tech.server.policy;

import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.ext.CertificateFinder;
import com.l7tech.policy.assertion.ext.ServiceException;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;


public class CertificateFinderImpl implements CertificateFinder {

    private final TrustedCertManager trustedCertManager;

    public CertificateFinderImpl(TrustedCertManager trustedCertManager) {
        this.trustedCertManager = trustedCertManager;
    }

    public X509Certificate[] findByUsageOption(String usageOption) throws ServiceException, IllegalArgumentException {
        try {
            Collection<TrustedCert> trustedCerts = trustedCertManager.findAll();
            if (usageOption.equalsIgnoreCase(CertificateFinder.TRUSTED_FOR_OUTBOUND_SSL)) {
                return findTrustedForSsl(trustedCerts);
            } else if (usageOption.equalsIgnoreCase(CertificateFinder.TRUSTED_FOR_OUTBOUND_SSL_CA)) {
                return findTrustedForSigningServerCerts(trustedCerts);
            } else if (usageOption.equalsIgnoreCase(CertificateFinder.TRUSTED_FOR_CLIENT_CERT_CA)) {
                return findTrustedForSigningClientCerts(trustedCerts);
            } else if (usageOption.equalsIgnoreCase(CertificateFinder.TRUSTED_FOR_SAML_ISSUER)) {
                return findTrustedAsSamlIssuer(trustedCerts);
            } else if (usageOption.equalsIgnoreCase(CertificateFinder.TRUSTED_FOR_SAML_ATTESTING_ENTITY)) {
                return findTrustedAsSamlAttestingEntity(trustedCerts);
            } else {
                throw new IllegalArgumentException("The usage option: " + usageOption + " is not valid.");
            }
        } catch (FindException fe) {
            throw new ServiceException(fe);
        } catch (CertificateException ce) {
            throw new ServiceException(ce);
        }
    }

    private X509Certificate[] findTrustedForSsl(Collection<TrustedCert> trustedCerts) throws FindException, CertificateException {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        for (TrustedCert trustedCert : trustedCerts) {
            if (trustedCert.isTrustedForSsl()) {
                certificates.add(trustedCert.getCertificate());
            }
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }

    private X509Certificate[] findTrustedForSigningServerCerts(Collection<TrustedCert> trustedCerts) throws FindException, CertificateException {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        for (TrustedCert trustedCert : trustedCerts) {
            if (trustedCert.isTrustedForSigningServerCerts()) {
                certificates.add(trustedCert.getCertificate());
            }
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }

    private X509Certificate[] findTrustedForSigningClientCerts(Collection<TrustedCert> trustedCerts) throws FindException, CertificateException {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        for (TrustedCert trustedCert : trustedCerts) {
            if (trustedCert.isTrustedForSigningClientCerts()) {
                certificates.add(trustedCert.getCertificate());
            }
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }

    private X509Certificate[] findTrustedAsSamlIssuer(Collection<TrustedCert> trustedCerts) throws FindException, CertificateException {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        for (TrustedCert trustedCert : trustedCerts) {
            if (trustedCert.isTrustedAsSamlIssuer()) {
                certificates.add(trustedCert.getCertificate());
            }
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }

    private X509Certificate[] findTrustedAsSamlAttestingEntity(Collection<TrustedCert> trustedCerts) throws FindException, CertificateException {
        ArrayList<X509Certificate> certificates = new ArrayList<X509Certificate>();
        for (TrustedCert trustedCert : trustedCerts) {
            if (trustedCert.isTrustedAsSamlAttestingEntity()) {
                certificates.add(trustedCert.getCertificate());
            }
        }
        return certificates.toArray(new X509Certificate[certificates.size()]);
    }
}
