/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.uddi;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class UDDIBusinessService implements Serializable {

    public UDDIBusinessService(String serviceName,
                               String serviceKey,
                               String wsdlServiceName,
                               String wsdlServiceNamespace,
                               Map<String, Set<String>> allBindingsAndTModelKeys) {

        if(serviceName == null || serviceName.trim().isEmpty()) throw new IllegalArgumentException("serviceName cannot be null or empty");
        if(serviceKey == null || serviceKey.trim().isEmpty()) throw new IllegalArgumentException("serviceKey cannot be null or empty");
        if(wsdlServiceName == null || wsdlServiceName.trim().isEmpty()) throw new IllegalArgumentException("wsdlServiceName cannot be null or empty");
        if(wsdlServiceNamespace != null && wsdlServiceNamespace.trim().isEmpty()) throw new IllegalArgumentException("wsdlServiceNameSpace cannot be empty if it is not null");
        if(allBindingsAndTModelKeys != null && allBindingsAndTModelKeys.isEmpty()) throw new IllegalArgumentException("allBindingsAndTModelKeys cannot be null or empty.");
        
        this.serviceName = serviceName;
        this.serviceKey = serviceKey;
        this.wsdlServiceName = wsdlServiceName;
        this.wsdlServiceNamespace = wsdlServiceNamespace;
        this.allBindingsAndTModelKeys = allBindingsAndTModelKeys;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public String getWsdlServiceName() {
        return wsdlServiceName;
    }

    public String getWsdlServiceNamespace() {
        return wsdlServiceNamespace;
    }

    public Map<String, Set<String>> getAllBindingsAndTModelKeys() {
        return allBindingsAndTModelKeys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UDDIBusinessService that = (UDDIBusinessService) o;

        if (!allBindingsAndTModelKeys.equals(that.allBindingsAndTModelKeys)) return false;
        if (!serviceKey.equals(that.serviceKey)) return false;
        if (!serviceName.equals(that.serviceName)) return false;
        if (!wsdlServiceName.equals(that.wsdlServiceName)) return false;
        if (!wsdlServiceNamespace.equals(that.wsdlServiceNamespace)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = serviceName.hashCode();
        result = 31 * result + serviceKey.hashCode();
        result = 31 * result + wsdlServiceName.hashCode();
        result = 31 * result + wsdlServiceNamespace.hashCode();
        result = 31 * result + allBindingsAndTModelKeys.hashCode();
        return result;
    }

    //- PRIVATE

    //assuming in lots of places that we only deal with one name
    final private String serviceName;
    final private String serviceKey;
    final private String wsdlServiceName;
    final private String wsdlServiceNamespace;
    /**
     * The bindingTemplateKey on the lhs and the Set of tModelKeys that bindingTemplate references
     */
    final private Map<String, Set<String>> allBindingsAndTModelKeys;
}
