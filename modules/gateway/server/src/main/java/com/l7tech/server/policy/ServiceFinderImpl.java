package com.l7tech.server.policy;

import com.l7tech.policy.assertion.ext.CertificateFinder;
import com.l7tech.policy.assertion.ext.SecurePasswordServices;
import com.l7tech.policy.assertion.ext.ServiceFinder;
import com.l7tech.policy.assertion.ext.VariableServices;



public class ServiceFinderImpl implements ServiceFinder {
    private final CertificateFinderImpl certificateFinder;
    private final VariableServicesImpl variableServices;
    private final SecurePasswordServicesImpl securePasswordServices;

    public ServiceFinderImpl(CertificateFinderImpl certificateFinder,
                             VariableServicesImpl variableServices,
                             SecurePasswordServicesImpl securePasswordServices) {
        this.certificateFinder = certificateFinder;
        this.variableServices = variableServices;
        this.securePasswordServices = securePasswordServices;
    }

    public Object lookupService(Class serviceInterface) {
        if (CertificateFinder.class.equals(serviceInterface)) {
            return certificateFinder;
        }
        if (VariableServices.class.equals(serviceInterface)) {
            return variableServices;
        }
        if (SecurePasswordServices.class.equals(serviceInterface)) {
            return securePasswordServices;
        }
        return null;
    }

}
