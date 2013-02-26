package com.l7tech.server.policy;

import com.l7tech.policy.assertion.ext.CertificateFinder;
import com.l7tech.policy.assertion.ext.ServiceFinder;
import com.l7tech.policy.assertion.ext.VariableServices;



public class ServiceFinderImpl implements ServiceFinder {
    private final CertificateFinderImpl certificateFinder;
    private final VariableServicesImpl variableServices;

    public ServiceFinderImpl(CertificateFinderImpl certificateFinder, VariableServicesImpl variableServices) {
        this.certificateFinder = certificateFinder;
        this.variableServices = variableServices;
    }

    public Object lookupService(Class serviceInterface) {
        if (CertificateFinder.class.equals(serviceInterface)) {
            return certificateFinder;
        }
        if (VariableServices.class.equals(serviceInterface)) {
            return variableServices;
        }
        return null;
    }

}
