package com.l7tech.server.policy;

import com.l7tech.policy.assertion.ext.CertificateFinder;
import com.l7tech.policy.assertion.ext.SecurePasswordServices;
import com.l7tech.policy.assertion.ext.ServiceFinder;
import com.l7tech.policy.assertion.ext.VariableServices;



public class ServiceFinderImpl implements ServiceFinder {
    private CertificateFinderImpl certificateFinder;
    private VariableServicesImpl variableServices;
    private SecurePasswordServicesImpl securePasswordServices;

    public ServiceFinderImpl() {
    }

    public void setCertificateFinderImpl(CertificateFinderImpl certificateFinder) {
        this.certificateFinder = certificateFinder;
    }

    public void setVariableServicesImpl(VariableServicesImpl variableServices) {
        this.variableServices = variableServices;
    }

    public void setSecurePasswordServicesImpl(SecurePasswordServicesImpl securePasswordServices) {
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
