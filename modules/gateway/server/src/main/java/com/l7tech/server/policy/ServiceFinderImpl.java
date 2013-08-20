package com.l7tech.server.policy;

import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.password.SecurePasswordServices;
import com.l7tech.policy.assertion.ext.store.KeyValueStoreServices;
import com.l7tech.server.store.KeyValueStoreServicesImpl;


public class ServiceFinderImpl implements ServiceFinder {
    private CertificateFinderImpl certificateFinder;
    private VariableServicesImpl variableServices;
    private SecurePasswordServicesImpl securePasswordServices;
    private KeyValueStoreServicesImpl keyValueStoreServices;

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

    public void setKeyValueStoreImpl(KeyValueStoreServicesImpl keyValueStoreServices) {
        this.keyValueStoreServices = keyValueStoreServices;
    }

    public <T> T lookupService(Class<T> serviceInterface) {
        if (CertificateFinder.class.equals(serviceInterface)) {
            return (T) certificateFinder;
        }
        if (VariableServices.class.equals(serviceInterface)) {
            return (T) variableServices;
        }
        if (SecurePasswordServices.class.equals(serviceInterface)) {
            return (T) securePasswordServices;
        }
        if (KeyValueStoreServices.class.equals(serviceInterface)) {
            return (T) keyValueStoreServices;
        }
        return null;
    }

}
