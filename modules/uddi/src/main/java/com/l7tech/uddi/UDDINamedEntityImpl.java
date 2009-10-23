package com.l7tech.uddi;

import java.io.Serializable;

/**
 * Default implementation of UDDINamedEntity
 *
 * @author Steve Jones
*/
class UDDINamedEntityImpl implements UDDINamedEntity, Serializable {

    //- PUBLIC

    public UDDINamedEntityImpl(String key, String name) {
        this(key, name, null, null);
    }

    public UDDINamedEntityImpl(String key, String name, String policyUrl, String wsdlUrl) {
        this.key = key;
        this.name = name;
        this.policyUrl = policyUrl;
        this.wsdlUrl = wsdlUrl;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPolicyUrl() {
        return policyUrl;
    }

    @Override
    public String getWsdlUrl() {
        return wsdlUrl;
    }

    //- PRIVATE

    private final String key;
    private final String name;
    private final String wsdlUrl;
    private final String policyUrl;    
}
