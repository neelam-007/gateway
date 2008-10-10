package com.l7tech.server.policy;

import com.l7tech.policy.assertion.ext.CertificateFinder;
import com.l7tech.policy.assertion.ext.ServiceFinder;


public class ServiceFinderImpl implements ServiceFinder {
    private final CertificateFinderImpl certificateFinder;

    public ServiceFinderImpl(CertificateFinderImpl certificateFinder) {
        this.certificateFinder = certificateFinder;
    }

    public Object lookupService(Class serviceInterface) {
        if (CertificateFinder.class.equals(serviceInterface)) {
            return certificateFinder;
        }
        return null;
    }
}
